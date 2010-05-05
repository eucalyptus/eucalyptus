package com.eucalyptus.auth.principal;

public interface Authorization<T> {
  public String getName( );
  public String getValue( );
  
  public String getDescription( );
  
  public boolean permits( T context );
  
  public abstract boolean equals( Object obj );
  
  public abstract int hashCode( );
}
