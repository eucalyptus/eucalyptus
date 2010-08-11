package com.eucalyptus.component;

import java.net.URI;
import com.eucalyptus.util.HasParent;
import com.eucalyptus.util.NetworkUtil;

public class Service implements ComponentInformation, Comparable<Service>, HasParent<Component> {
  public static String          LOCAL_HOSTNAME = "@localhost";
  private final Component       parent;
  private final String          name;
  private final Credentials     keys;
  private final ServiceEndpoint endpoint;
  private final Dispatcher      dispatcher;
  private final ServiceConfiguration serviceConfiguration;
  
  public ServiceConfiguration getServiceConfiguration( ) {
    return this.serviceConfiguration;
  }

  public Service( Component parent, ServiceConfiguration serviceConfig ) {
    this.parent = parent;
    this.serviceConfiguration = serviceConfig;
    if ( "cluster".equals( parent.getName( ) ) && com.eucalyptus.bootstrap.Component.eucalyptus.isLocal( ) ) /*ASAP: fix this disgusting hack.*/{
      this.name = parent.getName( ) + "@" + serviceConfig.getHostName( );
      URI uri = this.parent.getConfiguration( ).makeUri( "localhost", serviceConfig.getPort( ) );
      this.endpoint = new ServiceEndpoint( this, false, uri );
    } else if ( serviceConfig.isLocal( ) ) {
      URI uri = this.parent.getConfiguration( ).getLocalUri( );
      this.name = parent.getName( ) + LOCAL_HOSTNAME;
      this.endpoint = new ServiceEndpoint( this, true, uri );
    } else {
      Boolean local = false;
      try {
        if ( serviceConfig.getHostName( ) != null ) {
          local = NetworkUtil.testLocal( serviceConfig.getHostName( ) );
        } else {
          local = true;
        }
      } catch ( Exception e ) {
        local = true;
      }
      URI uri = null;
      if ( !local ) {
        this.name = parent.getName( ) + "@" + serviceConfig.getHostName( );
        uri = this.parent.getConfiguration( ).makeUri( serviceConfig.getHostName( ), serviceConfig.getPort( ) );
      } else {
        this.name = parent.getName( ) + LOCAL_HOSTNAME;
        uri = this.parent.getConfiguration( ).getLocalUri( );
      }
      this.endpoint = new ServiceEndpoint( this, local, uri );
    }
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

  @Override
  public Component getParent( ) {
    return this.parent;
  }

  @Override
  public String toString( ) {
    return String.format( "Service:parent=%s:name=%s:keys=%s:endpoint=%s:dispatcher=%s:serviceConfiguration=%s", this.parent, this.name, this.keys,
                          this.endpoint, this.dispatcher, this.serviceConfiguration );
  }

  
  
}
