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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage.util;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.util.EucalyptusCloudException;

public class BlockStorageUtil {
	private static Logger LOG = Logger.getLogger(BlockStorageUtil.class);
	
	
	/**
	 * Returns the corresponding partition for the requested componentId class running on the local host
	 * @param compClass
	 * @return
	 */
	public static <C extends ComponentId> Partition getPartitionForLocalService(Class<C> compClass) throws EucalyptusCloudException {
		try {
			return Partitions.lookup(Components.lookup(compClass).getLocalServiceConfiguration());
		} catch(Exception e) {
			LOG.error("Error finding partition for local component: " + compClass.getCanonicalName());
			throw new EucalyptusCloudException("Failed lookup", e);			
		}
	}
	
	public static String encryptNodeTargetPassword(String password, Partition partition) throws EucalyptusCloudException {
    try {
      if(partition == null) {
        throw new EucalyptusCloudException("Invalid partition specified. Got null");
      } else {        
        PublicKey ncPublicKey = partition.getNodeCertificate( ).getPublicKey();
        Cipher cipher = Ciphers.RSA_PKCS1.get();
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
			cipher = Ciphers.RSA_PKCS1.get();
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
			Cipher cipher = Ciphers.RSA_PKCS1.get();
			cipher.init(Cipher.DECRYPT_MODE, scPrivateKey);
			return new String(cipher.doFinal(Base64.decode(encryptedPassword)));
		} catch(Exception ex) {
			LOG.error(ex);
			throw new EucalyptusCloudException("Unable to decrypt storage target password", ex);
		}
	}
	
	//Encrypt data using the node public key
	public static String encryptForNode(String data, Partition partition) throws EucalyptusCloudException {
		try {
			if( partition == null) {
				throw new EucalyptusCloudException("Invalid partition specified. Got null");
			} else {
				PublicKey ncPublicKey = partition.getNodeCertificate( ).getPublicKey();
				Cipher cipher = Ciphers.RSA_PKCS1.get();
				cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey);
				return new String(Base64.encode(cipher.doFinal(data.getBytes())));
			}
		} catch ( Exception e ) {
			LOG.error( "Unable to encrypt data: " + e.getMessage( ), e );
			throw new EucalyptusCloudException("Unable to encrypt data: " + e.getMessage(), e);
		}
	}
	
	//Decrypt data using the node private key. Primarly for VMwareBroker
	public static String decryptForNode(String data, Partition partition) throws EucalyptusCloudException {
		try {
			if( partition == null) {
				throw new EucalyptusCloudException("Invalid partition specified. Got null");
			} else {
				PrivateKey ncPrivateKey = partition.getNodePrivateKey();
				Cipher cipher = Ciphers.RSA_PKCS1.get();
				cipher.init(Cipher.DECRYPT_MODE, ncPrivateKey);
				return new String(cipher.doFinal(Base64.decode(data)));
			}
		} catch ( Exception e ) {
			LOG.error( "Unable to dencrypt data with node private key: " + e.getMessage( ), e );
			throw new EucalyptusCloudException("Unable to encrypt data with node private key: " + e.getMessage(), e);
		}
	}
	
	//Encrypt data using the cloud public key
	public static String encryptForCloud(String data) throws EucalyptusCloudException {
		try {
			PublicKey clcPublicKey = SystemCredentials.lookup(Eucalyptus.class).getCertificate().getPublicKey();
			Cipher cipher = Ciphers.RSA_PKCS1.get();
			cipher.init(Cipher.ENCRYPT_MODE, clcPublicKey);
			return new String(Base64.encode(cipher.doFinal(data.getBytes())));	      
		} catch ( Exception e ) {
			LOG.error( "Unable to encrypt data: " + e.getMessage( ), e );
			throw new EucalyptusCloudException("Unable to encrypt data: " + e.getMessage(), e);
		}
	}
	
	//Decrypt data encrypted with the Cloud public key
	public static String decryptWithCloud(String data) throws EucalyptusCloudException {
		PrivateKey clcPrivateKey = SystemCredentials.lookup(Eucalyptus.class).getPrivateKey();
		try {
			Cipher cipher = Ciphers.RSA_PKCS1.get();
			cipher.init(Cipher.DECRYPT_MODE, clcPrivateKey);
			return new String(cipher.doFinal(Base64.decode(data)));
		} catch(Exception ex) {
			LOG.error(ex);
			throw new EucalyptusCloudException("Unable to decrypt data with cloud private key", ex);
		}
	}
	
	/**
	 * Looks up the blockstorage account, admin user and role and sets them up if they are not already present. Uses Accounts library directly rather than euare
	 * service API. The necessary resources are looked up first. If the lookup fails, an attempt is made to create or add new resource. Any failure is followed
	 * by a lookup again before exiting the method. This process is repeated for every resource separately as they could be concurrently processed by another SC
	 */
	public static Role checkAndConfigureBlockStorageAccount() throws EucalyptusCloudException {
		Account blockStorageAccount = null;
		Role role = null;

		// Lookup blockstorage account. It should have been setup by the database bootstrapper. If not set it up here
		try {
			blockStorageAccount = Accounts.lookupAccountByName(Account.BLOCKSTORAGE_SYSTEM_ACCOUNT);
		} catch (Exception e) {
			LOG.warn("Could not find account " + Account.BLOCKSTORAGE_SYSTEM_ACCOUNT + ". Account may not exist, trying to create it");
			try {
				blockStorageAccount = Accounts.addSystemAccountWithAdmin(Account.BLOCKSTORAGE_SYSTEM_ACCOUNT);
			} catch (Exception e1) {
				LOG.warn("Failed to create account " + Account.BLOCKSTORAGE_SYSTEM_ACCOUNT);
				throw new EucalyptusCloudException("Failed to create account " + Account.BLOCKSTORAGE_SYSTEM_ACCOUNT);
			}
		}

		// Lookup role of the account. Add the role if necessary. If that fails, lookup the role again before bailing out
		try {
			role = blockStorageAccount.lookupRoleByName(StorageProperties.EBS_ROLE_NAME);
		} catch (Exception e) {
			LOG.debug("Could not find " + StorageProperties.EBS_ROLE_NAME + " role for " + Account.BLOCKSTORAGE_SYSTEM_ACCOUNT
					+ " account. The role may not exist, trying to add role to the account");
			try {
				role = blockStorageAccount.addRole(StorageProperties.EBS_ROLE_NAME, "/blockstorage", StorageProperties.DEFAULT_ASSUME_ROLE_POLICY);
			} catch (Exception e1) {
				LOG.debug("Failed to add " + StorageProperties.EBS_ROLE_NAME + " role. Checking if the role is assigned to the account");
				try {
					role = blockStorageAccount.lookupRoleByName(StorageProperties.EBS_ROLE_NAME);
				} catch (Exception e2) {
					LOG.warn("Could not find " + StorageProperties.EBS_ROLE_NAME + " role for " + Account.BLOCKSTORAGE_SYSTEM_ACCOUNT
							+ " account and failed to assign the role to the account", e2);
					throw new EucalyptusCloudException("Could not find " + StorageProperties.EBS_ROLE_NAME + " role for " + Account.BLOCKSTORAGE_SYSTEM_ACCOUNT
							+ " account and failed to assign the role to the account");
				}
			}
		}

		try {
			boolean foundBucketPolicy = false;
			boolean foundObjectPolicy = false;

			List<Policy> policies = role.getPolicies();
			for (Policy policy : policies) {
				if (policy.getName().equals(StorageProperties.S3_BUCKET_ACCESS_POLICY_NAME)) {
					foundBucketPolicy = true;
				}
				if (policy.getName().equals(StorageProperties.S3_OBJECT_ACCESS_POLICY_NAME)) {
					foundObjectPolicy = true;
				}
				if (foundBucketPolicy && foundObjectPolicy) {
					break;
				}
			}

			if (!foundBucketPolicy) {
				try {
					role.putPolicy(StorageProperties.S3_BUCKET_ACCESS_POLICY_NAME, StorageProperties.S3_SNAPSHOT_BUCKET_ACCESS_POLICY);
				} catch (Exception e) {
					LOG.debug("Failed to assign " + StorageProperties.S3_BUCKET_ACCESS_POLICY_NAME + " policy to " + StorageProperties.EBS_ROLE_NAME
							+ " role. Checking if the policy is assigned to the role");

					foundBucketPolicy = false;
					policies = role.getPolicies();
					for (Policy policy : policies) {
						if (policy.getName().equals(StorageProperties.S3_BUCKET_ACCESS_POLICY_NAME)) {
							foundBucketPolicy = true;
							break;
						}
					}

					if (!foundBucketPolicy) {
						LOG.warn("Could not find " + StorageProperties.S3_BUCKET_ACCESS_POLICY_NAME + " policy assigned to " + StorageProperties.EBS_ROLE_NAME
								+ " role and failed to assign the policy to the role", e);
						throw new EucalyptusCloudException("Could not find " + StorageProperties.S3_BUCKET_ACCESS_POLICY_NAME + " policy assigned to "
								+ StorageProperties.EBS_ROLE_NAME + " role and failed to assign the policy to the role");
					}
				}
			}

			if (!foundObjectPolicy) {
				try {
					role.putPolicy(StorageProperties.S3_OBJECT_ACCESS_POLICY_NAME, StorageProperties.S3_SNAPSHOT_OBJECT_ACCESS_POLICY);
				} catch (Exception e) {
					LOG.debug("Failed to assign " + StorageProperties.S3_OBJECT_ACCESS_POLICY_NAME + " policy to " + StorageProperties.EBS_ROLE_NAME
							+ " role. Checking if the policy is assigned to the role");

					foundObjectPolicy = false;
					policies = role.getPolicies();
					for (Policy policy : policies) {
						if (policy.getName().equals(StorageProperties.S3_OBJECT_ACCESS_POLICY_NAME)) {
							foundObjectPolicy = true;
							break;
						}
					}

					if (!foundObjectPolicy) {
						LOG.warn("Could not find " + StorageProperties.S3_OBJECT_ACCESS_POLICY_NAME + " policy assigned to " + StorageProperties.EBS_ROLE_NAME
								+ " role and failed to assign the policy to the role", e);
						throw new EucalyptusCloudException("Could not find " + StorageProperties.S3_OBJECT_ACCESS_POLICY_NAME + " policy assigned to "
								+ StorageProperties.EBS_ROLE_NAME + " role and failed to assign the policy to the role");
					}
				}
			}

			return role;
		} catch (EucalyptusCloudException e) {
			throw e;
		} catch (Exception e) {
			LOG.warn("Could not fetch the policies for " + StorageProperties.EBS_ROLE_NAME + " role assigned to " + Account.BLOCKSTORAGE_SYSTEM_ACCOUNT
					+ " account", e);
			throw new EucalyptusCloudException("Could not fetch the policies for " + StorageProperties.EBS_ROLE_NAME + " role assigned to "
					+ Account.BLOCKSTORAGE_SYSTEM_ACCOUNT + " account");
		}
	}
}
