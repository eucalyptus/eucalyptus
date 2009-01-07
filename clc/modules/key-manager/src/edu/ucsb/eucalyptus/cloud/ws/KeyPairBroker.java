/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.keys.*;
import edu.ucsb.eucalyptus.msgs.*;
import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.encoders.Base64;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

public class KeyPairBroker {

  public static String RESOLVE = "vm://KeyPairResolve";
  private static Logger LOG = Logger.getLogger( KeyPairBroker.class );

  public VmKeyInfo populate( VmInfo vmInfo ) throws EucalyptusCloudException {
    if ( vmInfo.getKeyValue() != null || !"".equals( vmInfo.getKeyValue() ) ) {
      EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
      try {
        for ( SSHKeyPair kp : db.getUnique( new UserInfo( vmInfo.getOwnerId() ) ).getKeyPairs() )
          if ( kp.getPublicKey().equals( vmInfo.getKeyValue() ) )
            return new VmKeyInfo( kp.getName(), kp.getPublicKey(), kp.getFingerPrint() );
      } catch ( EucalyptusCloudException e ) {
        return new VmKeyInfo( "", "", "" );
      } finally {
        db.commit();
      }
    }
    return new VmKeyInfo( "", "", "" );
  }

  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try {
      user = db.getUnique( new UserInfo( vmAllocInfo.getRequest().getUserId() ) );
    }
    catch ( EucalyptusCloudException e ) {
      db.rollback();
      throw new EucalyptusInvalidRequestException( "Invalid user information", e );
    }
    //:: no keypair :://
    if ( SSHKeyPair.NO_KEY_NAME.equals( vmAllocInfo.getRequest().getKeyName() ) || vmAllocInfo.getRequest().getKeyName() == null ) {
      vmAllocInfo.setKeyInfo( new VmKeyInfo() );
      return vmAllocInfo;
    }
    //:: find keypair :://
    SSHKeyPair keypair = null;
    for ( SSHKeyPair k : user.getKeyPairs() )
      if ( k.getName().equals( vmAllocInfo.getRequest().getKeyName() ) )
        keypair = k;
    //:: failed to find keypair :://
    if ( keypair == null ) {
      db.rollback();
      throw new EucalyptusInvalidRequestException( "Failed to find keypair: " + vmAllocInfo.getRequest().getKeyName() );
    }
    //:: set keypair :://
    vmAllocInfo.setKeyInfo( new VmKeyInfo( keypair.getName(), keypair.getPublicKey(), keypair.getFingerPrint() ) );
    db.commit();
    return vmAllocInfo;
  }

  public CreateKeyPairResponseType CreateKeyPair( CreateKeyPairType request ) throws EucalyptusCloudException {
    if ( request.getKeyName() == null )
      throw new EucalyptusCloudException( "KeyPair generation error. Key name must be specified." );

    String userId = request.getUserId();
    String newKeyName = request.getKeyName();

    /** generate the key information **/
    String privKey;
    String fingerPrint;

    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try {
      user = db.getUnique( new UserInfo( request.getUserId() ) );
      if( user.getKeyPairs().contains( new SSHKeyPair( newKeyName ) ) )
        throw new EucalyptusCloudException( "KeyPair generation error. Key pair: " + newKeyName + " already exists." );

      //:: get the new key pair :://
      KeyTool keyTool = new KeyTool();
      KeyPair newKeys = keyTool.getKeyPair();

      //:: convert public key into an OpenSSH encoded public key :://
      RSAPublicKey publicKey = ( RSAPublicKey ) newKeys.getPublic();
      byte[] keyType = "ssh-rsa".getBytes(  );
      byte[] expBlob = publicKey.getPublicExponent().toByteArray();
      byte[] modBlob = publicKey.getModulus().toByteArray();
      byte[] authKeyBlob = new byte[ 3*4 + keyType.length + expBlob.length + modBlob.length ];

      byte[] lenArray = null;
      lenArray = BigInteger.valueOf( keyType.length ).toByteArray();
      System.arraycopy( lenArray , 0, authKeyBlob, 4 - lenArray.length, lenArray.length );
      System.arraycopy( keyType, 0, authKeyBlob, 4, keyType.length );

      lenArray = BigInteger.valueOf( expBlob.length ).toByteArray();
      System.arraycopy( lenArray , 0, authKeyBlob, 4 + keyType.length + 4 - lenArray.length, lenArray.length );
      System.arraycopy( expBlob, 0, authKeyBlob, 4 + ( 4 + keyType.length ), expBlob.length );

      lenArray = BigInteger.valueOf( modBlob.length ).toByteArray();
      System.arraycopy( lenArray , 0, authKeyBlob, 4 + expBlob.length + 4 + keyType.length + 4 - lenArray.length, lenArray.length );
      System.arraycopy( modBlob, 0, authKeyBlob, 4 + ( 4 + expBlob.length + ( 4 + keyType.length ) ), modBlob.length );

      String authKeyString = String.format( "%s %s %s@eucalyptus", new String( keyType ), new String( Base64.encode( authKeyBlob ) ), request.getUserId() );

      //:: convert the private key into a PEM encoded string :://
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      PEMWriter privOut = new PEMWriter( new OutputStreamWriter( byteOut ) );
      try {
        privOut.writeObject( newKeys.getPrivate() );
        privOut.close();
      } catch ( IOException e ) {
        LOG.error( e );
        throw new EucalyptusCloudException( e );
      }
      privKey = byteOut.toString();

      //:: get the fingerprint for the private key :://
      fingerPrint = Hashes.getFingerPrint( newKeys.getPrivate() );

      LOG.info( "Generated new key pair for entities: " + userId + " keypair name=" + newKeyName );
      user.getKeyPairs().add( new SSHKeyPair( newKeyName, fingerPrint, authKeyString ) );
      db.commit();
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e );
      LOG.debug( e, e );
      db.rollback();
      throw e;
    }

    CreateKeyPairResponseType reply = (CreateKeyPairResponseType) request.getReply();
    reply.setKeyFingerprint( fingerPrint );
    reply.setKeyMaterial( privKey );
    reply.setKeyName( newKeyName );
    return reply;
  }

  public DeleteKeyPairResponseType DeleteKeyPair( DeleteKeyPairType request ) throws EucalyptusCloudException {
    boolean foundKey = false;

    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    try {
      UserInfo user = db.getUnique( new UserInfo( request.getUserId() ) );
      foundKey = user.getKeyPairs().remove( new SSHKeyPair( request.getKeyName() ) );
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e );
    } finally {
      db.commit();
    }

    DeleteKeyPairResponseType reply = (DeleteKeyPairResponseType) request.getReply();
    reply.set_return( foundKey );
    return reply;
  }

  public DescribeKeyPairsResponseType DescribeKeyPairs( DescribeKeyPairsType request ) throws AxisFault {

    DescribeKeyPairsResponseType reply = (DescribeKeyPairsResponseType) request.getReply();
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    try {
      UserInfo user = db.getUnique( new UserInfo( request.getUserId() ) );
      for( SSHKeyPair kp : user.getKeyPairs() ) {
        if( request.getKeySet().isEmpty() || request.getKeySet().contains( kp.getName() ) ) {
          reply.getKeySet().add( new DescribeKeyPairsResponseItemType( kp.getName(), kp.getFingerPrint() ) );
        }
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e );
    } finally {
      db.commit();
    }
    return reply;
  }
}
