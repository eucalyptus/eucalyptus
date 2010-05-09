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
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.EventRecord;

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
      throw new NoSuchElementException( "Missing entry for component '" + name + "' info type: " + type.getSimpleName( ) + " ("
                                        + getRealType( type ).getCanonicalName( ) );
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
  
}
