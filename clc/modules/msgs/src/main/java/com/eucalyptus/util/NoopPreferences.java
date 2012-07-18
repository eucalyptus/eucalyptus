/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
