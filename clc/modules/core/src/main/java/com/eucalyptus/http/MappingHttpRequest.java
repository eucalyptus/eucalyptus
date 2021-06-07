/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.http;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class MappingHttpRequest extends MappingHttpMessage implements HttpRequest {
  private static Logger LOG = Logger.getLogger( MappingHttpRequest.class );

  private static final Set<String> HTTP_SCHEMES = ImmutableSet.of( "http", "https" );

  private HttpMethod                method;
  private String                    uri;
  private String                    uriHost;
  private String                    servicePath;
  private String                    query;
  private final Map<String, String> parameters; //Parameters are URLDecoded when populated
  private final Map<String, String> rawParameters; //Parameters in raw, non-decoded form
  private final Set<String>         nonQueryParameterKeys;
  private final Map<String, String> formFields;
  private String                    restNamespace;
  private String                    contentAsString;

  public MappingHttpRequest( HttpVersion httpVersion, HttpMethod method, String uri ) {
    super( httpVersion );
    this.method = method;
    this.uri = uri;
    try {
      final URL url = new URL( new URL( "http://eucalyptus/" ), uri );
      this.servicePath = url.getPath( );
      this.query = url.getQuery( );
      this.parameters = Maps.newHashMap( );
      this.rawParameters = Maps.newHashMap( );
      this.nonQueryParameterKeys = Sets.newHashSet( );
      this.formFields = Maps.newHashMap( );
      this.populateParameters( );
      if ( url.getProtocol( ) != null && !"eucalyptus".equals( url.getAuthority( ) ) ) { // check scheme and use host info
        if ( !HTTP_SCHEMES.contains( url.getProtocol( ).toLowerCase( ) ) ) {
          throw new IllegalArgumentException( "Invalid scheme: " + url.getProtocol( ) );
        }
        uriHost = HostAndPort.fromString( url.getAuthority( ) ).toString( );
      }
    } catch ( MalformedURLException | IllegalArgumentException e ) {
      throw new RuntimeException( e );
    }
  }
  
  private void populateParameters( ) {
    if ( this.query != null && !"".equals( this.query ) ) {
      for ( String p : this.query.split( "&" ) ) {
        //split with limit = 2 to account for "="s in the value itself
        String[] splitParam = p.split( "=", 2 );
        String lhs = splitParam[0];
        String rhs = splitParam.length == 2 ? splitParam[1] : null;
        this.rawParameters.put(lhs, rhs);
        try {
          if ( lhs != null ) lhs = new URLCodec( ).decode( lhs );
        } catch ( DecoderException e ) {}
        try {
          if ( rhs != null ) rhs = new URLCodec( ).decode( rhs );
        } catch ( DecoderException e ) {}
        this.parameters.put( lhs, rhs );
      }
    }
  }

  /**
   * Constructor for outbound requests. 
   */
  public MappingHttpRequest( final HttpVersion httpVersion, final HttpMethod method, final ServiceConfiguration serviceConfiguration, final Object source ) {
    super( httpVersion );
    this.method = method;
    URI fullUri = ServiceUris.internal( serviceConfiguration );
    this.uri = fullUri.toString();
    this.servicePath = fullUri.getPath( );
    this.query = null;
    this.parameters = null;
    this.rawParameters = null;
    this.nonQueryParameterKeys = null;
    this.formFields = null;
    this.message = source;
    if ( source instanceof BaseMessage ) this.setCorrelationId( ((BaseMessage)source).getCorrelationId() );
    this.addHeader( HttpHeaders.Names.HOST, fullUri.getHost( ) + ":" + fullUri.getPort( ) );
  }

  /**
   * Constructor for outbound requests. 
   */
  public MappingHttpRequest( final HttpVersion httpVersion, final HttpMethod method, final String host, final int port, final String servicePath,
                             final Object source ) {
    super( httpVersion );
    this.method = method;
    this.uri = "http://" + host + ":" + port + servicePath;
    this.servicePath = servicePath;
    this.query = null;
    this.parameters = null;
    this.rawParameters = null;
    this.nonQueryParameterKeys = null;
    this.formFields = null;
    this.message = source;
    if ( source instanceof BaseMessage ) this.setCorrelationId( ((BaseMessage)source).getCorrelationId() );
    this.addHeader( HttpHeaders.Names.HOST, host + ":" + port );
  }
  
  @Override
  public void setMessage( Object message ) {
    if ( message instanceof BaseMessage && this.getCorrelationId()!=null) {
      ( ( BaseMessage ) message ).setCorrelationId( this.getCorrelationId( ) );
    }
    super.setMessage( message );
  }
  
  public String getServicePath( ) {
    return this.servicePath;
  }
  
  public void setServicePath( String servicePath ) {
    this.servicePath = servicePath;
  }
  
  public String getQuery( ) {
    return this.query;
  }
  
  public void setQuery( String query ) {
    try {
      this.query = new URLCodec( ).decode( query );
    } catch ( DecoderException e ) {
      this.query = query;
    }
    this.populateParameters( );
  }
  
  @Override
  public HttpMethod getMethod( ) {
    return this.method;
  }

  @Override
  public void setMethod( final HttpMethod httpMethod ) {
    this.method = httpMethod;
  }

  @Override
  public String getUri( ) {
    return this.uri;
  }

  @Override
  public void setUri( final String uri ) {
    this.uri = uri;
  }

  @Override
  public String toString( ) {
    return this.getMethod( ).toString( ) + ' ' + this.getUri( ) + ' ' + super.getProtocolVersion( ).getText( );
  }

  public Map<String, String> getParameters( ) {
    return parameters;
  }

    public Map<String, String> getRawParameters( ) {
        return rawParameters;
    }

  public void addNonQueryParameterKeys( final Set<String> keys ) {
    if ( nonQueryParameterKeys != null ) {
      nonQueryParameterKeys.addAll( keys );
    }
  }

  public boolean isQueryParameter( final String key ) {
    return nonQueryParameterKeys != null && !nonQueryParameterKeys.contains( key );
  }

  public String getRestNamespace( ) {
    return restNamespace;
  }
  
  public void setRestNamespace( String restNamespace ) {
    this.restNamespace = restNamespace;
  }
  
  public Map getFormFields( ) {
    return formFields;
  }
  
  public String getAndRemoveHeader( String key ) {
    String value = getHeader( key );
    removeHeader( key );
    return value;
  }

  public String getContentAsString( ) {
    return getContentAsString( false );
  }

  public String getContentAsString( final boolean refresh ) {
    String content = contentAsString;
    if ( refresh || content == null ) {
      content = contentAsString = StandardCharsets.UTF_8.decode( getContent( ).toByteBuffer( ) ).toString( );
    }
    return content;
  }

  @Override
  public String logMessage( ) {
    StringBuffer buf = new StringBuffer();
    buf.append( "============================================\n" );
    buf.append( "HTTP" ).append( this.getProtocolVersion( ) ).append( " " ).append( this.getMethod( ) ).append( " " ).append( this.getUri( ) ).append( "\n" );
    for( String s : this.getHeaderNames( ) ) {
      buf.append( s ).append( ": " ).append( this.getHeader( s ) ).append( "\n" ); 
    }
    buf.append( "============================================\n" );
    buf.append( this.getContent( ).toString( "UTF-8" ) ).append( "\n" );
    buf.append( "============================================\n" );
    return buf.toString( );
  }

  public MappingHttpRequest validate( ) {
    if ( uriHost != null &&
        getHeader( HttpHeaders.Names.HOST ) != null &&
        !getHeader( HttpHeaders.Names.HOST ).equals( uriHost )  ) {
        throw new IllegalArgumentException( "Host header does not match uri host" );
    }
    return this;
  }
}
