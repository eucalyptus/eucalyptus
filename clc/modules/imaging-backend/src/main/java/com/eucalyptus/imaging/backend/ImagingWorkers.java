/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.imaging.backend;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.imaging.common.EucalyptusActivityTasks;
import com.eucalyptus.imaging.common.Imaging;
import com.eucalyptus.imaging.ImagingServiceProperties;
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
      if (!( Bootstrap.isFinished() &&
          Topology.isEnabledLocally( Imaging.class ) ) )
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
          if(ImagingWorker.STATE.DECOMMISSIONED.equals(worker.getState()) && shouldRemove(worker))
            toRemove.add(worker);
        }
        
        for(final ImagingWorker worker : timedout){
          LOG.warn(String.format("Imaging service worker %s is not responding", worker.getDisplayName()));
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
  
  private static boolean shouldRemove(final ImagingWorker worker) {
    final Date lastUpdated = worker.getWorkerUpdateTime();
    Calendar cal = Calendar.getInstance(); // creates calendar
    cal.setTime(lastUpdated); // sets calendar time/date
    cal.add(Calendar.MINUTE, 60); // remove records after 1 hour
    final Date expirationTime = cal.getTime();

    return expirationTime.before(new Date());
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
  
  public static void verifyWorker(final String instanceId, final String remoteHost) throws Exception{
    if(!verifiedWorkers.contains(instanceId)){
      try{
        final List<RunningInstancesItemType> instances=
            EucalyptusActivityTasks.getInstance().describeSystemInstances(Lists.newArrayList(instanceId));
        final RunningInstancesItemType workerInstance = instances.get(0);
        boolean tagFound = false;
        for(final ResourceTag tag : workerInstance.getTagSet()){
          if(ImagingServiceProperties.DEFAULT_LAUNCHER_TAG.equals(tag.getValue())){
            tagFound = true;
            break;
          } 
        }
        if(!tagFound)
          throw new Exception("Instance does not have a proper tag");
        if(! (remoteHost.equals(workerInstance.getIpAddress()) || remoteHost.equals(workerInstance.getPrivateIpAddress())))
          throw new Exception("Request came from invalid host address: "+remoteHost);        
        verifiedWorkers.add(instanceId);
      }catch(final Exception ex){
        throw new Exception("Failed to verify imaging worker", ex);
      }
    }
  }
  
  public static ImagingWorker createWorker(final String workerId){
    String availabilityZone = null;
    try{
      final List<RunningInstancesItemType> instances =
          EucalyptusActivityTasks.getInstance().describeSystemInstances(Lists.newArrayList(workerId));
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
  
  public static void retireWorker(final String workerId){
    setWorkerState(workerId, ImagingWorker.STATE.RETIRING);
  }
  
  private static void decommisionWorker(final String workerId){
    // terminate instance
    // set worker state DECOMMISSIONED
    String instanceId = null;
    try{
      final List<RunningInstancesItemType> instances = 
          EucalyptusActivityTasks.getInstance().describeSystemInstances(Lists.newArrayList(workerId));
      if(instances!=null && instances.size()==1)
        instanceId = instances.get(0).getInstanceId();
    }catch(final Exception ex){
      ;
    }
    if(instanceId!=null){
      try{
        EucalyptusActivityTasks.getInstance().terminateSystemInstance(workerId);
        LOG.debug("Terminated imaging worker: "+workerId);
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex); 
      }
    }
    setWorkerState(workerId, ImagingWorker.STATE.DECOMMISSIONED);
  }
  
  private static Set<String> FatalTaskErrors = Sets.newHashSet("FailureToAttachVolume", "FailureToDetachVolume");
  public static boolean isFatalError(final String errorCode){
      return FatalTaskErrors.contains(errorCode);
  }
}
