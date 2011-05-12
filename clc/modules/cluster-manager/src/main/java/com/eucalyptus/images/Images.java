package com.eucalyptus.images;

import java.util.List;
import java.util.zip.Adler32;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.WalrusUtil;
import com.eucalyptus.cloud.Image;
import com.eucalyptus.cloud.Image.State;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.ImageManifests.ImageManifest;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.TypeMapping;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.msgs.ImageDetails;

/**
 * @author decker
 */
public class Images {
  private static Logger LOG = Logger.getLogger( Images.class );
  private static String generateImageId( final String imagePrefix, final String imageLocation ) {
    Adler32 hash = new Adler32( );
    String key = imageLocation + System.currentTimeMillis( );
    hash.update( key.getBytes( ) );
    String imageId = String.format( "%s-%08X", imagePrefix, hash.getValue( ) );
    return imageId;
  }
  
  private static String newImageId( final String imagePrefix, final String imageLocation ) {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    String testId = generateImageId( imagePrefix, imageLocation );
    ImageInfo query = Images.exampleWithImageId( testId );
    LOG.info( "Trying to lookup using created AMI id=" + query.getDisplayName( ) );
    for ( ; db.query( query ).size( ) != 0; query.setDisplayName( generateImageId( imagePrefix, imageLocation ) ) );
    db.commit( );
    LOG.info( "Assigning imageId=" + query.getDisplayName( ) );
    return query.getDisplayName( );
  }

  public static class ImageInfoToDetails implements TypeMapping<ImageInfo, ImageDetails> {
    @Override
    public ImageDetails apply( ImageInfo arg0 ) {
      ImageDetails i = new ImageDetails( );
      i.setArchitecture( arg0.getArchitecture( ).toString( ) );
      i.setImageId( arg0.getDisplayName( ) );
      i.setImageLocation( arg0.getImageLocation( ) );
      i.setImageOwnerId( arg0.getOwnerAccountId( ).toString( ) );
      i.setImageState( arg0.getState( ).toString( ) );
      i.setIsPublic( arg0.getImagePublic( ) );
      if ( arg0 instanceof MachineImageInfo ) {
        i.setImageType( ( ( MachineImageInfo ) arg0 ).getImageType( ).toString( ) );
        i.setKernelId( ( ( MachineImageInfo ) arg0 ).getKernelId( ) );
        i.setRamdiskId( ( ( MachineImageInfo ) arg0 ).getRamdiskId( ) );
        i.setPlatform( ( ( MachineImageInfo ) arg0 ).getPlatform( ).toString( ) );
      }
      return i;
    }
  }
  
  public static ImageInfoToDetails TO_IMAGE_DETAILS = new ImageInfoToDetails( );
  public static ImageInfo          ALL              = new ImageInfo( );
  
  /**
   * TODO: DOCUMENT Images.java
   * 
   * @return
   */
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
      img.setState( Image.State.available );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new NoSuchImageException( "Failed to lookup image: " + imageId, e );
    }
  }
  
  public static void deregisterImage( String imageId ) throws NoSuchImageException {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      ImageInfo img = db.getUnique( Images.exampleWithImageId( imageId ) );
      if( Image.State.deregistered.equals( img.getState( ) ) ) {
        db.delete( img );
      } else {
        img.setState( Image.State.deregistered );
      }
      db.commit( );
      WalrusUtil.invalidate( img );
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
  
  public static ImageInfo exampleWithImageState( final Image.State state ) {
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
        setOwnerAccountId( ownerId );
      }
    };
  }
  
  public static ImageInfo createFromSnapshot( UserFullName creator, Snapshot snapshot ) {
    
  }
  
  
  public static ImageInfo createFromManifest( UserFullName creator, ImageManifest manifest ) throws EucalyptusCloudException {
    ImageInfo ret = null;
    switch ( manifest.getImageType( ) ) {
      case kernel:
        ret = new KernelImageInfo( creator, ImageUtil.newImageId( Image.Type.kernel.getTypePrefix( ), manifest.getImageLocation( ) ), manifest.getImageLocation( ),
                                    manifest.getArchitecture( ), manifest.getPlatform( ) );
      case ramdisk:
        ret = new RamdiskImageInfo( creator, ImageUtil.newImageId( Image.Type.kernel.getTypePrefix( ), manifest.getImageLocation( ) ), manifest.getImageLocation( ),
                                    manifest.getArchitecture( ), manifest.getPlatform( ) );
      case machine:
        ret =  new MachineImageInfo( creator, ImageUtil.newImageId( Image.Type.kernel.getTypePrefix( ), manifest.getImageLocation( ) ), manifest.getImageLocation( ),
                                     manifest.getArchitecture( ), manifest.getPlatform( ) );
    }
    if( ret == null ) {
      throw new IllegalArgumentException( "Failed to prepare image using the provided image manifest: " + manifest );
    } else {
      ret.setSignature( manifest.getSignature( ) );
      ret.setState( Image.State.available );
      EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
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
      LOG.info( "Triggering cache population in Walrus for: " + ret.getDisplayName( ) );
      WalrusUtil.triggerCaching( ret );
      return ret;
    }
  }
  
}
