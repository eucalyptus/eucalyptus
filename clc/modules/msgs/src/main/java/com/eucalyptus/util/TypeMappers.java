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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Ats;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

public class TypeMappers {
  private enum CompareClasses implements Comparator<Class> {
    INSTANCE;
    
    @Override
    public int compare( Class o1, Class o2 ) {
      if ( o1 == null && o2 == null ) {
        return 0;
      } else if ( o1 != null && o2 != null ) {
        return ( "" + o1.toString( ) ).compareTo( "" + o2.toString( ) );
      } else {
        return ( o1 != null
          ? 1
          : -1 );
      }
    }
    
  }
  
  private static Logger                          LOG          = Logger.getLogger( TypeMappers.class );
  private static SortedSetMultimap<Class, Class> knownMappers = TreeMultimap.create( CompareClasses.INSTANCE, CompareClasses.INSTANCE );
  private static Map<String, Function>           mappers      = Maps.newHashMap( );
  
  public static <A, B> B transform( A from, Class<B> to ) {
    Class target = from.getClass( );
    for ( Class p : Classes.ancestors( from ) ) {
      if ( knownMappers.containsKey( p ) && !knownMappers.get( p ).isEmpty( ) ) {
        target = p;
        break;
      }
    }
    Function func = lookup( target, to );
    return ( B ) func.apply( from );
  }
  
  public static <A, B> Function<A, B> lookup( Class<A> a, Class<B> b ) {
    checkParam( knownMappers.keySet(), hasItem( a ) );
    checkParam( knownMappers.get( a ), hasItem( b ) );
    String key = Joiner.on( "=>" ).join( a, b );
    return mappers.get( key );
  }
  
  public static class TypeMapperDiscovery extends ServiceJarDiscovery {
    
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( Ats.from( candidate ).has( TypeMapper.class ) && Function.class.isAssignableFrom( candidate ) ) {
        TypeMapper mapper = Ats.from( candidate ).get( TypeMapper.class );
        Class[] types = mapper.value( );
        List<Class> generics = Lists.newArrayList( );
        try {
          generics.addAll( Classes.genericsToClasses( Classes.newInstance( candidate ) ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
        if ( generics.size( ) != 2 ) {
          LOG.error( candidate + " looks like it is a @TypeMapper but needs generics: "
                     + generics );
          return false;
        } else {
          try {
            registerMapper( generics.get( 0 ), generics.get( 1 ), ( Function ) Classes.newInstance( candidate ) );
            return true;
          } catch ( Exception ex1 ) {
            LOG.error( ex1, ex1 );
          }
        }
      }
      return false;
    }
    
    @Override
    public Double getPriority( ) {
      return 0.3d;
    }
    
  }
  
  private static void registerMapper( Class from, Class to, Function mapper ) {
    EventRecord.here( TypeMapperDiscovery.class, EventType.BOOTSTRAP_INIT_DISCOVERY, "mapper", from.getCanonicalName( ), to.getCanonicalName( ),
                      mapper.getClass( ).getCanonicalName( ) ).info( );
    String key = Joiner.on( "=>" ).join( from, to );
    checkParam( knownMappers.get( from ), not( hasItem( to ) ) );
    checkParam( mappers, not( hasKey( key ) ) );
    knownMappers.put( from, to );
    mappers.put( key, mapper );
    
  }
}
