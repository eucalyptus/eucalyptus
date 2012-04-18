package com.eucalyptus.util;

import java.util.List;

public interface Tx<T> extends Callback<T> {
  public static final Tx<List<?>> LIST_NOOP = new Tx<List<?>>() {
    @Override
    public void fire( List<?> t ) {}
  };
  public static final Tx NOOP = new Tx() {
    @Override
    public void fire( Object t ) {}
  };
  @Override
  public void fire( T t );
}
