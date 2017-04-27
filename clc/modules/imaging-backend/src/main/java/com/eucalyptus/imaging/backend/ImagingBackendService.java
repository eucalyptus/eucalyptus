/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_IMAGINGSERVICE;
import static com.eucalyptus.auth.policy.PolicySpec.EC2_RESOURCE_INSTANCE;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.imaging.backend.AbstractTaskScheduler.WorkerTask;
import com.eucalyptus.imaging.common.backend.msgs.GetInstanceImportTaskResponseType;
import com.eucalyptus.imaging.common.backend.msgs.GetInstanceImportTaskType;
import com.eucalyptus.imaging.common.backend.msgs.PutInstanceImportTaskStatusResponseType;
import com.eucalyptus.imaging.common.backend.msgs.PutInstanceImportTaskStatusType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;

@ComponentNamed
public class ImagingBackendService {
  private static Logger LOG = Logger.getLogger( ImagingBackendService.class );

  private static final int MAX_TIMEOUT_AND_RETRY = 1; 

  public PutInstanceImportTaskStatusResponseType PutInstanceImportTaskStatus( PutInstanceImportTaskStatusType request ) throws EucalyptusCloudException {
    final PutInstanceImportTaskStatusResponseType reply = request.getReply( );
    try{
      if ( !isAuthorized( ) ) {
        throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to put import task status." );
      }
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to put import task status." );
    }    
    try{
      final String remoteHost = request.getSourceIp();
      ImagingWorkers.verifyWorker(request.getInstanceId(), remoteHost);
    }catch(final Exception ex){
      LOG.warn("Failed to verify worker", ex);
      throw new ImagingServiceException(ImagingServiceException.DEFAULT_CODE, "Not authorized to put import task status." );
    }
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
          if(imagingTask.isTimedOut()){
            try{
              if(imagingTask.getTimeoutCount() < MAX_TIMEOUT_AND_RETRY) {
                LOG.warn(String.format("Imaging import task %s has timed out; will retry later", imagingTask.getDisplayName()));
                ImagingTasks.timeoutTask(taskId);
                ImagingTasks.killAndRerunTask(taskId);
              }else{
                LOG.warn(String.format("Imaging import task %s has timed out and failed", imagingTask.getDisplayName()));
                ImagingTasks.setState(imagingTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_CONVERSION_TIMEOUT);
              }
            }catch(final Exception ex){
              LOG.error("Failed to handle timed-out task", ex);
              ImagingTasks.setState(imagingTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_CONVERSION_TIMEOUT);
            }finally{
              reply.setCancelled(true);
            }
          }
          break;

        case DONE:
          try{
            ImagingTasks.updateVolumeStatus((VolumeImagingTask)imagingTask, volumeId, ImportTaskState.COMPLETED, null);
            Volumes.setSystemManagedFlag(null, volumeId, false);
          }catch(final Exception ex){
            ImagingTasks.transitState(imagingTask, ImportTaskState.CONVERTING, 
                ImportTaskState.FAILED, ImportTaskState.STATE_MSG_FAILED_UNEXPECTED);
            LOG.error("Failed to update volume's state", ex);
            break;
          }
          
          try{
            if(imagingTask instanceof ImportVolumeImagingTask){
              ImagingTasks.transitState(imagingTask, ImportTaskState.CONVERTING, 
                  ImportTaskState.COMPLETED, ImportTaskState.STATE_MSG_DONE);
            }else if(ImagingTasks.isConversionDone((VolumeImagingTask)imagingTask)){
                ImagingTasks.transitState(imagingTask, ImportTaskState.CONVERTING, 
                    ImportTaskState.INSTANTIATING, ImportTaskState.STATE_MSG_LAUNCHING_INSTANCE);
            }
          }catch(final Exception ex){
            LOG.error("Failed to update imaging task's state to completed", ex);
          }
          break;

        case FAILED:
          String stateMsg = ImportTaskState.STATE_MSG_CONVERSION_FAILED;
          if(request.getMessage()!=null)
            stateMsg = request.getMessage();
          ImagingTasks.setState(imagingTask, ImportTaskState.FAILED, stateMsg);
          LOG.warn(String.format("Worker reported failed conversion: %s-%s", stateMsg, request.getErrorCode() !=null ? request.getErrorCode() : "no error code"));
          final String errorCode = request.getErrorCode();
          if(errorCode!=null && errorCode.length()>0 && ImagingWorkers.isFatalError(errorCode)){
            LOG.warn(String.format("A task %s experienced fatal error. Worker instance %s is being retired",
                request.getImportTaskId(), request.getInstanceId()));
            ImagingWorkers.retireWorker(request.getInstanceId());
          }
          break;
        }
      }else{ // state other than "CONVERTING" is not valid and worker should stop working
        reply.setCancelled(true);
      }
    }catch(final Exception ex){
      LOG.warn("Failed to update the task's state", ex);
    }
    if(reply.getCancelled()!=null && reply.getCancelled().booleanValue()){
      LOG.warn(String.format("Imaging task %s has been cancelled", request.getImportTaskId()));
    }
    return reply;
  }

  public GetInstanceImportTaskResponseType GetInstanceImportTask( GetInstanceImportTaskType request ) throws EucalyptusCloudException {
    final GetInstanceImportTaskResponseType reply = request.getReply( );
    try{
      if ( !isAuthorized( ) ) {
        throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to get import task." );
      }
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to get import task." );
    }   
    
    try{
      final String remoteHost = request.getSourceIp();
      ImagingWorkers.verifyWorker(request.getInstanceId(), remoteHost);
    }catch(final Exception ex){
      LOG.warn("Failed to verify worker", ex);
      throw new ImagingServiceException(ImagingServiceException.DEFAULT_CODE, "Not authorized to get instance import task." );
    }
    
    try{
      ImagingWorker worker = ImagingWorkers.getWorker(request.getInstanceId());
      if(worker!=null) {
        ImagingWorkers.markUpdate(request.getInstanceId());
        if(!ImagingWorkers.canAllocate(request.getInstanceId())){
          LOG.warn(String.format("The worker (%s) is marked invalid", request.getInstanceId()));
          return reply;
        }
      }else {
        worker = ImagingWorkers.createWorker(request.getInstanceId());
      }

      final ImagingTask prevTask = ImagingTasks.getConvertingTaskByWorkerId(request.getInstanceId());
      if(prevTask!=null){
        /// this should NOT happen; if it does, it's worker script's bug
        ImagingTasks.killAndRerunTask(prevTask.getDisplayName());
        LOG.info(String.format("A task (%s:%s) is gone missing and will be re-scheduled", 
            prevTask.getDisplayName(), request.getInstanceId()));
      }

      final WorkerTask task = AbstractTaskScheduler.getScheduler().getTask(worker.getAvailabilityZone());
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

  private static boolean isAuthorized( ) {
    final Context context = Contexts.lookup();
    return context.hasAdministrativePrivileges( ) ||
        ( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT.equals( context.getAccountAlias( ) ) && Permissions.isAuthorized(
            VENDOR_IMAGINGSERVICE,
            EC2_RESOURCE_INSTANCE, // resource type should not affect evaluation
            "",
            null,
            RestrictedTypes.getIamActionByMessageType( ),
            context.getAuthContext() ) );

  }
}
