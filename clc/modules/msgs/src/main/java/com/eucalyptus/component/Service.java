package com.eucalyptus.component;

import java.net.URI;
import com.eucalyptus.util.NetworkUtil;

public class Service implements ComponentInformation, Comparable<Service> {
  public static String          LOCAL_HOSTNAME = "@localhost";
  private final Component       parent;
  private final String          name;
  private final Credentials     keys;
  private final ServiceEndpoint endpoint;
  private final Dispatcher      dispatcher;
  
  public Service( Component parent ) {
    this.parent = parent;
    this.name = parent.getName( ) + LOCAL_HOSTNAME;
    URI uri = this.parent.getConfiguration( ).getLocalUri( );
    this.endpoint = new ServiceEndpoint( this, true, uri );
    this.keys = new Credentials( this );//TODO: integration with JAAS
    this.dispatcher = DispatcherFactory.build( parent, this );    
  }
  public Service( Component parent, String host, Integer port ) {
    this.parent = parent;
    Boolean local = false;
    URI uri = null;
    if ( NetworkUtil.testLocal( host ) ) {
      local = false;
      this.name = parent.getName( ) + "@" + host;
      uri = this.parent.getConfiguration( ).makeUri( host, port );
    } else {
      local = true;
      this.name = parent.getName( ) + LOCAL_HOSTNAME;
      uri = this.parent.getConfiguration( ).getLocalUri( );
    }
    this.endpoint = new ServiceEndpoint( this, local, uri );
    this.keys = new Credentials( this );//TODO: integration with JAAS
    this.dispatcher = DispatcherFactory.build( parent, this );
  }
  
  public Boolean isLocal( ) {
    return this.endpoint.isLocal( );
  }
  
  public Credentials getKeys( ) {
    return this.keys;
  }
  
  public URI getUri( ) {
    return this.endpoint.get( );
  }
  
  public String getHost( ) {
    return this.endpoint.get( ).getHost( );
  }
  
  public Integer getPort( ) {
    return this.endpoint.get( ).getPort( );
  }
  
  public String getName( ) {
    return this.name;
  }
  
  public ServiceEndpoint getEndpoint( ) {
    return this.endpoint;
  }
  
  public Dispatcher getDispatcher( ) {
    return this.dispatcher;
  }

  @Override
  public int compareTo( Service that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
}
