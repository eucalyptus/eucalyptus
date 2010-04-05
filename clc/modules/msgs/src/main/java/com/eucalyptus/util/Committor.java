package com.eucalyptus.util;

public interface Committor<O> {
  public abstract void commit( O object ) throws Exception;
}
