/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.pipeline.auth;

import com.eucalyptus.auth.AccessKeys;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.login.Hmacv4LoginModule;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAccessKeyIdException;
import com.eucalyptus.objectstorage.pipeline.auth.ObjectStorageWrappedCredentials.AuthVersion;
import com.google.common.io.BaseEncoding;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public class ObjectStorageLoginModule extends BaseLoginModule<ObjectStorageWrappedCredentials> {
  private static Logger LOG = Logger.getLogger(ObjectStorageLoginModule.class);

  @Override
  public boolean accepts() {
    return super.getCallbackHandler() instanceof ObjectStorageWrappedCredentials;
  }

  @Override
  public boolean authenticate(ObjectStorageWrappedCredentials credentials) throws Exception {
    if (credentials.authVersion.equals(AuthVersion.V2))
      return authV2(credentials);
    else if (credentials.authVersion.equals(AuthVersion.V4))
      return authV4(credentials);
    return false;
  }

  private boolean authV2(ObjectStorageWrappedCredentials credentials) throws Exception {
    AccessKey accessKey = lookupAccessKey(credentials.accessKeyId, credentials.securityToken);
    String computedSig = getHmacSHA1(accessKey.getSecretKey(), credentials.getLoginData());
    String providedSig = credentials.signature.replaceAll("=", "");

    // Compare signatures
    if (!computedSig.equals(providedSig))
      return false;

    super.setCredential(credentials.getCredential( AccessKeys.getKeyType( accessKey ) ));
    super.setPrincipal(accessKey.getPrincipal());
    return true;
  }

  private boolean authV4(ObjectStorageWrappedCredentials credentials) throws Exception {
    AccessKey accessKey = lookupAccessKey(credentials.credential.getAccessKeyId(), credentials.securityToken);
    byte[] signatureKey = Hmacv4LoginModule.getSignatureKey(accessKey.getSecretKey(), credentials.credential);
    byte[] computedSig = Hmacv4LoginModule.getHmacSHA256(signatureKey, credentials.getLoginData());
    byte[] providedSig = BaseEncoding.base16().lowerCase().decode(credentials.signature);

    // Compare signatures
    if (!MessageDigest.isEqual(computedSig, providedSig))
      return false;

    super.setCredential(credentials.getCredential( AccessKeys.getKeyType( accessKey ) ));
    super.setPrincipal(accessKey.getPrincipal());
    return true;
  }

  private static AccessKey lookupAccessKey(String accessKeyId, String securityToken) throws InvalidAccessKeyIdException {
    try {
      AccessKey key = AccessKeys.lookupAccessKey(accessKeyId, securityToken);
      if (!key.isActive())
        throw new InvalidAccessKeyIdException(accessKeyId);
      return key;
    } catch (AuthException e) {
      throw new InvalidAccessKeyIdException(accessKeyId);
    }
  }

  private static String getHmacSHA1(final String secretKey, final String subject) throws AuthenticationException {
    try {
      SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), Hmac.HmacSHA1.toString());
      Mac mac = Mac.getInstance(Hmac.HmacSHA1.toString());
      mac.init(signingKey);
      byte[] rawHmac = mac.doFinal(subject.getBytes("UTF-8"));
      return Base64.encode(rawHmac).replaceAll("=", "");
    } catch (Exception e) {
      LOG.error(e, e);
      throw new AuthenticationException("Failed to compute signature");
    }
  }
}
