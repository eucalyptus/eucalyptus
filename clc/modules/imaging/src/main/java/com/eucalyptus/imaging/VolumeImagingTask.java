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

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "metadata_imaging_task" )
public class VolumeImagingTask extends ImagingTask {
  private static Logger LOG  = Logger.getLogger( VolumeImagingTask.class );

  @Transient
  private ConversionTask task;
  
  @Transient
  private ImagingTaskRelationView view;
  
  @Column( name = "metadata_bytes_processed" )
  private final Long     bytesProcessed;
  
  @ElementCollection( fetch = FetchType.EAGER )
  @CollectionTable( name = "metadata_import_instance_download_manifest_url" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )  
  private List<ImportToDownloadManifestUrl> downloadManifestUrl;
  
  protected VolumeImagingTask( ) {
    this(null,null);
    task = null;
  }
  
  VolumeImagingTask( String displayName ) {
    this( null, displayName );
  }
  
  VolumeImagingTask( OwnerFullName owner, String displayName ) {
    super( owner, displayName );
    this.bytesProcessed = null;
  }

  protected VolumeImagingTask( OwnerFullName ownerFullName, ConversionTask task, ImportTaskState state, long bytesProcessed ) {
    super( ownerFullName, task.getConversionTaskId() );
    this.task = task;
    this.setState( state );
    this.bytesProcessed = bytesProcessed;
  }
  
  static VolumeImagingTask named(final OwnerFullName owner, final String taskId){
    return new VolumeImagingTask(owner,  taskId);
  }
  
  static VolumeImagingTask named(final OwnerFullName owner){
    return new VolumeImagingTask(owner, null);
  }
  
  static VolumeImagingTask named(final ImportTaskState state) {
    final VolumeImagingTask task = new VolumeImagingTask(null,null);
    task.setState(state);
    return task;
  }
  
  static VolumeImagingTask named(final String taskId){
    return new VolumeImagingTask(null, taskId);
  }
  
  static VolumeImagingTask named(){
    return new VolumeImagingTask();
  }
  
  public ConversionTask getTask(){
    return task;
  }
  
  @Override
  public String toString() {
    return getClass().getName() + "[name: " + displayName + ", state: " + getState() + "]";
  }
  
  public long getBytesProcessed( ) {
    return bytesProcessed;
  }
  
  @Override
  protected void createTaskFromJSON( ) {
    // recreate task from JSON string
    if ( this.taskInJSON != null ) {
      JsonSlurper jsonSlurper = new JsonSlurper( );
      JSONObject taskObject = ( JSONObject ) jsonSlurper.parseText( this.taskInJSON );
      this.task = new ConversionTask( taskObject );
    }
  }
  @Override
  public String getTaskExpirationTime() {
    return this.task.getExpirationTime();
  }

  @Override
  public JSONObject toJSON() {
    return this.task.toJSON();
  }

  @Override
  public void setTaskState(String state) {
    this.task.setState(state);    
  }

  @Override
  public String getTaskState() {
    return this.task.getState();
  }
  
  @Override
  public void setTaskStatusMessage(String msg){
    this.task.setStatusMessage(msg);  
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
  
  @Override
  @PostLoad
  protected void onLoad(){
    super.onLoad();
    if(this.view==null)
      this.view = new ImagingTaskRelationView(this);
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
    private VolumeImagingTask imagingTask = null;
    private ImmutableList<ImportToDownloadManifestUrlCoreView> downloadManifestUrls = null;
    ImagingTaskRelationView(final VolumeImagingTask task){
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
