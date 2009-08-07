/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import javax.security.auth.x500.X500Principal;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;

public class KeyTool
{
  private static Logger LOG = Logger.getLogger( KeyTool.class );

  private String keyAlgorithm;
  private String keySigningAlgorithm;
  private int keySize;
  public static String PROVIDER = "BC";

  public KeyTool()
  {
    this.keyAlgorithm = "RSA";
    this.keySigningAlgorithm = "SHA512WithRSA";
    this.keySize = 2048;
  }

  public KeyTool( final String keyAlgorithm, final String keySigningAlgorithm, final int keySize )
  {
    this.keyAlgorithm = keyAlgorithm;
    this.keySigningAlgorithm = keySigningAlgorithm;
    this.keySize = keySize;
  }

  public KeyPair getKeyPair()
  {
    KeyPairGenerator keyGen = null;
    try
    {
      keyGen = KeyPairGenerator.getInstance( this.keyAlgorithm );
      SecureRandom random = new SecureRandom();
      random.setSeed( System.currentTimeMillis() );
      keyGen.initialize( this.keySize, random );
      KeyPair keyPair = keyGen.generateKeyPair();
      return keyPair;
    }
    catch ( Exception e )
    {
      System.exit( 1 );
      return null;
    }
  }

  public X509Certificate getCertificate( KeyPair keyPair, String certDn )
  {
    X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
    X500Principal dnName = new X500Principal( certDn );

    certGen.setSerialNumber( BigInteger.valueOf( System.currentTimeMillis() ) );
    certGen.setIssuerDN( dnName );
    certGen.addExtension( X509Extensions.BasicConstraints, true, new BasicConstraints(true));

    Calendar cal = Calendar.getInstance();
    certGen.setNotBefore( cal.getTime() );
    cal.add( Calendar.YEAR, 5 );
    certGen.setNotAfter( cal.getTime() );
    certGen.setSubjectDN( dnName );
    certGen.setPublicKey( keyPair.getPublic() );
    certGen.setSignatureAlgorithm( this.keySigningAlgorithm );
    try
    {
      X509Certificate cert = certGen.generate( keyPair.getPrivate(), PROVIDER );
      return cert;
    }
    catch ( Exception e )
    {
      LOG.fatal(e,e);
      System.exit( 1 );
      return null;
    }
  }

  public void writePem( String fileName, Object securityToken )
  {
    PEMWriter privOut = null;
    try
    {
      privOut = new PEMWriter( new FileWriter( fileName ) );
      privOut.writeObject( securityToken );
      privOut.close();
    }
    catch ( IOException e )
    {
      LOG.error( e, e );
    }
  }
}
