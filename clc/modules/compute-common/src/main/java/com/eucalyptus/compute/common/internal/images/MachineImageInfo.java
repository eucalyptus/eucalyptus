/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.compute.common.internal.images;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@DiscriminatorValue( value = "machine" )
public class MachineImageInfo extends PutGetImageInfo implements BootableImageInfo {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  @Column( name = "metadata_image_kernel_id" )
  private String kernelId;
  @Column( name = "metadata_image_ramdisk_id" )
  private String ramdiskId;
  @Column( name = "metadata_image_ami" )
  private String ami;
  // user can specify root location while bundling image EUCA-9908
  @Column( name = "metadata_image_root_directive" )
  private String rootDirective;
  @Column( name = "metadata_image_virtualization_type" )
  @Enumerated(  EnumType.STRING )
  private ImageMetadata.VirtualizationType virtType;
  
  @Column ( name = "metadata_image_conversion_id", nullable = true)
  private String imageConversionId;
  
  @Column( name = "metadata_image_run_manifest_path")
  private String runManifestLocation;
  
  public MachineImageInfo( ) {
    super( ImageMetadata.Type.machine );
  }
  
  public MachineImageInfo( final String imageId ) {
    super( ImageMetadata.Type.machine, imageId );
  }
  
  public MachineImageInfo( final UserFullName userFullName, final String imageId,
                           final String imageName, final String imageDescription, final Long imageSizeBytes, final Architecture arch, final Platform platform,
                           final String imageLocation, final Long imageBundleSizeBytes, final String imageChecksum, final String imageChecksumType,
                           final String kernelId, final String ramdiskId, ImageMetadata.VirtualizationType virtType, final String ami, final String root,
                           final boolean imagePublic ) {
    super( userFullName, imageId, ImageMetadata.Type.machine, imageName, imageDescription, imageSizeBytes, arch, platform, imageLocation, imageBundleSizeBytes,
           imageChecksum, imageChecksumType, imagePublic );
    this.kernelId = kernelId;
    this.ramdiskId = ramdiskId;
    this.virtType = virtType;
    this.ami = ami;
    this.rootDirective = root;
  }
  
  @Override
  public String getKernelId( ) {
    return this.kernelId;
  }
  
  public void setKernelId( final String kernelId ) {
    this.kernelId = kernelId;
  }
  
  @Override
  public String getRamdiskId( ) {
    return this.ramdiskId;
  }
  
  public void setRamdiskId( final String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }
  
  @Override
  public String getManifestLocation( ) {
    return super.getManifestLocation( );
  }

  @Override
  public String getRootDeviceName( ) {
    if (ami == null || ami.isEmpty())
      return virtType == ImageMetadata.VirtualizationType.hvm ? Images.DEFAULT_ROOT_DEVICE : Images.DEFAULT_PARTITIONED_ROOT_DEVICE;
    else
      return ami.startsWith("/dev/") ? ami : "/dev/" + ami;
  }

  public String getShortRootDeviceName( ) {
    return getRootDeviceName().substring(5);
  }

  public String getRootDirective( ) {
    return rootDirective;
  }

  @Override
  public String getRootDeviceType( ) {
    return "instance-store";
  }

  @Override
  public ImageMetadata.VirtualizationType getVirtualizationType(){
	  return this.virtType;
  }
  
  public void setVirtualizationType(final ImageMetadata.VirtualizationType virtType){
    this.virtType = virtType;
  }

  public void setImageConversionId(final String conversionId){
    this.imageConversionId = conversionId;
  }
  
  public String getImageConversionId(){
    return this.imageConversionId;
  }
 
  public void setRunManifestLocation( final String runManifestLocation) {
    this.runManifestLocation = runManifestLocation;
  }
  
  @EntityUpgrade( entities = { MachineImageInfo.class }, since = Version.v4_0_0, value = Eucalyptus.class )
  public enum MachineImageInfo400Upgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( MachineImageInfo.MachineImageInfo400Upgrade.class );

    @Override
    public boolean apply(Class arg0) {
      try ( final TransactionResource db =
          Entities.transactionFor( MachineImageInfo.class )) {
        try{
          List<MachineImageInfo> images = Entities.query( new MachineImageInfo( ) );
          for(MachineImageInfo image : images) {
            LOG.info("Upgrading MachineImageInfo: " + image.toString());
            // Set all PV image's state to "PENDING_AVAILABLE"
            if(ImageMetadata.VirtualizationType.paravirtualized.equals(image.virtType)){
              image.setState(ImageMetadata.State.pending_available);
              image.setImageFormat(ImageMetadata.ImageFormat.partitioned.name());
            }else{
              image.setImageFormat(ImageMetadata.ImageFormat.fulldisk.name());
              image.setRunManifestLocation(image.getManifestLocation());
            }
            Entities.persist(image);
          }
          db.commit();
          return true;
        }catch(final Exception ex) {
          LOG.error("Error upgrading MachineImageInfo: ", ex);
          db.rollback();
          throw Exceptions.toUndeclared(ex);
        }
      }
    }
  }
}
