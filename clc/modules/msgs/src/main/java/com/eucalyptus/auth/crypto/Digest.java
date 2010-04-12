package com.eucalyptus.auth.crypto;

import java.security.MessageDigest;

public enum Digest {
  GOST3411,
  Tiger,
  Whirlpool,
  MD2,
  MD4,
  MD5,
  RipeMD128,
  RipeMD160,
  RipeMD256,
  RipeMD320,
  SHA1,
  SHA224,
  SHA256,
  SHA384,
  SHA512;

  public MessageDigest get( ) {
    try {
      return MessageDigest.getInstance( this.name( ) );
    } catch ( Exception e ) {
      e.printStackTrace( );
      System.exit( -4 );
      return null;
    }
  }
}
