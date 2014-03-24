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

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.imaging.AbstractTaskScheduler.WorkerTask;
import com.eucalyptus.imaging.worker.ImagingServiceLaunchers;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class ImagingService {
  private static Logger LOG = Logger.getLogger( ImagingService.class );

  public static ImportImageResponseType importImage(ImportImageType request) throws Exception {
    final ImportImageResponseType reply = request.getReply();
    try{
      if (!Bootstrap.isFinished() ||
           !Topology.isEnabled( Imaging.class )){
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "For import, Imaging service should be enabled");
      }
    }catch(final Exception ex){
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "For import, Imaging service should be enabled");
    }
    
    try{
      if(ImagingServiceLaunchers.getInstance().shouldEnable())
        ImagingServiceLaunchers.getInstance().enable();
    }catch(Exception ex){
      LOG.error("Failed to enable imaging service workers");
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "Could not launch imaging service workers");
    }
    
    DiskImagingTask task = null;
    try{
      task = ImagingTasks.createDiskImagingTask(request);
    }catch(final ImagingServiceException ex){
       throw ex;
    }catch(final Exception ex){
      LOG.error("Failed to import image", ex);
      throw new ImagingServiceException("Failed to import image", ex);
    }
    reply.setConversionTask(task.getTask());
    reply.set_return(true);  
 
    return reply; 
  }

  public static DescribeConversionTasksResponseType describeConversionTask(DescribeConversionTasksType request){
    DescribeConversionTasksResponseType reply = request.getReply( );
    Context ctx = Contexts.lookup( );
    boolean verbose = request.getConversionTaskIdSet( ).remove( "verbose" );
    Collection<String> ownerInfo = ( ctx.isAdministrator( ) && verbose )
        ? Collections.<String> emptyList( )
            : Collections.singleton( ctx.getAccount( ).getAccountNumber( ) );
        //TODO: extends for volumes
        final Predicate<? super DiskImagingTask> requestedAndAccessible = RestrictedTypes.filteringFor( DiskImagingTask.class )
            .byId( request.getConversionTaskIdSet( ) )
            .byOwningAccount( ownerInfo )
            .byPrivileges( )
            .buildPredicate( );

        Iterable<DiskImagingTask> tasksToList = ImagingTasks.getDiskImagingTasks(ctx.getUserFullName(), request.getConversionTaskIdSet());
        for ( DiskImagingTask task : Iterables.filter( tasksToList, requestedAndAccessible ) ) {
          DiskImageConversionTask t = (DiskImageConversionTask) task.getTask( );
          reply.getConversionTasks().add( t );
        }
        return reply;  
  }
  
  /**
   * <ol>
   * <li>Persist cancellation request state
   * <li>Submit cancellations for any outstanding import sub-tasks to imaging service
   * </ol>
   */
  public CancelConversionTaskResponseType CancelConversionTask( CancelConversionTaskType request ) throws Exception {
    final CancelConversionTaskResponseType reply = request.getReply( );
    try{
      ImagingTasks.setState(Contexts.lookup().getUserFullName(), request.getConversionTaskId(), 
          ImportTaskState.CANCELLING, null);
      reply.set_return(true);
    }catch(final NoSuchElementException ex){
      throw new ImagingServiceException("No task with id="+request.getConversionTaskId()+" is found");
    }catch(final Exception ex){
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "Failed to cancel conversion task", ex);
    }
    
    return reply;
  }

  public PutInstanceImportTaskStatusResponseType PutInstanceImportTaskStatus( PutInstanceImportTaskStatusType request ) throws EucalyptusCloudException {
    final PutInstanceImportTaskStatusResponseType reply = request.getReply( );
    reply.setCancelled(false);

    try{
      final String taskId = request.getImportTaskId();
      final String volumeId = request.getVolumeId();
      if(taskId==null)
        throw new Exception("Task id is null");
      
      ImagingTask imagingTask = null;
      try{
        imagingTask= ImagingTasks.lookup(taskId);
      }catch(final Exception ex){
        reply.setCancelled(true);
        throw new Exception("imaging task with "+taskId+" is not found");
      }
      final String instanceId = request.getInstanceId();
      if(instanceId!=null){
        ImagingWorkers.markUpdate(request.getInstanceId());
      }
      
      if(ImportTaskState.CONVERTING.equals(imagingTask.getState()) &&
          instanceId.equals(imagingTask.getWorkerId())){
        //EXTANT, FAILED, DONE
        final WorkerTaskState workerState = WorkerTaskState.fromString(request.getStatus());
        if(WorkerTaskState.EXTANT.equals(workerState) || WorkerTaskState.DONE.equals(workerState)){
          if(imagingTask instanceof VolumeImagingTask){
            try{
              final long bytesConverted= request.getBytesConverted();
              if(bytesConverted>0)
                ImagingTasks.updateBytesConverted(taskId, volumeId, bytesConverted);
            }catch(final Exception ex){
              LOG.warn("Failed to update bytes converted("+taskId+")");
            }
          }
        }
        
        switch(workerState){
        case EXTANT:
            ;
          break;

        case DONE:
          if(imagingTask instanceof VolumeImagingTask){
            try{
              ImagingTasks.updateVolumeStatus((VolumeImagingTask)imagingTask, volumeId, ImportTaskState.COMPLETED, null);
            }catch(final Exception ex){
              ImagingTasks.transitState(imagingTask, ImportTaskState.CONVERTING, 
                  ImportTaskState.FAILED, "Failed to update volume's state");
              LOG.error("Failed to update volume's state", ex);
              break;
            }
            try{
              if(ImagingTasks.isConversionDone((VolumeImagingTask)imagingTask)){
                if(imagingTask instanceof ImportInstanceImagingTask){
                  ImagingTasks.transitState(imagingTask, ImportTaskState.CONVERTING, 
                      ImportTaskState.INSTANTIATING, null);
                }else{
                  ImagingTasks.transitState(imagingTask, ImportTaskState.CONVERTING, 
                      ImportTaskState.COMPLETED, null);
                }
              }
            }catch(final Exception ex){
              LOG.error("Failed to update imaging task's state to completed", ex);
            }
          }else if(imagingTask instanceof DiskImagingTask){
            ImagingTasks.transitState(imagingTask, ImportTaskState.CONVERTING, 
                ImportTaskState.COMPLETED, null);
          }
          break;

        case FAILED:
          ImagingTasks.setState(imagingTask, ImportTaskState.FAILED, request.getStatusMessage());
          break;
        }
      }else{ // state other than "CONVERTING" is not valid and worker should stop working
        reply.setCancelled(true);
      }
    }catch(final Exception ex){
      LOG.warn("Failed to update the task's state", ex);
    }
    return reply;
  }

  public GetInstanceImportTaskResponseType GetInstanceImportTask( GetInstanceImportTaskType request ) throws EucalyptusCloudException {
    final GetInstanceImportTaskResponseType reply = request.getReply( );
    try{
      if(request.getInstanceId()!=null){
        if(ImagingWorkers.hasWorker(request.getInstanceId())) {
          ImagingWorkers.markUpdate(request.getInstanceId());
          if(!ImagingWorkers.canAllocate(request.getInstanceId())){
            LOG.warn(String.format("The worker (%s) is marked invalid", request.getInstanceId()));
            return reply;
          }
        }else
          ImagingWorkers.createWorker(request.getInstanceId());

        final ImagingTask prevTask = ImagingTasks.getConvertingTaskByWorkerId(request.getInstanceId());
        if(prevTask!=null){
          /// this should NOT happen; if it does, it's worker script's bug
          ImagingTasks.killAndRerunTask(prevTask.getDisplayName());
          LOG.warn(String.format("A task (%s:%s) is gone missing [BUG in worker script]", 
              prevTask.getDisplayName(), request.getInstanceId()));
        }
      }
      final WorkerTask task = AbstractTaskScheduler.getScheduler().getTask();
      if(task!=null){
        reply.setImportTaskId(task.getImportTaskId());
        reply.setImportTaskType(task.getImportTaskType().toString());
        if(task.getVolumeTask()!=null)
          reply.setVolumeTask(task.getVolumeTask());
        else if(task.getInstanceStoreTask()!=null)
          reply.setInstanceStoreTask(task.getInstanceStoreTask());
        if(request.getInstanceId()!=null){
          ImagingTasks.setWorkerId(task.getImportTaskId(), request.getInstanceId());
        }
      }
    }catch(final Exception ex){
      LOG.error("Failed to schedule a task", ex);
    }
    return reply;
  }
}
