package com.eucalyptus.imaging;

import java.util.Date;

import javax.annotation.Nullable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.imaging.worker.EucalyptusActivityTasks;
import com.eucalyptus.imaging.worker.ImagingServiceProperties;
import com.eucalyptus.util.Dates;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;

import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.DiskImageDescription;
import edu.ucsb.eucalyptus.msgs.DiskImageDetail;
import edu.ucsb.eucalyptus.msgs.DiskImageVolume;
import edu.ucsb.eucalyptus.msgs.DiskImageVolumeDescription;
import edu.ucsb.eucalyptus.msgs.ImportVolumeTaskDetails;
import edu.ucsb.eucalyptus.msgs.ImportVolumeType;

@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
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
  public void cleanUp(){
    final ImportVolumeTaskDetails volumeDetails = 
        this.getTask().getImportVolume();
    if(volumeDetails.getVolume()!=null &&
        volumeDetails.getVolume().getId()!=null){
      try{
        EucalyptusActivityTasks.getInstance().deleteVolumeAsUser(this.getOwnerUserId(), volumeDetails.getVolume().getId());
      }catch(final Exception ex){
        LOG.warn(String.format("Failed to delete the volume %s for import task %s", volumeDetails.getVolume().getId(), this.getDisplayName()));
      }
    }
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
      conversionTask.setExpirationTime( new Date( Dates.hoursFromNow( Integer.parseInt(ImagingServiceProperties.IMPORT_TASK_EXPIRATION_HOURS) ).getTime( ) ).toString( ) );
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
