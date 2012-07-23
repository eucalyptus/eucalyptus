/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.auth.login;

import java.util.Map;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.crypto.util.SecurityParameter;

public class HmacCredentials extends WrappedCredentials<String> {
  private Hmac    signatureMethod;
  private Integer signatureVersion;
  private String  queryId;
  private String  signature;
  private String  verb;
  private String  servicePath;
  private String  headerHost;
  private String  headerPort;
  private final Map<String,String> parameters;
  public HmacCredentials( String correlationId, String signature, Map<String,String> parameters, String verb, String servicePath, String headerHost, Integer signatureVersion, Hmac hmacType ) {
    super( correlationId, signature );
    this.parameters = parameters;
    this.queryId = this.parameters.get( SecurityParameter.AWSAccessKeyId.toString( ) );
    this.signature = signature;
    this.signatureVersion = signatureVersion;
    this.signatureMethod = hmacType;
    this.verb = verb;
    this.servicePath = servicePath;
    this.headerHost = headerHost;
    this.headerPort = ""+8773;
    if ( headerHost != null && headerHost.contains( ":" ) ) {
      String[] hostTokens = this.headerHost.split( ":" );
      this.headerHost = hostTokens[0];
      if ( hostTokens.length > 1 && hostTokens[1] != null && !"".equals( hostTokens[1] ) ) {
        this.headerPort = hostTokens[1];
      }
    }
  }
    
  public Integer getSignatureVersion( ) {
    return this.signatureVersion;
  }
  
  public String getQueryId( ) {
    return this.queryId;
  }

  public String getSignature( ) {
    return this.signature;
  }

  public String getVerb( ) {
    return this.verb;
  }

  public String getServicePath( ) {
    return this.servicePath;
  }

  public String getHeaderHost( ) {
    return this.headerHost;
  }

  public String getHeaderPort( ) {
    return this.headerPort;
  }

  public Hmac getSignatureMethod( ) {
    return this.signatureMethod;
  }

  public Map<String, String> getParameters( ) {
    return this.parameters;
  }
  
}
