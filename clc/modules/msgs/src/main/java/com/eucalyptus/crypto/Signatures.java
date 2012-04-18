package com.eucalyptus.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;

public enum Signatures {
  SHA256withRSA,
  SHA1WithRSA;
  private static Logger LOG = Logger.getLogger( Signatures.class );

  public String trySign( Class<? extends ComponentId> component, byte[] data ) {
    PrivateKey pk = SystemCredentials.lookup( component ).getPrivateKey( );
    return trySign( pk, data );
  }
  /**
   * Identical to Signatures#sign() except in that it throws no checked exceptions and instead returns null in the case of a failure.
   */
  public String trySign( PrivateKey pk, byte[] data ) {
    try {
      return this.sign( pk, data );
    } catch ( Exception e ) {
      return null;
    }
  }
  static final String HEXES = "0123456789ABCDEF";
  public String sign( PrivateKey pk, byte[] data ) throws InvalidKeyException, SignatureException {
    Signature signer = this.getInstance( );
    signer.initSign( pk );
    try {
      signer.update( data );
      byte[] sig = signer.sign( );
      final StringBuilder hex = new StringBuilder( 2 * sig.length );
      for ( final byte b : sig ) {
        hex.append(HEXES.charAt((b & 0xF0) >> 4))
           .append(HEXES.charAt((b & 0x0F)));
      }
      return hex.toString().toLowerCase( );
    } catch ( SignatureException e ) {
      LOG.debug( e, e );
      throw e;
    }
  }
  public Signature getInstance( ) {
    try {
      return Signature.getInstance( this.toString( ) );
    } catch ( NoSuchAlgorithmException e ) {
      LOG.fatal( e, e );
      throw new RuntimeException( e );
    }
  }
  
}
