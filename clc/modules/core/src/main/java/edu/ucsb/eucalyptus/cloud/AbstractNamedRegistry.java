/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud;

import edu.ucsb.eucalyptus.constants.HasName;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public abstract class AbstractNamedRegistry<TYPE extends HasName> {

  private ConcurrentNavigableMap<String, TYPE> activeMap;
  private ConcurrentNavigableMap<String, TYPE> disabledMap;

  protected AbstractNamedRegistry() {
    this.activeMap = new ConcurrentSkipListMap<String, TYPE>();
    this.disabledMap = new ConcurrentSkipListMap<String, TYPE>();
  }

  public Set<String> getKeys() {
    return this.activeMap.keySet();
  }

  public Set<String> getDisabledKeys() {
    return this.disabledMap.keySet();
  }

  public boolean isRegistered( String key ) {
    return this.activeMap.containsKey( key );
  }

  public Collection<TYPE> getEntries() {
    return this.activeMap.values();
  }

  public Collection<TYPE> getDisabledEntries() {
    return this.disabledMap.values();
  }

  public void deregister( String key ) {
    this.disabledMap.remove( key );
    this.activeMap.remove( key );
  }

  public void register( TYPE obj ) // TODO: should throw duplicate element exception
  {
    if ( this.disabledMap.containsKey( obj.getName() ) ) {
      TYPE tempObj = this.disabledMap.remove( obj.getName() );
      this.activeMap.putIfAbsent( tempObj.getName(), tempObj );
    } else
      this.activeMap.putIfAbsent( obj.getName(), obj );
  }

  public void registerDisabled( TYPE obj ) // TODO: should throw duplicate element exception
  {
    if ( this.activeMap.containsKey( obj.getName() ) ) {
      TYPE tempObj = this.activeMap.remove( obj.getName() );
      this.disabledMap.putIfAbsent( tempObj.getName(), tempObj );
    } else
      this.disabledMap.putIfAbsent( obj.getName(), obj );
  }

  public List<String> listDisabledKeys() {
    List<String> keyList = new ArrayList<String>();
    keyList.addAll( this.disabledMap.keySet() );
    return keyList;
  }

  public List<TYPE> listDisabledValues() {
    List<TYPE> valueList = new ArrayList<TYPE>();
    valueList.addAll( this.disabledMap.values() );
    return valueList;
  }


  public List<String> listKeys() {
    List<String> keyList = new ArrayList<String>();
    keyList.addAll( this.activeMap.keySet() );
    return keyList;
  }

  public List<TYPE> listValues() {
    List<TYPE> valueList = new ArrayList<TYPE>();
    valueList.addAll( this.activeMap.values() );
    return valueList;
  }

  public TYPE replace( String name, TYPE newValue ) throws NoSuchElementException {
    if ( this.activeMap.containsKey( name ) ) return this.activeMap.replace( name, newValue );
    if ( this.disabledMap.containsKey( name ) ) {
      this.disabledMap.remove( name );
      return this.activeMap.putIfAbsent( name, newValue );
    }
    throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass().getSimpleName() );
  }

  public TYPE lookupDisabled( String name ) throws NoSuchElementException {
    if ( !this.disabledMap.containsKey( name ) )
      throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass().getSimpleName() );
    return this.disabledMap.get( name );
  }

  public TYPE lookup( String name ) throws NoSuchElementException {
    if ( !this.activeMap.containsKey( name ) )
      throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass().getSimpleName() );
    return this.activeMap.get( name );
  }

  public void disable( String name ) throws NoSuchElementException {
    if ( !this.activeMap.containsKey( name ) )
      throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass().getSimpleName() );
    TYPE obj = this.activeMap.remove( name );
    this.disabledMap.putIfAbsent( obj.getName(), obj );
  }

  public void enable( String name ) throws NoSuchElementException {
    if ( !this.disabledMap.containsKey( name ) )
      throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass().getSimpleName() );
    TYPE obj = this.disabledMap.remove( name );
    this.activeMap.putIfAbsent( obj.getName(), obj );
  }

  public boolean contains( String name ) {
    return this.activeMap.containsKey( name ) || this.disabledMap.containsKey( name );
  }

  public ConcurrentNavigableMap<String, TYPE> getActiveMap() {
    return activeMap;
  }

  public ConcurrentNavigableMap<String, TYPE> getDisabledMap() {
    return disabledMap;
  }
}
