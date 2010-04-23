package com.eucalyptus.util;

public interface Tx<T> {
  public void fire( T t ) throws Throwable;
}
