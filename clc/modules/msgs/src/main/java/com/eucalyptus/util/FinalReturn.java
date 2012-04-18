package com.eucalyptus.util;

/**
 * @author decker
 *
 * @param <T>
 */
public class FinalReturn<T> {
  private FinalReturn( ) {}
  
  public static <T> FinalReturn<T> newInstance( ) {
    return new FinalReturn<T>( );
  }
  
  private T ret;
  
  public void set( T t ) {
    ret = t;
  }
  
  public T get( ) {
    return ret;
  }
}
