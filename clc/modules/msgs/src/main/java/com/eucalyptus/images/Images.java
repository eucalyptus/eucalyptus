package com.eucalyptus.images;

import java.util.List;
import javax.persistence.Transient;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

/**
 * @author decker
 */
public class Images {
  public static String IMAGE_RAMDISK_PREFIX = "eri";
  public static String IMAGE_KERNEL_PREFIX  = "eki";
  public static String IMAGE_MACHINE_PREFIX = "emi";
  
  public enum Architecture {
    i386, x86_64
  }
  
  public enum Platform {
    linux {
      public String toString( ) {
        return "";
      }
    },
    windows {
      public String toString( ) {
        return this.name( );
      }
    };
  }
  
  public enum Type {
    machine {
      @Override
      public String getTypePrefix( ) {
        return IMAGE_MACHINE_PREFIX;
      }
    },
    kernel {
      @Override
      public String getTypePrefix( ) {
        return IMAGE_KERNEL_PREFIX;
      }
    },
    ramdisk {
      @Override
      public String getTypePrefix( ) {
        return IMAGE_RAMDISK_PREFIX;
      }
    };
    public abstract String getTypePrefix( );
  }
  
  public enum State {
    pending, available, failed, deregistered
  }
  
  public enum VirtualizationType {
    paravirtualized, hvm
  }
  
  public enum Hypervisor {
    xen, kvm, vmware
  }
  
  public static EntityWrapper<ImageInfo> getEntityWrapper( ) {
    return new EntityWrapper<ImageInfo>( "eucalyptus_cloud" );
  }
  
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
      img.setImageState( Images.State.available );
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
      img.setImageState( Images.State.deregistered );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new NoSuchImageException( "Failed to lookup image: " + imageId, e );
    }
  }

  public static void deleteImage( String imageId ) throws NoSuchImageException {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      ImageInfo img = db.getUnique( Images.exampleWithImageId( imageId ) );
      db.delete( img );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new NoSuchImageException( "Failed to lookup image: " + imageId, e );
    }
  }
  

  public static MachineImageInfo exampleMachineWithImageId( final String imageId ) {
    return new MachineImageInfo( imageId );
  }
  public static KernelImageInfo exampleKernelWithImageId( final String imageId ) {
    return new KernelImageInfo( imageId );
  }
  public static RamdiskImageInfo exampleRamdiskWithImageId( final String imageId ) {
    return new RamdiskImageInfo( imageId );
  }

  public static ImageInfo exampleWithImageId( final String imageId ) {
    return new ImageInfo( ) {
      {
        setDisplayName( imageId );
      }
    };
  }
  

  public static ImageInfo exampleWithImageState( final State state ) {
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

  @Transient
  public static ImageInfo         ALL          = exampleWithImageId( null );

}
