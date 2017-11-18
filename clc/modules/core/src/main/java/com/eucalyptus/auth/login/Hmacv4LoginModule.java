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
package com.eucalyptus.auth.login;

import static com.eucalyptus.ws.util.HmacUtils.headerLookup;
import static com.eucalyptus.ws.util.HmacUtils.parameterLookup;
import static com.eucalyptus.ws.util.HmacUtils.SignatureCredential;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AccessKeys;
import com.eucalyptus.auth.InvalidSignatureAuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.crypto.util.SecurityHeader;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.ws.util.HmacUtils;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.io.BaseEncoding;

public class Hmacv4LoginModule extends HmacLoginModuleSupport {
  private static final Logger LOG = Logger.getLogger( Hmacv4LoginModule.class );
  private static final String V4_TERMINATOR = "aws4_request";

  public Hmacv4LoginModule() {
    super(4);
  }

  @Override
  public boolean authenticate( final HmacCredentials credentials ) throws Exception {
    if ( credentials.getSignatureMethod() != Hmac.HmacSHA256 ) {
      throw new AuthenticationException( "Invalid signature method for v4: " + credentials.getSignatureMethod() );
    }

    final String sig = credentials.getSignature( );
    final Function<String,List<String>> headerLookup = headerLookup( credentials.getHeaders() );
    final Function<String,List<String>> parameterLookup = parameterLookup( credentials.getParameters() );
    final Map<String,String> authorizationParameters = credentials.getVariant().getAuthorizationParameters( headerLookup, parameterLookup );
    final SignatureCredential signatureCredential = new SignatureCredential( authorizationParameters.get("Credential") );
    final AccessKey accessKey = lookupAccessKey( credentials );
    final Date date = HmacUtils.getSignatureDate( EnumSet.of(HmacUtils.SignatureVersion.SignatureV4), headerLookup, parameterLookup );
    signatureCredential.verify( date, null, null, V4_TERMINATOR ); //TODO Do we want to validate region and service name?
    final UserPrincipal user = accessKey.getPrincipal( );
    final String secretKey = accessKey.getSecretKey( );
    final byte[] signatureKey = getSignatureKey( secretKey, signatureCredential );
    final CharSequence canonicalString = this.makeSubjectString( credentials, signatureCredential, authorizationParameters, date, false );
    final byte[] computedSig = this.getHmacSHA256( signatureKey, canonicalString );
    final byte[] providedSig = BaseEncoding.base16( ).lowerCase( ).decode( sig );
    if ( !MessageDigest.isEqual( computedSig, providedSig ) ) {
      final CharSequence canonicalStringNoPath = this.makeSubjectString( credentials, signatureCredential, authorizationParameters, date, true );
      final byte[] computedSigNoPath = this.getHmacSHA256( signatureKey, canonicalStringNoPath );
      if( !MessageDigest.isEqual( computedSigNoPath, providedSig ) ) {
        throw new InvalidSignatureAuthException( "Signature validation failed" );
      }
    }
    super.setCredential( credentials.getCredential( AccessKeys.getKeyType( accessKey ) ) );
    super.setPrincipal( user );

    return true;
  }

  private CharSequence makeSubjectString( @Nonnull final HmacCredentials credentials,
                                          @Nonnull final SignatureCredential signatureCredential,
                                          @Nonnull final Map<String,String> authorizationParameters,
                                          @Nonnull final Date date,
                                          final boolean skipPath ) throws Exception {
    final String timestamp = Timestamps.formatShortIso8601Timestamp( date );
    final StringBuilder sb = new StringBuilder( 256 );
    sb.append( SecurityHeader.Value.AWS4_HMAC_SHA256.value() ).append( '\n' );
    sb.append( timestamp ).append( '\n' );
    sb.append( signatureCredential.getCredentialScope() ).append( '\n' );
    sb.append( digestUTF8( makeCanonicalRequest( credentials, authorizationParameters, skipPath ) ) );
    if ( signatureLogger.isTraceEnabled( ) ) signatureLogger.trace( "VERSION4: " + sb.toString( ) );
    return sb;
  }

  private CharSequence makeCanonicalRequest( @Nonnull final HmacCredentials credentials,
                                             @Nonnull final Map<String,String> authorizationParameters,
                                             final boolean skipPath ) throws Exception {
    final StringBuilder sb = new StringBuilder( 512 );
    sb.append( credentials.getVerb( ) );
    sb.append( '\n' );
    sb.append( skipPath ? "/" : canonicalizePath( credentials.getServicePath( ) ) ); // AWS Java SDK always uses "/"
    sb.append( '\n' );
    boolean addedParam = false;
    for ( final String parameter : Ordering.natural( ).sortedCopy( credentials.getParameters().keySet() ) ) {
      if ( credentials.getVariant() == HmacUtils.SignatureVariant.SignatureV4Query && SecurityParameter.X_Amz_Signature.parameter().equals( parameter ) ) {
        continue;
      }
      for ( final String value : Ordering.natural().sortedCopy( credentials.getParameters().get( parameter ) ) ) {
        sb.append( urlencode(parameter) );
        sb.append( '=' );
        sb.append( urlencode(value) );
        sb.append( '&' );
        addedParam = true;
      }
    }
    if ( addedParam ) sb.setLength( sb.length()-1 );
    sb.append( '\n' );
    for ( final String header : authorizationParameters.get("SignedHeaders").split(";") ) {
      final List<String> values = Lists.transform( credentials.getHeaders().get( header ), new Function<String, String>() {
        @Override
        public String apply( final String text ) {
          return text.trim();
        }
      } );
      sb.append( header );
      sb.append( ':' );
      sb.append( Joiner.on( ',' ).join( Ordering.<String>natural().sortedCopy( values ) ) );
      sb.append( '\n' );
    }
    sb.append( '\n' );
    sb.append( authorizationParameters.get("SignedHeaders") );
    sb.append( '\n' );
    sb.append( digestUTF8( credentials.getBody() ) );
    if ( signatureLogger.isTraceEnabled( ) ) signatureLogger.trace( "VERSION4: " + sb.toString( ) );
    return sb;
  }

  /**
   * Returns a hex encoded SHA256 hash of the {@code text}.
   */
  public static String digestUTF8( final CharSequence text ) {
    final ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode( CharBuffer.wrap( text ) );
    return BaseEncoding.base16( ).lowerCase( ).encode( Digest.SHA256.digestBinary( byteBuffer ) );
  }
  
  public static String canonicalizePath( final String servicePath ) throws URISyntaxException {
    return servicePath.isEmpty() ? "/" : new URI("http", "0.0.0.0", servicePath, null).normalize().getPath(); //TODO encode path here when it becomes necessary
  }

  public static byte[] getHmacSHA256( final byte[] signatureKey,
                                final CharSequence data ) throws AuthenticationException {
    final SecretKeySpec signingKey = new SecretKeySpec( signatureKey, Hmac.HmacSHA256.toString( ) );
    try {
      final ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode( CharBuffer.wrap( data ) );
      return Hmac.HmacSHA256.digestBinary( signingKey, byteBuffer );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new AuthenticationException( "Failed to compute signature" );
    }
  }

  public static byte[] getSignatureKey( final String key,
                                  final SignatureCredential credential ) throws Exception {
    return getHmacSHA256(
        getHmacSHA256(
            getHmacSHA256(
                getHmacSHA256( ("AWS4" + key).getBytes( StandardCharsets.UTF_8 ), credential.getDate() ),
                credential.getRegion() ),
            credential.getServiceName() ),
        credential.getTerminator() );
  }
}
