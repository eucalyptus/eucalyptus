package com.eucalyptus.auth;

public interface Contract<T> {

  public enum Type {
    EXPIRATION,
    MAXKEYS
  }
  
  public Type getType( );
  
  public T getValue( );
  
}
