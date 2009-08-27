/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package com.eucalyptus.ws;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.bouncycastle.util.encoders.UrlBase64;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.HttpHeaders;

public class MappingHttpRequest extends MappingHttpMessage implements HttpRequest {

  private final HttpMethod method;
  private final String     uri;
  private String     servicePath;
  private String     query;
  private final Map<String,String> parameters;
  private String restNamespace;
  private final Map<String, String> formFields;
  
  public MappingHttpRequest( HttpVersion httpVersion, HttpMethod method, String uri ) {
    super( httpVersion );
    this.method = method;
    this.uri = uri;
    try {
      URL url = new URL( "http://eucalyptus" + uri );
      this.servicePath = url.getPath( );
      this.parameters = new HashMap<String, String>( );
      this.query = this.query == url.toURI( ).getQuery( ) ? this.query : new URLCodec().decode( url.toURI( ).getQuery( ) ).replaceAll( " ", "+" );
      this.formFields = new HashMap<String, String>( );
      this.populateParameters();
    } catch ( MalformedURLException e ) {
      throw new RuntimeException( e );
    } catch ( URISyntaxException e ) {
      throw new RuntimeException( e );
    } catch ( DecoderException e ) {
      throw new RuntimeException( e );
    }
  }

  private void populateParameters( ) {
    if ( this.query != null && !"".equals(  this.query ) ) {
      for ( String p : this.query.split( "&" ) ) {
        String[] splitParam = p.split( "=" );
        String lhs = splitParam[0];
        String rhs = splitParam.length == 2 ? splitParam[1] : null;
        this.parameters.put( lhs, rhs );
      }
    }
  }
  
  public MappingHttpRequest( final HttpVersion httpVersion, final HttpMethod method, final String host, final int port, final String servicePath, final Object source ) {
    super( httpVersion );
    this.method = method;
    this.uri = "http://" + host + ":" + port + servicePath;
    this.servicePath = servicePath;
    this.query = null;
    this.parameters = null;
    this.formFields = null;
    super.setMessage( source );
    this.addHeader( HttpHeaders.Names.HOST, host + ":" + port );
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
      this.query = new URLCodec().decode( query );
    } catch ( DecoderException e ) {
      this.query = query;
    }
    this.populateParameters( );
  }


  public HttpMethod getMethod( ) {
    return this.method;
  }

  public String getUri( ) {
    return this.uri;
  }

  @Override
  public String toString( ) {
    return this.getMethod( ).toString( ) + ' ' + this.getUri( ) + ' ' + super.getProtocolVersion( ).getText( );
  }

  public Map<String,String> getParameters( ) {
    return parameters;
  }

  public String getRestNamespace( ) {
    return restNamespace;
  }

  public void setRestNamespace( String restNamespace ) {
    this.restNamespace = restNamespace;
  }

  public Map<String, String> getFormFields() {
	  return formFields;
  }
  
  public String getAndRemoveHeader(String key) {
	  String value = getHeader(key);
	  removeHeader(key);
	  return value;
  }
}
