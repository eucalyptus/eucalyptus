package com.eucalyptus.ws;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class MappingHttpResponse extends MappingHttpMessage implements HttpResponse {
  private final HttpResponseStatus status;
  
  public MappingHttpResponse( HttpVersion version ) {
    super( version );
    this.status = HttpResponseStatus.OK;
  }

  public MappingHttpResponse( final String[] initialLine ) {
    super( HttpVersion.valueOf( initialLine[0] ) );
    this.status = new HttpResponseStatus( Integer.valueOf( initialLine[1] ), initialLine[2] );
  }

  @Override
  public HttpResponseStatus getStatus( ) {
    return status;
  }


}
