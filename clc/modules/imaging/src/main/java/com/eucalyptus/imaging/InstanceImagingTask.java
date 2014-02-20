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
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Type;

import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Dates;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.DiskImage;
import edu.ucsb.eucalyptus.msgs.DiskImageDescription;
import edu.ucsb.eucalyptus.msgs.DiskImageVolumeDescription;
import edu.ucsb.eucalyptus.msgs.ImportInstanceGroup;
import edu.ucsb.eucalyptus.msgs.ImportInstanceLaunchSpecification;
import edu.ucsb.eucalyptus.msgs.ImportInstanceTaskDetails;
import edu.ucsb.eucalyptus.msgs.ImportInstanceType;
import edu.ucsb.eucalyptus.msgs.ImportInstanceVolumeDetail;

/**
 * @author Sang-Min Park
 *
 */

@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "instance-imaging-task" )
public class InstanceImagingTask extends ImagingTask {
  private static Logger LOG  = Logger.getLogger( InstanceImagingTask.class );

  @Column ( name = "metadata_launchspec_architecture")
  private String architecture;
  
  @ElementCollection
  @CollectionTable( name = "metadata_launchspec_security_groups" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<String> groupNames;
  
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
  
  private InstanceImagingTask(){}
  protected InstanceImagingTask(OwnerFullName ownerFullName,
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
    if(groupNames==null)
      groupNames = Lists.newArrayList();
    return groupNames;
  }
  
  public void addLaunchSpecGroupName(final String groupName){
    try ( final TransactionResource db =
        Entities.transactionFor( InstanceImagingTask.class ) ) {
       final InstanceImagingTask entity = Entities.merge(this);
       entity.getLaunchSpecGroupNames().add(groupName);
       db.commit();
    }
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
  
  public List<ImportInstanceVolumeDetail> getVolumes(){
    final ImportInstanceTaskDetails importTask = this.getTask().getImportInstance();
    return importTask.getVolumes();
  }
  
  @TypeMapper
  enum InstanceImagingTaskTransform implements Function<ImportInstanceType, InstanceImagingTask> {
    INSTANCE;

    @Nullable
    @Override
    public InstanceImagingTask apply(ImportInstanceType input) {
      final ConversionTask ct = new ConversionTask(); 
      // TODO: do we use the instance id when launched?
      String conversionTaskId = ResourceIdentifiers.generateString("import-i");
      conversionTaskId = conversionTaskId.toLowerCase();
      ct.setConversionTaskId( conversionTaskId );
      ct.setExpirationTime( new Date( Dates.daysFromNow( 30 ).getTime( ) ).toString( ) );
      ct.setState( ImportTaskState.NEW.getExternalVolumeStatusMessage( ) );
      ct.setStatusMessage( ImportTaskState.NEW.getExternalVolumeStatusMessage( ) );
      
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
          volume.getImage().setImportManifestUrl(disk.getImage().getImportManifestUrl());
          volume.getImage().setSize(disk.getImage().getBytes()); // TODO: is this right?
          
          volume.setVolume(new DiskImageVolumeDescription());
          volume.getVolume().setSize(disk.getVolume().getSize());
          volumes.add(volume);
        }
      }
      
      instanceTask.setVolumes((ArrayList<ImportInstanceVolumeDetail>) volumes);
      ct.setImportInstance(instanceTask);
      
      final InstanceImagingTask newTask = new InstanceImagingTask(Contexts.lookup().getUserFullName(), ct);
      newTask.serializeTaskToJSON();
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
      
      return newTask;
    }  
  }
}
