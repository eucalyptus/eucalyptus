package com.eucalyptus.component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Lifecycles.State;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.util.Transition;
import edu.ucsb.eucalyptus.msgs.EventRecord;

/**
 * @author decker
 */
public class Lifecycle implements ComponentInformation {
  private static Logger                         LOG             = Logger.getLogger( Lifecycle.class );
  private final NavigableMap<Transition, Class> transitions     = new ConcurrentSkipListMap<Transition, Class>( );
  private final String                          name;
  private final Component                       parent;
  private Boolean                               enabled;
  private Boolean                               local;
  private Lifecycles.State                      state;
  private final Uri                             uri;
  
  private static String                         DISABLE_PATTERN = "euca.disable.%s";
  private static String                         REMOTE_PATTERN  = "euca.remote.%s";
  
  public Lifecycle( Component parent ) {
    this.parent = parent;
    this.name = parent.getName( );
    Boolean enabled = false, local = false;
    if ( System.getProperty( String.format( DISABLE_PATTERN, this.name ) ) == null ) {
      enabled = true;
    }
    this.enabled = enabled;
    if ( !this.parent.getDelegate( ).isDummy( ) ) {
      if ( System.getProperty( String.format( REMOTE_PATTERN, this.name ) ) == null ) {
        local = true;
      }
      this.local = local;
    } else {
      this.local = true;
    }
    this.state = Lifecycles.State.INITIALIZED;
    for ( DefaultTransitionList t : DefaultTransitionList.values( ) ) {
      this.addTransition( t.newInstance( ) );
    }
    if ( this.local ) {
      this.uri = new Uri( this.parent.getConfiguration( ).getLocalUri( ) );
    } else {
      this.uri = new Uri( this.parent.getConfiguration( ).getLocalUri( ) );
    }
  }
  
  @Override
  public String getName( ) {
    return this.name;
  }
  
  public Boolean isInitialized( ) {
    return State.STARTED.equals( this.state );
  }
  
  public void setState( State newState ) {
    this.state = newState;
  }
  
  public Boolean isEnabled( ) {
    return this.enabled;
  }
  
  public Boolean isLocal( ) {
    return this.local;
  }
  
  public Lifecycles.State getState( ) {
    return this.state;
  }
  
  public Boolean getEnabled( ) {
    return this.enabled;
  }
  
  public void setEnabled( Boolean enabled ) {
    this.enabled = enabled;
  }
  
  public URI getUri( ) {
    return this.uri.get( );
  }
  
  public void setUri( URI uri ) {
    this.uri.change( uri );
  }
  
  public String getHost( ) {
    return this.uri.get( ).getHost( );
  }
  
  public void setHost( String address ) {
    this.uri.change( address, this.getPort( ) );
  }
  
  public int getPort( ) {
    return this.uri.get( ).getPort( );
  }
  
  public void setPort( Integer port ) {
    this.uri.change( port );
  }
  
  public URI getUri( String hostName ) {
    return this.uri.make( hostName, this.uri.getPort( ) );
  }
  
  class Uri extends AtomicReference<URI> {
    private Uri( URI initialValue ) {
      super( initialValue );
    }
    
    private InetSocketAddress getSocketAddress( String address, Integer port ) {
      if ( NetworkUtil.testLocal( address ) ) {
        Lifecycle.this.local = true;
        return InetSocketAddress.createUnresolved( "127.0.0.1", port );
      } else {
        return InetSocketAddress.createUnresolved( address, port );
      }
    }
    
    public URI make( String hostName, Integer port ) {
      if ( NetworkUtil.testLocal( hostName ) ) {
        hostName = "127.0.0.1";
      }
      return Lifecycle.this.parent.getConfiguration( ).makeUri( hostName, port );
    }
    
    private Uri change( String hostName, Integer port ) {
      this.change( Lifecycle.this.parent.getConfiguration( ).makeUri( hostName, port ) );
      return this;
    }
    
    public Uri change( String hostName ) {
      return this.change( hostName, this.getPort( ) );
    }
    
    public Uri change( Integer port ) {
      return this.change( this.getHostName( ), port );
    }
    
    public Uri change( URI uri ) {
      Exceptions.ifNullArgument( uri );
      InetSocketAddress sockAddr = null;
      if ( uri.getScheme( ).startsWith( "vm" ) ) { //simple local check just in case
        Lifecycle.this.local = true;
        sockAddr = InetSocketAddress.createUnresolved( "127.0.0.1", Lifecycle.this.parent.getConfiguration( ).getDefaultPort( ) );
      } else {
        sockAddr = this.getSocketAddress( uri.getHost( ), uri.getPort( ) );
        if ( NetworkUtil.testLocal( sockAddr.getAddress( ) ) ) {
          Lifecycle.this.local = true;
        } else {
          Lifecycle.this.local = false;
        }
      }
      System.setProperty( Lifecycle.this.parent.getConfiguration( ).getPropertyKey( ), uri.toASCIIString( ) );
      this.set( uri );
      return this;
    }
    
    public String getHostName( ) {
      return this.get( ).getHost( );
    }
    
    public Integer getPort( ) {
      return this.get( ).getPort( );
    }
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "Lifecycle [" );
    if ( this.enabled != null ) {
      builder.append( "enabled=" ).append( this.enabled ).append( ", " );
    }
    if ( this.local != null ) {
      builder.append( "local=" ).append( this.local ).append( ", " );
    }
    if ( this.name != null ) {
      builder.append( "name=" ).append( this.name ).append( ", " );
    }
    if ( this.state != null ) {
      builder.append( "state=" ).append( this.state ).append( ", " );
    }
    if ( this.uri != null ) {
      builder.append( "host=" ).append( this.uri.get( ).getHost( ) ).append( ", " );
      builder.append( "port=" ).append( this.uri.getPort( ) ).append( ", " );
      builder.append( "uri=" ).append( this.uri.get( ).toASCIIString( ) );
    }
    builder.append( "]" );
    return builder.toString( );
  }
  
  public void addTransition( Transition transition ) {
    Class old = this.transitions.put( transition, transition.getClass( ) );
    if ( old != null ) {
      EventRecord.caller( Lifecycle.class, EventType.LIFECYCLE_TRANSITION_DEREGISTERED, transition.toString( ), old.getCanonicalName( ) );
    }
    EventRecord.caller( Lifecycle.class, EventType.LIFECYCLE_TRANSITION_REGISTERED, transition.toString( ) );
  }
  
  public void markLocal( ) {
    this.local = true;
  }
}
