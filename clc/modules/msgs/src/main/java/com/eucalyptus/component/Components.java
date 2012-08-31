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

package com.eucalyptus.component;

import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.Mbeans;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Components {
  private static Logger                                                 LOG        = Logger.getLogger( Components.class );
  private static ConcurrentMap<Class<? extends ComponentId>, Component> components = Maps.newConcurrentMap( );
  
  public static List<ComponentId> toIds( final List<Component> components ) {
    return Lists.transform( components, ToComponentId.INSTANCE );
  }
  
  /**
   * Components which are staticly determined as ones to load. This determination is made
   * independent of access to the database; i.e. only the command line flags and presence/absence of
   * files determines this list.
   * 
   * @return
   */
  public static Iterable<Component> whichCanLoad( ) {
    return Iterables.filter( Components.list( ), Predicates.BOOTSTRAP_LOAD_LOCAL );
  }
  
  public static Iterable<Component> whichCanEnable( ) {
    return Iterables.filter( Components.list( ), Predicates.BOOTSTRAP_ENABLE_LOCAL );
  }
  
  public static Iterable<Component> whichCanLoadOnRemoteHost( ) {
    return Iterables.filter( Components.list( ), Predicates.BOOTSTRAP_LOAD_REMOTE );
  }
  
  public static Iterable<Component> whichCanEnabledOnRemoteHost( ) {
    return Iterables.filter( Components.list( ), Predicates.BOOTSTRAP_ENABLE_REMOTE );
  }
  
  /**
   * Component has a service instance which is present locally, independent of the service's state.
   * 
   * @return
   */
  public static List<Component> whichAreEnabledLocally( ) {
    return Lists.newArrayList( Iterables.filter( Components.list( ), Predicates.ARE_ENABLED_LOCAL ) );
  }
  
  /**
   * Component has a service instance which is present locally and the service is ENABLED.
   * 
   * @return
   */
  public static List<Component> whichAreEnabled( ) {
    return Lists.newArrayList( Iterables.filter( Components.list( ), Predicates.ARE_ENABLED ) );
  }
  
  public static List<Component> list( ) {//TODO:GRZE: review all usage of this and replace with Components.whichAre...
    return ImmutableList.copyOf( components.values( ) );
  }
  
  /**
   * GRZE:NOTE: this should only ever be applied to user input and /never/ called with a hardcoded
   * string.
   */
  public static <T extends ComponentId> Component lookup( final String componentIdName ) throws NoSuchElementException {
    return lookup( IdNameToId.INSTANCE.apply( componentIdName ) );
  }
  
  public static <T extends ComponentId> Component lookup( final Class<T> componentId ) throws NoSuchElementException {
    return lookup( IdClassToId.INSTANCE.apply( componentId ) );
  }
  
  public static Component lookup( final ComponentId componentId ) throws NoSuchElementException {
    return IdToComponent.INSTANCE.apply( componentId );
  }
  
  public static boolean contains( final ComponentId component ) {
    return components.containsKey( component );
  }
  
  public static Component create( final ComponentId id ) throws ServiceRegistrationException {
    if ( !components.containsKey( id ) ) {
      final Component c = new Component( id );
      components.put( id.getClass( ), c );
      EventRecord.here( Bootstrap.class, EventType.COMPONENT_REGISTERED, c.toString( ) ).info( );
      Mbeans.register( c );
      return c;
    } else {
      return components.get( id.getClass( ) );
    }
  }
  
  public static String describe( final Component comp ) {
    try {
      return componentToString( ).apply( comp );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      try {
        return "Error attempting to convert component to string: " + comp.toString( ) + " because of: " + ex.getMessage( );
      } catch ( final Exception ex1 ) {
        Logs.extreme( ).error( ex, ex );
        return "Error attempting to convert component to string: " + comp.getName( ) + " because of: " + ex.getMessage( );
      }
    }
  }
  
  enum ToComponentId implements Function<Component, ComponentId> {
    INSTANCE;
    @Override
    public ComponentId apply( final Component input ) {
      return input.getComponentId( );
    }
  }
  
  enum IdNameToId implements Function<String, ComponentId> {
    INSTANCE;
    @Override
    public ComponentId apply( final String input ) {
      return ComponentIds.lookup( input );
    }
  }
  
  enum IdClassToId implements Function<Class<? extends ComponentId>, ComponentId> {
    INSTANCE;
    @Override
    public ComponentId apply( final Class<? extends ComponentId> input ) {
      return ComponentIds.lookup( input );
    }
  }
  
  enum IdToComponent implements Function<ComponentId, Component> {
    INSTANCE;
    @Override
    public Component apply( final ComponentId input ) {
      if ( !components.containsKey( input.getClass( ) ) ) {
        try {
          Components.create( input );
          return Components.lookup( input );
        } catch ( ServiceRegistrationException ex ) {
          throw new NoSuchElementException( "Missing entry for component '" + input );
        }
      } else {
        return components.get( input.getClass( ) );
      }
    }
  }
  
  enum ComponentToLocalService implements Function<Component, ServiceConfiguration> {
    INSTANCE;
    @Override
    public ServiceConfiguration apply( final Component input ) {
      return input.getLocalServiceConfiguration( );
    }
  }
  
  enum ToString implements Function<Component, String> {
    INSTANCE;
    @Override
    public String apply( final Component comp ) {
      final StringBuilder buf = new StringBuilder( );
      buf.append( LogUtil.header( comp.toString( ) ) ).append( "\n" );
      for ( final Bootstrapper b : comp.getBootstrappers( ) ) {
        buf.append( "-> " + b.toString( ) ).append( "\n" );
      }
      buf.append( LogUtil.subheader( comp.getName( ) + " services" ) ).append( "\n" );
      for ( final ServiceConfiguration s : comp.services( ) ) {
        try {
          buf.append( "->  Service:          " ).append( s.getFullName( ) ).append( " " ).append( ServiceUris.remote( s ) ).append( "\n" );
          buf.append( "|-> Service config:   " ).append( s ).append( "\n" );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      }
      return buf.toString( );
    }
  }
  
  public static Function<Component, String> componentToString( ) {
    return ToString.INSTANCE;
  }
  
  private enum Predicates implements Predicate<Component> {
    BOOTSTRAP_LOAD_LOCAL {
      @Override
      public boolean apply( final Component component ) {
        final ComponentId c = component.getComponentId( );
        final boolean cloudLocal = BootstrapArgs.isCloudController( ) && c.isCloudLocal( ) && !c.isRegisterable( );
        final boolean isCloudItself = BootstrapArgs.isCloudController( ) && Eucalyptus.class.equals( c.getClass( ) );
        final boolean alwaysLocal = c.isAlwaysLocal( ) && !c.isRegisterable( );
        final boolean isBootrapperItself = Empyrean.class.equals( c.getClass( ) );
        return cloudLocal || alwaysLocal || isBootrapperItself || isCloudItself;
      }
    },
    BOOTSTRAP_ENABLE_LOCAL {
      @Override
      public boolean apply( final Component component ) {
        final ComponentId c = component.getComponentId( );
        final boolean cloudLocal = BootstrapArgs.isCloudController( ) && c.isCloudLocal( ) && !c.isRegisterable( );
        final boolean isCloudItself = BootstrapArgs.isCloudController( ) && Eucalyptus.class.equals( c.getClass( ) );
        final boolean alwaysLocal = c.isAlwaysLocal( ) && !c.isRegisterable( );
        final boolean isBootrapperItself = Empyrean.class.equals( c.getClass( ) );
        return cloudLocal || alwaysLocal || isBootrapperItself || isCloudItself;
      }
    },
    BOOTSTRAP_LOAD_REMOTE {
      @Override
      public boolean apply( final Component component ) {
        final ComponentId c = component.getComponentId( );
        final boolean cloudLocal = c.isCloudLocal( ) && !c.isRegisterable( );
        final boolean isCloudItself = Eucalyptus.class.equals( c.getClass( ) );
        final boolean alwaysLocal = c.isAlwaysLocal( ) && !c.isRegisterable( );
        final boolean isBootrapperItself = Empyrean.class.equals( c.getClass( ) );
        return cloudLocal || alwaysLocal || isBootrapperItself || isCloudItself;
      }
    },
    BOOTSTRAP_ENABLE_REMOTE {
      @Override
      public boolean apply( final Component component ) {
        return BOOTSTRAP_LOAD_REMOTE.apply( component );
      }
    },
    ARE_ENABLED_LOCAL {
      @Override
      public boolean apply( final Component component ) {
        final ComponentId compId = component.getComponentId( );
        final boolean cloudLocal = BootstrapArgs.isCloudController( ) && compId.isCloudLocal( ) && !compId.isRegisterable( );
        final boolean alwaysLocal = compId.isAlwaysLocal( );
        final boolean runningLocal = component.isEnabledLocally( );
        return cloudLocal || alwaysLocal || runningLocal;
      }
    },
    ARE_ENABLED {
      @Override
      public boolean apply( final Component c ) {
        final NavigableSet<ServiceConfiguration> services = c.services( );
        return services.isEmpty( )
          ? false
          : Component.State.ENABLED.equals( services.first( ).lookupState( ) );
      }
    }
    
  }
  
  /**
   * @param config
   * @return
   */
  public static Component lookup( ServiceConfiguration config ) {
    return lookup( config.getComponentId( ) );
  }
  
}
