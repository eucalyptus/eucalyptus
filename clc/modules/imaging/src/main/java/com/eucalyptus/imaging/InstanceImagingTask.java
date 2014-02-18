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

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Type;

import com.eucalyptus.util.OwnerFullName;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ConversionTask;

/**
 * @author Sang-Min Park
 *
 */

@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "instance-imaging-task" )
public class InstanceImagingTask extends ImagingTask {

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
  
  @ElementCollection
  @CollectionTable( name = "metadata_import_instance_download_manifest_url" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )  
  private List<ImportToDownloadManifestUrl> downloadManifestUrl;
  
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
  
  public void addLaunchSpecGroupName(final String groupName){
    if(groupNames==null)
      groupNames = Lists.newArrayList();
    groupNames.add(groupName);
  }
  
  public List<String> getLaunchSpecGroupNames(){
    return this.groupNames;
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
  
  public void addDownloadManifestUrl(final String importManifestUrl, final String downloadManifestUrl) {
    if(this.downloadManifestUrl == null)
      this.downloadManifestUrl = Lists.newArrayList();
    final ImportToDownloadManifestUrl mapping = 
        new ImportToDownloadManifestUrl(importManifestUrl, downloadManifestUrl);
    mapping.setParentTask(this);
    this.downloadManifestUrl.add(mapping);
  }
  
  public List<ImportToDownloadManifestUrl> getDownloadManifestUrl(){
    return this.downloadManifestUrl;
  }
  
  @Embeddable
  public static class ImportToDownloadManifestUrl {
    @Parent
    InstanceImagingTask parentTask;
    
    @Column (name = "metadata_import_manifest_url")
    private String importManifestUrl;
    
    @Column (name = "metadata_download_manifest_url")
    private String downloadManifestUrl;
    
    public ImportToDownloadManifestUrl(final String importManifestUrl, final String downloadManifestUrl){
      this.importManifestUrl = importManifestUrl;
      this.downloadManifestUrl = downloadManifestUrl;
    }
    
    public String getImportManifestUrl(){
      return this.importManifestUrl;
    }
    
    public String getDownloadManifestUrl(){
      return this.downloadManifestUrl;
    }
    
    public void setParentTask(final InstanceImagingTask task){
      this.parentTask = task;
    }
    
    public InstanceImagingTask getParentTask(){
      return this.parentTask;
    }
  }
}
