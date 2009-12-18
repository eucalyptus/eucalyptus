package com.eucalyptus.cluster;


public interface SuccessCallback<T> {
  public void apply( T t );
  public static SuccessCallback NOOP = new SuccessCallback() {public void apply( Object t ) {}};
}
