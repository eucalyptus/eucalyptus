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

package com.eucalyptus.crypto.util;

import static org.junit.Assert.*;
import static com.eucalyptus.crypto.util.SslUtils.SslCipherBuilder.ciphers;
import org.junit.Test;

/**
 *
 */
public class SslUtilsTest {

  @Test
  public void testCipherSuitesList() {
    final String[] enbabledCipherSuites =
      ciphers().with( "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA:TLS_PSK_WITH_AES_256_CBC_SHA:SSL_DH_anon_WITH_RC4_128_MD5" )
          .enabledCipherSuites( new String[]{ "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5" } );
    assertArrayEquals("Enabled cipher suites", new String[]{"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5"}, enbabledCipherSuites);
  }

  @Test
  public void testCipherStringExpansion() {
    final String[] enbabledCipherSuites =
        ciphers().with( "SHA" )
            .enabledCipherSuites( new String[]{ "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "TLS_PSK_WITH_AES_256_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5" } );
    assertArrayEquals("Enabled cipher suites", new String[]{"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "TLS_PSK_WITH_AES_256_CBC_SHA"}, enbabledCipherSuites);
  }

  @Test
  public void testCipherStringShifting() {
    final String[] enbabledCipherSuites =
        ciphers().with( "SHA:+SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA" )
            .enabledCipherSuites( new String[]{ "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "TLS_PSK_WITH_AES_256_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5" } );
    assertArrayEquals("Enabled cipher suites", new String[]{"TLS_PSK_WITH_AES_256_CBC_SHA","SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA"}, enbabledCipherSuites);

    // ensure shifting does not add the cipher
    final String[] enbabledCipherSuites2 =
        ciphers().with( "TLS_PSK_WITH_AES_256_CBC_SHA:+SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA" )
            .enabledCipherSuites( new String[]{ "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "TLS_PSK_WITH_AES_256_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5" } );
    assertArrayEquals("Enabled cipher suites", new String[]{"TLS_PSK_WITH_AES_256_CBC_SHA"}, enbabledCipherSuites2);
  }

  @Test
  public void testCipherStringExclusion() {
    final String[] enbabledCipherSuites =
        ciphers().with( "ALL:!MD5" )
            .enabledCipherSuites( new String[]{ "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "TLS_PSK_WITH_AES_256_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5" } );
    assertArrayEquals("Enabled cipher suites", new String[]{"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA","TLS_PSK_WITH_AES_256_CBC_SHA"}, enbabledCipherSuites);
  }

  @Test
  public void testNoWeakCiphers() {
    final String[] enbabledCipherSuites =
        ciphers().with( "ALL:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES" )
            .enabledCipherSuites( new String[]{
                "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
                "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
                "TLS_DH_anon_WITH_AES_128_CBC_SHA",
                "SSL_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA",
                "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
                "TLS_PSK_WITH_NULL_SHA",
                "TLS_SRP_SHA_WITH_AES_128_CBC_SHA",
            } );
    assertArrayEquals("Enabled cipher suites", new String[]{"SSL_DH_anon_WITH_3DES_EDE_CBC_SHA","TLS_DH_anon_WITH_AES_128_CBC_SHA","SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA","TLS_SRP_SHA_WITH_AES_128_CBC_SHA"}, enbabledCipherSuites);
  }

  @Test
  public void testCipherStringSeparators() {
    final String[] enbabledCipherSuites = // test space and comma separation (: preferred)
        ciphers().with( "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA, TLS_PSK_WITH_AES_256_CBC_SHA SSL_DH_anon_WITH_RC4_128_MD5" )
            .enabledCipherSuites( new String[]{ "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "TLS_PSK_WITH_AES_256_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5" } );
    assertArrayEquals("Enabled cipher suites", new String[]{"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA","TLS_PSK_WITH_AES_256_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5"}, enbabledCipherSuites);
  }

  @Test
  public void testCipherStringAnds() {
    final String[] enbabledCipherSuites =
        ciphers().with( "RSA+SHA" )
            .enabledCipherSuites( new String[]{
                "SSL_DH_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "TLS_DH_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DH_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DH_RSA_WITH_CAMELLIA_128_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA"
            } );
    assertArrayEquals("Enabled cipher suites", new String[]{"SSL_DH_RSA_EXPORT_WITH_DES40_CBC_SHA","TLS_DH_RSA_WITH_CAMELLIA_128_CBC_SHA"}, enbabledCipherSuites);
  }
}
