/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.login;

import static com.eucalyptus.ws.util.HmacUtils.headerLookup;
import static com.eucalyptus.ws.util.HmacUtils.parameterLookup;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.ws.util.HmacUtils;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

public class HmacCredentials extends WrappedCredentials<String> {
  private final HmacUtils.SignatureVariant variant;    
  private final String verb;
  private final String servicePath;
  private final Map<String,List<String>> parameters;
  private final Map<String, List<String>> headers;
  private final String body;
  private String headerHost;
  private String headerPort;
  private final String queryId;
  private final String securityToken;
  private Hmac signatureMethod;

  public HmacCredentials( final String correlationId,
                          final HmacUtils.SignatureVariant variant,
                          final Map<String,List<String>> parameters,
                          final Map<String,List<String>> headers,
                          final String verb,
                          final String servicePath,
                          final String body ) throws AuthenticationException {
    super( correlationId, variant.getSignature( headerLookup(headers), parameterLookup( parameters ) ) );
    final Function<String,List<String>> headerLookup = headerLookup( headers );
    final Function<String,List<String>> parameterLookup = parameterLookup( parameters );
    this.variant = variant;
    this.parameters = parameters;
    this.headers = headers;
    this.verb = verb;
    this.servicePath = servicePath;
    this.body = body;
    this.headerHost = Iterables.getFirst( Objects.firstNonNull( headers.get("host"), Collections.<String>emptyList() ), null );
    this.headerPort = ""+8773;
    if ( headerHost != null && headerHost.contains( ":" ) ) {
      String[] hostTokens = this.headerHost.split( ":" );
      this.headerHost = hostTokens[0];
      if ( hostTokens.length > 1 && hostTokens[1] != null && !"".equals( hostTokens[1] ) ) {
        this.headerPort = hostTokens[1];
      }
    }
    this.queryId = variant.getAccessKeyId( headerLookup, parameterLookup );
    this.securityToken = variant.getSecurityToken( headerLookup, parameterLookup );
    this.signatureMethod = variant.getSignatureMethod( headerLookup, parameterLookup );
  }

  public HmacUtils.SignatureVariant getVariant() {
    return variant;
  }
  
  public Integer getSignatureVersion( ) {
    return variant.getVersion().value();
  }
  
  public String getQueryId( ) {
    return this.queryId;
  }

  public QueryIdCredential getQueryIdCredential() {
    return new QueryIdCredential( getQueryId() );
  }

  public String getSecurityToken( ) {
    return securityToken;
  }

  public String getSignature( ) {
    return getLoginData( );
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

  public void setSignatureMethod( final Hmac hmac ) {
    this.signatureMethod = hmac;
  }

  public Map<String, List<String>> getParameters( ) {
    return this.parameters;
  }

  public String getBody() {
    return body;
  }

  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  public static final class QueryIdCredential {
    private final String queryId;

    private QueryIdCredential( final String queryId ) {
      this.queryId = queryId;
    }

    public String getQueryId() {
      return queryId;
    }

    public String toString() {
      return getQueryId();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final QueryIdCredential that = (QueryIdCredential) o;

      if (!queryId.equals(that.queryId)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return queryId.hashCode();
    }
  }
}
