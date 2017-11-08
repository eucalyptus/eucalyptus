/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.imaging.backend;

import java.util.List;
import java.util.NoSuchElementException;

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
import com.eucalyptus.compute.common.ConversionTask;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

/**
 * Entity base-class for the persisted state associated with the progress in execution of an image
 * conversion task.
 * 
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@DiscriminatorValue( value = "metadata_volume_imaging_task" )
public class VolumeImagingTask extends ImagingTask {
  private static Logger LOG  = Logger.getLogger( VolumeImagingTask.class );

  @Transient
  private ConversionTask task;
  
  @Transient
  private ImagingTaskRelationView view;
  
  @Column( name = "metadata_bytes_processed" )
  private final Long     bytesProcessed;
  
  @Column( name = "metadata_cleaned_up" )
  private Boolean  cleanUpDone;

  @ElementCollection( fetch = FetchType.EAGER )
  @CollectionTable( name = "metadata_import_instance_download_manifest_url" )
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
    this.cleanUpDone = false;
  }

  protected VolumeImagingTask( OwnerFullName ownerFullName, ConversionTask task, ImportTaskState state, long bytesProcessed ) {
    super( ownerFullName, task.getConversionTaskId() );
    this.task = task;
    this.setState( state );
    this.bytesProcessed = bytesProcessed;
    this.cleanUpDone = false;
  }
  
  static VolumeImagingTask named(final OwnerFullName owner, final String taskId){
    return new VolumeImagingTask(owner,  taskId);
  }
  
  static VolumeImagingTask namedByAccount(final String accountNumber){
    final VolumeImagingTask task = new VolumeImagingTask();
    task.setOwnerAccountNumber(accountNumber);
    return task;
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
  
  protected Boolean getCleanUpDone() {
    return cleanUpDone;
  }

  protected void setCleanUpDone(Boolean cleanUpDone) {
    this.cleanUpDone = cleanUpDone;
    try ( final TransactionResource db =
        Entities.transactionFor(VolumeImagingTask.class ) ) {
      LOG.debug("Setting clean up flag to " + cleanUpDone + " for task " + task.getConversionTaskId());
      try {
        VolumeImagingTask entity = Entities.uniqueResult(VolumeImagingTask.named(task.getConversionTaskId()));
        entity.cleanUpDone = cleanUpDone;
        Entities.persist(entity);
      } catch (Exception e) {
        LOG.error(e);
      }
      db.commit();
    }
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
  
  public void clearDownloadManifesturl(){
    this.downloadManifestUrl.clear();
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
