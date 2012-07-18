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

package com.eucalyptus.crypto.util;

import static org.junit.Assert.*;
import static com.eucalyptus.crypto.util.SslSetup.SslCipherBuilder.ciphers;
import org.junit.Test;

/**
 *
 */
public class SslSetupTest {

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
