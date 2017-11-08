/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.tokens

import com.eucalyptus.crypto.util.PEMFiles
import com.eucalyptus.tokens.oidc.JsonWebSignatureAlgorithm
import com.google.common.io.BaseEncoding
import groovy.transform.CompileStatic

import javax.crypto.Cipher
import java.nio.charset.StandardCharsets
import java.security.AlgorithmParameters
import java.security.GeneralSecurityException
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.function.Predicate

import static com.eucalyptus.tokens.oidc.JsonWebSignatureAlgorithm.JsonWebSignatureAlgorithmRegistry.register
import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*
import static org.junit.Assume.*
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.BeforeClass
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
 * Unit tests for the token service
 */
@CompileStatic
class TokensServiceTest {

  // 2048bit RSA X.509 certificate, public key used for signature tests with:
  // RS256 RS384 RS512 PS256 PS384 PS512
  String X509_RSA_2048_PEM = '''\
    -----BEGIN CERTIFICATE-----
    MIICnTCCAYUCBEReYeAwDQYJKoZIhvcNAQEFBQAwEzERMA8GA1UEAxMIand0LTIw
    NDgwHhcNMTQwMTI0MTMwOTE2WhcNMzQwMjIzMjAwMDAwWjATMREwDwYDVQQDEwhq
    d3QtMjA0ODCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKhWb9KXmv45
    +TKOKhFJkrboZbpbKPJ9Yp12xKLXf8060KfStEStIX+7dCuAYylYWoqiGpuLVVUL
    5JmHgXmK9TJpzv9Dfe3TAc/+35r8r9IYB2gXUOZkebty05R6PLY0RO/hs2ZhrOoz
    HMo+x216Gwz0CWaajcuiY5Yg1V8VvJ1iQ3rcRgZapk49RNX69kQrGS63gzj0gyHn
    Rtbqc/Ua2kobCA83nnznCom3AGinnlSN65AFPP5jmri0l79+4ZZNIerErSW96mUF
    8jlJFZI1yJIbzbv73tL+y4i0+BvzsWBs6TkHAp4pinaI8zT+hrVQ2jD4fkJEiRN9
    lAqLPUd8CNkCAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAnqBw3UHOSSHtU7yMi1+H
    E+9119tMh7X/fCpcpOnjYmhW8uy9SiPBZBl1z6vQYkMPcURnDMGHdA31kPKICZ6G
    LWGkBLY3BfIQi064e8vWHW7zX6+2Wi1zFWdJlmgQzBhbr8pYh9xjZe6FjPwbSEuS
    0uE8dWSWHJLdWsA4xNX9k3pr601R2vPVFCDKs3K1a8P/Xi59kYmKMjaX6vYT879y
    gWt43yhtGTF48y85+eqLdFRFANTbBFSzdRlPQUYa5d9PZGxeBTcg7UBkK/G+d6D5
    sd78T2ymwlLYrNi+cSDYD6S4hwZaLeEK6h7p/OoG02RBNuT4VqFRu5DJ6Po+C6Jh
    qQ==
    -----END CERTIFICATE-----
  '''.stripIndent( )

  // ECC public key x and y used for signature tests with ES256
  byte[] PUB_KEY_ECC_256_X = [ 4, 114, 29, 223, 58, 3, 191, 170, 67, 128, 229, 33, 242, 178, 157, 150, 133, 25, 209, 139, 166, 69, 55, 26, 84, 48, 169, 165, 67, 232, 98, 9 ];
  byte[] PUB_KEY_ECC_256_Y = [ 131, 116, 8, 14, 22, 150, 18, 75, 24, 181, 159, 78, 90, 51, 71, 159, 214, 186, 250, 47, 207, 246, 142, 127, 54, 183, 72, 72, 253, 21, 88, 53 ];

  // ECC public key x and y used for signature tests with ES384
  byte[] PUB_KEY_ECC_384_X = [ 70, 151, 220, 179, 62, 0, 79, 232, 114, 64, 58, 75, 91, 209, 232, 128, 7, 137, 151, 42, 13, 148, 15, 133, 93, 215, 7, 3, 136, 124, 14, 101, 242, 207, 192, 69, 212, 145, 88, 59, 222, 33, 127, 46, 30, 218, 175, 79 ];
  byte[] PUB_KEY_ECC_384_Y = [ 189, 202, 196, 30, 153, 53, 22, 122, 171, 4, 188, 42, 71, 2, 9, 193, 191, 17, 111, 180, 78, 6, 110, 153, 240, 147, 203, 45, 152, 236, 181, 156, 232, 223, 227, 148, 68, 148, 221, 176, 57, 149, 44, 203, 83, 85, 75, 55 ];

  // ECC public key x and y used for signature tests with ES512
  byte[] PUB_KEY_ECC_512_X = [ 0, 248, 73, 203, 53, 184, 34, 69, 111, 217, 230, 255, 108, 212, 241, 229, 95, 239, 93, 131, 100, 37, 86, 152, 87, 98, 170, 43, 25, 35, 80, 137, 62, 112, 197, 113, 138, 116, 114, 55, 165, 128, 8, 139, 148, 237, 109, 121, 40, 205, 3, 61, 127, 28, 195, 58, 43, 228, 224, 228, 82, 224, 219, 148, 204, 96 ];
  byte[] PUB_KEY_ECC_512_Y = [ 0, 60, 71, 97, 112, 106, 35, 121, 80, 182, 20, 167, 143, 8, 246, 108, 234, 160, 193, 10, 3, 148, 45, 11, 58, 177, 190, 172, 26, 178, 188, 240, 91, 25, 67, 79, 64, 241, 203, 65, 223, 218, 12, 227, 82, 178, 66, 160, 19, 194, 217, 172, 61, 250, 23, 78, 218, 130, 160, 105, 216, 208, 235, 124, 46, 32 ];

  @BeforeClass
  static void beforeClass() {
    if ( Security.getProvider( (String)BouncyCastleProvider.PROVIDER_NAME ) == null ) {
      Security.addProvider( new BouncyCastleProvider( ) )
    }
    assumeThat( 'Unlimited strength cryptography available', (Integer)Cipher.getMaxAllowedKeyLength("AES"), (Matcher<Integer>)equalTo( Integer.MAX_VALUE ) )
    register( new JsonWebSignatureAlgorithm.Es256JsonWebSignatureAlgorithm( ) )
    register( new JsonWebSignatureAlgorithm.Es384JsonWebSignatureAlgorithm( ) )
    register( new JsonWebSignatureAlgorithm.Es512JsonWebSignatureAlgorithm( ) )
    register( new JsonWebSignatureAlgorithm.Ps256JsonWebSignatureAlgorithm( ) )
    register( new JsonWebSignatureAlgorithm.Ps384JsonWebSignatureAlgorithm( ) )
    register( new JsonWebSignatureAlgorithm.Ps512JsonWebSignatureAlgorithm( ) )
    register( new JsonWebSignatureAlgorithm.Rs256JsonWebSignatureAlgorithm( ) )
    register( new JsonWebSignatureAlgorithm.Rs384JsonWebSignatureAlgorithm( ) )
    register( new JsonWebSignatureAlgorithm.Rs512JsonWebSignatureAlgorithm( ) )
  }

  @Test
  void testValidatePS256Signature( ) {
    String jwt = 'eyJhbGciOiJQUzI1NiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.S9xuR-IGfXEj5qsHcMtK-jcj1lezvVstw1AISp8dEQVRNgwOMZhUQnSCx9i1CA-pMucxR-lv4e7zd6h3cYCfMnyv7iuxraxNiNAgREhOT-bkBCZMNgb5t15xEtDSJ3MuBlK3YBtXyVcDDIdKH_Bwj-u363y6LuvZ8FEOGmIK5WSFi18Xjg-ihhvH1C6UzH1G82wrRbX6DyJKqrUnHAg8yzUJVP1AdgjWRt5BKpuYbXSib-MKZZkaE4q_hCb-j25xCzn8Ez8a7PO7p0fDGvZuOk_yzSfvXSavg7iE0GLuUTNv3nQ_xW-rfbrpYeyXNtstoK3JPFpdtORTyH1iIh7VVA'
    String jwksText = toJsonWebKeySet( "PS256", X509_RSA_2048_PEM )
    doSignatureVerification( jwt, jwksText )
  }

  @Test
  void testValidatePS384Signature( ) {
    String jwt = 'eyJhbGciOiJQUzM4NCIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.EKqVLw6nLGNt1h7KNFZbzkKhf788VBYCfnigYc0dBZBa64MrfbIFHtJuFgIGkCVSDYH-qs-i4w9ke6mD8mxTZFniMgzFXXaCFIrv6QZeMbKh6VYtSEPp7l0B1zMZiQw6egZbZ6a8VBkCRipuZggSlUTg5tHMMTj_jNVxxlY4uUwXlz7vakpbqgXe19pCDJrzEoXE0cNKV13eRCNA1tXOHx0dFL7Jm9NUq7blvhJ8iTw1jMFzK8bV6g6L7GclHBMoJ3MIvRp71m6idir-QeW1KCUfVtBs3HRn3a822LW02vGqopSkaGdRzQZOI28136AMeW4679UXE852srA2v3mWHQ'
    String jwksText = toJsonWebKeySet('PS384', X509_RSA_2048_PEM )
    doSignatureVerification( jwt, jwksText )
  }

  @Test
  void testValidatePS512Signature( ) {
    String jwt = 'eyJhbGciOiJQUzUxMiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.IvbnmxhKvM70C0n0grkF807wOQLyPOBwJOee-p7JHCQcSstNeml3Owdyw9C3HGHzOdK9db51yAkjJ2TCojxqHW4OR5Apna8tvafYgD2femn1V3GdkGj6ZvYdV3q4ldnmahVeO36vHYy5P0zFcEGU1_j3S3DwGmhw2ktZ4p5fLZ2up2qwhzlOjbtsQpWywHj7cLdeA32MLId9MTAPVGUHIZHw_W0xwjJRS6TgxD9vPQQnP70MY-q_2pVAhfRCM_pauPYO1XH5ldizrTvVr27q_-Uqtw-wV-UDUnyWYQUDDiMTpLBoX1EEXmsbvUGx0OH3yWEaNINoCsepgZvTKbiEQQ'
    String jwksText = toJsonWebKeySet('PS512', X509_RSA_2048_PEM )
    doSignatureVerification( jwt, jwksText )
  }

  @Test
  void testValidateRS256Signature( ) {
    String jwt = 'eyJhbGciOiJSUzI1NiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.NL_dfVpZkhNn4bZpCyMq5TmnXbT4yiyecuB6Kax_lV8Yq2dG8wLfea-T4UKnrjLOwxlbwLwuKzffWcnWv3LVAWfeBxhGTa0c4_0TX_wzLnsgLuU6s9M2GBkAIuSMHY6UTFumJlEeRBeiqZNrlqvmAzQ9ppJHfWWkW4stcgLCLMAZbTqvRSppC1SMxnvPXnZSWn_Fk_q3oGKWw6Nf0-j-aOhK0S0Lcr0PV69ZE4xBYM9PUS1MpMe2zF5J3Tqlc1VBcJ94fjDj1F7y8twmMT3H1PI9RozO-21R0SiXZ_a93fxhE_l_dj5drgOek7jUN9uBDjkXUwJPAyp9YPehrjyLdw'
    String jwksText = toJsonWebKeySet('RS256', X509_RSA_2048_PEM )
    doSignatureVerification( jwt, jwksText )
  }

  @Test
  void testValidateRS384Signature( ) {
    String jwt = 'eyJhbGciOiJSUzM4NCIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.cOPca7YEOxnXVdIi7cJqfgRMmDFPCrZG1M7WCJ23U57rAWvCTaQgEFdLjs7aeRAPY5Su_MVWV7YixcawKKYOGVG9eMmjdGiKHVoRcfjwVywGIb-nuD1IBzGesrQe7mFQrcWKtYD9FurjCY1WuI2FzGPp5YhW5Zf4TwmBvOKz6j2D1vOFfGsogzAyH4lqaMpkHpUAXddQxzu8rmFhZ54Rg4T-jMGVlsdrlAAlGA-fdRZ-V3F2PJjHQYUcyS6n1ULcy6ljEOgT5fY-_8DDLLpI8jAIdIhcHUAynuwvvnDr9bJ4xIy4olFRqcUQIHbcb5-WDeWul_cSGzTJdxDZsnDuvg'
    String jwksText = toJsonWebKeySet('RS384', X509_RSA_2048_PEM )
    doSignatureVerification( jwt, jwksText )
  }

  @Test
  void testValidateRS512Signature( ) {
    String jwt = 'eyJhbGciOiJSUzUxMiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.KP_mwCVRIxcF6ErdrzNcXZQDFGcL-Hlyocc4tIl3tJfzSfc7rz7qOLPjHpZ6UFH1ncd5TlpRc1B_pgvY-l0BNtx_s7n_QA55X4c1oeD8csrIoXQ6A6mtvdVGoSlGu2JnP6N2aqlDmlcefKqjl_Z-8nwDMGTMkDNhHKfHlIb2_Dliwxeq8LmNMREEdvNH2XVp_ffxBjiaKv2Eqbwc6I17241GCEmjDCvnagSgjX_5uu-da2H7TK2gtPJYUo8r9nzC7uzZJ5SB8suZH0COSofsP-9wvH0FESO40evCyEBylqg3bh9M9dIzeq8_bdTiC5kG93Fal44OEY8_Zm88wB_VjQ'
    String jwksText = toJsonWebKeySet('RS512', X509_RSA_2048_PEM )
    doSignatureVerification( jwt, jwksText )
  }

  @Test
  void testValidateES256Signature( ) {
    assumeTrue( "EC not available", ecAvailable( ) )
    String jwt = 'eyJhbGciOiJFUzI1NiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.EVnmDMlz-oi05AQzts-R3aqWvaBlwVZddWkmaaHyMx5Phb2NSLgyI0kccpgjjAyo1S5KCB3LIMPfmxCX_obMKA'
    String jwksText = toJsonWebKeySet('ES256', PUB_KEY_ECC_256_X, PUB_KEY_ECC_256_Y )
    doSignatureVerification( jwt, jwksText )
  }

  @Test
  void testValidateES384Signature( ) {
    assumeTrue( "EC not available", ecAvailable( ) )
    String jwt = 'eyJhbGciOiJFUzM4NCIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.jVTHd9T0fIQDJLNvAq3LPpgj_npXtWb64FfEK8Sm65Nr9q2goUWASrM9jv3h-71UrP4cBpM3on3yN--o6B-Tl6bscVUfpm1swPp94f7XD9VYLEjGMjQOaozr13iBZJCY'
    String jwksText = toJsonWebKeySet('ES384', PUB_KEY_ECC_384_X, PUB_KEY_ECC_384_Y )
    doSignatureVerification( jwt, jwksText )
  }

  @Test
  void testValidateES512Signature( ) {
    assumeTrue( "EC not available", ecAvailable( ) )
    String jwt = 'eyJhbGciOiJFUzUxMiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.AHxJYFeTVpZmrfZsltpQKkkplmbkycQKFOFucD7hE4Sm3rCswUDi8hlSCfeYByugySYLFzogTQGk79PHP6vdl39sAUc9k2bhnv-NxRmJsN8ZxEx09qYKbc14qiNWZztLweQg0U-pU0DQ66rwJ0HikzSqgmyD1bJ6RxitJwceYLAovv0v'
    String jwksText = toJsonWebKeySet('ES512', PUB_KEY_ECC_512_X, PUB_KEY_ECC_512_Y )
    doSignatureVerification( jwt, jwksText )
  }

  @Test
  void testValidateRS512SignatureFromGlobusAuth( ) {
    String jwt = 'eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJub25jZSI6bnVsbCwiYXRfaGFzaCI6IkxtdE9BTnRKTHVwUnN4ZUh2V0daMGRpa3hpYk0xdmluanNIM0dvc1VFbFkiLCJhdWQiOiI2NTkwNjdlYy05Njk4LTQ0YTgtODhlYS1kYjMxZTA3MTQ0N2EiLCJzdWIiOiI2ZjNhMTdkNC01MzhlLTQ0MzEtODg5Yi1mNjFhZWExNWNmMGMiLCJleHAiOjE0NzM0NDk1MDQsImlzcyI6Imh0dHBzOi8vYXV0aC5nbG9idXMub3JnIiwiaWF0IjoxNDczMjc2NzA0LCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJka2F2YW5hZ2hAZ21haWwuY29tIiwiZW1haWwiOiJka2F2YW5hZ2hAZ21haWwuY29tIiwibmFtZSI6IkRhdmlkIEthdmFuYWdoIn0.JHOByrbPN5MKSkzuMuCiKvg2DeL_HazpjGdLp2MX-560rISAzpyO9a0Nqu7dOrWduY-gMIafVkpsUJAzhuV0YoUSt-35vbaX2TUafyFDZwtDsJ9mhmD6Rr9QQGEJDSfTnde-WIor0mBVVSeVKAbBUTBlgn_JzWizwCCE6RtqZvenEFUtyPiw_1QCfjakPKsIpIPceFHTLlQ0tu44UUHTwcUrdIFDouQkPXuqiObuIeOAKHSO7yOK3PXxB8lmJMzf57MKRybwjYGwMCkmBe_43kvXB3wWDEuQNf_cePaYdHtSi3EsWW4hwL4mf6PDR62tMtA6ygrIijp5M8vegrtnuA'
    String jwksText = '{"keys":[{"kty":"RSA","n":"73l27Yp7WT2c0Ve7EoGJ13AuKzg-GHU7Mpgx0JKa_hO04gAXSVXRadQy7gmdLLtAK8uBVcV0fHGgsBl4J92t-I7hayiJSLbgbX-sZhI_OfegeOLcSNB9poPS9w60XGqR9buYOW2x-KXXitsmyHXNmg_-1u0uqfKHu9pmST8dcjUYXTM5F3oJpQKeJlSH8daMlDks4xb9Y83EEFRv-ppY965-WTm2NW4pwLlbgGTWFvZ6YS6GTb-mfGwGuzStI0lKZ7dOFx9ryYQ4wSoUVHtIrypT-gbuaT90Z2SkwOH-GaEZJkudctBeGpieOsyC7P40UXpwgGNFy3xoWL4vHpnHmQ==","e":"AQAB","alg":"RS512","use":"sig"}]}'
    doSignatureVerification( jwt, jwksText )
  }

  @Test( expected = GeneralSecurityException )
  void testValidateSignatureAlgorithmCheck( ) {
    String jwt = 'eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJub25jZSI6bnVsbCwiYXRfaGFzaCI6IkxtdE9BTnRKTHVwUnN4ZUh2V0daMGRpa3hpYk0xdmluanNIM0dvc1VFbFkiLCJhdWQiOiI2NTkwNjdlYy05Njk4LTQ0YTgtODhlYS1kYjMxZTA3MTQ0N2EiLCJzdWIiOiI2ZjNhMTdkNC01MzhlLTQ0MzEtODg5Yi1mNjFhZWExNWNmMGMiLCJleHAiOjE0NzM0NDk1MDQsImlzcyI6Imh0dHBzOi8vYXV0aC5nbG9idXMub3JnIiwiaWF0IjoxNDczMjc2NzA0LCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJka2F2YW5hZ2hAZ21haWwuY29tIiwiZW1haWwiOiJka2F2YW5hZ2hAZ21haWwuY29tIiwibmFtZSI6IkRhdmlkIEthdmFuYWdoIn0.JHOByrbPN5MKSkzuMuCiKvg2DeL_HazpjGdLp2MX-560rISAzpyO9a0Nqu7dOrWduY-gMIafVkpsUJAzhuV0YoUSt-35vbaX2TUafyFDZwtDsJ9mhmD6Rr9QQGEJDSfTnde-WIor0mBVVSeVKAbBUTBlgn_JzWizwCCE6RtqZvenEFUtyPiw_1QCfjakPKsIpIPceFHTLlQ0tu44UUHTwcUrdIFDouQkPXuqiObuIeOAKHSO7yOK3PXxB8lmJMzf57MKRybwjYGwMCkmBe_43kvXB3wWDEuQNf_cePaYdHtSi3EsWW4hwL4mf6PDR62tMtA6ygrIijp5M8vegrtnuA'
    String jwksText = '{"keys":[{"kty":"RSA","n":"73l27Yp7WT2c0Ve7EoGJ13AuKzg-GHU7Mpgx0JKa_hO04gAXSVXRadQy7gmdLLtAK8uBVcV0fHGgsBl4J92t-I7hayiJSLbgbX-sZhI_OfegeOLcSNB9poPS9w60XGqR9buYOW2x-KXXitsmyHXNmg_-1u0uqfKHu9pmST8dcjUYXTM5F3oJpQKeJlSH8daMlDks4xb9Y83EEFRv-ppY965-WTm2NW4pwLlbgGTWFvZ6YS6GTb-mfGwGuzStI0lKZ7dOFx9ryYQ4wSoUVHtIrypT-gbuaT90Z2SkwOH-GaEZJkudctBeGpieOsyC7P40UXpwgGNFy3xoWL4vHpnHmQ==","e":"AQAB","alg":"RS512","use":"sig"}]}'
    doSignatureVerification( jwt, jwksText, { false } )
  }

  @Test
  void testValidatePublicKeyAgainstCertificateMatchSuccess( ) {
    String jwt = 'eyJhbGciOiJSUzUxMiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.KP_mwCVRIxcF6ErdrzNcXZQDFGcL-Hlyocc4tIl3tJfzSfc7rz7qOLPjHpZ6UFH1ncd5TlpRc1B_pgvY-l0BNtx_s7n_QA55X4c1oeD8csrIoXQ6A6mtvdVGoSlGu2JnP6N2aqlDmlcefKqjl_Z-8nwDMGTMkDNhHKfHlIb2_Dliwxeq8LmNMREEdvNH2XVp_ffxBjiaKv2Eqbwc6I17241GCEmjDCvnagSgjX_5uu-da2H7TK2gtPJYUo8r9nzC7uzZJ5SB8suZH0COSofsP-9wvH0FESO40evCyEBylqg3bh9M9dIzeq8_bdTiC5kG93Fal44OEY8_Zm88wB_VjQ'
    final X509Certificate cert = PEMFiles.getCert( X509_RSA_2048_PEM.getBytes( StandardCharsets.UTF_8 ) )
    String jwksText = """\
    {
      "keys" : [
        {
          "kty": "RSA",
          "alg": "RS512",
          "use": "sig",
          "x5c": [
              "MIICnTCCAYUCBEReYeAwDQYJKoZIhvcNAQEFBQAwEzERMA8GA1UEAxMIand0LTIwNDgwHhcNMTQwMTI0MTMwOTE2WhcNMzQwMjIzMjAwMDAwWjATMREwDwYDVQQDEwhqd3QtMjA0ODCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKhWb9KXmv45+TKOKhFJkrboZbpbKPJ9Yp12xKLXf8060KfStEStIX+7dCuAYylYWoqiGpuLVVUL5JmHgXmK9TJpzv9Dfe3TAc/+35r8r9IYB2gXUOZkebty05R6PLY0RO/hs2ZhrOozHMo+x216Gwz0CWaajcuiY5Yg1V8VvJ1iQ3rcRgZapk49RNX69kQrGS63gzj0gyHnRtbqc/Ua2kobCA83nnznCom3AGinnlSN65AFPP5jmri0l79+4ZZNIerErSW96mUF8jlJFZI1yJIbzbv73tL+y4i0+BvzsWBs6TkHAp4pinaI8zT+hrVQ2jD4fkJEiRN9lAqLPUd8CNkCAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAnqBw3UHOSSHtU7yMi1+HE+9119tMh7X/fCpcpOnjYmhW8uy9SiPBZBl1z6vQYkMPcURnDMGHdA31kPKICZ6GLWGkBLY3BfIQi064e8vWHW7zX6+2Wi1zFWdJlmgQzBhbr8pYh9xjZe6FjPwbSEuS0uE8dWSWHJLdWsA4xNX9k3pr601R2vPVFCDKs3K1a8P/Xi59kYmKMjaX6vYT879ygWt43yhtGTF48y85+eqLdFRFANTbBFSzdRlPQUYa5d9PZGxeBTcg7UBkK/G+d6D5sd78T2ymwlLYrNi+cSDYD6S4hwZaLeEK6h7p/OoG02RBNuT4VqFRu5DJ6Po+C6JhqQ==",
              "MIIDQDCCAiigAwIBAgIGEdGWnioLMA0GCSqGSIb3DQEBDQUAMEcxCzAJBgNVBAYTAlVTMQ4wDAYDVQQKEwVDbG91ZDETMBEGA1UECxMKRXVjYWx5cHR1czETMBEGA1UEAxMKZXVjYWx5cHR1czAeFw0xNDAyMDgwOTEzMzVaFw0xOTAyMDgwOTEzMzVaMEcxCzAJBgNVBAYTAlVTMQ4wDAYDVQQKEwVDbG91ZDETMBEGA1UECxMKRXVjYWx5cHR1czETMBEGA1UEAxMKZXVjYWx5cHR1czCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJDACdbWOgt62luZU6kpex588T3ubRI7M20rYlH+qXA1nr/GgDfPuoGvmb0WJ2cu1lkzhYaYAnSUYlW/nnkL+tpIyRdqQVJ2fkVrghdATLw/lr++EXhRgkjBiZ/aFFfnybu4loFks+TeBtvcxxu4d1dlP+Bj9T46qAeavrkb+1XJd6QHtveq0BX8kTQwzOI8H3z8JebHhKWvfUN+FlKb8bE0qR81Cp1wrgX4L2tc5+rmsUJzOLw5JDGluBssbVM1WG4wcb7pkzbzo9cskU2aAHaJ+1nsQEwV+RhrzT61s5isDUbNyhXEcjFkYN8PUgrYChUFq2yf8gvE7VQPV0uT5ucCAwEAAaMyMDAwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUHhzS6zUUubb6FIbvslILf1mZ0gowDQYJKoZIhvcNAQENBQADggEBAEnJ4JOGziIu6zyywgHnhqSZqPEl0nQlw22jHcB34uqAsdaLFgfFQ46+RZY0rkAr+qqtwIKS+LnxxfAQKWIcmI+l5NhhuoMTfb9AGcyZoyfOuwwRH6Ms9cbrE8kc79A75AAdS7SgPlRc2R+5/gOQhu91CyGu6XomjOEluZoxQ0cYUlyurPwg5pM+/ILj6RPjVcZzzEgAEUkpMR8gep+pwHLAFrEcZDqba4KrEQMM4l9wAjkZoRMhSbXVHuu4qYUVeTGbneFQtlJjtdaQNkozUoub8pLFx3bFEkh7iqGw5MWnPNM/HkFwgqMN3dLxeqZPDpoMdWklxKpQWXeDrSciafo="
          ],
          "n": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).modulus.toByteArray( ) )}",
          "e": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).publicExponent.toByteArray( ) )}"
        }
      ]
    }
    """.stripIndent( )
    doSignatureVerification( jwt, jwksText )
  }

  @Test( expected = GeneralSecurityException )
  void testValidatePublicKeyAgainstCertificateMatchFailure( ) {
    String jwt = 'eyJhbGciOiJSUzUxMiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.KP_mwCVRIxcF6ErdrzNcXZQDFGcL-Hlyocc4tIl3tJfzSfc7rz7qOLPjHpZ6UFH1ncd5TlpRc1B_pgvY-l0BNtx_s7n_QA55X4c1oeD8csrIoXQ6A6mtvdVGoSlGu2JnP6N2aqlDmlcefKqjl_Z-8nwDMGTMkDNhHKfHlIb2_Dliwxeq8LmNMREEdvNH2XVp_ffxBjiaKv2Eqbwc6I17241GCEmjDCvnagSgjX_5uu-da2H7TK2gtPJYUo8r9nzC7uzZJ5SB8suZH0COSofsP-9wvH0FESO40evCyEBylqg3bh9M9dIzeq8_bdTiC5kG93Fal44OEY8_Zm88wB_VjQ'
    final X509Certificate cert = PEMFiles.getCert( X509_RSA_2048_PEM.getBytes( StandardCharsets.UTF_8 ) )
    String jwksText = """\
    {
      "keys" : [
        {
          "kty": "RSA",
          "alg": "RS512",
          "use": "sig",
          "x5c": [ "MIIDQDCCAiigAwIBAgIGEdGWnioLMA0GCSqGSIb3DQEBDQUAMEcxCzAJBgNVBAYTAlVTMQ4wDAYDVQQKEwVDbG91ZDETMBEGA1UECxMKRXVjYWx5cHR1czETMBEGA1UEAxMKZXVjYWx5cHR1czAeFw0xNDAyMDgwOTEzMzVaFw0xOTAyMDgwOTEzMzVaMEcxCzAJBgNVBAYTAlVTMQ4wDAYDVQQKEwVDbG91ZDETMBEGA1UECxMKRXVjYWx5cHR1czETMBEGA1UEAxMKZXVjYWx5cHR1czCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJDACdbWOgt62luZU6kpex588T3ubRI7M20rYlH+qXA1nr/GgDfPuoGvmb0WJ2cu1lkzhYaYAnSUYlW/nnkL+tpIyRdqQVJ2fkVrghdATLw/lr++EXhRgkjBiZ/aFFfnybu4loFks+TeBtvcxxu4d1dlP+Bj9T46qAeavrkb+1XJd6QHtveq0BX8kTQwzOI8H3z8JebHhKWvfUN+FlKb8bE0qR81Cp1wrgX4L2tc5+rmsUJzOLw5JDGluBssbVM1WG4wcb7pkzbzo9cskU2aAHaJ+1nsQEwV+RhrzT61s5isDUbNyhXEcjFkYN8PUgrYChUFq2yf8gvE7VQPV0uT5ucCAwEAAaMyMDAwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUHhzS6zUUubb6FIbvslILf1mZ0gowDQYJKoZIhvcNAQENBQADggEBAEnJ4JOGziIu6zyywgHnhqSZqPEl0nQlw22jHcB34uqAsdaLFgfFQ46+RZY0rkAr+qqtwIKS+LnxxfAQKWIcmI+l5NhhuoMTfb9AGcyZoyfOuwwRH6Ms9cbrE8kc79A75AAdS7SgPlRc2R+5/gOQhu91CyGu6XomjOEluZoxQ0cYUlyurPwg5pM+/ILj6RPjVcZzzEgAEUkpMR8gep+pwHLAFrEcZDqba4KrEQMM4l9wAjkZoRMhSbXVHuu4qYUVeTGbneFQtlJjtdaQNkozUoub8pLFx3bFEkh7iqGw5MWnPNM/HkFwgqMN3dLxeqZPDpoMdWklxKpQWXeDrSciafo=" ],
          "n": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).modulus.toByteArray( ) )}",
          "e": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).publicExponent.toByteArray( ) )}"
        }
      ]
    }
    """.stripIndent( )
    doSignatureVerification( jwt, jwksText )
  }

  @Test( expected = GeneralSecurityException )
  void testValidatePublicKeyAgainstCertificateDecodeFailure( ) {
    String jwt = 'eyJhbGciOiJSUzUxMiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.KP_mwCVRIxcF6ErdrzNcXZQDFGcL-Hlyocc4tIl3tJfzSfc7rz7qOLPjHpZ6UFH1ncd5TlpRc1B_pgvY-l0BNtx_s7n_QA55X4c1oeD8csrIoXQ6A6mtvdVGoSlGu2JnP6N2aqlDmlcefKqjl_Z-8nwDMGTMkDNhHKfHlIb2_Dliwxeq8LmNMREEdvNH2XVp_ffxBjiaKv2Eqbwc6I17241GCEmjDCvnagSgjX_5uu-da2H7TK2gtPJYUo8r9nzC7uzZJ5SB8suZH0COSofsP-9wvH0FESO40evCyEBylqg3bh9M9dIzeq8_bdTiC5kG93Fal44OEY8_Zm88wB_VjQ'
    final X509Certificate cert = PEMFiles.getCert( X509_RSA_2048_PEM.getBytes( StandardCharsets.UTF_8 ) )
    String jwksText = """\
    {
      "keys" : [
        {
          "kty": "RSA",
          "alg": "RS512",
          "use": "sig",
          "x5c": [ "MIIDQDCCAiigAwIBAgIGEdG" ],
          "n": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).modulus.toByteArray( ) )}",
          "e": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).publicExponent.toByteArray( ) )}"
        }
      ]
    }
    """.stripIndent( )
    doSignatureVerification( jwt, jwksText )
  }

  @Test( expected = GeneralSecurityException )
  void testValidateJsonWebKeyUseFailure( ) {
    String jwt = 'eyJhbGciOiJSUzUxMiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.KP_mwCVRIxcF6ErdrzNcXZQDFGcL-Hlyocc4tIl3tJfzSfc7rz7qOLPjHpZ6UFH1ncd5TlpRc1B_pgvY-l0BNtx_s7n_QA55X4c1oeD8csrIoXQ6A6mtvdVGoSlGu2JnP6N2aqlDmlcefKqjl_Z-8nwDMGTMkDNhHKfHlIb2_Dliwxeq8LmNMREEdvNH2XVp_ffxBjiaKv2Eqbwc6I17241GCEmjDCvnagSgjX_5uu-da2H7TK2gtPJYUo8r9nzC7uzZJ5SB8suZH0COSofsP-9wvH0FESO40evCyEBylqg3bh9M9dIzeq8_bdTiC5kG93Fal44OEY8_Zm88wB_VjQ'
    final X509Certificate cert = PEMFiles.getCert( X509_RSA_2048_PEM.getBytes( StandardCharsets.UTF_8 ) )
    String jwksText = """\
    {
      "keys" : [
        {
          "kty": "RSA",
          "alg": "RS512",
          "use": "enc",
          "n": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).modulus.toByteArray( ) )}",
          "e": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).publicExponent.toByteArray( ) )}"
        }
      ]
    }
    """.stripIndent( )
    doSignatureVerification( jwt, jwksText )
  }


  @Test( expected = GeneralSecurityException )
  void testValidateJsonWebKeyKeyOpsFailure( ) {
    String jwt = 'eyJhbGciOiJSUzUxMiIsImN0eSI6InRleHRcL3BsYWluIn0.eyJoZWxsbyI6ICJ3b3JsZCJ9.KP_mwCVRIxcF6ErdrzNcXZQDFGcL-Hlyocc4tIl3tJfzSfc7rz7qOLPjHpZ6UFH1ncd5TlpRc1B_pgvY-l0BNtx_s7n_QA55X4c1oeD8csrIoXQ6A6mtvdVGoSlGu2JnP6N2aqlDmlcefKqjl_Z-8nwDMGTMkDNhHKfHlIb2_Dliwxeq8LmNMREEdvNH2XVp_ffxBjiaKv2Eqbwc6I17241GCEmjDCvnagSgjX_5uu-da2H7TK2gtPJYUo8r9nzC7uzZJ5SB8suZH0COSofsP-9wvH0FESO40evCyEBylqg3bh9M9dIzeq8_bdTiC5kG93Fal44OEY8_Zm88wB_VjQ'
    final X509Certificate cert = PEMFiles.getCert( X509_RSA_2048_PEM.getBytes( StandardCharsets.UTF_8 ) )
    String jwksText = """\
    {
      "keys" : [
        {
          "kty": "RSA",
          "alg": "RS512",
          "key_ops": ["encrypt"],
          "n": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).modulus.toByteArray( ) )}",
          "e": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).publicExponent.toByteArray( ) )}"
        }
      ]
    }
    """.stripIndent( )
    doSignatureVerification( jwt, jwksText )
  }

  private static doSignatureVerification( String jwt, String jwksText, Predicate<String> algorithmPredicate = { true } ) {
    String [] jwtParts = jwt.split( "\\." )
    Boolean verified = TokensService.isSignatureVerified( jwtParts, jwksText, algorithmPredicate )
    assertTrue('Signature is verified', verified )
  }

  private static String toJsonWebKeySet( final String alg, final String certificatePem ) {
    final X509Certificate cert = PEMFiles.getCert( certificatePem.getBytes( StandardCharsets.UTF_8 ) )
    """\
    {
      "keys" : [
        {
          "kty": "RSA",
          "alg": "${alg}",
          "use": "sig",
          "n": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).modulus.toByteArray( ) )}",
          "e": "${BaseEncoding.base64Url( ).encode( ((RSAPublicKey)cert.getPublicKey( )).publicExponent.toByteArray( ) )}"
        }
      ]
    }
    """.stripIndent( )
  }

  private static String toJsonWebKeySet( final String alg, final byte[] x, final byte[] y ) {
    """\
    {
      "keys" : [
        {
          "kty": "EC",
          "alg": "${alg}",
          "use": "sig",
          "crv": "P-${alg.substring(2).replace('512','521')}",
          "x": "${BaseEncoding.base64Url( ).encode( x )}",
          "y": "${BaseEncoding.base64Url( ).encode( y )}"
        }
      ]
    }
    """.stripIndent( )
  }

  private static boolean ecAvailable() {
    try {
      AlgorithmParameters.getInstance( "EC" )
      true
    } catch( Exception e ) {
      false
    }
  }
}
