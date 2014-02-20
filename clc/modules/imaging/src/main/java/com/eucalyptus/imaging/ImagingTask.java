/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.imaging;

import java.util.List;

import javax.persistence.*;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.groovy.JsonSlurper;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Type;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ConversionTask;

/**
 * Entity base-class for the persisted state associated with the progress in execution of an image
 * conversion task.
 * 
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Table( name = "metadata_imaging_tasks" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "metadata_imaging_tasks_discriminator",
                      discriminatorType = DiscriminatorType.STRING )
@DiscriminatorValue( value = "metadata_imaging_task" )
public class ImagingTask extends UserMetadata<ImportTaskState> implements ImagingMetadata.ImagingTaskMetadata {
  private static Logger LOG  = Logger.getLogger( ImagingTask.class );

  @Transient
  // save task in JSON
  private ConversionTask task;
  @Column( name = "metadata_bytes_processed" )
  private final Long     bytesProcessed;
  @Type( type = "org.hibernate.type.StringClobType" )
  @Lob
  // so there is not need to worry about length of the JSON
  @Column( name = "metadata_task_in_json" )
  private String         taskInJSON;
  
  @Column( name = "metadata_state_message")
  private String  stateReason;
  
  @ElementCollection( fetch = FetchType.EAGER )
  @CollectionTable( name = "metadata_import_instance_download_manifest_url" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )  
  private List<ImportToDownloadManifestUrl> downloadManifestUrl;
  

  protected ImagingTask( ) {
    this(null,null);
    task = null;
  }
  
  ImagingTask( String displayName ) {
    this( null, displayName );
  }
  
  ImagingTask( OwnerFullName owner, String displayName ) {
    super( owner, displayName );
    this.bytesProcessed = null;
  }

  static ImagingTask named(final OwnerFullName owner, final String taskId){
    return new ImagingTask(owner,  taskId);
  }
  
  static ImagingTask named(final OwnerFullName owner){
    return new ImagingTask(owner, null);
  }
  
  static ImagingTask named(final ImportTaskState state) {
    final ImagingTask task = new ImagingTask(null,null);
    task.setState(state);
    return task;
  }
  
  static ImagingTask named(final String taskId){
    return new ImagingTask(null, taskId);
  }
  
  static ImagingTask named(){
    return new ImagingTask();
  }
  
  
  protected ImagingTask( OwnerFullName ownerFullName, ConversionTask task, ImportTaskState state, long bytesProcessed ) {
    super( ownerFullName, task.getConversionTaskId() );
    this.task = task;
    this.setState( state );
    this.bytesProcessed = bytesProcessed;
  }
  
  public ConversionTask getTask( ) {
    return task;
  }
  
  public long getBytesProcessed( ) {
    return bytesProcessed;
  }
  
  @PrePersist
  /* package */void serializeTaskToJSON( ) {
    if ( this.task != null ) {
      taskInJSON = ( JSONSerializer.toJSON( task.toJSON( ) ) ).toString( );
    }
  }
  
  @PostLoad
  public void createTaskFromJSON( ) {
    // recreate task from JSON string
    if ( this.taskInJSON != null ) {
      JsonSlurper jsonSlurper = new JsonSlurper( );
      JSONObject taskObject = ( JSONObject ) jsonSlurper.parseText( this.taskInJSON );
      this.task = new ConversionTask( taskObject );
    }
  }
  
  public String getTaskInJsons( ) {
    return taskInJSON;
  }
  
  public void setTaskInJsons(final String taskInJson){
    this.taskInJSON= taskInJson;
  }
  
  public void setStateReason(final String reason){
    this.stateReason = reason;
  }
  
  public String getStateReason(){
    return this.stateReason;
  }
  

  public List<ImportToDownloadManifestUrl> getDownloadManifestUrl(){
    if(this.downloadManifestUrl == null)
      this.downloadManifestUrl = Lists.newArrayList();
  
    return this.downloadManifestUrl;
  }
  
  public void addDownloadManifestUrl(final String importManifestUrl, final String downloadManifestUrl) {
    final ImportToDownloadManifestUrl mapping = 
        new ImportToDownloadManifestUrl(importManifestUrl, downloadManifestUrl);
    mapping.setParentTask(this);
    
    try ( final TransactionResource db =
        Entities.transactionFor( ImagingTask.class ) ) {
       final ImagingTask entity = Entities.merge(this);
       entity.getDownloadManifestUrl().add(mapping);
       db.commit();
    }
  }
  
  public boolean hasDownloadManifestUrl(final String importManifestUrl){
    if(this.downloadManifestUrl == null)
      return false;
    for(final ImportToDownloadManifestUrl mapping : this.downloadManifestUrl){
      if(importManifestUrl.equals(mapping.getImportManifestUrl()))
        return true;
    }
    return false;
  }
  
  @Override
  public String getPartition( ) {
    return null;
  }
  
  @Override
  public FullName getFullName( ) {
    return null;
  }
  
  @Embeddable
  public static class ImportToDownloadManifestUrl {
    @Parent
    ImagingTask parentTask;
    
    @Column (name = "metadata_import_manifest_url", length=4096)
    private String importManifestUrl;
    
    @Column (name = "metadata_download_manifest_url", length=4096)
    private String downloadManifestUrl;
    
    protected ImportToDownloadManifestUrl() {}
    
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
    
    public void setParentTask(final ImagingTask task){
      this.parentTask = task;
    }
    
    public ImagingTask getParentTask(){
      return this.parentTask;
    }
  }
}
