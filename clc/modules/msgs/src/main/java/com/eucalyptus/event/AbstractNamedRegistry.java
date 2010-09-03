/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.event;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.eucalyptus.util.HasName;
import com.google.common.collect.Lists;

public abstract class AbstractNamedRegistry<TYPE extends HasName<TYPE>> {
  protected ReadWriteLock                      canHas = new ReentrantReadWriteLock( );
  private ConcurrentNavigableMap<String, TYPE> activeMap;
  private ConcurrentNavigableMap<String, TYPE> disabledMap;
  
  protected AbstractNamedRegistry( ) {
    this.activeMap = new ConcurrentSkipListMap<String, TYPE>( );
    this.disabledMap = new ConcurrentSkipListMap<String, TYPE>( );
  }
  
  public Set<String> getKeys( ) {
    return this.activeMap.keySet( );
  }
  
  public Set<String> getDisabledKeys( ) {
    return this.disabledMap.keySet( );
  }
  
  public boolean isRegistered( String key ) {
    return this.activeMap.containsKey( key );
  }
  
  public Collection<TYPE> getEntries( ) {
    return this.activeMap.values( );
  }
  
  public Collection<TYPE> getDisabledEntries( ) {
    return this.disabledMap.values( );
  }
  
  public void deregister( String key ) {
    this.canHas.writeLock( ).lock( );
    try {
      this.disabledMap.remove( key );
      this.activeMap.remove( key );
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
  }
  
  public void register( TYPE obj ) {
    TYPE tempObj = null;
    this.canHas.writeLock( ).lock( );
    try {
      if ( ( tempObj = this.disabledMap.remove( obj.getName( ) ) ) == null ) {
        tempObj = obj;
      }
      this.activeMap.putIfAbsent( tempObj.getName( ), tempObj );
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
  }
  
  public void registerDisabled( TYPE obj ) {
    TYPE tempObj = null;
    this.canHas.writeLock( ).lock( );
    try {
      if ( ( tempObj = this.activeMap.remove( obj.getName( ) ) ) == null ) {
        tempObj = obj;
      }
      this.disabledMap.putIfAbsent( tempObj.getName( ), tempObj );
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
  }
  
  public List<String> listDisabledKeys( ) {
    this.canHas.readLock( ).lock( );
    try {
      return Lists.newArrayList( this.disabledMap.keySet( ) );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public List<TYPE> listDisabledValues( ) {
    this.canHas.readLock( ).lock( );
    try {
      return Lists.newArrayList( this.disabledMap.values( ) );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public List<String> listKeys( ) {
    this.canHas.readLock( ).lock( );
    try {
      return Lists.newArrayList( this.activeMap.keySet( ) );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public List<TYPE> listValues( ) {
    this.canHas.readLock( ).lock( );
    try {
      return Lists.newArrayList( this.activeMap.values( ) );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public TYPE replace( TYPE newValue ) throws NoSuchElementException {
    TYPE oldValue = null;
    this.canHas.writeLock( ).lock( );
    try {
      if ( ( oldValue = this.disabledMap.remove( newValue.getName( ) ) ) == null ) {
        oldValue = this.activeMap.replace( newValue.getName( ), newValue );
      } else {
        this.activeMap.put( newValue.getName( ), newValue );
      }
      if ( oldValue == null ) {
        throw new NoSuchElementException( "Can't find registered object: " + newValue.getName( ) + " in " + this.getClass( ).getSimpleName( ) );
      } else {
        return oldValue;
      }
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
  }
  
  public TYPE lookupDisabled( String name ) throws NoSuchElementException {
    this.canHas.readLock( ).lock( );
    try {
      if ( !this.disabledMap.containsKey( name ) ) {
        throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass( ).getSimpleName( ) );
      } else {
        return this.disabledMap.get( name );
      }
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public TYPE lookup( String name ) throws NoSuchElementException {
    this.canHas.readLock( ).lock( );
    try {
      if ( !this.activeMap.containsKey( name ) ) {
        throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass( ).getSimpleName( ) );
      } else {
        return this.activeMap.get( name );
      }
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public void disable( String name ) {
    this.canHas.writeLock( ).lock( );
    try {
      TYPE obj = null;
      if ( ( obj = this.activeMap.remove( name ) ) == null ) {
        throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass( ).getSimpleName( ) );
      } 
      this.disabledMap.putIfAbsent( obj.getName( ), obj );
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
  }
  
  public void enable( String name ) throws NoSuchElementException {
    this.canHas.writeLock( ).lock( );
    try {
      TYPE obj = null;
      if ( ( obj = this.disabledMap.remove( name ) ) == null ) {
        throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass( ).getSimpleName( ) );
      } 
      this.activeMap.putIfAbsent( obj.getName( ), obj );
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
  }
  
  public boolean contains( String name ) {
    this.canHas.readLock( ).lock( );
    try {
      return this.activeMap.containsKey( name ) || this.disabledMap.containsKey( name );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  protected ConcurrentNavigableMap<String, TYPE> getActiveMap( ) {
    return activeMap;
  }
  
  protected ConcurrentNavigableMap<String, TYPE> getDisabledMap( ) {
    return disabledMap;
  }

  public TYPE enableFirst() throws NoSuchElementException {
    this.canHas.writeLock( ).lock( );
    try {      
      Map.Entry<String,TYPE> entry = this.disabledMap.pollFirstEntry( );
      if( entry == null ) {
        throw new NoSuchElementException( "Disabled map is empty." );
      }
      TYPE first = entry.getValue( );
      this.activeMap.put( first.getName( ), first );
      return first;
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
  }
  
  @Override
  public String toString( ) {
    return String.format( "%s [activeMap=%s, disabledMap=%s]", this.getClass( ).getSimpleName( ), this.activeMap, this.disabledMap );
  }
  
}
