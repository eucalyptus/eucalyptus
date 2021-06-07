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

package com.eucalyptus.objectstorage.pipeline.auth;

import static com.eucalyptus.auth.principal.AccessKeyCredential.SignatureVersion.v2;
import static com.eucalyptus.auth.principal.AccessKeyCredential.SignatureVersion.v4;
import com.eucalyptus.auth.login.WrappedCredentials;
import com.eucalyptus.auth.principal.AccessKeyCredential;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.V4AuthComponent;
import com.eucalyptus.util.Assert;
import com.eucalyptus.ws.util.HmacUtils.SignatureCredential;
import io.vavr.control.Option;

import java.util.Date;
import java.util.Map;
import javax.annotation.Nonnull;

public class ObjectStorageWrappedCredentials extends WrappedCredentials<String> {
  enum AuthVersion {
    V2, V4
  }

  // Common
  public final AuthVersion authVersion;
  public final String signature;
  public final String securityToken;
  public final Long date; // optional signature date

  // V2
  public final String accessKeyId;

  // V4
  public final SignatureCredential credential;
  public final String signedHeaders;
  public final String payloadHash;
  /**
   * V2 auth constructor.
   *
   * @throws NullPointerException if accessKeyId or signature is null
   */
  public ObjectStorageWrappedCredentials(String correlationId, Long date, String signableString, String accessKeyId, String signature, String
      securityToken) {
    super(correlationId, signableString);
    this.authVersion = AuthVersion.V2;
    this.accessKeyId = Assert.notNull(accessKeyId, "accessKeyId");
    this.signature = Assert.notNull(signature, "signature");
    this.securityToken = securityToken;
    this.credential = null;
    this.signedHeaders = null;
    this.date = date;
    this.payloadHash = null;
  }

  /**
   * V4 auth constructor.
   *
   * @throws NullPointerException if credential or signedHeaders or signature is null
   */

  public ObjectStorageWrappedCredentials(String correlationId, Long date, String stringToSign, SignatureCredential credential, String signedHeaders,
                                         String signature, String securityToken, String payloadHash) {
    super(correlationId, stringToSign);
    this.date = date;
    this.authVersion = AuthVersion.V4;
    this.credential = Assert.notNull(credential, "credential");
    this.signedHeaders = Assert.notNull(signedHeaders, "signedHeaders");
    this.signature = Assert.notNull(signature, "signature");
    this.securityToken = securityToken;
    this.accessKeyId = null;
    this.payloadHash = payloadHash;
  }

  public AccessKeyCredential getCredential( @Nonnull final Option<TemporaryAccessKey.TemporaryKeyType> type ) {
    return authVersion==AuthVersion.V4 ?
        AccessKeyCredential.of( credential.getAccessKeyId( ), v4, date, type ) :
        AccessKeyCredential.of( accessKeyId, v2, date, type )  ;
  }
}
