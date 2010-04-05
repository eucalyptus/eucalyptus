package com.eucalyptus.component;

import java.net.URI;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Nameable;
import com.eucalyptus.util.NetworkUtil;
import edu.ucsb.eucalyptus.msgs.EventRecord;

public abstract class Component implements ComponentInformation, Nameable<Component> {
  private static Logger                            LOG            = Logger.getLogger( Component.class );
  private final String                             name;
  private final com.eucalyptus.bootstrap.Component component;
  private final Configuration                      configuration;
  private Lifecycle                                lifecycle;
  private final Credentials                        keys;
  private final Boolean                            singleton;
  public static String                             LOCAL_HOSTNAME = "@localhost";
  
  public String getChildKey( String hostName ) {
    String key = null;
    if ( LOCAL_HOSTNAME.equals( hostName ) || NetworkUtil.testLocal( hostName ) ) {
      key = this.getName( ) + LOCAL_HOSTNAME;
    } else {
      key = this.getName( ) + "@" + hostName;
    }
    return key;
  }

  protected Component( String name ) {
    this.name = name;
    this.component = initComponent( );
    if ( !component.isDummy( ) ) {
      this.singleton = false;
    } else {
      this.singleton = true;
    }    
    this.configuration = new Configuration( this );
    Components.register( this.configuration );
    this.lifecycle = new Lifecycle( this );
    this.keys = new Credentials( this );//TODO: integration with JAAS
    Components.register( this.lifecycle );    
  }
  
  protected Component( String name, URI uri ) {
    this.name = name;
    this.component = initComponent( );
    if ( !component.isDummy( ) ) {
      this.singleton = false;
    } else {
      this.singleton = true;
    }
    this.configuration = new Configuration( this, uri );
    Components.register( this.configuration );
    this.lifecycle = new Lifecycle( this );
    this.keys = new Credentials( this );//TODO: integration with JAAS
    Components.register( this.lifecycle );    
  }

  private com.eucalyptus.bootstrap.Component initComponent( ) {
    try {
      com.eucalyptus.bootstrap.Component component = com.eucalyptus.bootstrap.Component.valueOf( name );
      if ( component == null ) {
        throw BootstrapException.throwError( "Error loading component.  Failed to find component named '" + name );
      }
      return component;
    } catch ( Exception e ) {
      throw BootstrapException.throwError( "Error loading component.  Failed to find component named '" + name, e );
    }
  }  
  
  public com.eucalyptus.bootstrap.Component getPeer( ) {
    return this.component;
  }
  
  public String getName( ) {
    return this.name;
  }
  
  public String describe( ) {
    EventRecord rec = EventRecord.caller( Component.class, EventType.COMPONENT_INFO, this.getName( ), "enabled" )
                                 .append( this.lifecycle.isEnabled( ).toString( ), "local", this.lifecycle.isLocal( ).toString( ) ).next( )
                                 .append( "uri", this.lifecycle.getUri( ).toString( ), "host", this.lifecycle.getHost( ), "port" )
                                 .append( Integer.toString( this.lifecycle.getPort( ) ) ).next( ).append(
                                                                                                          this.configuration.toString( ).replaceAll( "=|(, )",
                                                                                                                                                     "" ) );
    for ( Bootstrapper b : this.configuration.getBootstrappers( ) ) {
      rec.next( ).append( EventRecord.caller( Component.class, EventType.COMPONENT_INFO, this.getName( ), b.getClass( ).getSimpleName( ) ) );
    }
    return rec.toString( );
  }
  
  public Configuration getConfiguration( ) {
    return this.configuration;
  }
  
  public Lifecycle getLifecycle( ) {
    return this.lifecycle;
  }
  
  public Credentials getKeys( ) {
    if ( this.keys == null ) {
      return Components.lookup( Components.delegate.eucalyptus ).getKeys( );
    } else {
      return this.keys;
    }
  }
  
  public String name( ) {
    return this.name;
  }
  
  public com.eucalyptus.bootstrap.Component getDelegate( ) {
    return this.component;
  }
  
  public abstract Component getChild( String childName );
  
  public abstract Dispatcher getDispatcher( );

  /**
   * @return the singleton
   */
  public final Boolean isSingleton( ) {
    return this.singleton;
  }
  
}
