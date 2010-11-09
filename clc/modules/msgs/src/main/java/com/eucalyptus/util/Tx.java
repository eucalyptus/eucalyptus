package com.eucalyptus.util;

import java.util.List;

public interface Tx<T> {
  public static final Tx<List<?>> LIST_NOOP = new Tx<List<?>>() {
    @Override
    public void fire( List<?> t ) throws Throwable {}
  };
  public static final Tx NOOP = new Tx() {
    @Override
    public void fire( Object t ) throws Throwable {}
  };
  public void fire( T t ) throws Throwable;
}
