/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.blockstorage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ComputationException;
import com.google.common.collect.Maps;

/**
 * Implements a storage manager lookup service with entries populated by class discovery based on annotations.
 * To include a class in this registry it must be annotated with @StorageManagerProperty
 * 
 * @author grze & zhill
 *
 */

public class StorageManagers extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( StorageManagers.class );
  private static final String UNSET = "unset";
  
  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface StorageManagerProperty {
    String value( );
    
    Class<? extends LogicalStorageManager> manager( ) default LogicalStorageManager.class;
  }
  
  private static final Map<String, Class> managers  = Maps.newHashMap( );
  private static final Map<String, Class> providers = Maps.newHashMap( );
  
  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( Ats.from( candidate ).has( StorageManagerProperty.class )
         && !Modifier.isAbstract( candidate.getModifiers( ) )
         && !Modifier.isInterface( candidate.getModifiers( ) ) ) {
      StorageManagerProperty candidateType = Ats.from( candidate ).get( StorageManagerProperty.class );
      String propName = candidateType.value( );
      if ( LogicalStorageManager.class.isAssignableFrom( candidate ) ) {
        managers.put( propName, candidate );
      } else {
        managers.put( propName, candidateType.manager( ) );
        providers.put( propName, candidate );
      }
      return true;
    } else {
      return false;
    }
  }
  
  @Override
  public Double getPriority( ) {
    return 0.0d;
  }
  
  private static final LoadingCache<String, LogicalStorageManager> managerInstances = CacheBuilder.newBuilder().build(
    new CacheLoader<String, LogicalStorageManager>() {
      @Override
      public LogicalStorageManager load( String arg0 ) {
        LogicalStorageManager bsm = Classes.newInstance( lookupManager( arg0 ) );
        try {
          bsm.checkPreconditions( );
          return bsm;
        } catch ( EucalyptusCloudException ex ) {
          throw new ComputationException( ex );
        }
      }
    });
  
  private static AtomicReference<String> lastManager = new AtomicReference<String>( );
  
  public static LogicalStorageManager getInstance( ) {
    if ( lastManager.get( ) == null || UNSET.equals(lastManager.get())) {
      throw new NoSuchElementException( "SC blockstorageamanger not configured. Found empty or unset manager(" + lastManager + ").  Legal values are: " + Joiner.on( "," ).join( managers.keySet( ) ) );
    } else {
      return managerInstances.getUnchecked( lastManager.get( ) );
    }
  }
  
  public static LogicalStorageManager getInstance( String propertyBackend ) throws InstantiationException, IllegalAccessException, EucalyptusCloudException {
    if ( managers.containsKey( propertyBackend ) ) {
      lastManager.set( propertyBackend );
    }
    return getInstance( );
  }
  
  public static Set<String> list( ) {
    return managers.keySet( );
  }
  
  public static boolean contains( Object key ) {
    return managers.containsKey( key );
  }
  
  public static synchronized void flushManagerInstances() throws EucalyptusCloudException {
  	LOG.debug("Flushing all block storage manager instances");
  	managerInstances.invalidateAll();
  	lastManager.set(UNSET);
  }
  
  public static synchronized void flushManagerInstance(String key) throws EucalyptusCloudException {
  	LOG.debug("Flusing block storage manager instance: " + key);
	lastManager.set(UNSET);
	managerInstances.invalidate(key);
  }
  
  public static Class<? extends LogicalStorageManager> lookupManager( String arg0 ) {
    if ( !managers.containsKey( arg0 ) ) {
      throw new NoSuchElementException( "Not a valid value:  " + arg0 + ".  Legal values are: " + Joiner.on( "," ).join( managers.keySet( ) ) );
    } else {
      return managers.get( arg0 );
    }
  }
  
  public static Class lookupProvider( String arg0 ) {
    if ( !providers.containsKey( arg0 ) ) {
      throw new NoSuchElementException( "Not a valid value:  " + arg0 + ".  Legal values are: " + Joiner.on( "," ).join( providers.keySet( ) ) );
    } else {
      return providers.get( arg0 );
    }
  }
}
