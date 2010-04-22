package com.eucalyptus.component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.util.Exceptions;

public class ServiceEndpoint extends AtomicReference<URI> {
  private static Logger LOG = Logger.getLogger( ServiceEndpoint.class );
  private final Service parent;
  private final Boolean local;
  ServiceEndpoint( Service parent, Boolean local, URI uri ) {
    super( null );
    this.parent = parent;
    this.local = local;
    Exceptions.ifNullArgument( uri );
    try {
      uri.parseServerAuthority( );
    } catch ( URISyntaxException e ) {
      LOG.error( e, e );
      System.exit( -1 );
    }
    this.set( uri );
  }
  
  public Service getParent( ) {
    return this.parent;
  }
  
  public Boolean isLocal( ) {
    return this.local;
  }

  public URI getUri( ) {
    return this.get( );
  }

  public String getHost( ) {
    return this.get( ).getHost( );
  }
  
  public Integer getPort( ) {
    return this.get( ).getPort( );
  }
  
}
