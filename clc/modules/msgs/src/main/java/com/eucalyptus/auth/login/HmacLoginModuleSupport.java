/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.login;

import java.net.URLDecoder;
import java.util.BitSet;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AccessKeys;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.InvalidAccessKeyAuthException;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.crypto.util.B64;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

/**
 * Support class for HMAC login modules
 */
public abstract class HmacLoginModuleSupport extends BaseLoginModule<HmacCredentials> {

  protected static final Logger signatureLogger = Logger.getLogger( "com.eucalyptus.auth.login.HMAC" );

  private final int signatureVersion;

  /**
   * Safe characters for URL parameters
   */
  protected static final BitSet URL_SAFE_CHARACTERS = new BitSet( 256 );

  /**
   * Do not URL-encode any of the unreserved characters that RFC 3986 defines:
   *
   *   A-Z, a-z, 0-9, hyphen ( - ), underscore ( _ ), period ( . ), and tilde ( ~ ).
   */
  static {
    for ( int i = 'A'; i <= 'Z'; i++ ) {
      URL_SAFE_CHARACTERS.set( i );
    }
    for ( int i = 'a'; i <= 'z'; i++ ) {
      URL_SAFE_CHARACTERS.set( i );
    }
    for ( int i = '0'; i <= '9'; i++ ) {
      URL_SAFE_CHARACTERS.set( i );
    }
    URL_SAFE_CHARACTERS.set( '-' );
    URL_SAFE_CHARACTERS.set( '_' );
    URL_SAFE_CHARACTERS.set( '.' );
    URL_SAFE_CHARACTERS.set( '~' );
  }

  protected HmacLoginModuleSupport( final int signatureVersion ) {
    this.signatureVersion = signatureVersion;
  }

  @Override
  public boolean accepts( ) {
    return super.getCallbackHandler( ) instanceof HmacCredentials && ((HmacCredentials)super.getCallbackHandler( )).getSignatureVersion( ).equals( signatureVersion );
  }

  @Override
  public void reset( ) {
  }

  protected AccessKey lookupAccessKey( final HmacCredentials credentials ) throws AuthException {
    final AccessKey key =
        AccessKeys.lookupAccessKey( credentials.getQueryId( ), credentials.getSecurityToken( ) );
    if ( !key.isActive() ) throw new InvalidAccessKeyAuthException( "Invalid access key or token" );
    return key;
  }
  
  protected String urldecode( final String text ) {
    return URLDecoder.decode( text );
  }
  
  public static String urlencode( final String text ) {
    final byte[] textBytes = Strings.nullToEmpty( text ).getBytes( Charsets.UTF_8 );
    return new String( URLCodec.encodeUrl( URL_SAFE_CHARACTERS, textBytes ), Charsets.US_ASCII );
  }

  protected String sanitize( final String b64text ) {
    // There should only be trailing =, it is not clear why
    // we replace = at other locations in B64 data
    return b64text.replace( "=", "" );
  }

  protected String normalize( final String signature ) {
    final String urldecoded = urldecode( signature );
    final String decoded = urldecoded.replace( ' ', '+' ); // url decoding could remove valid b64 characters
    final String sanitized = sanitize( decoded );
    final String normalized;
    int lastBlockLength = sanitized.length() % 4;
    if( lastBlockLength > 0 ) {
      normalized =
          sanitized + Strings.repeat( "=", 4 - lastBlockLength );
    } else {
      normalized = sanitized;
    }
    return B64.standard.encString(B64.standard.dec(normalized));
  }
}
