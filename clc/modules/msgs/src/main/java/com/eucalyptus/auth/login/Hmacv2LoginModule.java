package com.eucalyptus.auth.login;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.crypto.Hmac;
import com.eucalyptus.auth.principal.User;

public class Hmacv2LoginModule extends BaseLoginModule<HmacCredentials> {
  private static Logger LOG = Logger.getLogger( Hmacv2LoginModule.class );
  public Hmacv2LoginModule() {}
  
  @Override
  public boolean accepts( ) {
    return super.getCallbackHandler( ) instanceof HmacCredentials && ((HmacCredentials)super.getCallbackHandler( )).getSignatureVersion( ).equals( 2 );
  }

  @Override
  public boolean authenticate( HmacCredentials credentials ) throws Exception {
    String sig = credentials.getSignature( );
    SecurityContext.enqueueSignature( sig );
    User user = Users.lookupQueryId( credentials.getQueryId( ) );
    String secretKey = user.getSecretKey( );
    String canonicalString = this.makeSubjectString( credentials.getVerb( ), credentials.getHeaderHost( ), credentials.getServicePath( ), credentials.getParameters( ) );
    String canonicalStringWithPort = this.makeSubjectString( credentials.getVerb( ), credentials.getHeaderHost( ) + ":" + credentials.getHeaderPort( ), credentials.getServicePath( ), credentials.getParameters( ) );
    String computedSig = this.getSignature( secretKey, canonicalString, credentials.getSignatureMethod( ) );
    String computedSigWithPort = this.getSignature( secretKey, canonicalStringWithPort, credentials.getSignatureMethod( ) );
    if ( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) ) {
      sig = URLDecoder.decode( sig ).replaceAll("=","");
      computedSig = this.getSignature( secretKey, canonicalString.replaceAll("\\+","%2B"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
      computedSigWithPort = this.getSignature( secretKey, canonicalStringWithPort.replaceAll("\\+","%2B"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
      if( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) ) {
        computedSig = this.getSignature( secretKey, canonicalString.replaceAll("\\+","%20"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
        computedSigWithPort = this.getSignature( secretKey, canonicalStringWithPort.replaceAll("\\+","%20"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
        if( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) ) {
          return false;
        }
      }
    }
    super.setCredential( credentials.getQueryId( ) );
    super.setPrincipal( user );
    super.getGroups( ).addAll( Groups.lookupUserGroups( super.getPrincipal( ) ) );
    return true;
  }

  @Override
  public void reset( ) {}

  private String makeSubjectString( String httpMethod, String host, String path, final Map<String, String> parameters ) throws UnsupportedEncodingException {
    URLCodec codec = new URLCodec();
    parameters.remove("");
    StringBuilder sb = new StringBuilder( );
    sb.append( httpMethod );
    sb.append( "\n" );
    sb.append( host );
    sb.append( "\n" );
    sb.append( path );
    sb.append( "\n" );
    String prefix = sb.toString( );
    sb = new StringBuilder( );
    NavigableSet<String> sortedKeys = new TreeSet<String>( );
    sortedKeys.addAll( parameters.keySet( ) );
    String firstKey = sortedKeys.pollFirst( );
    if( firstKey != null ) { 
      sb.append( codec.encode( firstKey ,"UTF-8" ) ).append( "=" ).append( codec.encode( parameters.get( firstKey ).replaceAll( "\\+", " " ), "UTF-8" ) );
    } 
    while ( ( firstKey = sortedKeys.pollFirst( ) ) != null ) {
      sb.append( "&" ).append( codec.encode( firstKey, "UTF-8" ) ).append( "=" ).append( codec.encode( parameters.get( firstKey ).replaceAll( "\\+", " " ), "UTF-8" ) );
    }
    String subject = prefix + sb.toString( );
    LOG.trace( "VERSION2: " + subject );
    return subject;
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
