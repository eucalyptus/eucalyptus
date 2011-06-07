package com.eucalyptus.keys;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.PrivateKey;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.CreateKeyPairResponseType;
import edu.ucsb.eucalyptus.msgs.CreateKeyPairType;
import edu.ucsb.eucalyptus.msgs.DeleteKeyPairResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteKeyPairType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsType;
import edu.ucsb.eucalyptus.msgs.ImportKeyPairResponseType;
import edu.ucsb.eucalyptus.msgs.ImportKeyPairType;

public class KeyPairManager {
  private static Logger LOG = Logger.getLogger( KeyPairManager.class );
  
  public DescribeKeyPairsResponseType describe( DescribeKeyPairsType request ) throws Exception {
    DescribeKeyPairsResponseType reply = request.getReply( );
    Context ctx = Contexts.lookup( );
    for ( SshKeyPair kp : KeyPairUtil.getUserKeyPairs( ctx.getUserFullName( ) ) ) {
      if ( request.getKeySet( ).isEmpty( ) || request.getKeySet( ).contains( kp.getDisplayName( ) ) ) {
        if ( Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_KEYPAIR, kp.getDisplayName( ), ctx.getAccount( ),
                                       PolicySpec.requestToAction( request ), ctx.getUser( ) ) ) {
          reply.getKeySet( ).add( new DescribeKeyPairsResponseItemType( kp.getDisplayName( ), kp.getFingerPrint( ) ) );
        }
      }
    }
    return reply;
  }
  
  public DeleteKeyPairResponseType delete( DeleteKeyPairType request ) throws EucalyptusCloudException {
    DeleteKeyPairResponseType reply = ( DeleteKeyPairResponseType ) request.getReply( );
    Context ctx = Contexts.lookup( );
    try {
      SshKeyPair key = KeyPairUtil.deleteUserKeyPair( ctx.getUserFullName( ), request.getKeyName( ) );
      Account keyAccount = null;
      try {
        keyAccount = Accounts.lookupAccountById( key.getOwnerAccountId( ) );
      } catch ( AuthException e ) {
        throw new EucalyptusCloudException( e );
      }
      if ( !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_KEYPAIR, request.getKeyName( ), keyAccount,
                                      PolicySpec.requestToAction( request ), ctx.getUser( ) ) ) {
        throw new EucalyptusCloudException( "Permission denied while trying to delete keypair " + key.getName( ) + " by " + ctx.getUser( ) );
      }
      reply.set_return( true );
    } catch ( Exception e1 ) {
      reply.set_return( true );
    }
    return reply;
  }
  
  public CreateKeyPairResponseType create( CreateKeyPairType request ) throws EucalyptusCloudException {
    CreateKeyPairResponseType reply = request.getReply( );
    Context ctx = Contexts.lookup( );
    String action = PolicySpec.requestToAction( request );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      if ( !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_KEYPAIR, "", ctx.getAccount( ), action, ctx.getUser( ) ) ) {
        throw new EucalyptusCloudException( "Permission denied while trying to create keypair by " + ctx.getUser( ) );
      }
      if ( !Permissions.canAllocate( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_KEYPAIR, "", action, ctx.getUser( ), 1L ) ) {
        throw new EucalyptusCloudException( "Quota exceeded while trying to create keypair by " + ctx.getUser( ) );
      }
    }
    try {
      KeyPairUtil.getUserKeyPair( ctx.getUserFullName( ), request.getKeyName( ) );
    } catch ( Exception e1 ) {
      PrivateKey pk = KeyPairUtil.createUserKeyPair( ctx.getUserFullName( ), request.getKeyName( ) );
      reply.setKeyFingerprint( Certs.getFingerPrint( pk ) );
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
      PEMWriter privOut = new PEMWriter( new OutputStreamWriter( byteOut ) );
      try {
        privOut.writeObject( pk );
        privOut.close( );
      } catch ( IOException e ) {
        LOG.error( e );
        throw new EucalyptusCloudException( e );
      }
      reply.setKeyName( request.getKeyName( ) );
      reply.setKeyMaterial( byteOut.toString( ) );
      return reply;
    }
    throw new EucalyptusCloudException( "Creation failed.  Keypair already exists: " + request.getKeyName( ) );
  }
  
  public ImportKeyPairResponseType importKeyPair( ImportKeyPairType request ) throws AuthException {
    ImportKeyPairResponseType reply = request.getReply( );
    Context ctx = Contexts.lookup( );
    String action = PolicySpec.requestToAction( request );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      if ( !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_KEYPAIR, "", ctx.getAccount( ), action, ctx.getUser( ) ) ) {
        throw new AuthException( "Permission denied while trying to create keypair by " + ctx.getUser( ) );
      }
      if ( !Permissions.canAllocate( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_KEYPAIR, "", action, ctx.getUser( ), 1L ) ) {
        throw new AuthException( "Quota exceeded while trying to create keypair by " + ctx.getUser( ) );
      }
    }
    try {
      KeyPairUtil.getUserKeyPair( ctx.getUserFullName( ), request.getKeyName( ) );
    } catch ( Exception e1 ) {
      SshKeyPair newKey = new SshKeyPair( ctx.getUserFullName( ), request.getKeyName( ) );
      newKey.setPublicKey( request.getPublicKeyMaterial( ) );
      /**
       * TODO:GRZE:OMGFIXME:RELEASE
       * Supported formats:
       * OpenSSH public key format (e.g., the format in ~/.ssh/authorized_keys)
       * Base64 encoded DER format
       * SSH public key file format as specified in RFC4716
       * 
       * DSA keys are not supported. Make sure your key generator is set up to create RSA keys.
       * Supported lengths: 1024, 2048, and 4096.
       */
      //TODO:GRZE:replace bogus initial impl.
      byte[] digest = Digest.MD5.get( ).digest( request.getPublicKeyMaterial( ).getBytes( ) );
      String fingerprint = String.format("%032X",new BigInteger( digest ) );
      newKey.setFingerPrint( fingerprint );
      reply.setKeyName( request.getKeyName( ) );
    }
    return reply;
  }
}
