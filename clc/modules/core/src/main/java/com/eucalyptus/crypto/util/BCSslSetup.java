/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.crypto.util;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Hashtable;
import javax.crypto.spec.DHParameterSpec;
import org.apache.log4j.Logger;
import org.bouncycastle.jcajce.provider.asymmetric.dh.KeyPairGeneratorSpi;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import com.google.common.collect.ImmutableList;

/**
 *
 */
class BCSslSetup {

  private static Logger logger = Logger.getLogger( BCSslSetup.class );

  private static final ImmutableList<DHParameterSpec> parameterSpecs = ImmutableList.of(
      parameterSpec( // First Oakley Group - http://www.ietf.org/rfc/rfc2409.txt )(Section 6.1)
          "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1"
        + "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD"
        + "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245"
        + "E485B576 625E7EC6 F44C42E9 A63A3620 FFFFFFFF FFFFFFFF",
          2,
          768 ),
      parameterSpec( // Second Oakley Group - http://www.ietf.org/rfc/rfc2409.txt )(Section 6.2)
          "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1"
        + "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD"
        + "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245"
        + "E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED"
        + "EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE65381"
        + "FFFFFFFF FFFFFFFF",
          2,
          1024 )
  );

  private static DHParameterSpec parameterSpec( final String primeModulusHex,
                                                final int baseGenerator,
                                                final int sizeInBits ) {
    final BigInteger p = new BigInteger( primeModulusHex.replace( " ", "" ), 16 );
    final BigInteger g = BigInteger.valueOf( baseGenerator );
    return new DHParameterSpec( p, g, sizeInBits );
  }

  /**
   * Initialize Diffie-Hellman parameters using standard values.
   */
  static void initBouncyCastleDHParams( ) {
    try {
      initBouncyCastleDHParamsInternal( );
    } catch ( Throwable e ) {
      logger.error( e, e );
    }
  }

  @SuppressWarnings( "unchecked" )
  static void initBouncyCastleDHParamsInternal( ) throws NoSuchFieldException, IllegalAccessException {
    final Class<?> DH = KeyPairGeneratorSpi.class;
    final Field paramsField = DH.getDeclaredField( "params" );
    paramsField.setAccessible( true );
    final Hashtable<Integer,DHKeyGenerationParameters> params =
        (Hashtable<Integer,DHKeyGenerationParameters>) paramsField.get( null );
    if ( params.isEmpty() ) {
      final SecureRandom random = new SecureRandom();
      for ( final DHParameterSpec parameterSpec : parameterSpecs ) {
        params.put(
            parameterSpec.getL( ),
            new DHKeyGenerationParameters(
                random,
                new DHParameters( parameterSpec.getP( ), parameterSpec.getG( ), null, 0 ) ) );
      }
    }
  }
}
