package com.eucalyptus.ws.handlers;

import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.MappingHttpResponse;

public class NioHttpResponseDecoder extends HttpResponseDecoder {

  @Override
  protected HttpMessage createMessage( final String[] strings ) {
    return new MappingHttpResponse( strings );//HttpVersion.valueOf(strings[2]), HttpMethod.valueOf(strings[0]), strings[1] );
  }
}
