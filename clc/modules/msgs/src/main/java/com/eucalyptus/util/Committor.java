package com.eucalyptus.util;

//TODO:FIXME
public interface Committor<O> {
  public abstract void commit( O object ) throws Exception;
}
