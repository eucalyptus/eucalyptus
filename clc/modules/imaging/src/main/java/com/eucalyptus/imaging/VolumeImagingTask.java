package com.eucalyptus.imaging;

import java.util.Date;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Crypto;
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
public class VolumeImagingTask extends ImagingTask {
  
  @Column( name = "metadata_volume_id" )
  private String  volumeId;
  
  @Column( name = "metadata_volume_size" )
  private Integer volumeSize;
  
  @Column( name = "metadata_volume_az" )
  private String  availabilityZone;
  
  @Column( name = "metadata_volume_format" )
  private String  format;
  
  @Column( name = "metadata_volume_import_bytes" )
  private Long    bytes;
  
  @Column( name = "metadata_volume_importManifestUrl" )
  private String  importManifestUrl;
  
  @Column ( name = "metadata_download_menifest_url")
  private String downloadManifestUrl;
  
  @Column( name = "metadata_volume_description" )
  private String  description;
  
  private VolumeImagingTask( ) {}
  
  protected VolumeImagingTask(final OwnerFullName owner, final String taskId){
    super(owner, taskId);
  }
  
  protected VolumeImagingTask( OwnerFullName ownerFullName, ConversionTask conversionTask, Integer volumeSize, String availabilityZone,
                             String format, Long bytes, String importManifestUrl, String description ) {
    super( ownerFullName, conversionTask.getConversionTaskId( ), conversionTask, ImportTaskState.NEW, 0L );
    this.volumeId = null;
    this.volumeSize = volumeSize;
    this.availabilityZone = availabilityZone;
    this.format = format;
    this.bytes = bytes;
    this.importManifestUrl = importManifestUrl;
    this.description = description;
  }
  
  public static VolumeImagingTask create( final OwnerFullName ownerFullName,
                                          final ConversionTask conversionTask,
                                          Integer volumeSize,
                                          String availabilityZone,
                                          String format,
                                          Long bytes,
                                          String importManifestUrl,
                                          String description ) {
    return new VolumeImagingTask( ownerFullName,
                                  conversionTask,
                                  volumeSize,
                                  availabilityZone,
                                  format,
                                  bytes,
                                  importManifestUrl,
                                  description );
  }
  
  public static VolumeImagingTask named(final OwnerFullName owner, final String taskId){
    return new VolumeImagingTask(owner, taskId);
  }
  
  public String getAvailabilityZone( ) {
    return availabilityZone;
  }
  
  public String getVolumeId( ) {
    return volumeId;
  }
  
  public Integer getVolumeSize( ) {
    return volumeSize;
  }
  
  public void setVolumeId( String volumeId ) {
    this.getTask( ).getImportVolume( ).getVolume( ).setId( volumeId );
    this.volumeId = volumeId;
  }
  
  private void setVolumeSize( Integer volumeSize ) {
    this.volumeSize = volumeSize;
  }
  
  private void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }
  
  public String getFormat( ) {
    return format;
  }
  
  public void setFormat( String format ) {
    this.format = format;
  }
  
  public Long getBytes( ) {
    return bytes;
  }
  
  public void setBytes( Long bytes ) {
    this.bytes = bytes;
  }
  
  public String getImportManifestUrl( ) {
    return importManifestUrl;
  }
  
  public void setImportManifestUrl( String importManifestUrl ) {
    this.importManifestUrl = importManifestUrl;
  }
  
  public String getDescription( ) {
    return description;
  }
  
  public void setDescription( String description ) {
    this.description = description;
  }
  
  public String getDownloadManifestUrl(){
    return this.downloadManifestUrl;
  }
  
  public void setDownloadManifestUrl(final String url){
    this.downloadManifestUrl = url;
  }
  
  @TypeMapper
  enum VolumeImagingTaskTransform implements Function<ImportVolumeType, VolumeImagingTask> {
    INSTANCE;
    @Nullable
    @Override
    public VolumeImagingTask apply( @Nullable final ImportVolumeType request ) {
      final DiskImageDetail imageDetails = request.getImage( );
      final DiskImageVolume volumeDetails = request.getVolume( );
      Context ctx = Contexts.lookup( );
      String conversionTaskId = ResourceIdentifiers.generateString("import-vol");
      conversionTaskId = conversionTaskId.toLowerCase();
      ConversionTask conversionTask = new ConversionTask( );
      conversionTask.setConversionTaskId( conversionTaskId );
      conversionTask.setExpirationTime( new Date( Dates.daysFromNow( 30 ).getTime( ) ).toString( ) );
      conversionTask.setState( ImportTaskState.NEW.getExternalVolumeStatusMessage( ) );
      conversionTask.setStatusMessage( ImportTaskState.NEW.getExternalVolumeStatusMessage( ) );
//    conversionTask.setResourceTagSet( request.get );//GRZE:TODO: fill this in, where the hell does it come from?
      final DiskImageVolumeDescription volumeImageDescription = new DiskImageVolumeDescription( ) {
        {
          this.setSize( volumeDetails.getSize( ) );
        }
      };
      final DiskImageDescription diskImageDescription = new DiskImageDescription( ) {
        {
          this.setImportManifestUrl( imageDetails.getImportManifestUrl( ) );
          this.setFormat( imageDetails.getFormat( ) );
          this.setSize( imageDetails.getBytes( ) );
        }
      };
      ImportVolumeTaskDetails volumeTaskDetails = new ImportVolumeTaskDetails( ) {
        {
          this.setAvailabilityZone( request.getAvailabilityZone( ) );
          this.setBytesConverted( 0L );
          this.setDescription( request.getDescription( ) );
          this.setImage( diskImageDescription );
        }
      };
      volumeTaskDetails.setImage( diskImageDescription );
      volumeTaskDetails.setVolume( volumeImageDescription );
      conversionTask.setImportVolume( volumeTaskDetails );
      return create( ctx.getUserFullName( ),
                     conversionTask,
                     volumeDetails.getSize( ),
                     request.getAvailabilityZone( ),
                     imageDetails.getFormat( ),
                     imageDetails.getBytes( ),
                     imageDetails.getImportManifestUrl( ),
                     request.getDescription( ) );
    }
  }
}
