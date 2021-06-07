/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.keys

import com.eucalyptus.compute.common.internal.keys.KeyPairs

import static org.junit.Assert.*
import org.junit.Test
import java.security.GeneralSecurityException
import com.eucalyptus.crypto.util.B64
import java.security.interfaces.RSAPublicKey

/**
 * Unit tests for KeyPairManager
 */
@SuppressWarnings("GroovyAccessibility")
public class KeyPairManagerTest {

  /**
   * Test decoding an OpenSSH format public RSA key (1024 bit)
   */
  @Test
  public void testOpenSshDecoding() {    
    KeyPairManager.decodeKeyMaterial( B64.standard.encString("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQCq7plfA4ghB/X5050bpt4J8DTYkzO9tCDgD6m7EXDk+4zZl8wdAWUaGrUfZSMUZyjbl9IcrPNOF8wcfp5ksoZq7ZGTtZrRCSZVFeXTMhl4Wh0cX7Y85UGL7l3xuqBpwnGhRHIY91vwmE2hqYV65lY4Wxo/lL4/2NZojcyNJP1u8w== steve@fish.eucalyptus.com\n") )
  }

  /**
   * Test decoding a standard SSH format public RSA key (2048 bit)
   */
  @Test
  public void testSshDecoding() {
    KeyPairManager.decodeKeyMaterial( B64.standard.encString( 
'''---- BEGIN SSH2 PUBLIC KEY ----
Comment: "2048-bit RSA, converted by steve@fish.eucalyptus.com from Op"
AAAAB3NzaC1yc2EAAAADAQABAAABAQDHHe9SyuGP+I5EPCqFmLlrah9sUWt5zVGHhZACdj
26TlzXLTx1v/qLZSn3HYrjGSHKrBWjtm2SVv+MekfNR50i981nWCIISXFD2Chj0g3OM66y
eR/im7tPc92ddECDBMvYQVkRXC2F3SvZae+7HCL8xFKSsQDWRST608bX0pCznsF/RguMF/
RByBYd178JEUym2etpKfmR7SAluS+MsSnrsWMIS1uxvrgjmqWbZBOAm+FeorG3AW87j07b
cJK8e0eLzaO2zLKvNglaFydr/lEVRf8tnlZ5KHQb+QFYorS0K+bHbLrK5BmOVkr7u+1Vt2
IK1RXAu/72Ct6OAzRzn2v9
---- END SSH2 PUBLIC KEY ----
''' ) )
  }

  /**
   * Test decoding a binary/ASN.1 format public RSA key (4096 bit)
   */
  @Test
  public void testDecoding() {
    // ssh-keygen -e -m PKCS8 -f /tmp/id_rsa.4096.pub # then grab the Base64
    // Double Base64 encoded
    KeyPairManager.decodeKeyMaterial( B64.standard.encString(
        "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAy28wOHn+xHaT1+Ww3xpo\n" +
        "toT+Rl07ovqnk9ZjSugsM4Mo/Kh/hwPUltB7koIlE9D5uBAbzy+zxhk9U/2/psr2\n" +
        "59Iz6wlOCH7MoagJHMw/859Qh9thiVw9kSB7r19xIlf2QIZIzLoC79EO50exLN5g\n" +
        "T5CJchzTZb5veXVrNzOnevYa8P5RhXpuU5wpslRW2537+CGbb1uzvMNUZ8gdgA48\n" +
        "83Z0Ai/LvMj4sAatXg/LneZokxxXM5xQDYWC1MWN7VToiVByNhGXkr87v2qynkMI\n" +
        "Gz4zy4RnPLyka3VGXcluS9LgquQh9IEkA+m5QHRsDKfNA9GBqVxVNmDX2T3HGc8k\n" +
        "Ldqgiihn5ZP/xGnMEftndFOQhlhM7bVzam4ZmvhtDCS+jvorJBoIYPXZVtW9mo3P\n" +
        "WveVMfHPkcqEFHOPjJpWzEIrhIbPRUw9Xrcbi/cBE97XvMSf2BOjdGVU0TRIarTz\n" +
        "WbecAH5tNuFT+4tg9IH43opGMsdFeWCzxqIe0jkgnxmRxNAp6h6qh+nOZD0moSrQ\n" +
        "o4YywlXSr6Ju+yY80mqkvKK4zIBIsiWvd0ZreJbD5FlGnAt8OX3u686HijTghGUs\n" +
        "VUW44GdunXUmGZ5lugnYr+HUvZxSaN6NMG66mb9zbCUVl7+F3hqYLpBcDNTlImsg\n" +
        "+GMQNbx2u6FT0ojlTUzFg5cCAwEAAQ==" ) )
  }

  /**
   * Test invalid RSA key size. 
   */
  @Test(expected=GeneralSecurityException.class)
  public void testInvalidKeySize() {
    // 768 bit RSA key
    KeyPairManager.decodeKeyMaterial( B64.standard.encString("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAYQDQY7WCQNZwYXjqgJEHvqa6LRDxXt0W6Q6myBKM02wHa615Xt/DpgNaI/+RQD7i9VvSVZkQkabCnsUeKcnxe2hOg6RiuOl9xrBTEcPgc8IYntSLlYXXIF324hr97R8ZjE8= test@test.eucalyptus.com") )
  }

  @Test(expected=GeneralSecurityException.class)
  public void testNoInput() {
    KeyPairManager.decodeKeyMaterial( "" )
  }

  @Test(expected=GeneralSecurityException.class)
  public void testGarbageInput() {
    KeyPairManager.decodeKeyMaterial( B64.standard.encString("Test decoding of key material that is not in a valid format. Perhaps a text file was sent instead of a public key.") )
  }

  @Test(expected=GeneralSecurityException.class)
  public void testInvalidKeyMaterial() {
    KeyPairManager.decodeKeyMaterial( "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\u1234" )
  }

  @Test(expected=GeneralSecurityException.class)
  public void testInvalidOpenSshKey() {
    KeyPairManager.decodeKeyMaterial( B64.standard.encString("ssh-rsa AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\u1234") )
  }
  
  @Test
  public void testFingerPrint() {
    final RSAPublicKey key = KeyPairManager.decodeKeyMaterial( B64.standard.encString("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQCq7plfA4ghB/X5050bpt4J8DTYkzO9tCDgD6m7EXDk+4zZl8wdAWUaGrUfZSMUZyjbl9IcrPNOF8wcfp5ksoZq7ZGTtZrRCSZVFeXTMhl4Wh0cX7Y85UGL7l3xuqBpwnGhRHIY91vwmE2hqYV65lY4Wxo/lL4/2NZojcyNJP1u8w== steve@fish.eucalyptus.com\n") )
    assertEquals( "RFC 4716 Fingerprint mismatch", "e5:4c:8f:66:bc:91:69:b8:ce:48:b8:f4:26:4d:fc:e5", KeyPairs.getPublicKeyFingerprint( key ) )
  }
}
