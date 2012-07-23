/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.util;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;

public class BlockStorageUtil {
	private static Logger LOG = Logger.getLogger(BlockStorageUtil.class);

	public static String encryptNodeTargetPassword(String password) throws EucalyptusCloudException {
    try {
      List<ServiceConfiguration> clusterList = ServiceConfigurations.listPartition( ClusterController.class, StorageProperties.NAME );
      if( clusterList.size() < 1 ) {
        String msg = "Failed to find a cluster with the corresponding partition name for this SC: " + StorageProperties.NAME + "\nFound: " + clusterList.toString( ).replaceAll( ", ", ",\n" );
        throw new EucalyptusCloudException(msg);
      } else {
        ServiceConfiguration clusterConfig = clusterList.get( 0 );
        PublicKey ncPublicKey = Partitions.lookup( clusterConfig ).getNodeCertificate( ).getPublicKey();
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey);
        return new String(Base64.encode(cipher.doFinal(password.getBytes())));
      }
    } catch ( Exception e ) {
			LOG.error( "Unable to encrypt storage target password: " + e.getMessage( ), e );
			throw new EucalyptusCloudException("Unable to encrypt storage target password: " + e.getMessage(), e);
		}
	}

	public static String encryptSCTargetPassword(String password) throws EucalyptusCloudException {
		PublicKey scPublicKey = SystemCredentials.lookup(Storage.class).getKeyPair().getPublic();
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, scPublicKey);
			return new String(Base64.encode(cipher.doFinal(password.getBytes())));	      
		} catch (Exception e) {
			LOG.error("Unable to encrypted storage target password");
			throw new EucalyptusCloudException(e.getMessage(), e);
		}
	}

	public static String decryptSCTargetPassword(String encryptedPassword) throws EucalyptusCloudException {
		PrivateKey scPrivateKey = SystemCredentials.lookup(Storage.class).getPrivateKey();
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, scPrivateKey);
			return new String(cipher.doFinal(Base64.decode(encryptedPassword)));
		} catch(Exception ex) {
			LOG.error(ex);
			throw new EucalyptusCloudException("Unable to decrypt storage target password", ex);
		}
	}
}
