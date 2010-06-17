package com.eucalyptus.auth.principal;

/**
 * @author decker
 *
 * @param <T>
 */
public interface Authorization<T> {
  /**
   * TODO: DOCUMENT Authorization.java
   * @return
   */
  public String getDisplayName( );
  @Deprecated
  public String getName( );
  
  /**
   * TODO: DOCUMENT Authorization.java
   * @return
   */
  public String getValue( );
  
  /**
   * TODO: DOCUMENT Authorization.java
   * @return
   */
  public String getDescription( );

  /**
   * TODO: DOCUMENT Authorization.java
   * @param t
   * @return
   */
  public boolean check( T t );
  
  /**
   * TODO: DOCUMENT Authorization.java
   * @param obj
   * @return
   */
  public abstract boolean equals( Object obj );
  
}
