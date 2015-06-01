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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.compute.common.ConversionTask;
import com.eucalyptus.compute.common.DiskImage;
import com.eucalyptus.compute.common.DiskImageDescription;
import com.eucalyptus.compute.common.DiskImageVolumeDescription;
import com.eucalyptus.compute.common.ImportInstanceLaunchSpecification;
import com.eucalyptus.compute.common.ImportInstanceTaskDetails;
import com.eucalyptus.compute.common.ImportInstanceType;
import com.eucalyptus.compute.common.ImportInstanceVolumeDetail;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.imaging.ImportTaskProperties;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.util.Dates;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park
 *
 */

@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "instance-imaging-task" )
public class ImportInstanceImagingTask extends VolumeImagingTask {
  private static Logger LOG  = Logger.getLogger( ImportInstanceImagingTask.class );

  @Column ( name = "metadata_launchspec_architecture")
  private String architecture;
  
  @ElementCollection
  @CollectionTable( name = "metadata_launchspec_security_groups" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<String> groupNames;
  
  @Transient
  private ImmutableList<String> groupNamesCopy;
  
  @ElementCollection
  @CollectionTable( name = "metadata_snapshots")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<String> snapshotIds;
  
  @Transient
  private ImmutableList<String> snapshotIdsCopy;
  
  @Type( type = "org.hibernate.type.StringClobType" )
  @Lob
  @Column( name = "metadata_launchspec_userdata")
  private String userData;
  
  @Column ( name = "metadata_launchspec_instance_type")
  private String instanceType;
  
  @Column ( name = "metadata_launchspec_availability_zone")
  private String availabilityZone;
  
  @Column ( name = "metadata_launchspec_monitoring_enabled")
  private Boolean monitoringEnabled;
  
  /// VPC
  @Column ( name = "metadata_launchspec_subnet_id")
  private String subnetId;
  
  /// NOT SUPPORTED
  @Column ( name = "metadata_launchspec_shutdown_behavior")
  private String shutdownBehavior;
  
  /// VPC
  @Column ( name = "metadata_launchspec_private_ip_address")
  private String privateIpAddress;
  
  // EUCA-specific extension
  @Column ( name = "metadata_launchspec_key_name")
  private String keyName;
  
  @Column ( name = "metadata_image_id")
  private String imageId;
  
  protected ImportInstanceImagingTask(){}
  protected ImportInstanceImagingTask(OwnerFullName ownerFullName,
      ConversionTask conversionTask) {
    super(ownerFullName, conversionTask, ImportTaskState.NEW, 0L);
  }
  
  public void setLaunchSpecArchitecture(final String architecture){
    this.architecture = architecture;
  }
  
  public String getLaunchSpecArchitecture(){
    return this.architecture;
  }
  
  public List<String> getLaunchSpecGroupNames(){
    return groupNamesCopy;
  }
  
  public void addLaunchSpecGroupName(final String groupName){
    if(this.groupNames==null)
      this.groupNames = Lists.newArrayList();
    this.groupNames.add(groupName);
  }
  
  public void setLaunchSpecUserData(final String userData){
    this.userData = userData;
  }
  
  public String getLaunchSpecUserData(){
    return this.userData;
  }
  
  public void setLaunchSpecInstanceType(final String instanceType){
    this.instanceType = instanceType;
  }
  
  public String getLaunchSpecInstanceType(){
    return this.instanceType;
  }
  
  public void setLaunchSpecAvailabilityZone(final String availabilityZone){
    this.availabilityZone = availabilityZone;
  }
  
  public String getLaunchSpecAvailabilityZone(){
    return this.availabilityZone;
  }
  
  public void setLaunchSpecMonitoringEnabled(final Boolean monitoringEnabled){
    this.monitoringEnabled = monitoringEnabled;
  }
  
  public Boolean getLaunchSpecMonitoringEnabled(){
    return this.monitoringEnabled;
  }
  
  public void setLaunchSpecKeyName(final String keyName){
    this.keyName = keyName;
  }
  
  public String getLaunchSpecKeyName(){
    return this.keyName;
  }

  public String getLaunchSpecSubnetId() {
    return subnetId;
  }

  public void setLaunchSpecSubnetId( final String subnetId ) {
    this.subnetId = subnetId;
  }

  public String getLaunchSpecPrivateIpAddress() {
    return privateIpAddress;
  }

  public void setLaunchSpecPrivateIpAddress( final String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public List<ImportInstanceVolumeDetail> getVolumes(){
    final ImportInstanceTaskDetails importTask = this.getTask().getImportInstance();
    return importTask.getVolumes();
  }
  
  public List<String> getSnapshotIds(){
    return this.snapshotIdsCopy;
  }
  
  public void addSnapshotId(final String snapshotId){
    if(this.snapshotIds==null)
      this.snapshotIds = Sets.newHashSet();
    this.snapshotIds.add(snapshotId);
  }
  
  public void setImageId(final String imageId){
    this.imageId = imageId;
  }
  
  public String getImageId(){
    return this.imageId;
  }
  
  @PostLoad
  protected void onLoad(){
    this.snapshotIdsCopy = ImmutableList.copyOf(this.snapshotIds);
    this.groupNamesCopy = ImmutableList.copyOf(this.groupNames);
    super.onLoad();
  }
  
  @Override
  public void cleanUp(){
    if (getCleanUpDone())
      return;
    boolean cleanedAllVolumes = true;
    final ImportInstanceTaskDetails instanceDetails = 
        this.getTask().getImportInstance();
    if(instanceDetails.getVolumes()==null)
      return;
    for(final ImportInstanceVolumeDetail volumeDetail : instanceDetails.getVolumes()){
      if(volumeDetail.getVolume()!=null && volumeDetail.getVolume().getId()!=null){
        try{
          String volumeId = volumeDetail.getVolume().getId();
          Ec2Client.getInstance().deleteTags(null, Lists.newArrayList(volumeId),
              Lists.newArrayList(ImagingTaskStateManager.VOLUME_STATUS_TAG));
          final List<Volume> eucaVolumes =
            Ec2Client.getInstance().describeVolumes(this.getOwnerUserId(), Lists.newArrayList(volumeId));
          if (eucaVolumes.size() != 0) {
            Ec2Client.getInstance().deleteVolume(this.getOwnerUserId(), volumeId);
          }
        } catch(final Exception ex) {
          LOG.warn(String.format("Failed to delete the volume %s for import task %s", 
              volumeDetail.getVolume().getId(), this.getDisplayName()));
          cleanedAllVolumes = false;
        }
      }
    }
    if (cleanedAllVolumes)
      setCleanUpDone(true);
  }
  
  @TypeMapper
  enum InstanceImagingTaskTransform implements Function<ImportInstanceType, ImportInstanceImagingTask> {
    INSTANCE;

    @Nullable
    @Override
    public ImportInstanceImagingTask apply(ImportInstanceType input) {
      final ConversionTask ct = new ConversionTask(); 
      // TODO: do we use the instance id when launched?
      String conversionTaskId = ResourceIdentifiers.generateString("import-i");
      conversionTaskId = conversionTaskId.toLowerCase();
      ct.setConversionTaskId( conversionTaskId );
      ct.setExpirationTime( new Date( Dates.hoursFromNow( Integer.parseInt(ImportTaskProperties.IMPORT_TASK_EXPIRATION_HOURS) ).getTime( ) ).toString( ) );
      ct.setState( ImportTaskState.NEW.getExternalTaskStateName() );
      ct.setStatusMessage( "" );
      
      final ImportInstanceTaskDetails instanceTask = new ImportInstanceTaskDetails();
      instanceTask.setDescription(input.getDescription());
      instanceTask.setPlatform(input.getPlatform());
      final ImportInstanceLaunchSpecification launchSpec = input.getLaunchSpecification();
      final List<ImportInstanceVolumeDetail> volumes = Lists.newArrayList();
      final List<DiskImage> disks = input.getDiskImageSet();
      if(disks!=null){
        for(final DiskImage disk : disks){
          final ImportInstanceVolumeDetail volume = new ImportInstanceVolumeDetail();
          if(launchSpec!=null && launchSpec.getPlacement()!=null)
            volume.setAvailabilityZone(launchSpec.getPlacement().getAvailabilityZone());
          
          volume.setImage(new DiskImageDescription());
          volume.getImage().setFormat(disk.getImage().getFormat());
         
          //TODO: remove this later
          String manifestUrl = disk.getImage().getImportManifestUrl();
          volume.getImage().setImportManifestUrl(manifestUrl);
          volume.getImage().setSize(disk.getImage().getBytes()); // TODO: is this right?
          
          volume.setVolume(new DiskImageVolumeDescription());
          volume.getVolume().setSize(disk.getVolume().getSize());
          volume.setBytesConverted(0L);
          volume.setStatus( ImportTaskState.NEW.getExternalTaskStateName( ) );
          volumes.add(volume);
        }
      }
      
      instanceTask.setVolumes((ArrayList<ImportInstanceVolumeDetail>) volumes);
      ct.setImportInstance(instanceTask);
      
      final ImportInstanceImagingTask newTask = new ImportInstanceImagingTask(Contexts.lookup().getUserFullName(), ct);
      newTask.serializeTaskToJSON();
      if(launchSpec.getArchitecture()==null || launchSpec.getArchitecture().length()<=0)
        newTask.setLaunchSpecArchitecture("i386");
      else
        newTask.setLaunchSpecArchitecture(launchSpec.getArchitecture());
      
      if(launchSpec.getUserData()!=null && launchSpec.getUserData().getData()!=null) //base64 encoded string
        newTask.setLaunchSpecUserData(launchSpec.getUserData().getData());
      newTask.setLaunchSpecInstanceType(launchSpec.getInstanceType());
      if(launchSpec.getPlacement()!=null)
        newTask.setLaunchSpecAvailabilityZone( launchSpec.getPlacement().getAvailabilityZone() );
      if(launchSpec.getMonitoring()!=null)
        newTask.setLaunchSpecMonitoringEnabled( launchSpec.getMonitoring().getEnabled() );
      if(launchSpec.getGroupName()!=null){
        for(final String groupName : launchSpec.getGroupName()){
          newTask.addLaunchSpecGroupName(groupName);
        }
      }
      if(launchSpec.getKeyName()!=null && launchSpec.getKeyName().length()>0){
        newTask.setLaunchSpecKeyName(launchSpec.getKeyName());
      }
      if ( !Strings.isNullOrEmpty( launchSpec.getSubnetId( ) ) ) {
        newTask.setLaunchSpecSubnetId( launchSpec.getSubnetId( ) );
        newTask.setLaunchSpecPrivateIpAddress( launchSpec.getPrivateIpAddress( ) );
      }
      if(launchSpec.getInstanceInitiatedShutdownBehavior()!=null)
        LOG.warn("InitiatedShutdownBehavior is not supported for import-instance");

      return newTask;
    }  
  }
}
