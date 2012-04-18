package com.eucalyptus.auth.login;

import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.Element;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.crypto.util.WSSecurity;

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
      // this enqueues an empty string
      //SecurityContext.enqueueSignature( sig.getTextFromTextChild( ) );
      String sigValue = new String(sig.getSignatureValue());
      SecurityContext.enqueueSignature( sigValue );
      
      final X509Certificate cert = WSSecurity.verifySignature( secNode, sig );
      try {
        final User user = Accounts.lookupUserByCertificate( cert );
        super.setCredential( cert );
        super.setPrincipal( user );
        //super.getGroups( ).addAll( Groups.lookupUserGroups( super.getPrincipal( ) ) );
      } catch ( AuthException e ) {
        try {
          if ( !Accounts.lookupCertificate( cert ).isActive( ) ) {
            throw new AuthException( "Certificate is inactive or revoked: " + e.getMessage( ), e );
          } else {
            throw e;
          }
        } catch ( Exception ex ) {
        	// TODO: GRZE should it be "throw ex" instead?
        	throw e;
        }
      }
    } finally {
      HoldMe.canHas.unlock( );
    }
    return true;
  }
  
}
