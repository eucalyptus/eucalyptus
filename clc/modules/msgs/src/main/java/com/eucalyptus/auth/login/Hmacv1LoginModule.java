package com.eucalyptus.auth.login;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.crypto.Hmac;
import com.eucalyptus.auth.principal.User;

public class Hmacv1LoginModule extends BaseLoginModule<HmacCredentials> {
  private static Logger LOG = Logger.getLogger( Hmacv1LoginModule.class );
  public Hmacv1LoginModule() {}
  
  @Override
  public boolean accepts( ) {
    return super.getCallbackHandler( ) instanceof HmacCredentials && ((HmacCredentials)super.getCallbackHandler( )).getSignatureVersion( ).equals( 1 );
  }

  @Override
  public boolean authenticate( HmacCredentials credentials ) throws Exception {
    String sig = credentials.getSignature( );
    SecurityContext.enqueueSignature( sig );
    User user = Users.lookupQueryId( credentials.getQueryId( ) );
    String secretKey = user.getSecretKey( );

    String canonicalString = this.makeSubjectString( credentials.getParameters( ) );
    String computedSig = this.getSignature( secretKey, canonicalString, credentials.getSignatureMethod( ) );
    String decodedSig = URLDecoder.decode( sig ).replaceAll( "=", "" );
    if ( !computedSig.equals( sig.replaceAll( "=", "" ) ) && !computedSig.equals( decodedSig ) && !computedSig.equals( sig ) ) {
      return false;
    }
    super.setCredential( credentials.getQueryId( ) );
    super.setPrincipal( user );
    super.getGroups( ).addAll( Groups.lookupUserGroups( super.getPrincipal( ) ) );
    return true;
  }

  @Override
  public void reset( ) {}

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
      return Base64.encode( rawHmac ).replaceAll( "=", "" );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new AuthenticationException( "Failed to compute signature" );
    }
  }

}
