/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import static com.eucalyptus.auth.policy.PolicySpec.IMAGINGSERVICE_GETINSTANCEIMPORTTASK;
import static com.eucalyptus.auth.policy.PolicySpec.IMAGINGSERVICE_PUTINSTANCEIMPORTTASKSTATUS;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_IMAGINGSERVICE;
import static com.eucalyptus.auth.policy.PolicySpec.EC2_RESOURCE_INSTANCE;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.images.ImageConversionManager;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.compute.common.internal.images.MachineImageInfo;
import com.eucalyptus.imaging.backend.AbstractTaskScheduler.WorkerTask;
import com.eucalyptus.imaging.common.backend.msgs.CancelConversionTaskResponseType;
import com.eucalyptus.imaging.common.backend.msgs.CancelConversionTaskType;
import com.eucalyptus.imaging.common.backend.msgs.DescribeConversionTasksResponseType;
import com.eucalyptus.imaging.common.backend.msgs.DescribeConversionTasksType;
import com.eucalyptus.imaging.common.DiskImageConversionTask;
import com.eucalyptus.imaging.common.backend.msgs.GetInstanceImportTaskResponseType;
import com.eucalyptus.imaging.common.backend.msgs.GetInstanceImportTaskType;
import com.eucalyptus.imaging.common.backend.msgs.ImportImageResponseType;
import com.eucalyptus.imaging.common.backend.msgs.ImportImageType;
import com.eucalyptus.imaging.common.backend.msgs.PutInstanceImportTaskStatusResponseType;
import com.eucalyptus.imaging.common.backend.msgs.PutInstanceImportTaskStatusType;
import com.eucalyptus.imaging.common.ImagingBackend;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmInstance.Reason;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class ImagingBackendService {
  private static Logger LOG = Logger.getLogger( ImagingBackendService.class );

  private static final int MAX_TIMEOUT_AND_RETRY = 1; 
  public static ImportImageResponseType importImage(ImportImageType request) throws ImagingServiceException {
    final ImportImageResponseType reply = request.getReply();
    final Context context = Contexts.lookup( );
    try{
      if (!Bootstrap.isFinished() ||
           !Topology.isEnabled( ImagingBackend.class )){
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "For import, Imaging service should be enabled");
      }
    }catch(final Exception ex){
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "For import, Imaging service should be enabled");
    }
    
    try{
      /// api is callable only by sysadmin
      if ( !context.getUser().isSystemAdmin() ) {
        throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to import image." );
      }
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to import image." );
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

  public static DescribeConversionTasksResponseType describeConversionTask(DescribeConversionTasksType request) throws ImagingServiceException {
    DescribeConversionTasksResponseType reply = request.getReply( );
    Context ctx = Contexts.lookup( );
    boolean verbose = request.getConversionTaskIdSet( ).remove( "verbose" );
    try{
      /// api is callable only by sysadmin
      if ( !ctx.getUser().isSystemAdmin() ) {
        throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to describe conversion tasks." );
      }
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to describe conversion tasks." );
    }

    Collection<String> ownerInfo = ( ctx.isAdministrator( ) && verbose )
        ? Collections.<String> emptyList( )
            : Collections.singleton( ctx.getAccount( ).getAccountNumber( ) );
        //TODO: extends for volumes
        final Predicate<? super DiskImagingTask> requestedAndAccessible = RestrictedTypes.filteringFor( DiskImagingTask.class )
            .byId( request.getConversionTaskIdSet( ) )
            .byOwningAccount( ownerInfo )
            .byPrivileges( )
            .buildPredicate( );

        Iterable<DiskImagingTask> tasksToList = ImagingTasks.getDiskImagingTasks(AccountFullName.getInstance(ctx.getAccountNumber()),
            request.getConversionTaskIdSet());
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
  public CancelConversionTaskResponseType CancelConversionTask( CancelConversionTaskType request ) throws ImagingServiceException {
    final CancelConversionTaskResponseType reply = request.getReply( );
    Context ctx = Contexts.lookup( );
    try{
      /// api is callable only by sysadmin
      if ( ! (ctx.getUser().isSystemAdmin( ) )) {
        throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to cancel conversion tasks." );
      }
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to cancel conversion tasks." );
    }
    try{
      final ImagingTask task = ImagingTasks.lookup(request.getConversionTaskId());
      final ImportTaskState state = task.getState();
      if(state.equals(ImportTaskState.NEW) || 
          state.equals(ImportTaskState.PENDING) ||
          state.equals(ImportTaskState.CONVERTING) ||
          state.equals(ImportTaskState.INSTANTIATING) ) {
        ImagingTasks.setState(AccountFullName.getInstance(Contexts.lookup().getAccountNumber()), request.getConversionTaskId(),
          ImportTaskState.CANCELLING, ImportTaskState.STATE_MSG_USER_CANCELLATION);
      }
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
    final Context context = Contexts.lookup();
    try{
      if ( ! ( context.getUser().isSystemAdmin() || ( context.isAdministrator() && Permissions.isAuthorized(
          VENDOR_IMAGINGSERVICE,
          EC2_RESOURCE_INSTANCE, // resource type should not affect evaluation
          "",
          null,
          IMAGINGSERVICE_PUTINSTANCEIMPORTTASKSTATUS,
          context.getAuthContext() ) ) ) ) {
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
          if(imagingTask instanceof VolumeImagingTask){
            try{
              ImagingTasks.updateVolumeStatus((VolumeImagingTask)imagingTask, volumeId, ImportTaskState.COMPLETED, null);
              Ec2Client.getInstance().deleteTags(null, Lists.newArrayList(volumeId),
                  Lists.newArrayList(ImagingTaskStateManager.VOLUME_STATUS_TAG));
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
          }else if(imagingTask instanceof DiskImagingTask){
            ImagingTasks.transitState(imagingTask, ImportTaskState.CONVERTING, 
                ImportTaskState.COMPLETED, ImportTaskState.STATE_MSG_DONE);
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
          if(imagingTask instanceof DiskImagingTask){
            // terminate all instances that are waiting for the conversion
            String conversionTaskId = ((DiskImagingTask)imagingTask).getTask().getConversionTaskId();
            String imageId = null;
            for( ImageInfo imageInfo:ImageConversionManager.getPartitionedImages() ){
              if ( imageInfo instanceof MachineImageInfo ) {
                if ( conversionTaskId.equals( ((MachineImageInfo)imageInfo).getImageConversionId() ) ) {
                  imageId = imageInfo.getDisplayName();
                  break;
                }
              }
            }
            LOG.debug("Image that failed conversion: " + imageId);
            if ( imageId != null ) {
              for( VmInstance vm : VmInstances.list( new InstanceByImageId(imageId)) ) {
                LOG.debug("Shutting down instance: " + vm.getInstanceId());
                VmInstances.setState( vm, VmState.SHUTTING_DOWN, Reason.FAILED );
              }
            }
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

  private class InstanceByImageId implements Predicate<VmInstance> {
    private String imageId;
    public InstanceByImageId(String imageId) {
      this.imageId = imageId;
    }
    @Override
    public boolean apply( final VmInstance input ) {
      return imageId.equals(input.getImageId());
    }
  }

  public GetInstanceImportTaskResponseType GetInstanceImportTask( GetInstanceImportTaskType request ) throws EucalyptusCloudException {
    final GetInstanceImportTaskResponseType reply = request.getReply( );
    final Context context = Contexts.lookup();
    try{
      if ( ! ( context.getUser().isSystemAdmin() || ( context.isAdministrator() && Permissions.isAuthorized(
          VENDOR_IMAGINGSERVICE,
          EC2_RESOURCE_INSTANCE, // resource type should not affect evaluation
          "",
          null,
          IMAGINGSERVICE_GETINSTANCEIMPORTTASK,
          context.getAuthContext() ) ) ) ) {
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
      }else
        worker = ImagingWorkers.createWorker(request.getInstanceId());

      final ImagingTask prevTask = ImagingTasks.getConvertingTaskByWorkerId(request.getInstanceId());
      if(prevTask!=null){
        /// this should NOT happen; if it does, it's worker script's bug
        ImagingTasks.killAndRerunTask(prevTask.getDisplayName());
        LOG.warn(String.format("A task (%s:%s) is gone missing [BUG in worker script]", 
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

}
