package com.eucalyptus.util;

import com.eucalyptus.entities.EntityWrapper;

public interface JoinTx<T> {
  public void fire( EntityWrapper<T> db, T t ) throws Throwable;
}
