package com.eucalyptus.auth.login;

import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.PEMFiles;
import com.eucalyptus.bootstrap.Component;

public class WalrusInternalLoginModule extends BaseLoginModule<WalrusInternalWrappedCredentials> {
  private static Logger LOG = Logger.getLogger( WalrusInternalLoginModule.class );
  @Override
  public boolean accepts( ) {
    return super.getCallbackHandler( ) instanceof WalrusInternalWrappedCredentials;
  }

  @Override
  public boolean authenticate( WalrusInternalWrappedCredentials credentials ) throws Exception {
    Signature sig;
    boolean valid = false;
    try {
      PublicKey publicKey = SystemCredentialProvider.getCredentialProvider(Component.storage).getCertificate().getPublicKey();
      sig = Signature.getInstance("SHA1withRSA");
      sig.initVerify(publicKey);
      sig.update(credentials.getLoginData( ).getBytes());
      valid = sig.verify(Base64.decode(credentials.getSignature( )));
    } catch ( Exception e ) {
      LOG.warn ("Authentication: certificate not found in keystore");
    } finally {
      if( !valid && credentials.getCertString( ) != null ) {
        try {
          X509Certificate nodeCert = PEMFiles.getCert( Base64.decode( credentials.getCertString( ) ) );
          PublicKey publicKey = nodeCert.getPublicKey( );
          sig = Signature.getInstance( "SHA1withRSA" );
          sig.initVerify( publicKey );
          sig.update( credentials.getLoginData( ).getBytes( ) );
          valid = sig.verify( Base64.decode( credentials.getSignature( ) ) );
        } catch ( Exception e2 ) {
          LOG.warn ("Authentication exception: " + e2.getMessage());
        }            
      }
    }
    if(!valid) {
      throw new AuthenticationException( "User authentication failed." );
    }
    User user = Users.lookupUser( "admin" );
    super.setCredential( credentials.getSignature( ) );
    super.setPrincipal( user );
    super.getGroups( ).addAll( Groups.lookupGroups( super.getPrincipal( ) ) );
    return true;
  }

}
