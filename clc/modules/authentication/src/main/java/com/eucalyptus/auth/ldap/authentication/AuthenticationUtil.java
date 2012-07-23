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

package com.eucalyptus.auth.ldap.authentication;

import java.security.GeneralSecurityException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.crypto.StringCrypto;

public class AuthenticationUtil {

  public static final String CRYPTO_FORMAT = "RSA/ECB/PKCS1Padding";
  public static final String CRYPTO_PROVIDER = "BC";
  
  public static final Pattern ENCRYPTED_PATTERN = Pattern.compile( "\\{(.+)\\}(.+)" );

  private static final Logger LOG = Logger.getLogger( AuthenticationUtil.class );
  
  private static final StringCrypto CYPTO = new StringCrypto( CRYPTO_FORMAT, CRYPTO_PROVIDER );
  
  /**
   * @return a StringCrypto singleton.
   */
  private static synchronized StringCrypto getCrypto( ) {
    return CYPTO;
  }
  
  /**
   * Decrypt password in LIC.
   * 
   * @param licCred
   * @return
   * @throws LdapException
   */
  public static String decryptPassword( String licCred ) throws LdapException {
    try {
      Matcher matcher = ENCRYPTED_PATTERN.matcher( licCred );
      if ( matcher.matches( ) ) {
        return getCrypto( ).decryptOpenssl( matcher.group( 1 )/*format*/, matcher.group( 2 )/*passwordEncoded*/ );
      } else {
        // Not encrypted
        return licCred;
      }
    } catch ( GeneralSecurityException e ) {
      LOG.error( e, e );
      throw new LdapException( "Decryption failure", e );
    }
  }
  
}
