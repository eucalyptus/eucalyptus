package com.eucalyptus.keys;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.PrivateKey;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.crypto.Certs;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.SshKeyPair;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.msgs.CreateKeyPairResponseType;
import edu.ucsb.eucalyptus.msgs.CreateKeyPairType;
import edu.ucsb.eucalyptus.msgs.DeleteKeyPairResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteKeyPairType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsType;
import edu.ucsb.eucalyptus.msgs.ImportKeyPairResponseType;
import edu.ucsb.eucalyptus.msgs.ImportKeyPairType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class KeyPairManager {
  private static Logger LOG = Logger.getLogger( KeyPairManager.class );

  public VmKeyInfo resolve( VmInfo vmInfo ) throws EucalyptusCloudException {
    SshKeyPair kp = null;
    if ( vmInfo.getKeyValue() != null || !"".equals( vmInfo.getKeyValue() ) ) {
      try {
        kp = KeyPairUtil.getUserKeyPairByValue( vmInfo.getOwnerId( ), vmInfo.getKeyValue( ) );
      } catch ( Exception e ) {
        kp = SshKeyPair.NO_KEY;
      }
    } else {
      kp = SshKeyPair.NO_KEY;
    }
    return new VmKeyInfo( kp.getDisplayName(), kp.getPublicKey(), kp.getFingerPrint() );
  }

  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    if ( SshKeyPair.NO_KEY_NAME.equals( vmAllocInfo.getRequest().getKeyName() ) || vmAllocInfo.getRequest().getKeyName() == null ) {
//ASAP:FIXME:GRZE
      if( "windows".equals( vmAllocInfo.getPlatform( ) ) ) {
        throw new EucalyptusCloudException( "You must specify a keypair when running a windows vm: " + vmAllocInfo.getRequest().getImageId() );
      } else {
        vmAllocInfo.setKeyInfo( new VmKeyInfo() );
        return vmAllocInfo;
      }
    }
    RunInstancesType request = vmAllocInfo.getRequest( );
    String action = PolicySpec.requestToAction( request );
    String keyName = request.getKeyName( );
    Account account = Permissions.getAccountByUserId( request.getUserErn( ).getUniqueId( ) );
    SshKeyPair keypair = KeyPairUtil.getUserKeyPair( request.getUserErn( ).getUniqueId( ), keyName );
    if ( keypair == null ) {
      throw new EucalyptusCloudException( "Failed to find keypair: " + keyName );
    }
    if ( !Permissions.isAuthorized( PolicySpec.EC2_RESOURCE_KEYPAIR, keyName, account, action, request.getUser( ) ) ) {
      throw new EucalyptusCloudException( "Not authorized to use keypair " + keyName + " by " + request.getUser( ).getName( ) );
    }
    vmAllocInfo.setKeyInfo( new VmKeyInfo( keypair.getDisplayName( ), keypair.getPublicKey(), keypair.getFingerPrint() ) );
    return vmAllocInfo;
  }

  
  public DescribeKeyPairsResponseType describe( DescribeKeyPairsType request ) throws Exception {
    DescribeKeyPairsResponseType reply = ( DescribeKeyPairsResponseType ) request.getReply( );
    for ( SshKeyPair kp : KeyPairUtil.getUserKeyPairs( request.getUserErn( ).getUniqueId( ) ) ) {
      if ( request.getKeySet( ).isEmpty( ) || request.getKeySet( ).contains( kp.getDisplayName( ) ) ) {
        reply.getKeySet( ).add( new DescribeKeyPairsResponseItemType( kp.getDisplayName( ), kp.getFingerPrint( ) ) );
      }
    }
    return reply;
  }

  public DeleteKeyPairResponseType delete( DeleteKeyPairType request ) throws EucalyptusCloudException {
    DeleteKeyPairResponseType reply = ( DeleteKeyPairResponseType ) request.getReply( );
    try {
      SshKeyPair key = KeyPairUtil.deleteUserKeyPair( request.getUserErn( ).getUniqueId( ), request.getKeyName( ) );
      reply.set_return( true );
    } catch ( Exception e1 ) {
      reply.set_return( true );
    }
    return reply;
  }

  public CreateKeyPairResponseType create( CreateKeyPairType request ) throws EucalyptusCloudException {
    CreateKeyPairResponseType reply = ( CreateKeyPairResponseType ) request.getReply( );
    try {
      KeyPairUtil.getUserKeyPair( request.getUserErn( ).getUniqueId( ), request.getKeyName( ) );
    } catch ( Exception e1 ) {
      PrivateKey pk = KeyPairUtil.createUserKeyPair( request.getUserErn( ).getUniqueId( ), request.getKeyName( ) );
      reply.setKeyFingerprint( Certs.getFingerPrint( pk ) );
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      PEMWriter privOut = new PEMWriter( new OutputStreamWriter( byteOut ) );
      try {
        privOut.writeObject( pk );
        privOut.close();
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

  public ImportKeyPairResponseType importKeyPair(ImportKeyPairType request) {
    ImportKeyPairResponseType reply = request.getReply( );
    return reply;
  }
}
