/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.SSHKeyPair;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.msgs.*;

import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.util.KeyTool;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;

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

  public DescribeKeyPairsResponseType DescribeKeyPairs( DescribeKeyPairsType request ) throws Exception {

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
