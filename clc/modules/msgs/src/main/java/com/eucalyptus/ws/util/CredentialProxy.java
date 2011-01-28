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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.util;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CredentialProxy implements Crypto {

  private final X509Certificate cert;
  private final PrivateKey pk;
  
  public CredentialProxy( X509Certificate cert, PrivateKey pk ) {
    super( );
    this.cert = cert;
    this.pk = pk;
  }

  @Override
  public X509Certificate[] getCertificates( final String arg0 ) throws WSSecurityException {
    return new X509Certificate[] { this.cert };
  }

  @Override
  public PrivateKey getPrivateKey( final String alias, final String password ) throws Exception {
    return this.pk;
  }

  @Override public X509Certificate loadCertificate( final InputStream inputStream ) throws WSSecurityException { return null; }
  @Override public X509Certificate[] getX509Certificates( final byte[] bytes, final boolean b ) throws WSSecurityException { return new X509Certificate[0]; }
  @Override public byte[] getCertificateData( final boolean b, final X509Certificate[] x509Certificates ) throws WSSecurityException { return new byte[0]; }
  @Override public String getAliasForX509Cert( final Certificate certificate ) throws WSSecurityException { return null; }
  @Override public String getAliasForX509Cert( final String s ) throws WSSecurityException { return null; }
  @Override public String getAliasForX509Cert( final String s, final BigInteger bigInteger ) throws WSSecurityException { return null; }
  @Override public String getAliasForX509Cert( final byte[] bytes ) throws WSSecurityException { return null; }
  @Override public String getDefaultX509Alias( ) { return null; }
  @Override public byte[] getSKIBytesFromCert( final X509Certificate x509Certificate ) throws WSSecurityException { return new byte[0]; }
  @Override public String getAliasForX509CertThumb( final byte[] bytes ) throws WSSecurityException { return null; }
  @Override public KeyStore getKeyStore( ) { return null; }
  @Override public CertificateFactory getCertificateFactory( ) throws WSSecurityException { return null; }
  @Override public boolean validateCertPath( final X509Certificate[] x509Certificates ) throws WSSecurityException { return false; }
  @Override public String[] getAliasesForDN( final String s ) throws WSSecurityException { return new String[0]; }
}
