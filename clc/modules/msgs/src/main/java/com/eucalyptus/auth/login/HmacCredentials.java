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

  public QueryIdCredential getQueryIdCredential() {
    return new QueryIdCredential( getQueryId() );
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
