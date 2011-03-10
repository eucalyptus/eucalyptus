package com.eucalyptus.system;

import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class NoopPreferences extends AbstractPreferences {
  private static final String[] EMPTY = {};
  private static final NoopPreferences ROOT = new NoopPreferences();
  
  public NoopPreferences( AbstractPreferences parent, String name ) {
    super( parent, name );
  }

  protected NoopPreferences( ) {
    this( null, "" );
  }

  public static class Factory implements PreferencesFactory {
    @Override
    public Preferences systemRoot( ) {
      return new NoopPreferences( );
    }
    
    @Override
    public Preferences userRoot( ) {
      return new NoopPreferences( );
    }
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
