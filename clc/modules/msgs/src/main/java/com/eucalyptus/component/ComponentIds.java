/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.util.Classes;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MutableClassToInstanceMap;

public class ComponentIds {
  private static Logger                                          LOG        = Logger.getLogger( ComponentIds.class );
  private static final ClassToInstanceMap<ComponentId>           compIdMap  = MutableClassToInstanceMap.create( );
  private static final Map<String, Class<? extends ComponentId>> compIdName = Maps.newHashMap( );
  private static final Map<String, Class<? extends ComponentId>> compIdNames = Maps.newHashMap( );

  public static List<Class<? extends ComponentId>> listTypes( ) {
    return Lists.newArrayList( compIdMap.keySet( ) );
  }

  public static List<ComponentId> list( ) {
    return Lists.newArrayList( compIdMap.values( ) );
  }
  
  @SuppressWarnings( "unchecked" )
  public final static <T extends ComponentId> T lookup( final Class<T> compIdClass ) {
    if ( !compIdMap.containsKey( compIdClass ) ) {
      T newInstance = Classes.newInstance( compIdClass );
      compIdMap.putInstance( compIdClass, newInstance );
      compIdName.put( newInstance.name( ), compIdClass );
      for ( final String name : newInstance.getServiceNames( ) ) {
        compIdNames.put( name, compIdClass );
      }
      LOG.debug( "Registered ComponentId: " + compIdClass.toString( ) );
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

  /**
   * Lookup the ComponentId with dns name <tt>name</tt>.
   *
   * @throws NoSuchElementException If not found
   */
  public static ComponentId lookupByDnsName( final String name ) {
    if ( !compIdNames.containsKey( name.toLowerCase( ) ) ) {
      throw new NoSuchElementException( "No ComponentId with dns name: " + name.toLowerCase( ) );
    } else {
      return compIdMap.get( compIdNames.get( name.toLowerCase( ) ) );
    }
  }

  public static Predicate<ComponentId> manyToOne( ) {
    return ComponentIdPredicates.MANY_TO_ONE;
  }
  
  static ComponentId createEphemeral( String componentIdName ) {
    return new ComponentId( componentIdName ) {{}};
  }

  public static Function<ComponentId,String> name( ) {
    return ComponentIdStringFunctions.NAME;
  }

  private enum ComponentIdPredicates implements Predicate<ComponentId> {
    MANY_TO_ONE{
      @Override
      public boolean apply( @Nullable final ComponentId componentId ) {
        return componentId != null && componentId.isManyToOnePartition( );
      }
    }
  }

  private enum ComponentIdStringFunctions implements Function<ComponentId,String> {
    NAME {
      @Nullable
      @Override
      public String apply( @Nullable final ComponentId componentId ) {
        return componentId == null ? null : componentId.name( );
      }
    }
  }
}
