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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.eucalyptus.resources.client.Ec2Client;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.ConversionTask;
import com.eucalyptus.compute.common.ImportInstanceTaskDetails;
import com.eucalyptus.compute.common.ImportInstanceVolumeDetail;
import com.eucalyptus.compute.common.Snapshot;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.images.ImageConfiguration;
import com.eucalyptus.imaging.common.ImagingBackend;
import com.eucalyptus.imaging.common.UrlValidator;
import com.eucalyptus.imaging.manifest.ImportImageManifest;
import com.eucalyptus.util.Dates;
import com.eucalyptus.util.XMLParser;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Sang-Min Park
 *
 */
public class ImagingTaskStateManager implements EventListener<ClockTick> {
  private static Logger LOG  = Logger.getLogger( ImagingTaskStateManager.class );
  public static final int TASK_PURGE_EXPIRATION_HOURS = 24;
  public static void register( ) {
        Listeners.register( ClockTick.class, new ImagingTaskStateManager() );
  }

  @Override
  public void fireEvent(ClockTick event) {
    if (!( Bootstrap.isOperational( ) &&
         Topology.isEnabledLocally( ImagingBackend.class ) ) )
       return;
    
    final Map <ImportTaskState, List<ImagingTask>> taskByState =
        Maps.newHashMap();
    final List<ImagingTask> allTasks = ImagingTasks.getImagingTasks();
    for(final ImagingTask task : allTasks){
      if(! taskByState.containsKey(task.getState()))
        taskByState.put(task.getState(), Lists.<ImagingTask>newArrayList());
     taskByState.get(task.getState()).add(task); 
    }
    
    /*
     *  NEW, PENDING, CONVERTING, CANCELLING, CANCELLED, COMPLETED, FAILED
     */
    if(taskByState.containsKey(ImportTaskState.NEW)){
      this.processNewTasks(taskByState.get(ImportTaskState.NEW));
    }
    if(taskByState.containsKey(ImportTaskState.PENDING)){
      this.processPendingTasks(taskByState.get(ImportTaskState.PENDING));
    }
    if(taskByState.containsKey(ImportTaskState.CONVERTING)){
      this.processConvertingTasks(taskByState.get(ImportTaskState.CONVERTING));
    }
    if(taskByState.containsKey(ImportTaskState.INSTANTIATING)){
      this.processInstantiatingTasks(taskByState.get(ImportTaskState.INSTANTIATING));
    }
    if(taskByState.containsKey(ImportTaskState.CANCELLING)){
      this.processCancellingTasks(taskByState.get(ImportTaskState.CANCELLING));
    }
    if(taskByState.containsKey(ImportTaskState.COMPLETED)){
      this.processCompletedTasks(taskByState.get(ImportTaskState.COMPLETED));
    }
    if(taskByState.containsKey(ImportTaskState.CANCELLED)){
      this.processCancelledTasks(taskByState.get(ImportTaskState.CANCELLED));
    }
    if(taskByState.containsKey(ImportTaskState.FAILED)){
      this.processFailedTasks(taskByState.get(ImportTaskState.FAILED));
    }
  }
  
  private void processPendingTasks(final List<ImagingTask> tasks){
    for(final ImagingTask task : tasks){
      if(! ImportTaskState.STATE_MSG_PENDING_CONVERSION.equals(task.getStateReason())) {
        try{
          ImagingTasks.transitState(task, ImportTaskState.PENDING, ImportTaskState.PENDING, ImportTaskState.STATE_MSG_PENDING_CONVERSION);
        }catch(final Exception ex){
          ;
        }
      }
      if(isExpired(task)){
        try{
          ImagingTasks.transitState(task, ImportTaskState.PENDING, ImportTaskState.CANCELLING, ImportTaskState.STATE_MSG_TASK_EXPIRED);
        }catch(final Exception ex){
          ;
        }
      }
    }
  }
  
  private void processConvertingTasks(final List<ImagingTask> tasks){
    for(final ImagingTask task : tasks){
      if(! ImportTaskState.STATE_MSG_IN_CONVERSION.equals(task.getStateReason())) {
        try{
          ImagingTasks.transitState(task, ImportTaskState.CONVERTING, ImportTaskState.CONVERTING, ImportTaskState.STATE_MSG_IN_CONVERSION);
        }catch(final Exception ex){
          ;
        }
      }
      if(isExpired(task)){
        try{
          ImagingTasks.transitState(task, ImportTaskState.CONVERTING, ImportTaskState.CANCELLING, ImportTaskState.STATE_MSG_TASK_EXPIRED);
        }catch(final Exception ex){
          ;
        }
      }
    }
  }
  
  private void processInstantiatingTasks(final List<ImagingTask> tasks){
    for(final ImagingTask task : tasks){
      if(!(task instanceof ImportInstanceImagingTask)){
        try{
          ImagingTasks.transitState(task, ImportTaskState.INSTANTIATING, ImportTaskState.COMPLETED, ImportTaskState.STATE_MSG_DONE);
        }catch(final Exception ex){
          ;
        }
      }
     
      final ImportInstanceImagingTask instanceTask = (ImportInstanceImagingTask) task;
      final ConversionTask conversionTask = instanceTask.getTask();
      if(conversionTask.getImportInstance()==null){
        LOG.warn("Import instance task should contain ImportInstanceTaskDetail");
        continue;
      }
        
      String instanceId = conversionTask.getImportInstance().getInstanceId();
      if(instanceId!=null && instanceId.length() > 0){
        try{
          ImagingTasks.transitState(task, ImportTaskState.INSTANTIATING , ImportTaskState.COMPLETED, ImportTaskState.STATE_MSG_DONE);
        }catch(final Exception ex){
          LOG.error("Failed to update task's state to completed", ex);
        }
        continue;
      }
      
      String imageId = instanceTask.getImageId();
      if(imageId!=null && imageId.length() > 0){
        try{
          // launch the image with the launch spec
          final String userData = Strings.emptyToNull( instanceTask.getLaunchSpecUserData( ) );
          final String instanceType = Strings.emptyToNull( instanceTask.getLaunchSpecInstanceType( ) );
          final String keyName = Strings.emptyToNull( instanceTask.getLaunchSpecKeyName( ) );
          final String subnetId = Strings.emptyToNull( instanceTask.getLaunchSpecSubnetId( ) );
          final String privateIp = subnetId==null ? null : Strings.emptyToNull( instanceTask.getLaunchSpecPrivateIpAddress( ) );
          final ArrayList<String> groupNames = new ArrayList<String>();
          if( subnetId == null && instanceTask.getLaunchSpecGroupNames( ) != null ){
            groupNames.addAll( instanceTask.getLaunchSpecGroupNames( ) );
          }
          final String availabilityZone = subnetId == null ?
            instanceTask.getLaunchSpecAvailabilityZone( ) :
            null;

          final Boolean monitoringEnabled = instanceTask.getLaunchSpecMonitoringEnabled();
          final boolean monitoring = monitoringEnabled!=null && monitoringEnabled;
          List<String> instances = Ec2Client.getInstance( ).runInstances(
              instanceTask.getOwnerUserId( ),
              imageId,
              groupNames,
              userData,
              instanceType,
              availabilityZone,
              subnetId,
              privateIp,
              monitoring,
              keyName,
              1
          );
          if (instances.isEmpty())
            throw new Exception("Failed to run instances after conversion task");
          instanceId = instances.get(0);
          conversionTask.getImportInstance().setInstanceId(instanceId);
          ImagingTasks.updateTaskInJson(instanceTask);
        }catch(final Exception ex){
          LOG.error("Failed to run instances after conversion task", ex);
          try{
            ImagingTasks.transitState(instanceTask, ImportTaskState.INSTANTIATING , 
                ImportTaskState.COMPLETED, String.format("Image registered: %s, but run instance failed", imageId));
            // this will set the task state to completed in the next timer run
          }catch(final Exception ex1){
            ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_RUN_FAILURE);
          }
        }
        continue;
      }
      
      final List<String> snapshotIds = instanceTask.getSnapshotIds();
      if(snapshotIds!=null && snapshotIds.size()>0){
        try{
         // see if the snapshots are ready and register them as images
          final List<Snapshot> snapshots =
              Ec2Client.getInstance().describeSnapshots(instanceTask.getOwnerUserId(), snapshotIds);
          int numCompleted = 0;
          int numError = 0;
          for(final Snapshot snapshot: snapshots){
            if("completed".equals(snapshot.getStatus()))
              numCompleted++;
            else if("error".equals(snapshot.getStatus()) || "failed".equals(snapshot.getStatus()))
              numError++;
          }
          if(numError>0){
           ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_SNAPSHOT_FAILURE);
          }else if(numCompleted == snapshotIds.size()){
            // TODO : multiple snapshots (i.e., multiple images from import-instance). what to do?
            // register the image
            String snapshotId = null;
            if(snapshots.size()>1){
              LOG.warn("More than one snapshots found for import-instance task "+instanceTask.getDisplayName());
            }
            snapshotId = snapshotIds.get(0);
            final String imageName = String.format("image-%s", instanceTask.getDisplayName());
            final String description = conversionTask.getImportInstance().getDescription();
            final String architecture = instanceTask.getLaunchSpecArchitecture();
            String platform = null;
            if(conversionTask.getImportInstance().getPlatform()!=null && conversionTask.getImportInstance().getPlatform().length()>0)
              platform = conversionTask.getImportInstance().getPlatform().toLowerCase();
            try{
              imageId = 
                  Ec2Client.getInstance().registerEBSImage(instanceTask.getOwnerUserId(), 
                      snapshotId, imageName, architecture, platform, description, false);
              if(imageId==null)
                throw new Exception("Null image id");
              ImagingTasks.setImageId(instanceTask, imageId);
            }catch(final Exception ex){
              ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_REGISTER_FAILURE);
            }
          }
        }catch(final Exception ex){
          ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_REGISTER_FAILURE);
        }
        continue;
      }
      
      /// snapshot volumes
      final List<ImportInstanceVolumeDetail> volumes = conversionTask.getImportInstance().getVolumes();
      if(volumes==null || volumes.size()<=0){
        ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_TASK_INSUFFICIENT_PARAMETERS +":volume");
      }
      final List<String> volumeIds = Lists.newArrayList();
      for(final ImportInstanceVolumeDetail volume : volumes){
        if(volume.getVolume()==null || volume.getVolume().getId()==null)
          continue;
        volumeIds.add(volume.getVolume().getId());
      }
      if(volumeIds.size()<=0){
        ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_TASK_INSUFFICIENT_PARAMETERS +":volume");
      }
      for(final String volumeId : volumeIds){
        try{
          final String snapshotId = 
              Ec2Client.getInstance().createSnapshot(instanceTask.getOwnerUserId(), volumeId);
          ImagingTasks.addSnapshotId(instanceTask, snapshotId);
        }catch(final Exception ex){
          ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_SNAPSHOT_FAILURE);
          break;
        }
      }
    } /// end of for
  }
  
  private final static Map<String, Date> cancellingTimer = Maps.newHashMap();
  private final static int CANCELLING_WAIT_MIN = 2;
  private void processCancellingTasks(final List<ImagingTask> tasks){
    for(final ImagingTask task : tasks){
      try{
        if(!cancellingTimer.containsKey(task.getDisplayName())){
          cancellingTimer.put(task.getDisplayName(), Dates.minutesFromNow(CANCELLING_WAIT_MIN));
        }
        final Date cancellingExpired = cancellingTimer.get(task.getDisplayName());
        if(cancellingExpired.before(new Date())){
          try{
            if(task.cleanUp())
              ImagingTasks.transitState(task, ImportTaskState.CANCELLING, ImportTaskState.CANCELLED, null);     
          }catch(final Exception ex){
            LOG.warn("Failed to cleanup resources for "+task.getDisplayName());
          }
        }
      }catch(final Exception ex){
        LOG.error("Could not process cancelling task "+task.getDisplayName());
      }
    }
  }
  
  private void processCompletedTasks(final List<ImagingTask> tasks){
    for(final ImagingTask task : tasks){
      if(shouldPurge(task)){
        try{
          LOG.debug("forgetting about conversion task(completed) "+task.getDisplayName());
          ImagingTasks.deleteTask(task);
        }catch(final Exception ex){
          LOG.error("Failed to delete the conversion task", ex);
        }
      }
    }
  }
  
  private void processCancelledTasks(final List<ImagingTask> tasks){
    for(final ImagingTask task : tasks){
      if(shouldPurge(task)){
        try{
          LOG.debug("forgetting about conversion task(cancelled) "+task.getDisplayName());
          ImagingTasks.deleteTask(task);
        }catch(final Exception ex){
          LOG.error("Failed to delete the conversion task", ex);
        }
      }
    }
  }
  
  private void processFailedTasks(final List<ImagingTask> tasks){
    for(final ImagingTask task : tasks){
      try{
        task.cleanUp();
      }catch(final Exception ex){
        LOG.warn("Failed to cleanup resources for "+task.getDisplayName());
      }
      if(shouldPurge(task)){
        try{
          LOG.debug("forgetting about conversion task(failed) "+task.getDisplayName());
          ImagingTasks.deleteTask(task);
        }catch(final Exception ex){
          LOG.error("Failed to delete the conversion task", ex);
        }
      }
    }
  }
  
  private boolean isExpired(final ImagingTask task) {
    final Date expirationTime = task.getExpirationTime();
    return expirationTime.before(new Date());
  }
  
  private boolean shouldPurge(final ImagingTask task){
    final Date lastUpdated = task.getLastUpdateTimestamp();
    Calendar cal = Calendar.getInstance(); // creates calendar
    cal.setTime(lastUpdated); // sets calendar time/date
    cal.add(Calendar.HOUR_OF_DAY, TASK_PURGE_EXPIRATION_HOURS); // adds one hour
    final Date expirationTime = cal.getTime(); //
    
    return expirationTime.before(new Date());
  }
  
  private void processNewTasks(final List<ImagingTask> tasks) {
    if( !Bootstrap.isFinished() || !Topology.isEnabled( ImagingBackend.class) ) {
      LOG.warn("Imaging worker is not currently enabled");
      return;
    }
    
    for(final ImagingTask task : tasks){
      try{
        if(isExpired(task)){
          try{
            ImagingTasks.transitState(task, ImportTaskState.NEW, ImportTaskState.CANCELLING, ImportTaskState.STATE_MSG_TASK_EXPIRED);
          }catch(final Exception ex){
            ;
          }
          continue;
        }
        // create a volume and update the database
       if(task instanceof ImportVolumeImagingTask)
         processNewImportVolumeImagingTask((ImportVolumeImagingTask) task); 
       else if(task instanceof ImportInstanceImagingTask)
         processNewImportInstanceImagingTask((ImportInstanceImagingTask)task);
       else
         throw new Exception("Invalid ImagingTask");
      }catch(final Exception ex){
        try{
          ImagingTasks.transitState(task, ImportTaskState.NEW, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_FAILED_UNEXPECTED);
        }catch(final Exception ex2){
          ;
        }
        LOG.error("Failed to process new task", ex);
      }
    }
  }
  
  private void processNewImportInstanceImagingTask(final ImportInstanceImagingTask instanceTask) throws Exception{
    // for each disk image, create a volume and set its state accordingly
    final ImportInstanceTaskDetails taskDetail=
        instanceTask.getTask().getImportInstance();
    final List<ImportInstanceVolumeDetail> volumes = taskDetail.getVolumes();
    if(volumes==null)
      return;
    for(final ImportInstanceVolumeDetail volume: volumes){
      if(volume.getImage().getImportManifestUrl()!=null)
        try{
          if(! doesManifestExist(volume.getImage().getImportManifestUrl())) {
            if(! ImportTaskState.STATE_MSG_PENDING_UPLOAD.equals(instanceTask.getStateReason())){
              try{
                ImagingTasks.transitState(instanceTask, ImportTaskState.NEW, ImportTaskState.NEW, ImportTaskState.STATE_MSG_PENDING_UPLOAD);
              }catch(final Exception ex){
                ;
              }
            }
            return;
          }
        }catch(final Exception ex){
          throw new Exception("Failed to check import manifest", ex);
        }
    }

    try{
      ImagingTasks.transitState(instanceTask, ImportTaskState.NEW, ImportTaskState.NEW, ImportTaskState.STATE_MSG_CREATING_VOLUME);
    }catch(final Exception ex){
      ;
    }
    try{
      int numVolumeCreated = 0;
      for(final ImportInstanceVolumeDetail volume : volumes){
        if(volume.getVolume()==null || volume.getVolume().getId() == null ||  
            volume.getVolume().getId().length()<=0){
          final String zone = volume.getAvailabilityZone();
          final Integer size = volume.getVolume().getSize();
          if(zone==null)
            throw new Exception("Availability zone is missing from the volume detail");
          if(size==null || size <=0 )
            throw new Exception("Volume size is missing from the volume detail");
          try{
            final String volumeId = 
                Ec2Client.getInstance().createVolume(instanceTask.getOwnerUserId(), zone, size);
            volume.getVolume().setId(volumeId);
            Volumes.setSystemManagedFlag(null, volumeId, true);
          }catch(final Exception ex){
            throw new Exception("Failed to create the volume", ex);
          }
        }else{
          String volumeStatus= null;
          try{
            final List<Volume> eucaVolumes =
                Ec2Client.getInstance().describeVolumes(null,
                    Lists.newArrayList(volume.getVolume().getId()));
            final Volume eucaVolume = eucaVolumes.get(0);
            volumeStatus = eucaVolume.getStatus();
          }catch(final Exception ex){
            throw new Exception("Failed to check the state of the volume "+volume.getVolume().getId());
          }
          if("available".equals(volumeStatus)){
            volume.setStatus("active");
            numVolumeCreated++;
          }else if ("creating".equals(volumeStatus)){
            volume.setStatus("active");
          }else{
            volume.setStatus("cancelled");
            volume.setStatusMessage("Failed to create the volume");
            throw new Exception("Volume "+volume.getVolume().getId()+" is in "+volumeStatus);
          }
        } 
      }
      if(numVolumeCreated == volumes.size()){
        try{
          ImagingTasks.transitState(instanceTask, ImportTaskState.NEW, ImportTaskState.PENDING, "");
        }catch(final Exception ex){
          ;
        }
      }
    }catch(Exception ex){
      throw ex;
    }finally{
      ImagingTasks.updateTaskInJson(instanceTask); 
    }
  }
  
  private void processNewImportVolumeImagingTask(final ImportVolumeImagingTask volumeTask) throws Exception{
    if(volumeTask.getImportManifestUrl() !=null){
      try{
        if(! doesManifestExist(volumeTask.getImportManifestUrl())) {
          if(! ImportTaskState.STATE_MSG_PENDING_UPLOAD.equals(volumeTask.getStateReason())){
            try{
              ImagingTasks.transitState(volumeTask, ImportTaskState.NEW, ImportTaskState.NEW, ImportTaskState.STATE_MSG_PENDING_UPLOAD);
            }catch(final Exception ex){
              ;
            }
          }
          return;
        }
      }catch(final Exception ex){
        throw new Exception("Failed to check import manifest", ex);
      }
    }
    
    try{
      ImagingTasks.transitState(volumeTask, ImportTaskState.NEW, ImportTaskState.NEW, ImportTaskState.STATE_MSG_CREATING_VOLUME);
    }catch(final Exception ex){
      ;
    }
    if(volumeTask.getVolumeId()==null || volumeTask.getVolumeId().length()<=0){
      final String zone = volumeTask.getAvailabilityZone();
      final int size = volumeTask.getVolumeSize();
      //create volume (already sanitized)
      try{
        final String volumeId = Ec2Client.getInstance().createVolume(volumeTask.getOwnerUserId(), zone, size);
        Volumes.setSystemManagedFlag(null, volumeId, true);
        ImagingTasks.setVolumeId(volumeTask, volumeId);
      }catch(final Exception ex){
        throw new Exception("Failed to create the volume", ex);
      }
    } else { /// check status
      // describe volume as system user since it is not visible to regular user until conversion is over
      final List<Volume> volumes = 
          Ec2Client.getInstance().describeVolumes(null, Lists.newArrayList(volumeTask.getVolumeId()));
      final Volume volume = volumes.get(0);
      final String volumeStatus = volume.getStatus();
      if("available".equals(volumeStatus)){
        final ConversionTask conversionTask = volumeTask.getTask();
        if(conversionTask.getImportVolume() != null){
          try{
            ImagingTasks.transitState(volumeTask, ImportTaskState.NEW, ImportTaskState.PENDING, "");
          }catch(final Exception ex){
            ;
          }
        }else{
          throw new Exception("No importVolume detail is found in the conversion task");
        }
      }else if ("creating".equals(volumeStatus)){
        ; // continue to poll
      }else{
        throw new Exception("The volume "+volume.getVolumeId()+"'s state is "+volumeStatus);
      }
    }  
  }
  
  private boolean doesManifestExist(final String manifestUrl) throws Exception {
    // validate urls per EUCA-9144
    final UrlValidator urlValidator = new UrlValidator();
    if (!urlValidator.isEucalyptusUrl(manifestUrl))
      throw new RuntimeException("Manifest's URL is not in the Eucalyptus format: " + manifestUrl);
    final HttpClient client = new HttpClient();
    client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
    client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000);
    client.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, 30000);
    GetMethod method = new GetMethod(manifestUrl);
    String manifest = null;
    try {
      // avoid TCP's CLOSE_WAIT  
      method.setRequestHeader("Connection", "close");
      client.executeMethod(method);
      manifest = method.getResponseBodyAsString( ImageConfiguration.getInstance( ).getMaxManifestSizeBytes( ) );
      if (manifest == null) {
        return false;
      }else if (manifest.contains("<Code>NoSuchKey</Code>") || manifest.contains("The specified key does not exist")){
        return false;
      }
    } catch(final Exception ex){
      return false;
    } finally {
      method.releaseConnection();
    }
    final List<String> partsUrls = getPartsHeadUrl(manifest);
    for(final String url : partsUrls){
      if (!urlValidator.isEucalyptusUrl(url))
        throw new RuntimeException("Manifest's URL is not in the Eucalyptus format: " + url);
      HeadMethod partCheck = new HeadMethod(url);
      int res = client.executeMethod(partCheck);
      if ( res != HttpStatus.SC_OK){
        return false;
      }
    }
    return true;
  }
  
  private List<String> getPartsHeadUrl(final String manifest) throws Exception{
    final XPath xpath = XPathFactory.newInstance( ).newXPath();
    final DocumentBuilder builder = XMLParser.getDocBuilder( );
    final Document inputSource = builder.parse( new ByteArrayInputStream( manifest.getBytes( ) ) );
   
    final List<String> parts = Lists.newArrayList();
    final NodeList nodes = 
        (NodeList) xpath.evaluate( ImportImageManifest.INSTANCE.getPartsPath()+"/head-url",
            inputSource, XPathConstants.NODESET );
    for (int i = 0; i < nodes.getLength(); i++) {
      parts.add(nodes.item(i).getTextContent());
    }
    return parts;
  }
}

