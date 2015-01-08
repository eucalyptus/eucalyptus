package com.eucalyptus.component.groups;

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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

import java.util.Collection;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.util.Classes;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
public class ServiceGroups {
  private static Logger LOG = Logger.getLogger( ServiceGroups.class );
  
  enum ServiceGroupTableSupplier implements Supplier<Multimap<ServiceGroup, ? extends ComponentId>> {
    INSTANCE {
      @Override
      public Multimap<ServiceGroup, ? extends ComponentId> get( ) {
        return memo.get( );
      }
    },
    REAL {
      @Override
      public Multimap<ServiceGroup, ? extends ComponentId> get( ) {
        Multimap<ServiceGroup, ComponentId> map = ArrayListMultimap.create( );
        for ( Class<? extends ComponentId> g : listGroups( ) ) {
          ServiceGroup svc = ( ServiceGroup ) Classes.newInstance( g );
          map.putAll( svc, svc.list( ) );
        }
        return map;
      }
    };
    private static Supplier<Multimap<ServiceGroup, ? extends ComponentId>> memo = Suppliers.memoize( REAL );
    
  }
  
  public static ServiceGroup lookup( ServiceConfiguration config ) {
    if ( isGroup( config ) ) {
      return ( ServiceGroup ) config.getComponentId( );
    } else {
      throw new NoSuchElementException( "Failed to lookup service group for: " + config );
    }
  }
  
  public static boolean isGroup( ServiceConfiguration config ) {
    return config.getComponentId( ) instanceof ServiceGroup;
  }
  
  public static Collection<ServiceGroup> listMembership( ComponentId member ) {
    Collection<ServiceGroup> members = Lists.newArrayList( );
    for ( ServiceGroup g : ServiceGroupTableSupplier.INSTANCE.get( ).keySet( ) ) {
      if ( g.apply( member ) ) {
        members.add( g );
      }
    }
    return members;
  }
  
  public static Collection<? extends ComponentId> listMembers( ServiceGroup g ) {
    return ServiceGroupTableSupplier.INSTANCE.get( ).get( g );
  }
  
  public static Collection<Class<? extends ComponentId>> listGroups( ) {
    return Collections2.filter( ComponentIds.listTypes( ), Classes.subclassOf( ServiceGroup.class ) );
  }
  
  public static Collection<ServiceConfiguration> list( final ServiceGroupConfiguration config ) {
    Collection<ServiceConfiguration> members = Lists.newArrayList( );
    for ( ComponentId compId : Collections2.filter( ComponentIds.list( ), config.lookupServiceGroup( ) ) ) {
      members.addAll( Collections2.filter( Components.lookup( compId ).services( ), config.filterMembers() ) );
    }
    return members;
  }

}
