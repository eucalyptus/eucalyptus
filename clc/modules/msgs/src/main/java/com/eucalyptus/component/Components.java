/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.Callback;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class Components {
  private static Logger                            LOG                  = Logger
                                                                                                                                      .getLogger( Components.class );
  private static ConcurrentMap<Class, Map>         componentInformation = new ConcurrentHashMap<Class, Map>( ) {
                                                                          {
                                                                            put( Service.class, new ConcurrentHashMap<String, Service>( ) );
                                                                            put( Component.class, new ConcurrentHashMap<String, Component>( ) );
                                                                            put( ComponentId.class, new ConcurrentHashMap<String, ComponentId>( ) );
                                                                          }
                                                                        };
  
  public static List<Component> listEnabled( ) {
    List<Component> components = Lists.newArrayList( );
    if ( Components.lookup( Eucalyptus.class ).isAvailableLocally( ) ) {
      for ( Component comp : Components.list( ) ) {
        if ( comp.getIdentity( ).isCloudLocal( ) ) {
          components.add( comp );
        }
      }
    }
    for ( Component comp : Components.list( ) ) {
      if ( comp.isRunningLocally( ) ) {
        if ( !comp.getIdentity( ).isCloudLocal( ) ) {
          components.add( comp );
        }
      }
    }
    return components;
  }

  @SuppressWarnings( "unchecked" )
  public static List<Component> list( ) {
    return new ArrayList( Components.lookupMap( Component.class ).values( ) );
  }
  
  private static <T extends ComponentInformation> Class getRealType( Class<T> maybeSubclass ) {
    Class type = null;
    for ( Class c : componentInformation.keySet( ) ) {
      if ( c.isAssignableFrom( maybeSubclass ) ) {
        type = c;
        return type;
      }
    }
    Components.dumpState( );
    throw BootstrapException.throwFatal( "Failed bootstrapping component registry.  Missing entry for component info type: " + maybeSubclass.getSimpleName( ) );
  }
  
  static <T> Map<String, T> lookupMap( Class type ) {
    return ( Map<String, T> ) componentInformation.get( getRealType( type ) );
  }
  
  static void dumpState( ) {
    for ( Class c : componentInformation.keySet( ) ) {
      for ( Entry<String, ComponentInformation> e : (Set<Entry<String, ComponentInformation>>)componentInformation.get( c ).entrySet( ) ) {
        LOG.info( EventRecord.here( Bootstrap.class, EventType.COMPONENT_REGISTRY_DUMP, c.getSimpleName( ), e.getKey( ), e.getValue( ).getClass( )
                                                                                                                          .getCanonicalName( ) ) );
      }
    }
  }
  
  public static <T extends ComponentInformation> boolean contains( Class<T> type, String name ) {
    return Components.lookupMap( type ).containsKey( name );
  }
  
  private static <T extends ComponentInformation> void remove( T componentInfo ) {
    Map<String, T> infoMap = lookupMap( componentInfo.getClass( ) );
    infoMap.remove( componentInfo.getName( ) );
  }
  
  private static <T extends ComponentInformation> void put( T componentInfo ) {
    Map<String, T> infoMap = lookupMap( componentInfo.getClass( ) );
    if ( infoMap.containsKey( componentInfo.getName( ) ) ) {
      throw BootstrapException.throwFatal( "Failed bootstrapping component registry.  Duplicate information for component '" + componentInfo.getName( ) + "': "
                                           + componentInfo.getClass( ).getSimpleName( ) + " as " + getRealType( componentInfo.getClass( ) ) );
    } else {
      infoMap.put( componentInfo.getName( ), componentInfo );
    }
  }
  
  public static <T extends ComponentInformation> void deregister( T componentInfo ) {
    remove( componentInfo );
    if ( componentInfo instanceof Component ) {
      EventRecord.here( Bootstrap.class, EventType.COMPONENT_DEREGISTERED, componentInfo.toString( ) ).info( );
    } else {
      EventRecord.here( Bootstrap.class, EventType.COMPONENT_DEREGISTERED, componentInfo.getName( ), componentInfo.getClass( ).getSimpleName( ) ).trace( );
    }
  }
  
  static <T extends ComponentInformation> void register( T componentInfo ) {
    if ( !contains( componentInfo.getClass( ), componentInfo.getName( ) ) ) {
      if ( componentInfo instanceof Component ) {
        EventRecord.here( Bootstrap.class, EventType.COMPONENT_REGISTERED, componentInfo.toString( ) ).info( );
      } else {
        EventRecord.here( Bootstrap.class, EventType.COMPONENT_REGISTERED, componentInfo.getName( ), componentInfo.getClass( ).getSimpleName( ) ).trace( );
      }
      Components.put( componentInfo );
    }
  }
  
  public static <T extends ComponentInformation> T lookup( Class<T> type, String name ) throws NoSuchElementException {
    if ( !contains( type, name ) ) {
      try {
        ComponentId compId = ComponentIds.lookup( name );
        Components.create( compId );
        return Components.lookup( type, name );
      } catch ( ServiceRegistrationException ex ) {
        throw new NoSuchElementException( "Missing entry for component '" + name + "' info type: " + type.getSimpleName( ) + " ("
                                          + getRealType( type ).getCanonicalName( ) );
      }
    } else {
      return ( T ) Components.lookupMap( type ).get( name );
    }
  }
  
  public static Component lookup( String componentName ) throws NoSuchElementException {
    return Components.lookup( Component.class, componentName );
  }
  
  public static <T extends ComponentId> Component lookup( Class<T> componentId ) throws NoSuchElementException {
    return Components.lookup( ComponentIds.lookup( componentId ) );
  }

  public static Component lookup( ComponentId componentId ) throws NoSuchElementException {
    return Components.lookup( Component.class, componentId.getName( ) );
  }

  public static Service lookup( ServiceConfiguration config ) throws NoSuchElementException {
    for( Service s : Components.lookup( config.getComponentId( ) ).getServices( ) ) {
      if( s.getServiceConfiguration( ).equals( config ) ) {
        return s;
      }
    }
    throw new NoSuchElementException( "Failed to find service corresponding to " + config.toString( ) );
  }

  public static boolean contains( String componentName ) {
    return Components.contains( Component.class, componentName );
  }
  
  public static Component create( ComponentId id ) throws ServiceRegistrationException {
    Component c = new Component( id );
    register( c );
    return c;
  }

  private final static Function<Component, String> componentToString = componentToString( );
  
  public static Function<Component, String> componentToString( ) {
    if ( componentToString != null ) {
      return componentToString;
    } else {
      synchronized ( Components.class ) {
        return new Function<Component, String>( ) {
          
          @Override
          public String apply( Component comp ) {
            final StringBuilder buf = new StringBuilder( );
            buf.append( LogUtil.header( comp.getName( ) + " component configuration" ) ).append( "\n" );
            buf.append( "-> Enabled/Local:      " + comp.isAvailableLocally( ) + "/" + comp.isLocal( ) ).append( "\n" );
            buf.append( "-> State/Running:      " + comp.getState( ) + "/" + comp.isRunningLocally( ) ).append( "\n" );
            buf.append( "-> Builder:            "
                        + comp.getBuilder( ).getClass( ).getSimpleName( ) ).append( "\n" );
            buf.append( "-> Disable/Remote cli: "
                        + System.getProperty( "euca." + comp.getIdentity( ).name( ) + ".disable" )
                        + "/"
                        + System.getProperty( "euca." + comp.getIdentity( ).name( ) + ".remote" ) ).append( "\n" );
            for ( Bootstrapper b : comp.getBootstrapper( ).getBootstrappers( ) ) {
              buf.append( "-> " + b.toString( ) ).append( "\n" );
            }
            buf.append( LogUtil.subheader( comp.getName( ) + " services" ) ).append( "\n" );
            for ( Service s : comp.getServices( ) ) {
              buf.append( "->  Service:          " + s.getName( ) + " " + s.getUri( ) ).append( "\n" );
              buf.append( "|-> Dispatcher:       " + s.getDispatcher( ).getName( ) + " for "
                          + s.getDispatcher( ).getAddress( ) ).append( "\n" );
              buf.append( "|-> Service Endpoint: " + s.getEndpoint( ) ).append( "\n" );
              buf.append( "|-> Service config:   "
                          + LogUtil.dumpObject( s.getServiceConfiguration( ) ) ).append( "\n" );
            //TODO: restore this.          destinationBuffer.append( "|-> Credential DN:    " + s.getKeys( ).getCertificate( ).getSubjectDN( ).toString( ) );
            }
            return buf.toString( );
          }
        };
      }
    }
  }
  
  public static Component oneWhichHandles( Class c ) {
    return ServiceBuilderRegistry.handles( c ).getComponent( );
  }
  
  private static final Callback.Success<Component> componentPrinter = componentPrinter( );
  
  public static Callback.Success<Component> componentPrinter( ) {
    if ( componentPrinter != null ) {
      return componentPrinter;
    } else {
      synchronized ( Components.class ) {
        return new Callback.Success<Component>( ) {
          
          @Override
          public void fire( Component comp ) {
            LOG.info( componentToString.apply( comp ) );
          }
        };
      }
    }
  }
  
  private static final Function<Dispatcher, String> dispatcherToString = dispatcherToString( );
  
  public static Function<Dispatcher, String> dispatcherToString( ) {
    if ( dispatcherToString != null ) {
      return dispatcherToString;
    } else {
      synchronized ( Components.class ) {
        return new Function<Dispatcher, String>( ) {
          
          @Override
          public String apply( Dispatcher comp ) {
            final StringBuilder buf = new StringBuilder( );
            buf.append( "-> Dispatcher key=" ).append( comp.getName( ) ).append( " entry=" ).append( comp );
            return buf.toString( );
          }
        };
      }
    }
  }
  
  private static final Callback.Success<Dispatcher> dispatcherPrinter = dispatcherPrinter( );
  
  public static Callback.Success<Dispatcher> dispatcherPrinter( ) {
    if ( dispatcherPrinter != null ) {
      return dispatcherPrinter;
    } else {
      synchronized ( Components.class ) {
        return new Callback.Success<Dispatcher>( ) {
          
          @Override
          public void fire( Dispatcher arg0 ) {
            LOG.info( dispatcherToString.apply( arg0 ) );
          }
        };
      }
    }
  }
  
  private final static Callback.Success<Component> configurationPrinter = configurationPrinter( );
  
  public static Callback.Success<Component> configurationPrinter( ) {
    if ( configurationPrinter != null ) {
      return configurationPrinter;
    } else {
      synchronized ( Components.class ) {
        return new Callback.Success<Component>( ) {
          @Override
          public void fire( Component comp ) {
            LOG.info( configurationToString.apply( comp ) );
          }
        };
      }
    }
  }
  
  private final static Function<Component, String>    configurationToString = configurationToString( );
  private static final Function<Bootstrapper, String> bootstrapperToString  = new Function<Bootstrapper, String>( ) {
                                                                              @Override
                                                                              public String apply( Bootstrapper b ) {
                                                                                return b.getClass( ).getName( )
                                                                                       + " provides=" + b.getProvides( )
                                                                                       + " deplocal=" + b.getDependsLocal( )
                                                                                       + " depremote=" + b.getDependsRemote( );
                                                                              }
                                                                            };
  
  public static Function<Component, String> configurationToString( ) {
    if ( configurationToString != null ) {
      return configurationToString;
    } else {
      synchronized ( Components.class ) {
        return new Function<Component, String>( ) {
          
          @Override
          public String apply( Component comp ) {
            final StringBuilder buf = new StringBuilder( );
            buf.append( String.format( "%s -> disable/remote cli:   %s/%s",
                                       comp.getName( ),
                                       System.getProperty( String.format( "euca.%s.disable", comp.getIdentity( ).name( ) ) ),
                                       System.getProperty( String.format( "euca.%s.remote", comp.getIdentity( ).name( ) ) ) ) ).append( "\n" );
            buf.append( String.format( "%s -> enabled/local/init:   %s/%s/%s",
                                       comp.getName( ), comp.isAvailableLocally( ), comp.isLocal( ), comp.isRunningLocally( ) ) ).append( "\n" );
            buf.append( String.format( "%s -> bootstrappers:        %s", comp.getName( ),
                                       Iterables.transform( comp.getBootstrapper( ).getBootstrappers( ), bootstrapperToString ) ) ).append( "\n" );
            return buf.toString( );
          }
        };
        
      }
    }
  }
}
