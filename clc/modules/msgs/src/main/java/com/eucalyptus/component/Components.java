package com.eucalyptus.component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.Callback;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class Components {
  private static Logger                                                                                  LOG                  = Logger
                                                                                                                                      .getLogger( Components.class );
  private static Map<Class<? extends ComponentInformation>, Map<String, ? extends ComponentInformation>> componentInformation = new ConcurrentHashMap<Class<? extends ComponentInformation>, Map<String, ? extends ComponentInformation>>( );
  static {
    componentInformation.put( Service.class, new ConcurrentHashMap<String, Service>( ) );
    componentInformation.put( Component.class, new ConcurrentHashMap<String, Component>( ) );
    componentInformation.put( Lifecycle.class, new ConcurrentHashMap<String, Lifecycle>( ) );
    componentInformation.put( Configuration.class, new ConcurrentHashMap<String, Configuration>( ) );
    //TODO: do this during discovery!
  }
  public static com.eucalyptus.bootstrap.Component                                                       delegate             = com.eucalyptus.bootstrap.Component.eucalyptus;
  
  @SuppressWarnings( "unchecked" )
  public static List<Component> list( ) {
    return new ArrayList( Components.lookup( Component.class ).values( ) );
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
  
  private static <T> Map<String, T> lookup( Class type ) {
    return ( Map<String, T> ) componentInformation.get( getRealType( type ) );
  }
  
  private static void dumpState( ) {
    for ( Class c : componentInformation.keySet( ) ) {
      for ( Entry<String, ? extends ComponentInformation> e : componentInformation.get( c ).entrySet( ) ) {
        LOG.info( EventRecord.here( Bootstrap.class, EventType.COMPONENT_REGISTRY_DUMP, c.getSimpleName( ), e.getKey( ), e.getValue( ).getClass( )
                                                                                                                          .getCanonicalName( ) ) );
      }
    }
  }
  
  public static <T extends ComponentInformation> boolean contains( Class<T> type, String name ) {
    return Components.lookup( type ).containsKey( name );
  }
  
  private static <T extends ComponentInformation> void remove( T componentInfo ) {
    Map<String, T> infoMap = lookup( componentInfo.getClass( ) );
    infoMap.remove( componentInfo.getName( ) );
  }

  private static <T extends ComponentInformation> void put( T componentInfo ) {
    Map<String, T> infoMap = lookup( componentInfo.getClass( ) );
    if ( infoMap.containsKey( componentInfo.getName( ) ) ) {
      throw BootstrapException.throwFatal( "Failed bootstrapping component registry.  Duplicate information for component '" + componentInfo.getName( ) + "': "
                                           + componentInfo.getClass( ).getSimpleName( ) + " as " + getRealType( componentInfo.getClass( ) ) );
    } else {
      infoMap.put( componentInfo.getName( ), componentInfo );
    }
  }

  public static <T extends ComponentInformation> void deregister( T componentInfo ) {
    remove( componentInfo );
    EventRecord.here( Bootstrap.class, EventType.COMPONENT_DEREGISTERED, componentInfo.getName( ), componentInfo.getClass( ).getSimpleName( ) ).info( );
  }
  
  static <T extends ComponentInformation> void register( T componentInfo ) {
    if ( !contains( componentInfo.getClass( ), componentInfo.getName( ) ) ) {
      EventRecord.here( Bootstrap.class, EventType.COMPONENT_REGISTERED, componentInfo.getName( ), componentInfo.getClass( ).getSimpleName( ) ).info( );
      Components.put( componentInfo );
    }
  }
  
  public static <T extends ComponentInformation> T lookup( Class<T> type, String name ) throws NoSuchElementException {
    if ( !contains( type, name ) ) {
      try {
        Components.create( name, null );
        return Components.lookup( type, name );
      } catch ( ServiceRegistrationException ex ) {
        throw new NoSuchElementException( "Missing entry for component '" + name + "' info type: " + type.getSimpleName( ) + " ("
                                          + getRealType( type ).getCanonicalName( ) );
      }
    } else {
      return ( T ) Components.lookup( type ).get( name );
    }
  }
  
  public static Component lookup( String componentName ) throws NoSuchElementException {
    return Components.lookup( Component.class, componentName );
  }
  
  public static Component lookup( com.eucalyptus.bootstrap.Component component ) throws NoSuchElementException {
    return Components.lookup( Component.class, component.name( ) );
  }
  
  public static boolean contains( String componentName ) {
    return Components.contains( Component.class, componentName );
  }
  
  public static boolean contains( com.eucalyptus.bootstrap.Component component ) {
    return Components.contains( Component.class, component.name( ) );
  }
  
  public static Component create( String name, URI uri ) throws ServiceRegistrationException {
    Component c = new Component( name, uri );
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
            buf.append( "-> Enabled/Local:      " + comp.isEnabled( ) + "/" + comp.isLocal( ) ).append( "\n" );
            buf.append( "-> State/Running:      " + comp.getState( ) + "/" + comp.isRunning( ) ).append( "\n" );
            buf.append( "-> Builder:            "
                        + comp.getBuilder( ).getClass( ).getSimpleName( ) ).append( "\n" );
            buf.append( "-> Disable/Remote cli: "
                        + System.getProperty( "euca." + comp.getPeer( ).name( ) + ".disable" )
                        + "/"
                        + System.getProperty( "euca." + comp.getPeer( ).name( ) + ".remote" ) ).append( "\n" );
            buf.append( "-> Configuration:      "
                        + ( comp.getConfiguration( ).getResource( ) != null
                          ? comp.getConfiguration( ).getResource( ).getOrigin( )
                          : "null" ) ).append( "\n" );
            for ( Bootstrapper b : comp.getConfiguration( ).getBootstrappers( ) ) {
              buf.append( "-> " + b.toString( ) ).append( "\n" );
            }
            buf.append( LogUtil.subheader( comp.getName( ) + " services" ) ).append( "\n" );
            for ( Service s : comp.getServices( ) ) {
              buf.append( "->  Service:          " + s.getName( ) + " " + s.getUri( ) ).append( "\n" );
              buf.append( "|-> Dispatcher:       " + s.getDispatcher( ).getName( ) + " for "
                          + s.getDispatcher( ).getAddress( ) ).append( "\n" );
              buf.append( "|-> Service Endpoint: " + s.getEndpoint( ) ).append( "\n" );
//TODO: restore this.          destinationBuffer.append( "|-> Credential DN:    " + s.getKeys( ).getCertificate( ).getSubjectDN( ).toString( ) );
              buf.append( "|-> Service config:   "
                          + LogUtil.dumpObject( s.getServiceConfiguration( ) ) ).append( "\n" );
            }
            return buf.toString( );
          }
        };
      }
    }
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
                                       System.getProperty( String.format( "euca.%s.disable", comp.getPeer( ).name( ) ) ),
                                       System.getProperty( String.format( "euca.%s.remote", comp.getPeer( ).name( ) ) ) ) ).append( "\n" );
            buf.append( String.format( "%s -> enabled/local/init:   %s/%s/%s",
                                       comp.getName( ), comp.isEnabled( ), comp.isLocal( ), comp.isRunning( ) ) ).append( "\n" );
            buf.append( String.format( "%s -> configuration:        %s",
                                       comp.getName( ), ( comp.getConfiguration( ).getResource( ) != null
                                         ? comp.getConfiguration( ).getResource( ).getOrigin( )
                                         : "null" ) ) ).append( "\n" );
            buf.append( String.format( "%s -> bootstrappers:        %s", comp.getName( ),
                                       Iterables.transform( comp.getConfiguration( ).getBootstrappers( ), bootstrapperToString ) ) ).append( "\n" );
            return buf.toString( );
          }
        };
        
      }
    }
  }
}
