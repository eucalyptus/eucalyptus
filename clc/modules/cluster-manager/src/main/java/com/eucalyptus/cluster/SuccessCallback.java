package com.eucalyptus.cluster;


public interface SuccessCallback<T> {
  public void apply( T t );
}
