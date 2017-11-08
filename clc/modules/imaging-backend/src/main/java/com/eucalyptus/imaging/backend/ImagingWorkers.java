/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.imaging.backend;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.internal.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.imaging.ImagingServiceProperties;
import com.eucalyptus.imaging.common.ImagingBackend;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park
 *
 */
public class ImagingWorkers {
  private static Logger LOG = Logger.getLogger( ImagingWorkers.class );
  public static final int WORKER_TIMEOUT_MIN = 10;
  private static Set<String> verifiedWorkers = new HashSet<String>();
  public static class ImagingWorkerStateManager implements EventListener<ClockTick> {
    public static void register( ) {
      Listeners.register( ClockTick.class, new ImagingWorkerStateManager() );
    }

    @Override
    public void fireEvent(ClockTick event) {
      if (!( Bootstrap.isOperational( ) &&
          Topology.isEnabledLocally( ImagingBackend.class ) &&
          Topology.isEnabled( Compute.class) ) )
        return;
      if(!ImagingServiceProperties.HEALTHCHECK)
        return;
      /// if there's a worker that has not reported for the last {WORKER_TIMEOUT_MIN},
      /// reschedule the task assigned to the worker and terminate the instance
      try{
        final List<ImagingWorker> workers = listWorkers();
        final List<ImagingWorker> timedout = Lists.newArrayList();
        final List<ImagingWorker> retiring = Lists.newArrayList();
        final List<ImagingWorker> toRemove = Lists.newArrayList();
        for(final ImagingWorker worker : workers){
          if(isTimedOut(worker))
            timedout.add(worker);
          if(ImagingWorker.STATE.RETIRING.equals(worker.getState()))
            retiring.add(worker);
          if(ImagingWorker.STATE.DECOMMISSIONED.equals(worker.getState()) && timeToRemove(worker) <= 0)
            toRemove.add(worker);
        }
        
        for(final ImagingWorker worker : timedout){
          LOG.info(String.format("Imaging service worker %s is not responding and might be "
              + "decommissioned in about %d minutes.", worker.getDisplayName(), timeToRemove(worker)));
          retireWorker(worker.getDisplayName());
        }
        
        for(final ImagingWorker worker : retiring){
          final ImagingTask task = ImagingTasks.getConvertingTaskByWorkerId(worker.getDisplayName());
          if(task!=null && ImportTaskState.CONVERTING.equals(task.getState())) {
            ImagingTasks.killAndRerunTask(task.getDisplayName());
            LOG.debug(String.format("Imaging worker task %s is moved back into queue", task.getDisplayName()));
          }
          decommisionWorker(worker.getDisplayName());
        }
        
        for(final ImagingWorker worker : toRemove){
          LOG.debug("Forgetting about imaging worker "+worker.getDisplayName());
          removeWorker(worker.getDisplayName());
        }
      }catch(final Exception ex){
        LOG.error("Failed to check imaging worker's state", ex);
      }
    }
  }

  private static long MINUTE=1000*60l;
  private static long timeToRemove(final ImagingWorker worker) {
    final Date lastUpdated = worker.getWorkerUpdateTime();
    Calendar cal = Calendar.getInstance(); // creates calendar
    cal.setTime(lastUpdated); // sets calendar time/date
    cal.add(Calendar.MINUTE, 60); // remove records after 1 hour
    final Date expirationTime = cal.getTime();

    return Math.abs((expirationTime.getTime() - new Date().getTime())/MINUTE);
  }
  
  private static boolean isTimedOut(final ImagingWorker worker){
    final Date lastUpdated = worker.getWorkerUpdateTime();
    Calendar cal = Calendar.getInstance(); // creates calendar
    cal.setTime(lastUpdated); // sets calendar time/date
    cal.add(Calendar.MINUTE, WORKER_TIMEOUT_MIN); // adds 5 minutes
    final Date expirationTime = cal.getTime(); //

    return expirationTime.before(new Date());
  }

  public static boolean hasWorker(final String workerId){
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingWorker.class ) ) {
      try{
      final ImagingWorker entity = Entities.uniqueResult(ImagingWorker.named(workerId));
      }catch(final Exception ex){
        return false;
      }
      return true;
    }
  }
  
  public static boolean canAllocate(final String workerId) {
    final ImagingWorker worker = getWorker(workerId);
    if(worker==null)
      return false;
    return ImagingWorker.STATE.RUNNING.equals(worker.getState());
  }
  
  public static ImagingWorker getWorker(final String workerId){
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingWorker.class ) ) {
      try{
        final ImagingWorker entity = Entities.uniqueResult(ImagingWorker.named(workerId));
        return entity;
      }catch(final Exception ex){
        return null;
      }
    }
  }
  
  public static List<ImagingWorker> listWorkers(){
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingWorker.class ) ) {
      try{
        final List<ImagingWorker> workers = Entities.query(ImagingWorker.named());
        return workers;
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  private static final String DEFAULT_LAUNCHER_TAG = "euca-internal-imaging-workers";

  public static void verifyWorker(final String instanceId, final String remoteHost) throws ImagingWorkerVerifyException {
    try {
      ResourceIdentifiers.parse( "i", instanceId );
    } catch ( InvalidResourceIdentifier e ) {
      throw new ImagingWorkerVerifyException( "Failed to verify imaging worker "+remoteHost+". The '" + instanceId + "' can't be an instance ID" );
    }
    if(!verifiedWorkers.contains(instanceId)){
      try{
        final List<RunningInstancesItemType> instances=
            Ec2Client.getInstance().describeInstances(
                Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
                Lists.newArrayList(instanceId));
        final RunningInstancesItemType workerInstance = instances.get(0);
        boolean tagFound = false;
        for(final ResourceTag tag : workerInstance.getTagSet()){
          if(DEFAULT_LAUNCHER_TAG.equals(tag.getValue())){
            tagFound = true;
            break;
          } 
        }
        if(!tagFound)
          throw new ImagingWorkerVerifyException("Instance does not have a proper tag");
        if(! (remoteHost.equals(workerInstance.getIpAddress()) || remoteHost.equals(workerInstance.getPrivateIpAddress())))
          throw new ImagingWorkerVerifyException("Request came from invalid host address: "+remoteHost);        
        verifiedWorkers.add(instanceId);
      } catch( final ImagingWorkerVerifyException e ) {
        throw e;
      } catch( final Exception ex ){
        final EucalyptusActivityException eae = Exceptions.findCause( ex, EucalyptusActivityException.class );
        if ( eae != null ) {
          throw new ImagingWorkerVerifyException( "Failed to verify imaging worker. " + eae.getMessage( ) );
        }
        throw new ImagingWorkerVerifyException("Failed to verify imaging worker", ex);
      }
    }
  }
  
  public static ImagingWorker createWorker(final String workerId){
    String availabilityZone = null;
    try{
      final List<RunningInstancesItemType> instances =
          Ec2Client.getInstance().describeInstances(
              Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
              Lists.newArrayList(workerId));
      availabilityZone = instances.get(0).getPlacement();
    }catch(final Exception ex){
      throw Exceptions.toUndeclared("Unable to find the instance named: "+workerId);
    }
    
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingWorker.class ) ) {
      try{
        final ImagingWorker entity = Entities.uniqueResult(ImagingWorker.named(workerId));
        throw Exceptions.toUndeclared(new Exception("Worker already exists"));
      }catch(final NoSuchElementException ex){
        final ImagingWorker worker = new ImagingWorker(ImagingWorker.STATE.RUNNING, workerId);
        worker.setWorkerUpdateTime();
        worker.setAvailabilityZone(availabilityZone);
        Entities.persist(worker);
        db.commit();
        return worker;
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    } 
  }
  
  public static void removeWorker(final String workerId){
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingWorker.class ) ) {
      try{
        final ImagingWorker entity = Entities.uniqueResult(ImagingWorker.named(workerId));
        Entities.delete(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public static void markUpdate(final String workerId){
    // address workers in TIMEOUT state
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingWorker.class ) ) {
      try{
        final ImagingWorker entity = Entities.uniqueResult(ImagingWorker.named(workerId));
        entity.setWorkerUpdateTime();
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  private static void setWorkerState(final String workerId, ImagingWorker.STATE state){
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingWorker.class ) ) {
      try{
        final ImagingWorker entity = Entities.uniqueResult(ImagingWorker.named(workerId));
        entity.setState(state);
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public static void retireWorker(final String workerId) {
    // check if system knows about instance
    List<RunningInstancesItemType> instances = null;
    try {
      instances = Ec2Client.getInstance().describeInstances(
          Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
          Lists.newArrayList(workerId));
    } catch(final Exception ex) {
      LOG.error("Can't list instances", ex);
    }
    if (instances != null && instances.size() == 1) {
      setWorkerState(workerId, ImagingWorker.STATE.RETIRING);
    } else {
      LOG.debug("Forgetting about imaging worker " + workerId);
      removeWorker(workerId);
    }
  }
  
  private static void decommisionWorker(final String workerId){
    // terminate instance
    // set worker state DECOMMISSIONED
    String instanceId = null;
    try{
      final List<RunningInstancesItemType> instances = 
          Ec2Client.getInstance().describeInstances(
              Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
              Lists.newArrayList(workerId));
      if(instances!=null && instances.size()==1)
        instanceId = instances.get(0).getInstanceId();
    }catch(final Exception ex){
      LOG.error("Can't list instances", ex);
    }
    if(instanceId!=null){
      try{
        Ec2Client.getInstance().terminateInstances(
            Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
            Lists.newArrayList(workerId));
        LOG.debug("Terminated imaging worker: " + workerId);
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex); 
      }
    }
    setWorkerState(workerId, ImagingWorker.STATE.DECOMMISSIONED);
  }
  
  private static Set<String> FatalTaskErrors = Sets.newHashSet("FailureToAttachVolume", "FailureToDetachVolume",
      "CertificateFailure");
  public static boolean isFatalError(final String errorCode){
      return FatalTaskErrors.contains(errorCode);
  }

  public static class ImagingWorkerVerifyException extends Exception {
    private static final long serialVersionUID = 1L;

    public ImagingWorkerVerifyException( final String message ) {
      super( message );
    }

    public ImagingWorkerVerifyException( final String message, final Throwable cause ) {
      super( message, cause );
    }
  }
}
