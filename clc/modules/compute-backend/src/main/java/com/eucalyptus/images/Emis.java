/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

package com.eucalyptus.images;

import java.security.PublicKey;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.images.BootableImageInfo;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.compute.common.internal.images.KernelImageInfo;
import com.eucalyptus.compute.common.internal.images.MachineImageInfo;
import com.eucalyptus.compute.common.internal.images.RamdiskImageInfo;
import com.eucalyptus.compute.common.internal.util.NoSuchImageIdException;

import org.apache.log4j.Logger;

import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.compute.common.ImageMetadata.Platform;
import com.eucalyptus.compute.common.RunInstancesType;
import com.eucalyptus.compute.common.StaticDiskImage;
import com.eucalyptus.compute.common.internal.util.IllegalMetadataAccessException;
import com.eucalyptus.compute.common.internal.util.InvalidMetadataException;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.imaging.manifest.BundleImageManifest;
import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.RestrictedTypes.Resolver;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;

import com.eucalyptus.cluster.common.msgs.VmTypeInfo;

public class Emis {
  private static Logger LOG = Logger.getLogger( Emis.class );
  
  @Resolver( ImageMetadata.class )
  public enum LookupImage implements Function<String, ImageInfo> {
    INSTANCE;
    
    @Override
    public ImageInfo apply( final String input ) {
      if ( CloudMetadatas.isKernelImageIdentifier( input ) ) {
        return LookupKernel.INSTANCE.apply( input );
      } else if ( CloudMetadatas.isRamdiskImageIdentifier( input ) ) {
        return LookupRamdisk.INSTANCE.apply( input );
      } else if ( CloudMetadatas.isMachineImageIdentifier( input ) ) {
        try {
          return LookupMachine.INSTANCE.apply( input );
        } catch ( final Exception ex ) {
          return LookupAvailableBlockStorage.INSTANCE.apply( input );
        }
      } else {
        throw new NoSuchElementException( "Failed to lookup image: " + input );
      }
    }
  }
  
  public enum LookupBlockStorage implements Function<String, BlockStorageImageInfo> {
    INSTANCE;
    @Override
    public BlockStorageImageInfo apply( final String identifier ) {
      final EntityTransaction db = Entities.get( BlockStorageImageInfo.class );
      try {
        final BlockStorageImageInfo ret = Entities.uniqueResult( Images.exampleBlockStorageWithImageId( identifier ) );
        if ( Platform.windows.name( ).equals( ret.getKernelId( ) ) || ret.getImageName( ).startsWith( Platform.windows.name( ) ) ) {
          ret.setPlatform( Platform.windows );
        }
        db.rollback( );
        return ret;
      } catch ( final Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db.rollback( );
        throw new NoSuchElementException( "Failed to lookup image: " + identifier + " because of " + ex.getMessage( ) );
      }
    }
  }
  
  public enum LookupAvailableBlockStorage implements Function<String, BlockStorageImageInfo> {
    INSTANCE;
    @Override
    public BlockStorageImageInfo apply( final String identifier ) {
      final EntityTransaction db = Entities.get( BlockStorageImageInfo.class );
      try {
        final BlockStorageImageInfo ret = Entities.uniqueResult( Images.exampleBlockStorageWithImageId( identifier ) );
        if ( Platform.windows.name( ).equals( ret.getKernelId( ) ) || ret.getImageName( ).startsWith( Platform.windows.name( ) ) ) {
          ret.setPlatform( Platform.windows );
        }
        if ( !ImageMetadata.State.available.equals( ret.getState( )) &&
        		!ImageMetadata.State.pending.equals( ret.getState( )) ) {
          db.rollback( );
          throw new NoSuchElementException( "Unable to start instance with deregistered/failed image : " + ret );
        } else {
          db.rollback( );
          return ret;
        }
      } catch ( final Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db.rollback( );
        throw new NoSuchElementException( "Failed to lookup image: " + identifier + " because of " + ex.getMessage( ) );
      }
    }
  }
  
  public enum LookupMachine implements NonNullFunction<String, MachineImageInfo> {
    INSTANCE;
    @Nonnull
    @Override
    public MachineImageInfo apply( final String identifier ) {
      final EntityTransaction db = Entities.get( MachineImageInfo.class );
      try {
        final MachineImageInfo ret = Entities.uniqueResult( Images.exampleMachineWithImageId( identifier ) );
        if ( !ImageMetadata.State.available.name().equals( ret.getState( ).getExternalStateName() ) ) {
          db.rollback( );
          throw new NoSuchElementException( "Unable to start instance with deregistered/failed image : " + ret );
        } else {
          db.rollback( );
          return ret;
        }
      } catch ( final Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db.rollback( );
        throw new NoSuchElementException( "Failed to lookup image: " + identifier + " because of " + ex.getMessage( ) );
      }
    }
  }
  
  public enum LookupKernel implements NonNullFunction<String, KernelImageInfo> {
    INSTANCE;
    @Nonnull
    @Override
    public KernelImageInfo apply( final String identifier ) {
      final EntityTransaction db = Entities.get( KernelImageInfo.class );
      try {
        final KernelImageInfo ret = Entities.uniqueResult( Images.exampleKernelWithImageId( identifier ) );
        if ( !ImageMetadata.State.available.name().equals( ret.getState( ).getExternalStateName() ) ) {
          db.rollback( );
          throw new NoSuchElementException( "Unable to start instance with deregistered/failed image : " + ret );
        } else {
          db.rollback( );
          return ret;
        }
      } catch ( final Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db.rollback( );
        throw new NoSuchElementException( "Failed to lookup image: " + identifier + " because of " + ex.getMessage( ) );
      }
    }
  }
  
  public enum LookupRamdisk implements NonNullFunction<String, RamdiskImageInfo> {
    INSTANCE;
    @Nonnull
    @Override
    public RamdiskImageInfo apply( final String identifier ) {
      final EntityTransaction db = Entities.get( RamdiskImageInfo.class );
      try {
        final RamdiskImageInfo ret = Entities.uniqueResult( Images.exampleRamdiskWithImageId( identifier ) );
        if ( !ImageMetadata.State.available.name().equals( ret.getState( ).getExternalStateName() ) ) {
          db.rollback( );
          throw new NoSuchElementException( "Unable to start instance with deregistered/failed image : " + ret );
        } else {
          db.rollback( );
          return ret;
        }
      } catch ( final Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db.rollback( );
        throw new NoSuchElementException( "Failed to lookup image: " + identifier + " because of " + ex.getMessage( ) );
      }
    }
  }
  
  public static class BootableSet {
    private final BootableImageInfo disk;
    
    private BootableSet( final BootableImageInfo bootableImageInfo ) {
    	this.disk = bootableImageInfo;
    }
    
    public BootableImageInfo getMachine( ) {
      return this.disk;
    }
    
    public RamdiskImageInfo getRamdisk( ) {
      throw new NoSuchElementException( "BootableSet:machine=" + this.getMachine( ) + " does not have a ramdisk." );
    }
    
    public KernelImageInfo getKernel( ) {
      throw new NoSuchElementException( "BootableSet:machine=" + this.getMachine( ) + " does not have a kernel." );
    }
    
    public boolean hasKernel( ) {
      try {
        this.getKernel( );
        return true;
      } catch ( final NoSuchElementException ex ) {
        return false;
      }
    }
    
    public boolean hasRamdisk( ) {
      try {
        this.getRamdisk( );
        return true;
      } catch ( final NoSuchElementException ex ) {
        return false;
      }
    }

    public String getRamdiskDownloadManifest(PublicKey signingKey, String partitionName ) throws MetadataException {
      throw new NoSuchElementException( "BootableSet:machine=" + this.getMachine( ) + " does not have a ramdisk." );
    }

    public String getKernelDownloadManifest(PublicKey signingKey, String partitionName ) throws MetadataException {
      throw new NoSuchElementException( "BootableSet:machine=" + this.getMachine( ) + " does not have a kernel." );
    }

    public String getImageDownloadManifest(PublicKey signingKey, String partitionName) throws MetadataException {
      if(! (this.getMachine() instanceof StaticDiskImage)) {
        throw new NoSuchElementException("BootableSet:machine=" + this.getMachine() + " does not have a bundled-type image.");
      }

      StaticDiskImage img = (StaticDiskImage)this.getMachine();
      try {
        if (!Strings.isNullOrEmpty(img.getManifestHash())
            && !img.getManifestHash().equals(
            ImageManifests.getManifestHash(img.getManifestLocation())))
          throw new MetadataException("Image manifest was changed after registration for: " + img.getDisplayName());
        return DownloadManifestFactory.generateDownloadManifest(
            new ImageManifestFile(img.getManifestLocation(),
                                  BundleImageManifest.INSTANCE,
                                  ImageConfiguration.getInstance().getMaxManifestSizeBytes()),
            signingKey,
            img.getDisplayName() + "-" + partitionName, true);
      } catch (Exception ex){
        throw new MetadataException(ex);
      }
    }

    
    public boolean isBlockStorage( ) {
      return this.getMachine( ) instanceof BlockStorageImageInfo;
    }
    
    public boolean isLinux( ) {
      return ImageMetadata.Platform.linux.equals( this.getMachine( ).getPlatform( ) ) || ( this.getMachine( ).getPlatform( ) == null );
    }
    
    public boolean isHvm(){
    	return ImageMetadata.VirtualizationType.hvm.equals(this.getMachine().getVirtualizationType());
    }
    
    @Override
    public String toString( ) {
      return String.format( "BootableSet:machine=%s:ramdisk=%s:kernel=%s:isLinux=%s", this.getMachine( ),
                            this.hasRamdisk( )
                                              ? this.getRamdisk( )
                                              : "false",
                              this.hasKernel( )
                                               ? this.getKernel( )
                                               : "false",
                                this.isLinux( ) );
    }
    
    public VmTypeInfo populateVirtualBootRecord( final VmType vmType, final Partition partition,
    		final String reservationId) throws MetadataException {
      final Supplier<Pair<String,String>> rootInfoSupplier = () -> {
        if ( this.getMachine( ) instanceof StaticDiskImage ) { // BootableImage+StaticDiskImage = MachineImageInfo
          try {
            final StaticDiskImage diskImage = (StaticDiskImage) this.getMachine( );
            if (!Strings.isNullOrEmpty( diskImage.getManifestHash() )
                && !diskImage.getManifestHash().equals( ImageManifests.getManifestHash(diskImage.getManifestLocation() )) ) {
              throw new MetadataException("Instance manifest was changed after registration");
            }
            return Pair.of(
                diskImage.getDisplayName( ),
                this.getImageDownloadManifest(partition.getNodeCertificate().getPublicKey(), partition.getName()) );
          } catch ( Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
        }
        throw Exceptions.toUndeclared( new MetadataException("Instance manifest location not available") );
      };
      try {
        final VmTypeInfo vmTypeInfo = VmTypes.asVmTypeInfo( vmType, this.getMachine( ), rootInfoSupplier );
        if ( this.isLinux( ) ) {
          if ( this.hasKernel( ) ) {
            String manifestLocation = this.getKernelDownloadManifest(partition.getNodeCertificate().getPublicKey(), partition.getName());
            vmTypeInfo.setKernel( this.getKernel().getDisplayName(), manifestLocation,
                                  this.getKernel( ).getImageSizeBytes() );
          }
          if ( this.hasRamdisk( ) ) {
            String manifestLocation = this.getRamdiskDownloadManifest(partition.getNodeCertificate().getPublicKey(), partition.getName());
            vmTypeInfo.setRamdisk( this.getRamdisk( ).getDisplayName( ), manifestLocation,
                                   this.getRamdisk( ).getImageSizeBytes() );
          }
        }
        return vmTypeInfo;
      } catch (Exception ex){
        Exceptions.findAndRethrow( ex, MetadataException.class );
        throw new MetadataException(ex);
      }
    }
  }

  static class NoRamdiskBootableSet extends BootableSet {
    private final KernelImageInfo kernel;
    
    private NoRamdiskBootableSet( final BootableImageInfo bootableImageInfo, final KernelImageInfo kernel ) {
      super( bootableImageInfo );
      this.kernel = kernel;
    }
    
    @Override
    public KernelImageInfo getKernel( ) {
      return this.kernel;
    }

    public String getKernelDownloadManifest(PublicKey signingKey, String partitionName)  throws MetadataException {
      try {
        if (!Strings.isNullOrEmpty(this.kernel.getManifestHash())
            && !kernel.getManifestHash().equals(
            ImageManifests.getManifestHash(this.kernel.getManifestLocation())))
          throw new MetadataException("Image manifest was changed after registration for: " + this.kernel.getDisplayName());
        return DownloadManifestFactory.generateDownloadManifest(
            new ImageManifestFile(this.kernel.getManifestLocation(),
                                  BundleImageManifest.INSTANCE,
                                  ImageConfiguration.getInstance().getMaxManifestSizeBytes()),
            signingKey,
            this.kernel.getDisplayName() + "-" + partitionName, true);
      } catch (Exception ex){
        throw new MetadataException(ex);
      }
    }

  }
  
  static class TrifectaBootableSet extends NoRamdiskBootableSet {
    private final RamdiskImageInfo ramdisk;
    
    public TrifectaBootableSet( final BootableImageInfo bootableImageInfo, final KernelImageInfo kernel, final RamdiskImageInfo ramdisk ) {
      super( bootableImageInfo, kernel );
      this.ramdisk = ramdisk;
    }
    
    @Override
    public RamdiskImageInfo getRamdisk( ) {
      return this.ramdisk;
    }

    public String getRamdiskDownloadManifest(PublicKey signingKey, String partitionName) throws MetadataException {
        try {
          if (!Strings.isNullOrEmpty(this.ramdisk.getManifestHash())
              && !ramdisk.getManifestHash().equals(
              ImageManifests.getManifestHash(ramdisk.getManifestLocation())))
            throw new MetadataException("Image manifest was changed after registration for: " + ramdisk.getDisplayName());
          return DownloadManifestFactory.generateDownloadManifest(
              new ImageManifestFile(ramdisk.getManifestLocation(),
                                    BundleImageManifest.INSTANCE,
                                    ImageConfiguration.getInstance().getMaxManifestSizeBytes()),
              signingKey,
              this.ramdisk.getDisplayName() + "-" + partitionName, true);
        } catch (Exception ex){
          throw new MetadataException(ex);
        }
    }

  }
  
  public static BootableSet recreateBootableSet( final VmInstance vm ) {
    final BootableImageInfo bootableImageInfo = vm.getBootRecord( ).getMachine( );
    final KernelImageInfo kernel = vm.getBootRecord( ).getKernel( );
    final RamdiskImageInfo ramdisk = vm.getBootRecord( ).getRamdisk( );
    if ( ( kernel != null ) && ( ramdisk != null ) ) {
      return new TrifectaBootableSet( bootableImageInfo, kernel, ramdisk );
    } else if ( kernel != null ) {
      return new NoRamdiskBootableSet( bootableImageInfo, kernel );
    } else {
      return new BootableSet( bootableImageInfo );
    }
  }
  
  public static BootableSet recreateBootableSet( final String imageId, final String kernelId, final String ramdiskId ) throws MetadataException {
    try {
      final BootsetBuilder builder = new BootsetBuilder( ).imageId( imageId );
      if ( kernelId != null ) {
        builder.kernelId( kernelId );
        if ( ramdiskId != null ) {
          builder.ramdiskId( ramdiskId );
        }
      }
      return builder.start( );
    } catch ( final MetadataException ex ) {
      throw ex;
    } catch ( final Exception ex ) {
      throw new InvalidMetadataException( "Failed to construct bootset for image id: " + imageId + " because of: " + ex.getMessage( ), ex );
    }
  }

  public static BootableSet unavailableBootableSet( final Platform platform ) {
    return new BootableSet( new UnavailableImageInfo( platform ) );
  }
  
  public static BootableSet newBootableSet(
      final String imageId,
      @Nullable final String kernelId,
      @Nullable final String ramdiskId
  ) throws MetadataException {
    try {
      return new BootsetBuilder( )
          .imageId( imageId )
          .kernelId( kernelId )
          .ramdiskId( ramdiskId )
          .run( );
    } catch ( final MetadataException ex ) {
      throw ex;
    } catch ( final Exception ex ) {
      throw new InvalidMetadataException( "Failed to construct bootset for image id: " + imageId + " because of: " + ex.getMessage( ), ex );
    }
  }
  
  private static class BootsetBuilder {
    private String imageId;
    private String kernelId;
    private String ramdiskId;
    
    public BootsetBuilder imageId( final String imageId ) {
      this.imageId = imageId;
      return this;
    }
    
    public BootsetBuilder ramdiskId( final String ramdiskId ) {
      this.ramdiskId = ramdiskId;
      return this;
    }
    
    public BootsetBuilder kernelId( final String kernelId ) {
      this.kernelId = kernelId;
      return this;
    }
    
    public BootableSet run( ) throws MetadataException {
      final Function<String, BootableSet> create =
          Functions.compose(
              BootsetWithRamdisk.INSTANCE.apply(this.ramdiskId),
              Functions.compose(
                  BootsetWithKernel.INSTANCE.apply(this.kernelId),
                  BootsetFromId.INSTANCE ) );
      return this.prepareBootset( create );
    }
    
    private BootableSet prepareBootset( final Function<String, BootableSet> create ) throws MetadataException {
      try {
        final BootableSet bootSet = create.apply( this.imageId );
        return bootSet;
      } catch ( final RuntimeException ex ) {
        if ( ex.getCause( ) instanceof MetadataException ) {
          throw ( MetadataException ) ex.getCause( );
        } else {
          throw ex;
        }
      }
    }
    
    public BootableSet start( ) throws MetadataException {
      final Function<String, BootableSet> create = new Function<String, BootableSet>( ) {
        @Override
        public BootableSet apply( final String input ) {
          final BootableSet b = BootsetFromId.INSTANCE.apply( BootsetBuilder.this.imageId );
          try {
            final KernelImageInfo kernel = ( BootsetBuilder.this.kernelId == null
                                                                           ? null
                                                                           : LookupKernel.INSTANCE.apply( BootsetBuilder.this.kernelId ) );
            final RamdiskImageInfo ramdisk = ( BootsetBuilder.this.ramdiskId == null
                                                                              ? null
                                                                              : LookupRamdisk.INSTANCE.apply( BootsetBuilder.this.ramdiskId ) );
            if ( ( kernel != null ) && ( ramdisk != null ) ) {
              return new TrifectaBootableSet( b.getMachine( ), kernel, ramdisk );
            } else if ( kernel != null ) {
              return new NoRamdiskBootableSet( b.getMachine( ), kernel );
            } else {
              return b;
            }
          } catch ( NoSuchElementException e ) {
            throw Exceptions.toUndeclared( new NoSuchMetadataException( e.getMessage(), e ) );
          }
        }
      };
      return this.prepareBootset( create );
    }
  }
  
  private static <T extends ImageInfo> T resolveDiskImage( final String imageId, final Function<String, T> resolver ) throws IllegalMetadataAccessException {
    final T img = resolver.apply( imageId );
    if ( Contexts.exists( ) ) {
      final Predicate<T> filter = Predicates.and( Images.FilterPermissions.INSTANCE, RestrictedTypes.filterPrivilegedWithoutOwner( ) );
      if ( filter.apply( img ) ) {
    	  if( ! Images.FilterImageStates.INSTANCE.apply( img )) 
    		  throw Exceptions.toUndeclared(new EucalyptusCloudException("Image state is not available"));
    	  return img;
      } else {
        throw new IllegalMetadataAccessException( imageId + ": permission denied." );
      }
    } else {
      return img;
    }
  }
  
  enum BootsetFromId implements Function<String, BootableSet> {
    INSTANCE;
    
    @Override
    public BootableSet apply( final String input ) {
      BootableSet bootSet;
      try {
        bootSet = new BootableSet( resolveDiskImage( input, LookupMachine.INSTANCE ) );
      } catch ( final IllegalContextAccessException ex ) {
        throw Exceptions.toUndeclared( new IllegalMetadataAccessException( ex ) );
      } catch ( final IllegalMetadataAccessException ex ) {
        throw Exceptions.toUndeclared( ex );
      } catch ( final Exception e ) {
        try {
          bootSet = new BootableSet( resolveDiskImage( input, LookupAvailableBlockStorage.INSTANCE ) );
        } catch ( final IllegalContextAccessException ex ) {
          throw Exceptions.toUndeclared( new IllegalMetadataAccessException( ex ) );
        } catch ( final IllegalMetadataAccessException ex ) {
          throw Exceptions.toUndeclared( ex );
        } catch ( final NoSuchElementException ex ) {
          throw Exceptions.toUndeclared( new NoSuchImageIdException( "Failed to lookup image named: " + input, ex ) );
        } catch ( final PersistenceException ex ) {
          throw Exceptions.toUndeclared( new InvalidMetadataException( "Error occurred while trying to lookup image named: " + input, ex ) );
        }
      }
      return bootSet;
    }
    
  }
  
  enum BootsetWithRamdisk implements Function<String, Function<BootableSet, BootableSet>> {
    INSTANCE;

    public Function<BootableSet, BootableSet> apply(@Nullable final String overrideRamdiskId) {
      return input -> {
        if ( !input.isLinux( ) || input.isHvm()) {
          return input;
        } else {
          String ramdiskId = null;
          try {
            ramdiskId = determineRamdiskId( input, overrideRamdiskId );
            LOG.debug( "Determined the appropriate ramdiskId to be " + ramdiskId + " for " + input.toString( ) );
            if ( ramdiskId == null ) {
              return input;
            } else {
              final RamdiskImageInfo ramdisk = RestrictedTypes.doPrivilegedWithoutOwner( ramdiskId, LookupRamdisk.INSTANCE );
              return new TrifectaBootableSet( input.getMachine( ), input.getKernel( ), ramdisk );
            }
          } catch ( final InvalidMetadataException ex ) {
            return input;
          } catch ( final Exception ex ) {
            if ( input.isBlockStorage( ) ) {
              return input;
            } else {
              throw Exceptions.toUndeclared( new NoSuchMetadataException( "Failed to lookup ramdisk image information: " + ramdiskId
                                                                          + " because of: "
                                                                          + ex.getMessage( ), ex ) );
            }
          }
        }
      };
    }
  }
  
  enum BootsetWithKernel implements Function<String, Function<BootableSet, BootableSet>> {
    INSTANCE;

    @Override
    public Function<BootableSet, BootableSet> apply(@Nullable final String overrideKernelId) {
      return input -> {
        if ( !input.isLinux( ) || input.isHvm() ) {
          return input;
        } else {
          String kernelId = "unknown";
          try {
            kernelId = determineKernelId( input, overrideKernelId );
            LOG.debug( "Determined the appropriate kernelId to be " + kernelId + " for " + input.toString( ) );
            final KernelImageInfo kernel = RestrictedTypes.doPrivilegedWithoutOwner( kernelId, LookupKernel.INSTANCE );
            return new NoRamdiskBootableSet( input.getMachine( ), kernel );
          } catch ( final Exception ex ) {
            if ( input.isBlockStorage( ) ) {
              return input;
            } else if (input.isHvm()) {
              return input;
            } else {
              throw Exceptions.toUndeclared( new NoSuchMetadataException( "Failed to lookup kernel image information " + kernelId
                  + " because of: "
                  + ex.getMessage( ), ex ) );
            }
          }
        }
      };
    }
  }
  
  private static String determineKernelId( final BootableSet bootSet, final String overrideKernelId ) throws MetadataException {
    final BootableImageInfo disk = bootSet.getMachine( );
    String kernelId = overrideKernelId;
    Context ctx = null;
    try {
      ctx = Contexts.lookup( );
    } catch ( final IllegalContextAccessException ex ) {
      LOG.debug( "Context not found when determining kernel id:" + ex.getMessage( ) );
    }
    if ( ( kernelId == null ) || "".equals( kernelId ) ) {
      kernelId = disk.getKernelId( );
    }
   
    Preconditions.checkNotNull( kernelId, "Attempt to resolve a kerneId for "
                                          + bootSet.toString( )
                                          + " during request "
                                          + ( ctx != null
                                                         ? ctx.getRequest( ).toSimpleString( )
                                                         : "UNKNOWN" ) );
    if ( kernelId == null ) {
      throw new NoSuchMetadataException( "Unable to determine required kernel image for " + disk.getDisplayName( ) );
    } else if ( !CloudMetadatas.isKernelImageIdentifier( kernelId ) ) {
      throw new InvalidMetadataException( "Image specified is not a kernel: " + kernelId );
    }
    return kernelId;
  }
  
  private static String determineRamdiskId( final BootableSet bootSet, final String overrideRamdiskId ) throws MetadataException {
    if ( !bootSet.hasKernel( ) ) {
      throw new InvalidMetadataException( "Image specified does not have a kernel: " + bootSet );
    }
    String ramdiskId = bootSet.getMachine( ).getRamdiskId( );//GRZE: use the ramdisk that is part of the registered image definition to start.
    if ( ( overrideRamdiskId != null ) && !"".equals( overrideRamdiskId ) ) {
      ramdiskId = overrideRamdiskId;//GRZE: maybe update w/ a specific ramdisk user requests
    }
    //GRZE: perfectly legitimate for there to be no ramdisk, carry on. **/
    if ( ramdiskId == null ) {
      return ramdiskId;
    } else if ( !CloudMetadatas.isRamdiskImageIdentifier( ramdiskId ) ) {
      throw new InvalidMetadataException( "Image specified is not a ramdisk: " + ramdiskId );
    } else {
      return ramdiskId;
    }
  }
}
