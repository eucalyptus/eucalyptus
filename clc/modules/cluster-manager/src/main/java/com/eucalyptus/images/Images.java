package com.eucalyptus.images;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.Adler32;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Example;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.WalrusUtil;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.ImageMetadata.StaticDiskImage;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.TransactionExecutionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.images.ImageManifests.ImageManifest;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.EbsDeviceMapping;
import edu.ucsb.eucalyptus.msgs.ImageDetails;

/**
 * @author decker
 */
public class Images {
  private static Logger LOG  = Logger.getLogger( Images.class );
  
  static final String   SELF = "self";
  
  public static Predicate<ImageInfo> filterExecutableBy( final Collection<String> executableSet ) {
    final boolean executableSelf = executableSet.remove( SELF );
    final boolean executableAll = executableSet.remove( "all" );
    return new Predicate<ImageInfo>( ) {
      
      @Override
      public boolean apply( ImageInfo image ) {
        if ( executableSet.isEmpty( ) ) {
          return true;
        } else {
          UserFullName userFullName = Contexts.lookup( ).getUserFullName( );
          boolean filtered = ( executableAll && image.getImagePublic( ) );
          filtered |= ( executableSelf && ( image.getOwner( ).isOwner( userFullName ) || image.hasPermission( userFullName.getAccountNumber( ) ) ) );
          filtered |= ( image.getOwner( ).isOwner( userFullName ) && image.hasPermission( executableSet.toArray( new String[] {} ) ) );
          return filtered;
        }
      }
      
    };
  }
  
  public enum FilterPermissions implements Predicate<ImageInfo> {
    INSTANCE;
    
    @Override
    public boolean apply( ImageInfo input ) {
      try {
        Context ctx = Contexts.lookup( );
        if ( ctx.hasAdministrativePrivileges( ) ) {
          return true;
        } else {
          UserFullName luser = ctx.getUserFullName( );
          /** GRZE: record why this must be so **/
          if ( input.getImagePublic( ) ) {
            return true;
          } else if ( input.getOwnerAccountNumber( ).equals( luser.getAccountNumber( ) ) ) {
            return true;
          } else if ( input.hasPermission( luser.getAccountNumber( ), luser.getAccountName( ), luser.getUserId( ) ) ) {
            return true;
          } else {
            for ( AccessKey key : ctx.getUser( ).getKeys( ) ) {
              if ( input.hasPermission( key.getAccessKey( ) ) ) {
                return true;
              }
            }
            return false;
          }
        }
      } catch ( Exception ex ) {
        return false;
      }
    }
  }
  
  @QuantityMetricFunction( ImageMetadata.class )
  public enum CountImages implements Function<OwnerFullName, Long> {
    INSTANCE;
    
    @Override
    public Long apply( final OwnerFullName input ) {
      EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
      int i = db.createCriteria( ImageInfo.class ).add( Example.create( ImageInfo.named( input, null ) ) ).setReadOnly( true ).setCacheable( false ).list( ).size( );
      db.rollback( );
      return ( long ) i;
    }
  }
  
  @TypeMapper
  public enum KernelImageDetails implements Function<KernelImageInfo, ImageDetails> {
    INSTANCE;
    
    @Override
    public ImageDetails apply( KernelImageInfo arg0 ) {
      ImageDetails i = new ImageDetails( );
      i.setName( arg0.getName( ) );
      i.setDescription( arg0.getDescription( ) );
      i.setArchitecture( arg0.getArchitecture( ).toString( ) );
      i.setImageId( arg0.getDisplayName( ) );
      i.setImageLocation( arg0.getManifestLocation( ) );
      i.setImageOwnerId( arg0.getOwnerAccountNumber( ).toString( ) );//TODO:GRZE:verify imageOwnerAlias
      i.setImageState( arg0.getState( ).toString( ) );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setIsPublic( arg0.getImagePublic( ) );
      i.setPlatform( ImageMetadata.Platform.linux.toString( ) );
//      i.setStateReason( arg0.getStateReason( ) );//TODO:GRZE:NOW
//      i.setVirtualizationType( arg0.getVirtualizationType( ) );//TODO:GRZE:NOW
//      i.getProductCodes().addAll( arg0.getProductCodes() );//TODO:GRZE:NOW
//      i.getTags().addAll( arg0.getTags() );//TODO:GRZE:NOW
//      i.setHypervisor( arg0.getHypervisor( ) );//TODO:GRZE:NOW
      return i;
    }
  }
  
  @TypeMapper
  public enum RamdiskImageDetails implements Function<RamdiskImageInfo, ImageDetails> {
    INSTANCE;
    
    @Override
    public ImageDetails apply( RamdiskImageInfo arg0 ) {
      ImageDetails i = new ImageDetails( );
      i.setName( arg0.getName( ) );
      i.setDescription( arg0.getDescription( ) );
      i.setArchitecture( arg0.getArchitecture( ).toString( ) );
      i.setImageId( arg0.getDisplayName( ) );
      i.setImageLocation( arg0.getManifestLocation( ) );
      i.setImageOwnerId( arg0.getOwnerAccountNumber( ).toString( ) );//TODO:GRZE:verify imageOwnerAlias
      i.setImageState( arg0.getState( ).toString( ) );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setIsPublic( arg0.getImagePublic( ) );
      i.setPlatform( ImageMetadata.Platform.linux.toString( ) );
//      i.setStateReason( arg0.getStateReason( ) );//TODO:GRZE:NOW
//      i.setVirtualizationType( arg0.getVirtualizationType( ) );//TODO:GRZE:NOW
//      i.getProductCodes().addAll( arg0.getProductCodes() );//TODO:GRZE:NOW
//      i.getTags().addAll( arg0.getTags() );//TODO:GRZE:NOW
//      i.setHypervisor( arg0.getHypervisor( ) );//TODO:GRZE:NOW
      return i;
    }
  }
  
  @TypeMapper
  public enum BlockStorageImageDetails implements Function<BlockStorageImageInfo, ImageDetails> {
    INSTANCE;
    
    @Override
    public ImageDetails apply( BlockStorageImageInfo arg0 ) {
      ImageDetails i = new ImageDetails( );
      i.setName( arg0.getImageName( ) );
      i.setDescription( arg0.getDescription( ) );
      i.setArchitecture( arg0.getArchitecture( ).toString( ) );
      i.setRootDeviceName( "/dev/sda1" );
      i.setRootDeviceType( "ebs" );
      i.setImageId( arg0.getDisplayName( ) );
      i.setImageLocation( arg0.getOwnerAccountNumber( ) + "/" + arg0.getImageName( ) );
      i.setImageOwnerId( arg0.getOwnerAccountNumber( ).toString( ) );//TODO:GRZE:verify imageOwnerAlias
      i.setImageState( arg0.getState( ).toString( ) );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setIsPublic( arg0.getImagePublic( ) );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setKernelId( arg0.getKernelId( ) );
      i.setRamdiskId( arg0.getRamdiskId( ) );
      i.setPlatform( arg0.getPlatform( ).toString( ) );
      i.setPlatform( ImageMetadata.Platform.linux.toString( ) );
      i.getBlockDeviceMappings( ).addAll( Collections2.transform( arg0.getDeviceMappings( ), DeviceMappingDetails.INSTANCE ) );
//      i.setStateReason( arg0.getStateReason( ) );//TODO:GRZE:NOW
//      i.setVirtualizationType( arg0.getVirtualizationType( ) );//TODO:GRZE:NOW
//      i.getProductCodes().addAll( arg0.getProductCodes() );//TODO:GRZE:NOW
//      i.getTags().addAll( arg0.getTags() );//TODO:GRZE:NOW
//      i.setHypervisor( arg0.getHypervisor( ) );//TODO:GRZE:NOW
      return i;
    }
  }
  
  @TypeMapper
  public enum MachineImageDetails implements Function<MachineImageInfo, ImageDetails> {
    INSTANCE;
    
    @Override
    public ImageDetails apply( MachineImageInfo arg0 ) {
      ImageDetails i = new ImageDetails( );
      i.setName( arg0.getName( ) );
      i.setDescription( arg0.getDescription( ) );
      i.setArchitecture( arg0.getArchitecture( ).toString( ) );
      //TODO      i.setRootDeviceName( arg0.getD )
      i.setRootDeviceName( "/dev/sda1" );
      i.setRootDeviceType( "instance-store" );
      i.setImageId( arg0.getDisplayName( ) );
      i.setImageLocation( arg0.getManifestLocation( ) );
      i.setImageLocation( arg0.getManifestLocation( ) );
      i.setImageOwnerId( arg0.getOwnerAccountNumber( ).toString( ) );//TODO:GRZE:verify imageOwnerAlias
      i.setImageState( arg0.getState( ).toString( ) );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setIsPublic( arg0.getImagePublic( ) );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setKernelId( arg0.getKernelId( ) );
      i.setRamdiskId( arg0.getRamdiskId( ) );
      i.setPlatform( arg0.getPlatform( ).toString( ) );
      i.getBlockDeviceMappings( ).addAll( Collections2.transform( arg0.getDeviceMappings( ), DeviceMappingDetails.INSTANCE ) );
//      i.setStateReason( arg0.getStateReason( ) );//TODO:GRZE:NOW
//      i.setVirtualizationType( arg0.getVirtualizationType( ) );//TODO:GRZE:NOW
//      i.getProductCodes().addAll( arg0.getProductCodes() );//TODO:GRZE:NOW
//      i.getTags().addAll( arg0.getTags() );//TODO:GRZE:NOW
//      i.setHypervisor( arg0.getHypervisor( ) );//TODO:GRZE:NOW
      return i;
    }
  }
  
  @TypeMapper
  public enum DeviceMappingDetails implements Function<DeviceMapping, BlockDeviceMappingItemType> {
    INSTANCE;
    @Override
    public BlockDeviceMappingItemType apply( DeviceMapping input ) {
      BlockDeviceMappingItemType ret = new BlockDeviceMappingItemType( );
      ret.setDeviceName( input.getDeviceName( ) );
      if ( input instanceof BlockStorageDeviceMapping ) {
        final BlockStorageDeviceMapping ebsDev = ( BlockStorageDeviceMapping ) input;
        ret.setEbs( new EbsDeviceMapping( ) {
          {
            this.setVirtualName( ebsDev.getVirtualName( ) );
            this.setSnapshotId( ebsDev.getSnapshotId( ) );
            this.setVolumeSize( ebsDev.getSize( ) );
            this.setDeleteOnTermination( ebsDev.getDelete( ) );
          }
        } );
      } else {
        ret.setVirtualName( input.getVirtualName( ) );
      }
      return ret;
    }
  }
  
  public static Function<BlockDeviceMappingItemType, DeviceMapping> deviceMappingGenerator( final ImageInfo parent ) {
    return new Function<BlockDeviceMappingItemType, DeviceMapping>( ) {
      @Override
      public DeviceMapping apply( BlockDeviceMappingItemType input ) {
        assertThat( input, notNullValue( ) );
        assertThat( input.getDeviceName( ), notNullValue( ) );
        if ( input.getEbs( ) != null ) {
          EbsDeviceMapping ebsInfo = input.getEbs( );
          Snapshot snap;
          Integer size;
          try {
            snap = Transactions.find( Snapshot.named( null, ebsInfo.getSnapshotId( ) ) );
            size = snap.getVolumeSize( );
            if ( ebsInfo.getVolumeSize( ) != null && ebsInfo.getVolumeSize( ) >= snap.getVolumeSize( ) ) {
              size = ebsInfo.getVolumeSize( );
            }
          } catch ( ExecutionException ex ) {
            LOG.error( ex, ex );
            size = input.getEbs( ).getVolumeSize( );
          }
          return new BlockStorageDeviceMapping( parent, input.getDeviceName( ), input.getEbs( ).getVirtualName( ), ebsInfo.getSnapshotId( ), size,
                                                ebsInfo.getDeleteOnTermination( ) );
        } else if ( input.getVirtualName( ) != null && input.getVirtualName( ).matches( "ephemeral[0123]" ) ) {
          return new EphemeralDeviceMapping( parent, input.getDeviceName( ), input.getVirtualName( ) );
        } else {
          return new SuppressDeviceMappping( parent, input.getDeviceName( ) );
        }
      }
    };
  }
  
  public static Function<ImageInfo, ImageDetails> TO_IMAGE_DETAILS = new Function<ImageInfo, ImageDetails>( ) {
                                                                     
                                                                     @Override
                                                                     public ImageDetails apply( ImageInfo input ) {
                                                                       return TypeMappers.transform( input, ImageDetails.class );
                                                                     }
                                                                   };
  public static ImageInfo                         ALL              = new ImageInfo( );
  
  public static List<ImageInfo> listAllImages( ) {
    List<ImageInfo> images = Lists.newArrayList( );
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      List<ImageInfo> found = db.query( Images.ALL );
      images.addAll( found );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
    }
    return images;
  }
  
  public static void enableImage( String imageId ) throws NoSuchImageException {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      ImageInfo img = db.getUnique( Images.exampleWithImageId( imageId ) );
      img.setState( ImageMetadata.State.available );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new NoSuchImageException( "Failed to lookup image: " + imageId, e );
    }
  }
  
  public static void deregisterImage( String imageId ) throws ConstraintViolationException, NoSuchImageException {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      ImageInfo img = db.getUnique( Images.exampleWithImageId( imageId ) );
      if ( ImageMetadata.State.deregistered.equals( img.getState( ) ) ) {
        db.delete( img );
      } else {
        img.setState( ImageMetadata.State.deregistered );
      }
      db.commit( );
      if ( img instanceof ImageMetadata.StaticDiskImage ) {
        WalrusUtil.invalidate( ( StaticDiskImage ) img );
      }
      
    } catch ( ConstraintViolationException cve ) {
      db.rollback( );
      // Need to add message that the image is associated with running instances.
      throw cve;
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new NoSuchImageException( "Failed to lookup image: " + imageId, e );
    }
  }
  
//  public static void deleteImage( String imageId ) throws NoSuchImageException {
//    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
//    try {
//      ImageInfo img = db.getUnique( Images.exampleWithImageId( imageId ) );
//      db.delete( img );
//      db.commit( );
//    } catch ( EucalyptusCloudException e ) {
//      db.rollback( );
//      throw new NoSuchImageException( "Failed to lookup image: " + imageId, e );
//    }
//  }
//  
  public static MachineImageInfo exampleMachineWithImageId( final String imageId ) {
    return new MachineImageInfo( imageId );
  }
  
  public static BlockStorageImageInfo exampleBlockStorageWithImageId( final String imageId ) {
    return new BlockStorageImageInfo( imageId );
  }
  
  public static KernelImageInfo exampleKernelWithImageId( final String imageId ) {
    return new KernelImageInfo( imageId );
  }
  
  public static RamdiskImageInfo exampleRamdiskWithImageId( final String imageId ) {
    return new RamdiskImageInfo( imageId );
  }
  
  public static ImageInfo lookupImage( String imageId ) {
    return EntityWrapper.get( ImageInfo.class ).lookupAndClose( Images.exampleWithImageId( imageId ) );
  }
  
  public static ImageInfo exampleWithImageId( final String imageId ) {
    return new ImageInfo( imageId );
  }
  
  public static ImageInfo exampleWithImageState( final ImageMetadata.State state ) {
    ImageInfo img = new ImageInfo( ) {
      {
        setState( state );
      }
    };
    
    return img;
  }
  
  public static ImageInfo exampleWithOwnerAccountId( final String ownerId ) {
    return new ImageInfo( ) {
      {
        setOwnerAccountNumber( ownerId );
      }
    };
  }
  
  public static Predicate<BlockDeviceMappingItemType> findEbsRoot( final String rootDevName ) {
    return new Predicate<BlockDeviceMappingItemType>( ) {
      @Override
      public boolean apply( BlockDeviceMappingItemType input ) {
        return rootDevName.equals( input.getDeviceName( ) ) && input.getEbs( ) != null && input.getEbs( ).getSnapshotId( ) != null;
      }
    };
  }
  
  public static ImageInfo createFromDeviceMapping( UserFullName userFullName, String imageName, String imageDescription,
                                                   String eki, String eri,
                                                   String rootDeviceName, final List<BlockDeviceMappingItemType> blockDeviceMappings ) throws EucalyptusCloudException {
    ImageMetadata.Architecture imageArch = ImageMetadata.Architecture.x86_64;//TODO:GRZE:OMGFIXME: track parent vol info; needed here 
    ImageMetadata.Platform imagePlatform = ImageMetadata.Platform.linux;
    if ( ImageMetadata.Platform.windows.name( ).equals( eki ) ) {
      imagePlatform = ImageMetadata.Platform.windows;
      eki = null;
    }
    BlockDeviceMappingItemType rootBlockDevice = Iterables.find( blockDeviceMappings, findEbsRoot( rootDeviceName ) );
    String snapshotId = rootBlockDevice.getEbs( ).getSnapshotId( );
    try {
      Snapshot snap = Transactions.find( Snapshot.named( userFullName, snapshotId ) );
      if ( !userFullName.getUserId( ).equals( snap.getOwnerUserId( ) ) ) {
        throw new EucalyptusCloudException( "Failed to create image from specified block device mapping: " + rootBlockDevice
                                            + " because of: you must the owner of the source snapshot." );
      }
      Integer snapVolumeSize = snap.getVolumeSize( );
      Integer suppliedVolumeSize = ( rootBlockDevice.getEbs( ).getVolumeSize( ) != null )
        ? rootBlockDevice.getEbs( ).getVolumeSize( )
        : -1;
      suppliedVolumeSize = ( suppliedVolumeSize == null )
        ? rootBlockDevice.getSize( )
        : suppliedVolumeSize;
      Integer targetVolumeSizeGB = ( snapVolumeSize <= suppliedVolumeSize )
        ? suppliedVolumeSize
        : snapVolumeSize;
      Long imageSizeBytes = targetVolumeSizeGB * 1024l * 1024l * 1024l;
      Boolean targetDeleteOnTermination = Boolean.TRUE.equals( rootBlockDevice.getEbs( ).getDeleteOnTermination( ) );
      String imageId = Crypto.generateId( snapshotId, ImageMetadata.Type.machine.getTypePrefix( ) );
      
      BlockStorageImageInfo ret = new BlockStorageImageInfo( userFullName, imageId, imageName, imageDescription, imageSizeBytes,
                                                             imageArch, imagePlatform,
                                                             eki, eri,
                                                             snap.getDisplayName( ), targetDeleteOnTermination );
      EntityWrapper<BlockStorageImageInfo> db = EntityWrapper.get( BlockStorageImageInfo.class );
      try {
        ret = db.merge( ret );
        ret.getDeviceMappings( ).addAll( Lists.transform( blockDeviceMappings, Images.deviceMappingGenerator( ret ) ) );
        ret.setState( ImageMetadata.State.available );
        db.commit( );
        LOG.info( "Registering image pk=" + ret.getDisplayName( ) + " ownerId=" + userFullName );
      } catch ( Exception e ) {
        db.rollback( );
        throw new EucalyptusCloudException( "Failed to register image using snapshot: " + snapshotId + " because of: " + e.getMessage( ), e );
      }
      
      return ret;
    } catch ( TransactionExecutionException ex ) {
      throw new EucalyptusCloudException( "Failed to create image from specified block device mapping: " + rootBlockDevice + " because of: " + ex.getMessage( ) );
    } catch ( ExecutionException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( "Failed to create image from specified block device mapping: " + rootBlockDevice + " because of: " + ex.getMessage( ) );
    }
  }
  
  public static ImageInfo createFromManifest( UserFullName creator, String imageNameArg, String imageDescription, ImageMetadata.Architecture requestArch, String eki, String eri, ImageManifest manifest ) throws EucalyptusCloudException {
    PutGetImageInfo ret = null;
    String imageName = ( imageNameArg != null )
      ? imageNameArg
      : manifest.getName( );
    eki = ( eki != null )
      ? eki
      : manifest.getKernelId( );
    eki = ( eki != null )
      ? eki
      : ImageConfiguration.getInstance( ).getDefaultKernelId( );
    eri = ( eri != null )
      ? eri
      : manifest.getRamdiskId( );
    eri = ( eri != null )
      ? eri
      : ImageConfiguration.getInstance( ).getDefaultRamdiskId( );
    ImageMetadata.Architecture imageArch = ( requestArch != null )
      ? requestArch
      : manifest.getArchitecture( );
    ImageMetadata.Platform imagePlatform = manifest.getPlatform( );    
    switch ( manifest.getImageType( ) ) {
      case kernel:
        ret = new KernelImageInfo( creator, ImageUtil.newImageId( ImageMetadata.Type.kernel.getTypePrefix( ), manifest.getImageLocation( ) ),
                                   imageName, imageDescription, manifest.getSize( ), imageArch, imagePlatform,
                                    manifest.getImageLocation( ), manifest.getBundledSize( ), manifest.getChecksum( ), manifest.getChecksumType( ) );
        break;
      case ramdisk:
        ret = new RamdiskImageInfo( creator, ImageUtil.newImageId( ImageMetadata.Type.ramdisk.getTypePrefix( ), manifest.getImageLocation( ) ),
                                    imageName, imageDescription, manifest.getSize( ), imageArch, imagePlatform,
                                    manifest.getImageLocation( ), manifest.getBundledSize( ), manifest.getChecksum( ), manifest.getChecksumType( ) );
        break;
      case machine:
    	if(ImageMetadata.Platform.windows.equals(imagePlatform)){
    	    	eki = null; 
    	    	eri = null;
    	}
        ret = new MachineImageInfo( creator, ImageUtil.newImageId( ImageMetadata.Type.machine.getTypePrefix( ), manifest.getImageLocation( ) ),
                                    imageName, imageDescription, manifest.getSize( ), imageArch, imagePlatform,
                                    manifest.getImageLocation( ), manifest.getBundledSize( ), manifest.getChecksum( ), manifest.getChecksumType( ), eki, eri );
        break;
    }
    if ( ret == null ) {
      throw new IllegalArgumentException( "Failed to prepare image using the provided image manifest: " + manifest );
    } else {
      ret.setSignature( manifest.getSignature( ) );
      ret.setState( ImageMetadata.State.available );
      EntityWrapper<PutGetImageInfo> db = EntityWrapper.get( PutGetImageInfo.class );
      try {
        ret = db.merge( ret );
        db.commit( );
        LOG.info( "Registering image pk=" + ret.getDisplayName( ) + " ownerId=" + creator );
      } catch ( Exception e ) {
        db.rollback( );
        throw new EucalyptusCloudException( "Failed to register image: " + manifest + " because of: " + e.getMessage( ), e );
      }
      //TODO:GRZE:RESTORE
//    for( String p : extractProductCodes( inputSource, xpath ) ) {
//      imageInfo.addProductCode( p );
//    }
//    imageInfo.grantPermission( ctx.getAccount( ) );
      maybeUpdateDefault( ret );
      LOG.info( "Triggering cache population in Walrus for: " + ret.getDisplayName( ) );
      if ( ret instanceof ImageMetadata.StaticDiskImage ) {
        WalrusUtil.triggerCaching( ( StaticDiskImage ) ret );
      }
      return ret;
    }
  }
  
  private static void maybeUpdateDefault( PutGetImageInfo ret ) {
    final String id = ret.getDisplayName( );
    if ( ImageMetadata.Type.kernel.equals( ret.getImageType( ) ) && ImageConfiguration.getInstance( ).getDefaultKernelId( ) == null ) {
      try {
        ImageConfiguration.modify( new Callback<ImageConfiguration>( ) {
          @Override
          public void fire( ImageConfiguration t ) {
            t.setDefaultKernelId( id );
          }
        } );
      } catch ( ExecutionException ex ) {
        LOG.error( ex, ex );
      }
    } else if ( ImageMetadata.Type.ramdisk.equals( ret.getImageType( ) ) && ImageConfiguration.getInstance( ).getDefaultRamdiskId( ) == null ) {
      try {
        ImageConfiguration.modify( new Callback<ImageConfiguration>( ) {
          @Override
          public void fire( ImageConfiguration t ) {
            t.setDefaultRamdiskId( id );
          }
        } );
      } catch ( ExecutionException ex ) {
        LOG.error( ex, ex );
      }
    }
  }
  
  public static ImageConfiguration configuration( ) {
    return ImageConfiguration.getInstance( );
  }
  
  public static String lookupDefaultKernelId( ) {
    return ImageConfiguration.getInstance( ).getDefaultKernelId( );
  }
  
  public static String lookupDefaultRamdiskId( ) {
    return ImageConfiguration.getInstance( ).getDefaultRamdiskId( );
  }
  
}
