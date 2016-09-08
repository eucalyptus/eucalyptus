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
 ************************************************************************/
package com.eucalyptus.tokens

import groovy.transform.CompileStatic

import javax.crypto.Cipher

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*
import org.hamcrest.Matcher
import org.junit.Test

import org.junit.BeforeClass
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import com.eucalyptus.auth.principal.Certificate

import static org.junit.Assume.assumeThat

/**
 * Unit tests for assume role with web identity token validation
 */
@CompileStatic
class WebIdentityTest {

  @BeforeClass
  static void beforeClass() {
    if ( Security.getProvider( (String)BouncyCastleProvider.PROVIDER_NAME ) == null ) {
      Security.addProvider( new BouncyCastleProvider( ) )
    }
    assumeThat( "Unlimited strength cryptography available", (Integer)Cipher.getMaxAllowedKeyLength("AES"), (Matcher<Integer>)equalTo( Integer.MAX_VALUE ) )
  }

  @Test
  void testValidateSignature() {
    String jwt = "yJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJub25jZSI6bnVsbCwiYXRfaGFzaCI6IkxtdE9BTnRKTHVwUnN4ZUh2V0daMGRpa3hpYk0xdmluanNIM0dvc1VFbFkiLCJhdWQiOiI2NTkwNjdlYy05Njk4LTQ0YTgtODhlYS1kYjMxZTA3MTQ0N2EiLCJzdWIiOiI2ZjNhMTdkNC01MzhlLTQ0MzEtODg5Yi1mNjFhZWExNWNmMGMiLCJleHAiOjE0NzM0NDk1MDQsImlzcyI6Imh0dHBzOi8vYXV0aC5nbG9idXMub3JnIiwiaWF0IjoxNDczMjc2NzA0LCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJka2F2YW5hZ2hAZ21haWwuY29tIiwiZW1haWwiOiJka2F2YW5hZ2hAZ21haWwuY29tIiwibmFtZSI6IkRhdmlkIEthdmFuYWdoIn0.JHOByrbPN5MKSkzuMuCiKvg2DeL_HazpjGdLp2MX-560rISAzpyO9a0Nqu7dOrWduY-gMIafVkpsUJAzhuV0YoUSt-35vbaX2TUafyFDZwtDsJ9mhmD6Rr9QQGEJDSfTnde-WIor0mBVVSeVKAbBUTBlgn_JzWizwCCE6RtqZvenEFUtyPiw_1QCfjakPKsIpIPceFHTLlQ0tu44UUHTwcUrdIFDouQkPXuqiObuIeOAKHSO7yOK3PXxB8lmJMzf57MKRybwjYGwMCkmBe_43kvXB3wWDEuQNf_cePaYdHtSi3EsWW4hwL4mf6PDR62tMtA6ygrIijp5M8vegrtnuA"
    String jwkText = "{\"keys\":[{\"kty\":\"RSA\",\"n\":\"73l27Yp7WT2c0Ve7EoGJ13AuKzg-GHU7Mpgx0JKa_hO04gAXSVXRadQy7gmdLLtAK8uBVcV0fHGgsBl4J92t-I7hayiJSLbgbX-sZhI_OfegeOLcSNB9poPS9w60XGqR9buYOW2x-KXXitsmyHXNmg_-1u0uqfKHu9pmST8dcjUYXTM5F3oJpQKeJlSH8daMlDks4xb9Y83EEFRv-ppY965-WTm2NW4pwLlbgGTWFvZ6YS6GTb-mfGwGuzStI0lKZ7dOFx9ryYQ4wSoUVHtIrypT-gbuaT90Z2SkwOH-GaEZJkudctBeGpieOsyC7P40UXpwgGNFy3xoWL4vHpnHmQ==\",\"e\":\"AQAB\",\"alg\":\"RS512\",\"use\":\"sig\"}]}"
    String [] jwtParts = jwt.split( "\\." )

    Boolean verified = TokensService.isSignatureVerified( jwtParts, jwkText )
    assertTrue( "Signature is verified", verified )
  }
}
