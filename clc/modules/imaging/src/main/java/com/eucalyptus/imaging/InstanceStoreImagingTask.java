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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.imaging.worker.ImagingServiceProperties;
import com.eucalyptus.util.Dates;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.DiskImageDescription;
import edu.ucsb.eucalyptus.msgs.DiskImageDetail;
import edu.ucsb.eucalyptus.msgs.DiskImageVolumeDescription;
import edu.ucsb.eucalyptus.msgs.ImportDiskImage;
import edu.ucsb.eucalyptus.msgs.ImportImageType;
import edu.ucsb.eucalyptus.msgs.ImportInstanceGroup;
import edu.ucsb.eucalyptus.msgs.ImportInstanceLaunchSpecification;
import edu.ucsb.eucalyptus.msgs.ImportInstanceTaskDetails;
import edu.ucsb.eucalyptus.msgs.ImportInstanceVolumeDetail;

/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "instance-store-imaging-task" )
public class InstanceStoreImagingTask extends InstanceImagingTask {
  protected InstanceStoreImagingTask(){}
  protected InstanceStoreImagingTask(OwnerFullName ownerFullName,
      ConversionTask conversionTask) {
    super(ownerFullName, conversionTask);
  }
  private static Logger LOG  = Logger.getLogger( InstanceStoreImagingTask.class );
  
  @Column ( name = "metadata_destination_bucket", nullable=true)
  private String destinationBucket;
  
  @Column ( name = "metadata_destination_prefix", nullable=true)
  private String destinationPrefix;
  
  public void setDestinationBucket(final String bucket){
    this.destinationBucket = bucket;
  }
  
  public String getDestinationBucket(){
    return this.destinationBucket;
  }
  
  public void setDestinationPrefix(final String prefix){
    this.destinationPrefix = prefix;
  }
  
  public String getDestinationPrefix(){
    return this.destinationPrefix;
  }
  
  
  @TypeMapper
  enum InstanceStoreImagingTaskTransform implements Function<ImportImageType, InstanceStoreImagingTask> {
    INSTANCE;

    @Nullable
    @Override
    public InstanceStoreImagingTask apply(ImportImageType input) {
      final ConversionTask ct = new ConversionTask(); 
      // TODO: do we use the instance id when launched?
      String conversionTaskId = ResourceIdentifiers.generateString("import-is");
      conversionTaskId = conversionTaskId.toLowerCase();
      ct.setConversionTaskId( conversionTaskId );
      ct.setExpirationTime( new Date( Dates.hoursFromNow( Integer.parseInt(ImagingServiceProperties.IMPORT_TASK_EXPIRATION_HOURS) ).getTime( ) ).toString( ) );
      ct.setState( ImportTaskState.NEW.getExternalTaskStateName() );
      ct.setStatusMessage( "" );
      
      final ImportInstanceLaunchSpecification launchSpec = input.getLaunchSpecification();
      final ImportDiskImage image = input.getImage();
      
      final ImportInstanceTaskDetails instanceTask = new ImportInstanceTaskDetails();
      instanceTask.setDescription(input.getDescription());
      instanceTask.setPlatform(input.getPlatform());
      
      final List<ImportInstanceVolumeDetail> volumes = Lists.newArrayList();
      final List<DiskImageDetail> importImages = input.getImage().getDiskImageSet();
      for(final DiskImageDetail imageDetail : importImages){
        final ImportInstanceVolumeDetail volumeDetail = new ImportInstanceVolumeDetail();
        if(launchSpec!=null && launchSpec.getPlacement()!=null)
          volumeDetail.setAvailabilityZone(launchSpec.getPlacement().getAvailabilityZone());
        
        volumeDetail.setImage(new DiskImageDescription());
        volumeDetail.getImage().setFormat(imageDetail.getFormat());
        String manifestUrl = imageDetail.getImportManifestUrl();
        volumeDetail.getImage().setImportManifestUrl(manifestUrl);
        volumeDetail.getImage().setSize(imageDetail.getBytes()); // TODO: is this right?
        
        volumeDetail.setBytesConverted(0L);
        volumeDetail.setVolume(new DiskImageVolumeDescription());
        volumeDetail.setStatus(ImportTaskState.NEW.getExternalTaskStateName());
        volumes.add(volumeDetail);
      }
      instanceTask.setVolumes((ArrayList<ImportInstanceVolumeDetail>) volumes);
      
      ct.setImportInstance(instanceTask);
      
      final InstanceStoreImagingTask newTask = new InstanceStoreImagingTask(Contexts.lookup().getUserFullName(), ct);
      newTask.serializeTaskToJSON();
      if(launchSpec!=null){
        if(launchSpec.getArchitecture()==null || launchSpec.getArchitecture().length()<=0)
          newTask.setLaunchSpecArchitecture("i386");
        else
          newTask.setLaunchSpecArchitecture(launchSpec.getArchitecture());
        
        if(launchSpec.getUserData()!=null && launchSpec.getUserData().getData()!=null) //base64 encoded string
          newTask.setLaunchSpecUserData(launchSpec.getUserData().getData());
        newTask.setLaunchSpecInstanceType(launchSpec.getInstanceType());
        if(launchSpec.getPlacement()!=null)
          newTask.setLaunchSpecAvailabilityZone(launchSpec.getPlacement().getAvailabilityZone());
        if(launchSpec.getMonitoring()!=null)
          newTask.setLaunchSpecMonitoringEnabled(launchSpec.getMonitoring().getEnabled());
        if(launchSpec.getGroupSet()!=null){
          for(final ImportInstanceGroup group : launchSpec.getGroupSet()){
            if(group.getGroupName()!=null)
              newTask.addLaunchSpecGroupName(group.getGroupName());
            else if(group.getGroupId()!=null)
              newTask.addLaunchSpecGroupName(group.getGroupId());
          }
        }
        if(launchSpec.getSubnetId()!=null)
          LOG.warn("SubnetId is not supported for import-instance");
        if(launchSpec.getInstanceInitiatedShutdownBehavior()!=null)
          LOG.warn("InitiatedShutdownBehavior is not supported for import-instance");
        if(launchSpec.getPrivateIpAddress()!=null)
          LOG.warn("Private Ip address is not supported for import-instance");
      }
      
      if(image.getConvertedImage()!=null){
        newTask.setDestinationBucket(image.getConvertedImage().getBucket());
        final String prefix = image.getConvertedImage().getPrefix();
        if(prefix !=null && prefix.length()>0)
          newTask.setDestinationPrefix(prefix);
      }
      return newTask;
    }
  }
}
