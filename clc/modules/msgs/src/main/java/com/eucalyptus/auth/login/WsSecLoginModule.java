package com.eucalyptus.auth.login;

import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.Element;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.WSSecurity;
import com.eucalyptus.binding.HoldMe;

public class WsSecLoginModule extends BaseLoginModule<WsSecCredentials> {
  private static Logger LOG = Logger.getLogger( WsSecLoginModule.class );
  
  public WsSecLoginModule( ) {}
  
  @Override
  public boolean accepts( ) {
    return super.getCallbackHandler( ) instanceof WsSecCredentials;
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public boolean authenticate( final WsSecCredentials wrappedCredentials ) throws Exception {
    HoldMe.canHas.lock( );
    try {
      final Element secNode = WSSecurity.getSecurityElement( wrappedCredentials.getLoginData( ) );
      final XMLSignature sig = WSSecurity.getXMLSignature( secNode );
      SecurityContext.enqueueSignature( sig.getTextFromTextChild( ) );
      final X509Certificate cert = WSSecurity.verifySignature( secNode, sig );
      try {
        final User user = Users.lookupCertificate( cert );
        super.setCredential( cert );
        super.setPrincipal( user );
        super.getGroups( ).addAll( Groups.lookupUserGroups( super.getPrincipal( ) ) );
      } catch ( NoSuchUserException e ) {
        try {
          if ( Users.getUserProvider( ).checkRevokedCertificate( cert ) ) {
            throw new NoSuchUserException( "Certificate has been revoked: " + e.getMessage( ), e );
          } else {
            throw e;
          }
        } catch ( Exception ex ) {
          throw e;
        }
      }
    } finally {
      HoldMe.canHas.unlock( );
    }
    return true;
  }
  
}
