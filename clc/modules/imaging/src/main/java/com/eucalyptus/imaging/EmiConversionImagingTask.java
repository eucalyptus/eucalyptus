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

import java.util.Date;
import java.util.List;

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
import com.eucalyptus.imaging.worker.EucalyptusActivityTasks;
import com.eucalyptus.imaging.worker.ImagingServiceProperties;
import com.eucalyptus.util.Dates;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.DiskImageDescription;
import edu.ucsb.eucalyptus.msgs.DiskImageDetail;
import edu.ucsb.eucalyptus.msgs.DiskImageVolume;
import edu.ucsb.eucalyptus.msgs.DiskImageVolumeDescription;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.ImportVolumeTaskDetails;
import edu.ucsb.eucalyptus.msgs.ImportVolumeType;

/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "emi-imaging-task" )
public class EmiConversionImagingTask extends ImportVolumeImagingTask {

  protected EmiConversionImagingTask(){}
  protected EmiConversionImagingTask(OwnerFullName ownerFullName,
      ConversionTask conversionTask) {
    super(ownerFullName, conversionTask);
  }
  protected EmiConversionImagingTask(final OwnerFullName owner, final String taskId){
    super(owner, taskId);
  }
  
  public static EmiConversionImagingTask create( final OwnerFullName ownerFullName,
                                          final ConversionTask conversionTask ) {
    return new EmiConversionImagingTask( ownerFullName, conversionTask);
  }
  
  public static EmiConversionImagingTask named(final OwnerFullName owner, final String taskId){
    return new EmiConversionImagingTask(owner, taskId);
  }
  

  @Column ( name = "metadata_kernel_manifest_url")
  private String kernelManifestUrl;
  
  @Column ( name = "metadata_ramdisk_manifest_url")
  private String ramdiskManifestUrl;
  
  public void setKernelManifestUrl(final String kernelManifestUrl){
    this.kernelManifestUrl = kernelManifestUrl;
  }
  
  public String getKernelManifestUrl(){
    return this.kernelManifestUrl;
  }
  
  public void setRamdiskManifestUrl(final String ramdiskManifestUrl){
    this.ramdiskManifestUrl = ramdiskManifestUrl;
  }
  
  public String getRamdiskManifestUrl(){
    return this.ramdiskManifestUrl;
  }

  @TypeMapper
  enum EMIConversionImagingTaskTransform implements Function<ImportVolumeType, EmiConversionImagingTask> {
    INSTANCE;
    @Nullable
    @Override
    public EmiConversionImagingTask apply( @Nullable final ImportVolumeType request ) {
      Context ctx = Contexts.lookup( );
      String conversionTaskId = ResourceIdentifiers.generateString("import-emi");

      final DiskImageDetail imageDetails = request.getImage( );
      final DiskImageVolume volumeDetails = request.getVolume( );
      conversionTaskId = conversionTaskId.toLowerCase();
      ConversionTask conversionTask = new ConversionTask( );
      conversionTask.setConversionTaskId( conversionTaskId );
      conversionTask.setExpirationTime( new Date( Dates.hoursFromNow( 
          Integer.parseInt(ImagingServiceProperties.IMPORT_TASK_EXPIRATION_HOURS) ).getTime( ) ).toString( ) );
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
      EmiConversionImagingTask task = create( ctx.getUserFullName( ), conversionTask); 
      
      try{
        // look for EMI that has the imageLocation=manifestUrl
        String kernelId = null;
        String ramdiskId = null;
        String emiLocation = null;
        final List<ImageDetails> allEmis = 
            EucalyptusActivityTasks.getInstance().describeImages(Lists.<String>newArrayList(), true);
        for(final ImageDetails emi : allEmis){
          if(imageDetails.getImportManifestUrl().endsWith(emi.getImageLocation())){
            if("hvm".equals(emi.getVirtualizationType())){
              throw new ImagingServiceException("The requested image is hvm type; only paravirtualized format can be converted");
            }
            emiLocation = emi.getImageLocation();
            kernelId = emi.getKernelId();
            ramdiskId = emi.getRamdiskId();
          }
        }
        
        if(kernelId==null || ramdiskId==null)
          throw new Exception("Kernel and ramdisk ID are not found");
        // find the associated kernel and ramdisk
        
        final List<ImageDetails> ekiList = 
            EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(kernelId), true);
        final List<ImageDetails> eriList=
            EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(ramdiskId), true);
        if(ekiList==null || ekiList.size()<=0)
          throw new Exception("kernel "+kernelId+" is not found");
        if(eriList==null || eriList.size()<=0)
          throw new Exception("ramdisk "+ramdiskId+" is not found");
        final String ekiLocation =
            ekiList.get(0).getImageLocation();
        final String eriLocation =
            eriList.get(0).getImageLocation();
        
        final String servicePrefix = imageDetails.getImportManifestUrl().replace(emiLocation, "");
        task.setKernelManifestUrl(String.format("%s%s", servicePrefix, ekiLocation));
        task.setRamdiskManifestUrl(String.format("%s%s", servicePrefix, eriLocation));
         // find the imageLocation or the kernel and ramdisk
      }catch(final ImagingServiceException ex){
        throw Exceptions.toUndeclared(ex);
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, 
            "Unable to find the kernel and ramdisk of the requested EMI", ex));
      }
      
      return task;
    }
  }
}
