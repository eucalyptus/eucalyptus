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

package com.eucalyptus.auth.login;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.crypto.Hmac;

public class Hmacv1LoginModule extends HmacLoginModuleSupport {
  private static Logger LOG = Logger.getLogger( Hmacv1LoginModule.class );

  public Hmacv1LoginModule() {
    super(1);
  }

  @Override
  public boolean authenticate( HmacCredentials credentials ) throws Exception {
    String sig = credentials.getSignature( );
    checkForReplay( sig );
    AccessKey accessKey = lookupAccessKey(credentials);
    User user = accessKey.getUser( );
    String secretKey = accessKey.getSecretKey( );

    String canonicalString = this.makeSubjectString( credentials.getParameters( ) );
    String computedSig = this.getSignature( secretKey, canonicalString, credentials.getSignatureMethod( ) );
    String decodedSig = sanitize( urldecode( sig ) );
    if ( !computedSig.equals( sanitize(sig) ) && !computedSig.equals( decodedSig ) && !computedSig.equals( sig ) ) {
      return false;
    }
    super.setCredential( credentials.getQueryId( ) );
    super.setPrincipal( user );
    //super.getGroups( ).addAll( Groups.lookupUserGroups( super.getPrincipal( ) ) );
    return true;
  }

  private String makeSubjectString( final Map<String, String> parameters ) throws UnsupportedEncodingException {
    String paramString = "";
    Set<String> sortedKeys = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
    sortedKeys.addAll( parameters.keySet( ) );
    for ( String key : sortedKeys )
      paramString = paramString.concat( key ).concat( parameters.get( key ).replaceAll( "\\+", " " ) );
    try {
      return new String(URLCodec.decodeUrl( paramString.getBytes() ) );
    } catch ( DecoderException e ) {
      return paramString;
    }
  }

  public String getSignature( final String queryKey, final String subject, final Hmac mac ) throws AuthenticationException {
    SecretKeySpec signingKey = new SecretKeySpec( queryKey.getBytes( ), mac.toString( ) );
    try {
      Mac digest = mac.getInstance( );
      digest.init( signingKey );
      byte[] rawHmac = digest.doFinal( subject.getBytes( ) );
      return sanitize( Base64.encode( rawHmac ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new AuthenticationException( "Failed to compute signature" );
    }
  }

}
