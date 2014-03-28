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
package com.eucalyptus.imaging;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.imaging.worker.EucalyptusActivityTasks;
import com.eucalyptus.imaging.worker.ImagingServiceProperties;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;

/**
 * @author Sang-Min Park
 *
 */
public class ImagingWorkers {
  private static Logger LOG = Logger.getLogger( ImagingWorkers.class );
  public static final int WORKER_TIMEOUT_MIN = 10;
  
  public static class ImagingWorkerStateManager implements EventListener<ClockTick> {
    public static void register( ) {
      Listeners.register( ClockTick.class, new ImagingWorkerStateManager() );
    }

    @Override
    public void fireEvent(ClockTick event) {
      if (!( Bootstrap.isFinished() &&
           Topology.isEnabled( Eucalyptus.class ) ) )
         return;
      if(!ImagingServiceProperties.IMAGING_WORKER_HEALTHCHECK)
        return;
      /// if there's a worker that has not reported for the last {WORKER_TIMEOUT_MIN},
      /// reschedule the task assigned to the worker and terminate the instance
      try{
        final List<ImagingWorker> workers = listWorkers();
        final List<ImagingWorker> timedout = Lists.newArrayList();
        final List<ImagingWorker> toRemove = Lists.newArrayList();
        for(final ImagingWorker worker : workers){
          if(isTimedOut(worker))
            timedout.add(worker);
          if(ImagingWorker.STATE.DECOMMISSIONED.equals(worker.getState()) && shouldRemove(worker))
            toRemove.add(worker);
        }
        
        for(final ImagingWorker worker : timedout){
          LOG.warn(String.format("Imaging service worker %s is not responding", worker.getDisplayName()));
          final ImagingTask task = ImagingTasks.getConvertingTaskByWorkerId(worker.getDisplayName());
          if(task!=null) {
            ImagingTasks.killAndRerunTask(task.getDisplayName());
            LOG.debug(String.format("Imaging worker task %s is moved back into queue", task.getDisplayName()));
          }
          retireWorker(worker.getDisplayName());
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
    cal.add(Calendar.HOUR, 1); // remove records after 1 hour
    final Date expirationTime = cal.getTime();

    return ImagingWorker.STATE.DECOMMISSIONED.equals(worker.getState()) &&
        expirationTime.before(new Date());
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
        throw Exceptions.toUndeclared(ex);
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
  
  public static void createWorker(final String workerId){
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingWorker.class ) ) {
      try{
        final ImagingWorker entity = Entities.uniqueResult(ImagingWorker.named(workerId));
        throw Exceptions.toUndeclared(new Exception("Worker already exists"));
      }catch(final NoSuchElementException ex){
        final ImagingWorker worker = new ImagingWorker(ImagingWorker.STATE.RUNNING, workerId);
        worker.setWorkerUpdateTime();
        Entities.persist(worker);
        db.commit();
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
  
  private static void retireWorker(final String workerId){
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
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex); 
      }
    }
    setWorkerState(workerId, ImagingWorker.STATE.DECOMMISSIONED);
  }
}
