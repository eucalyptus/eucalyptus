package com.eucalyptus.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

public class NoopPreferences extends AbstractPreferences {
  private static final String[] EMPTY = {};
  
  NoopPreferences( AbstractPreferences parent, String name ) {
    super( parent, name );
  }
  
  NoopPreferences( ) {
    this( null, "" );
  }
  
  @Override
  protected void putSpi( String key, String value ) {}
  
  @Override
  protected String getSpi( String key ) {
    return null;
  }
  
  @Override
  protected void removeSpi( String key ) {}
  
  @Override
  protected void removeNodeSpi( ) throws BackingStoreException {}
  
  @Override
  protected String[] keysSpi( ) throws BackingStoreException {
    return EMPTY;
  }
  
  @Override
  protected String[] childrenNamesSpi( ) throws BackingStoreException {
    return EMPTY;
  }
  
  @Override
  protected AbstractPreferences childSpi( String name ) {
    return new NoopPreferences( this, name );
  }
  
  @Override
  protected void syncSpi( ) throws BackingStoreException {}
  
  @Override
  protected void flushSpi( ) throws BackingStoreException {}
  
}
