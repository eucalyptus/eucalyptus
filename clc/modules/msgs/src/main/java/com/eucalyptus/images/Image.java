package com.eucalyptus.images;


public interface Image {
  
  public abstract String getArchitecture( );
  
  public abstract void setArchitecture( String architecture );
  
  public abstract String getImageId( );
  
  public abstract void setImageId( String imageId );
  
  public abstract String getImageLocation( );
  
  public abstract void setImageLocation( String imageLocation );
  
  public abstract String getImageOwnerId( );
  
  public abstract void setImageOwnerId( String imageOwnerId );
  
  public abstract String getImageState( );
  
  public abstract void setImageState( String imageState );
  
  public abstract String getImageType( );
  
  public abstract void setImageType( String imageType );
  
  public abstract Boolean getImagePublic( );
  
  public abstract void setImagePublic( Boolean aPublic );
  
  public abstract String getKernelId( );
  
  public abstract void setKernelId( String kernelId );
  
  public abstract String getRamdiskId( );
  
  public abstract void setRamdiskId( String ramdiskId );
  
  public abstract boolean equals( final Object o );
  
  public abstract int hashCode( );
  
  public abstract String toString( );
  
}