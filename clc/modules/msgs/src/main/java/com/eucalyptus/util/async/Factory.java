package com.eucalyptus.util.async;

public interface Factory<T> {
  public abstract T newInstance( );
}
