package com.eucalyptus.keys;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
    boolean showAll = request.getKeySet( ).remove( "verbose" );
    for ( SshKeyPair kp : Iterables.filter( KeyPairs.list( ( ctx.hasAdministrativePrivileges( ) &&  showAll ) ? null : Contexts.lookup( ).getUserFullName( ).asAccountFullName( ) ), RestrictedTypes.filterPrivileged( ) ) ) {
      if ( request.getKeySet( ).isEmpty( ) || request.getKeySet( ).contains( kp.getDisplayName( ) ) ) {
        reply.getKeySet( ).add( new DescribeKeyPairsResponseItemType( kp.getDisplayName( ), kp.getFingerPrint( ) ) );
      }
    }
    return reply;
  }
  
  public DeleteKeyPairResponseType delete( DeleteKeyPairType request ) throws EucalyptusCloudException {
    DeleteKeyPairResponseType reply = ( DeleteKeyPairResponseType ) request.getReply( );
    Context ctx = Contexts.lookup( );
    try {
      SshKeyPair key = KeyPairs.lookup( ctx.getUserFullName( ).asAccountFullName( ), request.getKeyName( ) );
      if ( !RestrictedTypes.filterPrivileged( ).apply( key ) ) {
        throw new EucalyptusCloudException( "Permission denied while trying to delete keypair " + key.getName( ) + " by " + ctx.getUser( ) );
      }
      KeyPairs.delete( ctx.getUserFullName( ).asAccountFullName( ), request.getKeyName( ) );
      reply.set_return( true );
    } catch ( Exception e1 ) {
      LOG.error( e1 );
      reply.set_return( false );
    }
    return reply;
  }
  
  public CreateKeyPairResponseType create( final CreateKeyPairType request ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    final CreateKeyPairResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    Supplier<SshKeyPair> allocator = new Supplier<SshKeyPair>( ) {
      
      @Override
      public SshKeyPair get( ) {
        try {
          PrivateKey pk = KeyPairs.create( ctx.getUserFullName( ), request.getKeyName( ) );
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
          return KeyPairs.lookup( ctx.getUserFullName( ), request.getKeyName( ) );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    RestrictedTypes.allocateUnitlessResource( allocator );
    return reply;
  }
  
  public ImportKeyPairResponseType importKeyPair( final ImportKeyPairType request ) throws AuthException {
    final ImportKeyPairResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      KeyPairs.lookup( ctx.getUserFullName( ), request.getKeyName( ) );
    } catch ( Exception e1 ) {
      Supplier<SshKeyPair> allocator = new Supplier<SshKeyPair>() {

        @Override
        public SshKeyPair get( ) {
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
          String fingerprint = String.format( "%032X", new BigInteger( digest ) );
          newKey.setFingerPrint( fingerprint );
          reply.setKeyName( request.getKeyName( ) );
          return newKey;
        }};
      RestrictedTypes.allocateUnitlessResource( allocator );
    }
    return reply;
  }
}
