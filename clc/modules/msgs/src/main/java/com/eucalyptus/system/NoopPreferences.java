package com.eucalyptus.system;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class NoopPreferences extends AbstractPreferences {
  private static final String[] EMPTY = {};
  private static final NoopPreferences ROOT = new NoopPreferences();
  private Map<String,String> fakePrefs = new HashMap<String,String>();
  private ConcurrentMap<String,AbstractPreferences> fakeKids = new ConcurrentHashMap<String,AbstractPreferences>();
  
  public NoopPreferences( AbstractPreferences parent, String name ) {
    super( parent, name );
  }

  protected NoopPreferences( ) {
    this( null, "" );
  }

  public static class Factory implements PreferencesFactory {
    private static final Preferences SYSTEM = new NoopPreferences( );
    private static final Preferences USER = new NoopPreferences( );
    @Override
    public Preferences systemRoot( ) {
      return SYSTEM;
    }
    
    @Override
    public Preferences userRoot( ) {
      return USER;
    }
  }
  
  @Override
  protected void putSpi( String key, String value ) {
    this.fakePrefs.put( key, value );
  }
  
  @Override
  protected String getSpi( String key ) {
    return this.fakePrefs.get( key );
  }
  
  @Override
  protected void removeSpi( String key ) {
    this.fakePrefs.remove( key );
  }
  
  @Override
  protected void removeNodeSpi( ) throws BackingStoreException {
    this.fakeKids.clear( );
    this.fakePrefs.clear( );
  }
  
  @Override
  protected String[] keysSpi( ) throws BackingStoreException {
    return this.fakePrefs.keySet( ).toArray( EMPTY );
  }
  
  @Override
  protected String[] childrenNamesSpi( ) throws BackingStoreException {
    return this.fakeKids.keySet( ).toArray( EMPTY );
  }
  
  @Override
  protected AbstractPreferences childSpi( String name ) {
    return this.fakeKids.putIfAbsent( name, new NoopPreferences( this, name ) );
  }
  
  @Override
  protected void syncSpi( ) throws BackingStoreException {}
  
  @Override
  protected void flushSpi( ) throws BackingStoreException {}
  
}
