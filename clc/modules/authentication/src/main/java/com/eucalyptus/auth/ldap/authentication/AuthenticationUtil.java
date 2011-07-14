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
