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

package com.eucalyptus.component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.util.Classes;
import com.google.common.base.Predicates;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MutableClassToInstanceMap;

public class ComponentIds {
  private static Logger                                          LOG        = Logger.getLogger( ComponentIds.class );
  private static final ClassToInstanceMap<ComponentId>           compIdMap  = MutableClassToInstanceMap.create( );
  private static final Map<String, Class<? extends ComponentId>> compIdName = Maps.newHashMap( );
  
  public static List<ComponentId> list( ) {
    return Lists.newArrayList( compIdMap.values( ) );
  }
  
  @SuppressWarnings( "unchecked" )
  public final static <T extends ComponentId> T lookup( final Class<T> compIdClass ) {
    if ( !compIdMap.containsKey( compIdClass ) ) {
      T newInstance = Classes.newInstance( compIdClass );
      compIdMap.putInstance( compIdClass, newInstance );
      compIdName.put( newInstance.name( ), compIdClass );
      LOG.debug( "Registered ComponentId: " + compIdClass.getCanonicalName( ) );
      return newInstance;
    } else {
      return ( T ) compIdMap.get( compIdClass );
    }
  }
  
  /**
   * Lookup the ComponentId with name <tt>name</tt>. Note that this method is case-insensitive in
   * that the lower-case of <tt>name</tt> is compared to the l-case of ComponentId names.
   * 
   * @param name
   * @throws NoSuchElementException
   * @return
   */
  public final static ComponentId lookup( final String name ) {
    if ( !compIdName.containsKey( name.toLowerCase( ) ) ) {
      throw new NoSuchElementException( "No ComponentId with name: " + name.toLowerCase( ) );
    } else {
      return compIdMap.get( compIdName.get( name.toLowerCase( ) ) );
    }
  }
  
  static ComponentId createEphemeral( String componentIdName ) {
    return new ComponentId( componentIdName ) {{}};
  }
}
