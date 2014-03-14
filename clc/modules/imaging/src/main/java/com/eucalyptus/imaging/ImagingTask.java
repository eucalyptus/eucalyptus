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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.*;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.groovy.JsonSlurper;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

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
  
  @Transient
  private ImagingTaskRelationView view;
  
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

  @Override
  public String toString() {
    return getClass().getName() + "[name: " + displayName + ", state: " + getState() + "]";
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
  
  private void createTaskFromJSON( ) {
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
  

  public List<ImportToDownloadManifestUrlCoreView> getDownloadManifestUrl(){
    return this.view.getDownloadManifstUrls();
  }
  
  public void addDownloadManifestUrl(final String importManifestUrl, final String downloadManifestUrl) {
    final ImportToDownloadManifestUrl mapping = 
        new ImportToDownloadManifestUrl(importManifestUrl, downloadManifestUrl);
    this.downloadManifestUrl.add(mapping);
  }
  
  public boolean hasDownloadManifestUrl(final String importManifestUrl){
    for(final ImportToDownloadManifestUrlCoreView mapping : this.view.getDownloadManifstUrls()){
      if(importManifestUrl.equals(mapping.getImportManifestUrl()))
        return true;
    }
    return false;
  }
  
  public String getDownloadManifestUrl(final String importManifestUrl){
    for(final ImportToDownloadManifestUrlCoreView mapping : this.view.getDownloadManifstUrls()){
      if(importManifestUrl.equals(mapping.getImportManifestUrl()))
        return mapping.getDownloadManifestUrl();
    }
    return null;
  }
  
  public Date getExpirationTime(){
    try{
      return (new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")).parse(this.task.getExpirationTime());
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }
  
  public void cleanUp() {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public String getPartition( ) {
    return null;
  }
  
  @Override
  public FullName getFullName( ) {
    return null;
  }
  

  @PostLoad
  protected void onLoad(){
    if(this.view==null)
      this.view = new ImagingTaskRelationView(this);
    createTaskFromJSON();
  }
  
  @Embeddable
  public static class ImportToDownloadManifestUrl {
    @Column (name = "metadata_import_manifest_url", length=4096)
    private String importManifestUrl;
    
    @Column (name = "metadata_download_manifest_url", length=4096)
    private String downloadManifestUrl;
    
    private ImportToDownloadManifestUrl(){}
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
  }
  
  public static class ImagingTaskRelationView{
    private ImagingTask imagingTask = null;
    private ImmutableList<ImportToDownloadManifestUrlCoreView> downloadManifestUrls = null;
    ImagingTaskRelationView(final ImagingTask task){
      this.imagingTask = task;
      if(task.downloadManifestUrl != null)
        this.downloadManifestUrls = ImmutableList.copyOf(Collections2.transform(task.downloadManifestUrl ,
            ImportToDownloadManifestUrlCoreViewTransform.INSTANCE));
    }
    
    public ImmutableList<ImportToDownloadManifestUrlCoreView> getDownloadManifstUrls(){
      return this.downloadManifestUrls;
    }
  }
  
  public enum ImportToDownloadManifestUrlCoreViewTransform implements Function<ImportToDownloadManifestUrl, ImportToDownloadManifestUrlCoreView>{
    INSTANCE;

    @Override
    public ImportToDownloadManifestUrlCoreView apply(
        ImportToDownloadManifestUrl arg0) {
      return new ImportToDownloadManifestUrlCoreView(arg0);
    }
  }
  
  public static class ImportToDownloadManifestUrlCoreView {
    private ImportToDownloadManifestUrl sourceObj = null;
    public ImportToDownloadManifestUrlCoreView(final ImportToDownloadManifestUrl source){
      sourceObj = source;
    }

    public String getImportManifestUrl(){
      return sourceObj.importManifestUrl;
    }
    
    public String getDownloadManifestUrl(){
      return sourceObj.downloadManifestUrl;
    }
  }
}
