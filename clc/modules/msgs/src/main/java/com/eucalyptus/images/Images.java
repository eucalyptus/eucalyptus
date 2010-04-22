package com.eucalyptus.images;

import java.util.List;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities._anon;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

/**
 * @author decker
 */
public class Images {
  public static EntityWrapper<ImageInfo> getEntityWrapper() {
    return new EntityWrapper<ImageInfo>( "eucalyptus_general" );
  }
  public static class _byId extends _anon<ImageInfo> {
    public _byId( String imageId ) {
      super( new ImageInfo( imageId ) );
    }
  }

   /**
   * TODO: DOCUMENT Images.java
   * @return
   */
  public static List<Image> listAllImages( ) {
    List<Image> images = Lists.newArrayList( );
    EntityWrapper<ImageInfo> db = Images.getEntityWrapper( );
    try {
      images.addAll( db.query( new ImageInfo( ) ) );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
    }
    return images;
  }
  public static void deleteImage( String imageId ) throws NoSuchImageException {
    EntityWrapper<ImageInfo> db = Images.getEntityWrapper( );
    try {
      ImageInfo img = db.getUnique( ImageInfo.named( imageId ) );
      db.delete( img );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new NoSuchImageException( "Failed to lookup image: " + imageId, e );
    }
  }
}
