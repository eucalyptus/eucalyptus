/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Hashtable;
import javax.crypto.spec.DHParameterSpec;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.jce.provider.JDKKeyPairGenerator;
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
    final Class<?> DH = JDKKeyPairGenerator.DH.class;
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
