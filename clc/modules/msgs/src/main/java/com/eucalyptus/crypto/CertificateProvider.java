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

package com.eucalyptus.crypto;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

public interface CertificateProvider extends BaseSecurityProvider {
  /**
   * TODO: DOCUMENT CryptoProvider.java
   * @return
   */
  public abstract KeyPair generateKeyPair( );
  public abstract String getFingerPrint( Key privKey );
  
  /**
   * Following operations produce a signed certificate. If the <tt>signer</tt> certificate is null, then the certificate generated is self signed.
   * 
   * @param keys
   * @param subjectDn
   * @param signer
   * @return
   */
  public abstract X509Certificate generateCertificate( KeyPair keys, X500Principal subjectDn, X500Principal signer, PrivateKey signingKey );
  public abstract X509Certificate generateCertificate( KeyPair keys, String userName );
  public abstract X509Certificate generateCertificate( KeyPair keys, X500Principal subjectDn );

  /**
   * Mechanically identical to the above, but signed by the root cert.
   */
  public abstract X509Certificate generateServiceCertificate( KeyPair keys, String userName );

}
