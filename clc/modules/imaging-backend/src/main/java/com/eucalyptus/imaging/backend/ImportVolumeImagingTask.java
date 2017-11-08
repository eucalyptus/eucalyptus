/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.compute.common.ConversionTask;
import com.eucalyptus.compute.common.DiskImageDescription;
import com.eucalyptus.compute.common.DiskImageDetail;
import com.eucalyptus.compute.common.DiskImageVolume;
import com.eucalyptus.compute.common.DiskImageVolumeDescription;
import com.eucalyptus.compute.common.ImportVolumeTaskDetails;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.compute.common.backend.ImportVolumeType;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.imaging.ImportTaskProperties;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.util.Dates;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@DiscriminatorValue( value = "volume-imaging-task" )
public class ImportVolumeImagingTask extends VolumeImagingTask {
  private static Logger LOG  = Logger.getLogger( ImportVolumeImagingTask.class );
  
  protected ImportVolumeImagingTask( ) {}
  
  protected ImportVolumeImagingTask(final OwnerFullName owner, final String taskId){
    super(owner, taskId);
  }
  
  protected ImportVolumeImagingTask( OwnerFullName ownerFullName, ConversionTask conversionTask) {
    super( ownerFullName, conversionTask, ImportTaskState.NEW, 0L );
  }
  
  public static ImportVolumeImagingTask create( final OwnerFullName ownerFullName,
                                          final ConversionTask conversionTask ) {
    return new ImportVolumeImagingTask( ownerFullName, conversionTask);
  }
  
  public static ImportVolumeImagingTask named(final OwnerFullName owner, final String taskId){
    return new ImportVolumeImagingTask(owner, taskId);
  }
  
  public String getAvailabilityZone( ) {
    try{
      return this.getTask().getImportVolume().getAvailabilityZone();
    }catch(final Exception ex){
      return null;
    }
  }
  
  public String getVolumeId( ) {
    try{
      return this.getTask().getImportVolume().getVolume().getId();
    }catch(final Exception ex){
      return null;
    }
  }
  
  public Integer getVolumeSize( ) {
    try{
      return this.getTask().getImportVolume().getVolume().getSize();
    }catch(final Exception ex){
      return null;
    }
  }
  
  public void setVolumeId( String volumeId ) {
    this.getTask().getImportVolume( ).getVolume( ).setId( volumeId );
    this.serializeTaskToJSON();
  }
  
  private void setVolumeSize( Integer volumeSize ) {
    this.getTask().getImportVolume().getVolume().setSize(volumeSize);
    this.serializeTaskToJSON();
  }
  
  private void setAvailabilityZone( String availabilityZone ) {
    this.getTask().getImportVolume().setAvailabilityZone(availabilityZone);
    this.serializeTaskToJSON();
  }
  
  public String getFormat( ) {
    try{
      return this.getTask().getImportVolume().getImage().getFormat();
    }catch(final Exception ex){
      return null;
    }
  }
  
  public void setFormat( String format ) {
    this.getTask().getImportVolume().getImage().setFormat(format);
    this.serializeTaskToJSON();
  }
  
  public Long getBytes( ) {
    try{
      return this.getTask().getImportVolume().getBytesConverted();
    }catch(final Exception ex){
      return null;
    }
  }
  
  public void setBytes( Long bytes ) {
    this.getTask().getImportVolume().setBytesConverted(bytes);
    this.serializeTaskToJSON();
  }
  
  public String getImportManifestUrl( ) {
    try{
      return this.getTask().getImportVolume().getImage().getImportManifestUrl();
    }catch(final Exception ex){
      return null;
    }
  }
  
  public void setImportManifestUrl( String importManifestUrl ) {
    this.getTask().getImportVolume().getImage().setImportManifestUrl(importManifestUrl);
    this.serializeTaskToJSON();
  }
  
  public String getDescription( ) {
    try{
      return this.getTask().getImportVolume().getDescription();
    }catch(final Exception ex){
      return null;
    }
  }
  
  public void setDescription( String description ) {
    this.getTask().getImportVolume().setDescription(description);
    this.serializeTaskToJSON();
  }
  
  @Override
  public boolean cleanUp(){
    if (getCleanUpDone())
      return true;
    final ImportVolumeTaskDetails volumeDetails =
        this.getTask().getImportVolume();
    if(volumeDetails.getVolume()!=null &&
        volumeDetails.getVolume().getId()!=null){
      String volumeId = volumeDetails.getVolume().getId();
      try{
        // verify that volume actually exist
        Volumes.setSystemManagedFlag(null, volumeId, false);
        final List<Volume> eucaVolumes =
            Ec2Client.getInstance().describeVolumes(this.getOwnerUserId(), Lists.newArrayList(volumeId));
        if (eucaVolumes.size() != 0) {
          Ec2Client.getInstance().deleteVolume(this.getOwnerUserId(), volumeId);
        }
        setCleanUpDone(true);
      } catch(final NoSuchElementException ex) {
        LOG.debug("Can't find volume with ID " + volumeId + ". Probably it was already deleted");
        setCleanUpDone(true);
      } catch(final Exception ex){
        LOG.error(ex);
        LOG.warn(String.format("Failed to delete the volume %s for import task %s",
            volumeDetails.getVolume().getId(), this.getDisplayName()));
      }
    }
    return true;
  }
  
  @TypeMapper
  enum VolumeImagingTaskTransform implements Function<ImportVolumeType, ImportVolumeImagingTask> {
    INSTANCE;
    @Nullable
    @Override
    public ImportVolumeImagingTask apply( @Nullable final ImportVolumeType request ) {
      final DiskImageDetail imageDetails = request.getImage( );
      final DiskImageVolume volumeDetails = request.getVolume( );
      Context ctx = Contexts.lookup( );
      String conversionTaskId = ResourceIdentifiers.generateString("import-vol");
      conversionTaskId = conversionTaskId.toLowerCase();
      ConversionTask conversionTask = new ConversionTask( );
      conversionTask.setConversionTaskId( conversionTaskId );
      conversionTask.setExpirationTime( new Date( Dates.hoursFromNow( Integer.parseInt(ImportTaskProperties.IMPORT_TASK_EXPIRATION_HOURS) ).getTime( ) ).toString( ) );
      conversionTask.setState( ImportTaskState.NEW.getExternalTaskStateName() );
      conversionTask.setStatusMessage( "" );
//    conversionTask.setResourceTagSet( request.get );//GRZE:TODO: fill this in, where the hell does it come from?
      final DiskImageVolumeDescription volumeImageDescription = new DiskImageVolumeDescription( ) {
        {
          this.setSize( volumeDetails.getSize( ) );
        }
      };
      final DiskImageDescription diskImageDescription = new DiskImageDescription( ) {
        {
          ///TODO: remove this later
          String manifestUrl = imageDetails.getImportManifestUrl( );
          this.setImportManifestUrl( manifestUrl );
          this.setFormat( imageDetails.getFormat( ) );
          this.setSize( imageDetails.getBytes( ) );
        }
      };
      ImportVolumeTaskDetails volumeTaskDetails = new ImportVolumeTaskDetails( ) {
        {
          this.setAvailabilityZone( request.getAvailabilityZone( ) );
          this.setBytesConverted( 0L );
          this.setDescription( request.getDescription( ) );
        }
      };
      volumeTaskDetails.setImage( diskImageDescription );
      volumeTaskDetails.setVolume( volumeImageDescription );
      conversionTask.setImportVolume( volumeTaskDetails );
      return create( ctx.getUserFullName( ),
                     conversionTask);
    }
  }
}
