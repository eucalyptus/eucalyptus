package com.eucalyptus.ws.handlers;

import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.ws.MappingHttpRequest;

public class NioHttpResponseDecoder extends HttpResponseDecoder {

  @Override
  protected HttpMessage createMessage( final String[] strings ) {
    return new MappingHttpRequest( HttpVersion.valueOf(strings[2]), HttpMethod.valueOf(strings[0]), strings[1] );
  }
}
