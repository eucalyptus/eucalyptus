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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
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
