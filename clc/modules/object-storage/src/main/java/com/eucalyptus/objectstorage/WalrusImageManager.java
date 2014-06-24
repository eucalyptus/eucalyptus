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

package com.eucalyptus.objectstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.RollbackException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.objectstorage.Walrus;
import com.eucalyptus.objectstorage.entities.BucketInfo;
import com.eucalyptus.objectstorage.entities.ImageCacheInfo;
import com.eucalyptus.objectstorage.entities.ObjectInfo;
import com.eucalyptus.objectstorage.entities.WalrusInfo;
import com.eucalyptus.objectstorage.exceptions.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.DecryptionFailedException;
import com.eucalyptus.objectstorage.exceptions.EntityTooLargeException;
import com.eucalyptus.objectstorage.exceptions.NoSuchBucketException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.exceptions.NotAuthorizedException;
import com.eucalyptus.objectstorage.exceptions.WalrusException;
import com.eucalyptus.objectstorage.msgs.CacheImageResponseType;
import com.eucalyptus.objectstorage.msgs.CacheImageType;
import com.eucalyptus.objectstorage.msgs.CheckImageResponseType;
import com.eucalyptus.objectstorage.msgs.CheckImageType;
import com.eucalyptus.objectstorage.msgs.FlushCachedImageResponseType;
import com.eucalyptus.objectstorage.msgs.FlushCachedImageType;
import com.eucalyptus.objectstorage.msgs.GetDecryptedImageResponseType;
import com.eucalyptus.objectstorage.msgs.GetDecryptedImageType;
import com.eucalyptus.objectstorage.msgs.ValidateImageResponseType;
import com.eucalyptus.objectstorage.msgs.ValidateImageType;
import com.eucalyptus.objectstorage.msgs.WalrusDataMessenger;
import com.eucalyptus.objectstorage.msgs.WalrusMonitor;
import com.eucalyptus.objectstorage.util.WalrusProperties;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Lookups;
import com.eucalyptus.util.XMLParser;

import edu.ucsb.eucalyptus.util.EucaSemaphore;
import edu.ucsb.eucalyptus.util.EucaSemaphoreDirectory;

public class WalrusImageManager {
	private static Logger LOG = Logger.getLogger( WalrusImageManager.class );
	private static ConcurrentHashMap<String, ImageCacher> imageCachers = new ConcurrentHashMap<String, ImageCacher>();

	private StorageManager storageManager;
	private  WalrusDataMessenger imageMessenger;

	public WalrusImageManager(StorageManager storageManager, WalrusDataMessenger imageMessenger) {
		this.storageManager = storageManager;
		this.imageMessenger = imageMessenger;
	}
	
	private static void logWithContext(String message, Level logLevel, String correlationId, String account ) {
		String fullMessage = "[CorrelationId: " + correlationId + " | Account: " + account + "] " + message;
		if(logLevel == null || logLevel.equals(Level.INFO)) {
			LOG.info(fullMessage);
		} else {				
			if(logLevel.equals(Level.ALL)) {
				LOG.trace(fullMessage);
			} else if(logLevel.equals(Level.DEBUG)) {
				LOG.debug(fullMessage);
			} else if(logLevel.equals(Level.ERROR)) {
				LOG.error(fullMessage);
			} else if(logLevel.equals(Level.TRACE)) {
				LOG.trace(fullMessage);
			} else if(logLevel.equals(Level.FATAL)) {
				LOG.fatal(fullMessage);
			} else if(logLevel.equals(Level.WARN)) {
				LOG.warn(fullMessage);
			}
		}
	}
	
	/**
	 * Will the image size specified fit in the cache at all. Just does a basic check against
	 * the total cache size, does not consider evictions or other images.
	 * @param imageSizeInBytes
	 * @return true if image size is <= total cache capacity
	 */
	private boolean willFitInCache(long imageSizeInBytes) {
		long maxSize = WalrusInfo.getWalrusInfo().getStorageMaxCacheSizeInMB() * WalrusProperties.M;
		LOG.debug("Checking image size of " + imageSizeInBytes + " bytes against current cache max size of " + maxSize + " bytes");
		return imageSizeInBytes <= maxSize;
	}

	private String decryptImage(String bucketName, String objectKey, Account account, boolean isAdministrator, final String correlationId) throws EucalyptusCloudException {		
		logWithContext("Decrypting image with manifest: " + bucketName + "/" + objectKey, Level.INFO, correlationId, account.getAccountNumber());
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);
				if(isAdministrator || (
						objectInfo.canRead(account.getAccountNumber()) &&
						Lookups.checkPrivilege(PolicySpec.S3_GETOBJECT,
								PolicySpec.VENDOR_S3,
								PolicySpec.S3_RESOURCE_OBJECT,
								PolicySpec.objectFullName(bucketName, objectKey),
								objectInfo.getOwnerId()))) {
					String objectName = objectInfo.getObjectName();
					File file = new File(storageManager.getObjectPath(bucketName, objectName));
					XMLParser parser = new XMLParser(file);
					//Read manifest
					String imageKey = parser.getValue("//image/name");
					String encryptedKey = parser.getValue("//ec2_encrypted_key");
					String encryptedIV = parser.getValue("//ec2_encrypted_iv");
					String signature = parser.getValue("//signature");
					Long bundledSize = Long.valueOf(parser.getValue("//bundled_size"));
					Long claimedSize = Long.valueOf(parser.getValue("//size"));

					String image = parser.getXML("image");
					String machineConfiguration = parser.getXML("machine_configuration");

					String verificationString = machineConfiguration + image;

					Signature sigVerifier;
					try {
						sigVerifier = Signature.getInstance("SHA1withRSA");
					} catch (NoSuchAlgorithmException ex) {
						LOG.error(ex, ex);
						throw new DecryptionFailedException("SHA1withRSA not found");
					}

					if(isAdministrator) {
						try {
							boolean verified = false;
							for(User u:Accounts.listAllUsers( )) {
								for (Certificate c : u.getCertificates()) {
									X509Certificate cert = c.getX509Certificate( );
									if(cert != null)
										verified = canVerifySignature(sigVerifier, cert, signature, verificationString);
									if(verified)
										break;
								}
								if(verified) break;
							}
							if(!verified) {
								X509Certificate cert = SystemCredentials.lookup(Eucalyptus.class).getCertificate();
								if(cert != null)
									verified = canVerifySignature(sigVerifier, cert, signature, verificationString);
							}

							if(!verified){
								final List<Partition> partitions = Partitions.list();
								for(final Partition p : partitions){
									X509Certificate cert = p.getNodeCertificate();
									if(cert != null)
										verified = canVerifySignature(sigVerifier, cert, signature, verificationString);
									if(verified)
										break;
								}
							}
							if(!verified) {
								throw new NotAuthorizedException("Invalid signature");
							}
						} catch (Exception ex) {
							db.rollback();
							LOG.error(ex, ex);
							throw new DecryptionFailedException("signature verification");
						}
					} else {
						boolean signatureVerified = false;
						try {
							for(User user: account.getUsers()) {
								for(Certificate c : user.getCertificates()) {
									X509Certificate cert = c.getX509Certificate( );
									if(cert != null) {
										signatureVerified = canVerifySignature(sigVerifier, cert, signature, verificationString);
									}
									if(signatureVerified)
										break;
								}
								if(signatureVerified) break;
							}
						} catch(Exception ex) {
							db.rollback();
							LOG.error(ex, ex);
							throw new DecryptionFailedException("signature verification");
						}
						if(!signatureVerified) {
							try {
								X509Certificate cert = SystemCredentials.lookup(Eucalyptus.class).getCertificate();
								if(cert != null)
									signatureVerified = canVerifySignature(sigVerifier, cert, signature, verificationString);
							} catch(Exception ex) {
								db.rollback();
								LOG.error(ex, ex);
								throw new DecryptionFailedException("signature verification");
							}
						}
						if(!signatureVerified) {
							throw new NotAuthorizedException("Invalid signature");
						}
					}
					List<String> parts = parser.getValues("//image/parts/part/filename");
					if(parts == null) {
						logWithContext("Decryption failed due to invalid manifest:" + bucketName + "/" + objectKey, Level.ERROR, correlationId, account.getAccountNumber());
						throw new DecryptionFailedException("Invalid manifest");
					}
					ArrayList<String> qualifiedPaths = new ArrayList<String>();
					searchObjectInfo = new ObjectInfo();
					searchObjectInfo.setBucketName(bucketName);
					List<ObjectInfo> bucketObjectInfos = dbObject.queryEscape(searchObjectInfo);

					for (String part: parts) {
						for(ObjectInfo object : bucketObjectInfos) {
							if(part.equals(object.getObjectKey())) {
								qualifiedPaths.add(storageManager.getObjectPath(bucketName, object.getObjectName()));
							}
						}
					}
					//Assemble parts
					String encryptedImageKey = UUID.randomUUID().toString() + ".crypt.gz";
					String encryptedImageName = storageManager.getObjectPath(bucketName, encryptedImageKey);
					String decryptedImageKey = encryptedImageKey.substring(0, encryptedImageKey.lastIndexOf("crypt.gz")) + "tgz";

					String decryptedImageName = storageManager.getObjectPath(bucketName, decryptedImageKey);
					
					/*
					 * Check the max cache size. Don't try to decrypt if size is too large. This is for fast-fail
					 */
					if(claimedSize != null && !willFitInCache(claimedSize.longValue())) {
						logWithContext("Aborting image part assembly because image is too large to fit in cache", Level.ERROR, correlationId, account.getAccountNumber());
						throw new WalrusException("EntityTooLarge", "Image is too large to fit in the cache.", "image", bucketName + "/" + objectKey , HttpResponseStatus.BAD_REQUEST );
					}
					
					logWithContext("Assembling parts for image " + bucketName + "/" + objectKey + " into file: " + encryptedImageName, Level.INFO, correlationId, account.getAccountNumber());					
					WalrusImageUtils.assembleParts(encryptedImageName, qualifiedPaths);
					logWithContext("Assembly of parts complete for image " + bucketName + "/" + objectKey, Level.INFO , correlationId, account.getAccountNumber());

					//Decrypt key and IV
					byte[] key;
					byte[] iv;
					try {
						PrivateKey pk = SystemCredentials.lookup(Eucalyptus.class ).getPrivateKey();
						Cipher cipher = Ciphers.RSA_PKCS1.get();
						cipher.init(Cipher.DECRYPT_MODE, pk);
						String keyString = new String(cipher.doFinal(Hashes.hexToBytes(encryptedKey)));
						key = Hashes.hexToBytes(keyString);
						String ivString = new String(cipher.doFinal(Hashes.hexToBytes(encryptedIV)));
						iv = Hashes.hexToBytes(ivString);
					} catch(Exception ex) {
						db.rollback();
						LOG.error(ex);
						try {
							logWithContext("Cleaning up encrypted temporary image file for: " + bucketName + "/" + objectKey, Level.DEBUG , correlationId, account.getAccountNumber());
							storageManager.deleteAbsoluteObject(encryptedImageName);
						} catch (Exception e) {
							LOG.error(e);
						}
						logWithContext("Decryption failed for: " + bucketName + "/" + objectKey + " due to AES params", Level.ERROR, correlationId, account.getAccountNumber());
						throw new DecryptionFailedException("AES params");
					}

					//Unencrypt image
					try {
						db.commit();
						Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
						IvParameterSpec salt = new IvParameterSpec(iv);
						SecretKey keySpec = new SecretKeySpec(key, "AES");
						cipher.init(Cipher.DECRYPT_MODE, keySpec, salt);
						logWithContext("Starting decryption for image " + bucketName + "/" + objectKey + " in file " + encryptedImageName, Level.INFO, correlationId, account.getAccountNumber());
						WalrusImageUtils.decryptImage(encryptedImageName, decryptedImageName, cipher);
						logWithContext("Finished decryption for image " + bucketName + "/" + objectKey + " from file " + encryptedImageName + " to " + decryptedImageName + " successfully", Level.INFO, correlationId, account.getAccountNumber());
					} catch (Exception ex) {
						db.rollback();
						LOG.error(ex);
						try {
							logWithContext("Cleaning up encrypted and decrypted temporary image files for: " + bucketName + "/" + objectKey, Level.DEBUG, correlationId, account.getAccountNumber());
							storageManager.deleteAbsoluteObject(encryptedImageName);
							storageManager.deleteAbsoluteObject(decryptedImageName);
						} catch (Exception e) {
							LOG.error(e);
						}
						throw new DecryptionFailedException("decryption failed");
					}
					
					//Clean up encrypted file, leaving only decrypted one to pass to next phase
					try {
						logWithContext("Cleaning up encrypted temporary image file for: " + bucketName + "/" + objectKey, Level.DEBUG, correlationId, account.getAccountNumber());
						storageManager.deleteAbsoluteObject(encryptedImageName);
					} catch (Exception ex) {
						LOG.error(ex);
					}
					return decryptedImageKey;
				} else {
					//no permissions, do nothing
					logWithContext("Cannot perform decryption. Insufficient S3/IAM permissions on manifest object: " + bucketName + "/" + objectKey, Level.ERROR, correlationId, account.getAccountNumber());
				}
			} else {
				//Data not found
				logWithContext("Cannot perform decryption. Manifest object not found: " + bucketName + "/" + objectKey, Level.ERROR, correlationId, account.getAccountNumber());
			}
		} else {
			//Bucket not found
			logWithContext("Cannot perform decryption. Bucket not found: " + bucketName, Level.ERROR, correlationId, account.getAccountNumber());
		}
		return null;
	}

	private boolean canVerifySignature(Signature sigVerifier, X509Certificate cert, String signature, String verificationString) throws Exception {
		PublicKey publicKey = cert.getPublicKey();
		sigVerifier.initVerify(publicKey);
		sigVerifier.update((verificationString).getBytes());
		return sigVerifier.verify(Hashes.hexToBytes(signature));
	}


	private void checkManifest(String bucketName, String objectKey, Account account) throws EucalyptusCloudException {
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		BucketInfo bucket = null;
		try {
			bucket = db.getUniqueEscape(bucketInfo);
		} catch(Exception t) {
			throw new WalrusException("Unable to get bucket: " + bucketName, t);
		}

		if (bucket != null) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);

				if(objectInfo.canRead(account.getAccountNumber())) {
					String objectName = objectInfo.getObjectName();
					File file = new File(storageManager.getObjectPath(bucketName, objectName));
					XMLParser parser = new XMLParser(file);
					//Read manifest
					String encryptedKey = parser.getValue("//ec2_encrypted_key");
					String encryptedIV = parser.getValue("//ec2_encrypted_iv");
					String signature = parser.getValue("//signature");

					String image = parser.getXML("image");
					String machineConfiguration = parser.getXML("machine_configuration");

					boolean signatureVerified = false;

					Signature sigVerifier;
					try {
						sigVerifier = Signature.getInstance("SHA1withRSA");
					} catch (NoSuchAlgorithmException ex) {
						LOG.error(ex, ex);
						throw new DecryptionFailedException("SHA1withRSA not found");
					}

					try {
						for(User u:Accounts.listAllUsers( )) {
							for (Certificate cert : u.getCertificates()) {
								if(cert != null&&cert instanceof X509Certificate)
									signatureVerified = canVerifySignature(sigVerifier, ( X509Certificate ) cert, signature, (machineConfiguration + image));
								if(signatureVerified)
									break;
							}
						}
						if(!signatureVerified) {
							X509Certificate cert = SystemCredentials.lookup(Eucalyptus.class).getCertificate();
							if(cert != null)
								signatureVerified = canVerifySignature(sigVerifier, cert, signature, (machineConfiguration + image));
						}
					} catch(Exception ex) {
						db.rollback();
						LOG.error(ex, ex);
						throw new DecryptionFailedException("signature verification");
					}
					if(!signatureVerified) {
						throw new NotAuthorizedException("Invalid signature");
					}
					//Decrypt key and IV

					byte[] key;
					byte[] iv;
					try {
						PrivateKey pk = SystemCredentials.lookup(Eucalyptus.class).getPrivateKey();
						Cipher cipher = Ciphers.RSA_PKCS1.get();
						cipher.init(Cipher.DECRYPT_MODE, pk);
						key = Hashes.hexToBytes(new String(cipher.doFinal(Hashes.hexToBytes(encryptedKey))));
						iv = Hashes.hexToBytes(new String(cipher.doFinal(Hashes.hexToBytes(encryptedIV))));
					} catch(Exception ex) {
						db.rollback();
						LOG.error(ex, ex);
						throw new DecryptionFailedException("AES params");
					}
					db.commit();
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey);
				}
			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
	}

	/**
	 * Determines if the image is cached and ready for use.
	 * @param bucketName
	 * @param manifestKey
	 * @return
	 */
	private boolean isCached(String bucketName, String manifestKey) {
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
		EntityWrapper<ImageCacheInfo> db = EntityWrapper.get(ImageCacheInfo.class);		
		try {
			ImageCacheInfo foundImageCacheInfo = db.getUniqueEscape(searchImageCacheInfo);
			if(foundImageCacheInfo.getInCache())
				return true;
			else
				return false;
		} catch(Exception ex) {
			return false;
		} finally {
			db.rollback();
		}
	}

	/**
	 * Returns the total size of bytes cached thus far, negative indicates not found
	 * @param bucketName
	 * @param manifestKey
	 * @return
	 */
	private long checkCachingProgress(String bucketName, String manifestKey) {
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
		EntityWrapper<ImageCacheInfo> db = EntityWrapper.get(ImageCacheInfo.class);		
		try {
			ImageCacheInfo foundImageCacheInfo = db.getUniqueEscape(searchImageCacheInfo);
			String cacheImageKey = foundImageCacheInfo.getImageName().substring(0, foundImageCacheInfo.getImageName().lastIndexOf(".tgz"));
			return storageManager.getObjectSize(bucketName, cacheImageKey);
		} catch (Exception ex) {
			return -1L;
		} finally {
			db.rollback();
		}
	}

	/**
	 * Handles process of caching an image, including triggering the asynchronous thread to do the data processing
	 * @param bucketName
	 * @param manifestKey
	 * @param account
	 * @param isAdministrator
	 * @throws EucalyptusCloudException
	 */
	private void cacheImage(String bucketName, String manifestKey, Account account, boolean isAdministrator, final String correlationId) throws EucalyptusCloudException {
		final String accountNumber = (account == null || account.getAccountNumber() == null ? "unknown" : account.getAccountNumber());
		logWithContext("Attempting to cache image " + bucketName + "/" + manifestKey + ".", Level.DEBUG, correlationId, accountNumber);
		
		String decryptedImageKey = null;
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
		EntityWrapper<ImageCacheInfo> db = EntityWrapper.get(ImageCacheInfo.class);
		try {
			List<ImageCacheInfo> imageCacheInfos = db.queryEscape(searchImageCacheInfo);
			if(imageCacheInfos.size() != 0) {
				ImageCacheInfo icInfo = imageCacheInfos.get(0);
				if(!icInfo.getInCache()) {
					decryptedImageKey = icInfo.getImageName();
				} else {
					//In the cache already.
					logWithContext("Found image " + bucketName + "/" + manifestKey + " already in cache. No further action required.", Level.INFO, correlationId, accountNumber);			
					return;
				}
			}	
		} catch(Exception e) {
			logWithContext("Failed looking up cache records for image: " + bucketName + "/" + manifestKey + ". Exception: " + e.getMessage(), Level.ERROR, correlationId, accountNumber);
			return;
		} finally {
			db.rollback();
		}
		
		//unzip, untar image in the background
		ImageCacher imageCacher = imageCachers.putIfAbsent(bucketName + manifestKey, new ImageCacher(bucketName, manifestKey, decryptedImageKey));		
		if(imageCacher == null) {
			logWithContext("No current caching tasks found for image: " + bucketName + "/" + manifestKey + " Initiating one.", Level.DEBUG, correlationId, accountNumber);
			if(decryptedImageKey == null) {
				try {
					decryptedImageKey = decryptImage(bucketName, manifestKey, account, isAdministrator, correlationId);
				} catch(EucalyptusCloudException ex) {
					imageCachers.remove(bucketName + manifestKey);
					throw ex;
				}
				//decryption worked. Add it.
				ImageCacheInfo foundImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
				foundImageCacheInfo.setImageName(decryptedImageKey);
				foundImageCacheInfo.setInCache(false);
				foundImageCacheInfo.setUseCount(0);
				foundImageCacheInfo.setSize(0L);
				db = EntityWrapper.get(ImageCacheInfo.class);
				try {
					db.add(foundImageCacheInfo);
					db.commit();
				} catch(Exception e) {
					logWithContext("Failed to add new cache record: " + e.getMessage(), Level.ERROR, correlationId, accountNumber);
					imageCachers.remove(bucketName + manifestKey);
					throw new EucalyptusCloudException(e);
				} finally {
					db.rollback();
				}
			}
			imageCacher = imageCachers.get(bucketName + manifestKey);
			imageCacher.setDecryptedImageKey(decryptedImageKey);
			Threads.lookup(Walrus.class, WalrusImageManager.ImageCacher.class).limitTo(10).submit(imageCacher);
		} else {
			//Another image cacher found, this thread just waits now.
			logWithContext("Another thread already caching image: " + bucketName + "/" + manifestKey + ". No further action required.", Level.INFO, correlationId, accountNumber);
		}
	}
		
	private void flushCachedImage (String bucketName, String objectKey) throws Exception {
		EucaSemaphore semaphore = EucaSemaphoreDirectory.getSemaphore(bucketName + "/" + objectKey);
		while(semaphore.inUse()) {
			try {
				synchronized (semaphore) {
					semaphore.wait();
				}
			} catch(InterruptedException ex) {
				LOG.error(ex);
			}
		}
		EucaSemaphoreDirectory.removeSemaphore(bucketName + "/" + objectKey);
		EntityWrapper<ImageCacheInfo> db = EntityWrapper.get(ImageCacheInfo.class);
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, objectKey);
		List<ImageCacheInfo> foundImageCacheInfos = db.queryEscape(searchImageCacheInfo);

		if(foundImageCacheInfos.size() > 0) {
			ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
			LOG.info("Attempting to flush cached image: " + bucketName + "/" + objectKey);
			if(foundImageCacheInfo.getInCache() && (imageCachers.get(bucketName + objectKey) == null)) {
				db.delete(foundImageCacheInfo);
				storageManager.deleteObject(bucketName, foundImageCacheInfo.getImageName());
			}
			db.commit();
		} else {
			db.rollback();
			LOG.warn("Cannot find image in cache" + bucketName + "/" + objectKey);
		}
	}

	private void validateManifest(String bucketName, String objectKey, String accountId) throws EucalyptusCloudException {
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		BucketInfo bucket = null;
		try {
			bucket = db.getUniqueEscape(bucketInfo);
		} catch(Exception t) {
			throw new WalrusException("Unable to get bucket: " + bucketName, t);
		}

		if (bucket != null) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);

				if(objectInfo.canRead(accountId)) {
					String objectName = objectInfo.getObjectName();
					File file = new File(storageManager.getObjectPath(bucketName, objectName));
					XMLParser parser = new XMLParser(file);
					String image = parser.getXML("image");
					String machineConfiguration = parser.getXML("machine_configuration");
					String verificationString = machineConfiguration + image;
					FileInputStream inStream = null;

					FileInputStream fileInputStream = null;
					try {
						PrivateKey pk = SystemCredentials.lookup(Eucalyptus.class).getPrivateKey();
						Signature sigCloud = Signature.getInstance("SHA1withRSA");
						sigCloud.initSign(pk);
						sigCloud.update(verificationString.getBytes());
						String signature = new String(Hashes.bytesToHex(sigCloud.sign()));
						//TODO: refactor
						DocumentBuilder docBuilder = XMLParser.getDocBuilder();
						fileInputStream = new FileInputStream( file );
						Document docRoot = docBuilder.parse( fileInputStream );
						Element sigElement = docRoot.createElement("signature");						
						sigElement.setTextContent(signature);						
						Node manifestElem = docRoot.getFirstChild();
						manifestElem.appendChild(sigElement);

						fileInputStream.close();
						Source source = new DOMSource(docRoot);
						Result result = new StreamResult(file);
						Transformer xformer = TransformerFactory.newInstance().newTransformer();
						xformer.transform(source,result);
						try {
							MessageDigest digest = Digest.MD5.get();
							inStream = new FileInputStream(file);
							byte[] bytes = new byte[WalrusProperties.IO_CHUNK_SIZE];
							int bytesRead = -1;
							long totalBytesRead = 0;
							try {
								while((bytesRead = inStream.read(bytes, 0, bytes.length)) > 0) {
									digest.update(bytes, 0, bytesRead);
									totalBytesRead += bytesRead;
								}
							} catch (IOException e) {
								LOG.error(e);
								throw new WalrusException(e.getMessage());
							} finally {
								try {
									inStream.close();
								} catch (IOException e) {
									LOG.error(e);
									throw new WalrusException(e.getMessage());
								}
							}
							String md5 = Hashes.bytesToHex(digest.digest());
							objectInfo.setEtag(md5);
							objectInfo.setSize(totalBytesRead);
						} catch (FileNotFoundException e) {
							LOG.error(e, e);
							throw new WalrusException(e.getMessage());
						}
					} catch(Exception ex) {
						if(inStream != null) {
							try {
								inStream.close();
							} catch(IOException e) {
								LOG.error(e);
							}
						}
						if(fileInputStream != null) {
							try {
								fileInputStream.close();
							} catch(IOException e) {
								LOG.error(e);
							}
						}
						db.rollback();
						LOG.error(ex, ex);
						throw new WalrusException("Unable to sign manifest: " + bucketName + "/" + objectKey);
					}
					db.commit();
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey);
				}
			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
	}

	public void startImageCacheFlusher(String bucketName, String manifestName) {
		ImageCacheFlusher imageCacheFlusher = new ImageCacheFlusher(bucketName, manifestName);
		Threads.lookup(Walrus.class, WalrusImageManager.ImageCacheFlusher.class).limitTo(10).submit(imageCacheFlusher);
	}

	private class ImageCacheFlusher implements Runnable {
		private String bucketName;
		private String objectKey;

		public ImageCacheFlusher(String bucketName, String objectKey) {
			this.bucketName = bucketName;
			this.objectKey = objectKey;
		}

		public void run() {
			try {
				flushCachedImage(bucketName, objectKey);
			} catch(Exception ex) {
				LOG.error(ex);
			}
		}
	}


	/**
	 * Thread that actually does the caching of a single image.
	 * Upon termination of the thread, the image is either ready for
	 * delivery a client and has metadata accurately in the image cache db
	 * or else there is no record and all artifacts of the caching process
	 * have been removed.
	 * @author zhill
	 *
	 */
	private class ImageCacher implements Runnable {

		private String bucketName;
		private String manifestKey;
		private String decryptedImageKey;
		private boolean imageSizeExceeded;
		private long spaceNeeded; //Space needed for the cache, the image size after unzip and untar.
		private long myThreadId; //Threads Id, used for logging

		public ImageCacher(String bucketName, String manifestKey, String decryptedImageKey) {
			this.bucketName = bucketName;
			this.manifestKey = manifestKey;
			this.decryptedImageKey = decryptedImageKey;
			this.imageSizeExceeded = false;
			this.myThreadId = -1L;
			this.spaceNeeded = -1L;
		}

		public void setDecryptedImageKey(String key) {
			this.decryptedImageKey = key;
		}
		
		/**
		 * Logs a status update message, by default uses INFO level if logLevel is null
		 * @param message
		 */
		private void logCachingStatus(String message, Level logLevel) {
			String taskId = (this.myThreadId <= 0 ? "unknown" : String.valueOf(this.myThreadId));
			String fullMessage = "[Caching Task: " + taskId + "] " + message;
			if(logLevel == null || logLevel.equals(Level.INFO)) {
				LOG.info(fullMessage);
			} else {				
				if(logLevel.equals(Level.ALL)) {
					LOG.trace(fullMessage);
				} else if(logLevel.equals(Level.DEBUG)) {
					LOG.debug(fullMessage);
				} else if(logLevel.equals(Level.ERROR)) {
					LOG.error(fullMessage);
				} else if(logLevel.equals(Level.TRACE)) {
					LOG.trace(fullMessage);
				} else if(logLevel.equals(Level.FATAL)) {
					LOG.fatal(fullMessage);
				} else if(logLevel.equals(Level.WARN)) {
					LOG.warn(fullMessage);
				}
			}
		}
		
		/**
		 * Attempts caching of the given file including the unpacking and decryption phases.
		 * If caching cannot be completed, a negative value is returned. On success, a positive
		 * value that is the size, in Bytes, of the unencrypted image is returned.
		 * @param decryptedImageName
		 * @param tarredImageName
		 * @param imageName
		 * @return
		 */
		private long tryToCache(String decryptedImageName, String tarredImageName, String imageName) {
			Long unencryptedSize = 0L;
			boolean failed = false;
			
			logCachingStatus("Trying to cache image: " + bucketName + "/" + manifestKey + " space needed = " + spaceNeeded + ", image size exceeded = " + imageSizeExceeded, Level.DEBUG);
			try {
				if(!imageSizeExceeded) {
					//this was not called as a retry after eviction, so expand the image to full size.
					logCachingStatus("Unzipping image: " + bucketName + "/" + manifestKey + " in file: " + decryptedImageName + " into: " + tarredImageName, Level.DEBUG);
					WalrusImageUtils.unzipImage(decryptedImageName, tarredImageName);
					logCachingStatus("Unzip completed for: " + bucketName + "/" + manifestKey, Level.DEBUG);
					
					logCachingStatus("Untarring image: " + bucketName + "/" + manifestKey + " in file " + tarredImageName + " into " + imageName, Level.DEBUG);
					unencryptedSize = WalrusImageUtils.untarImage(tarredImageName, imageName);
					logCachingStatus(" Untarring completed for: " + bucketName + "/" + manifestKey, Level.DEBUG);
				} else {
					//Image size was exceeded, so it is already present/expanded.
					File imageFile = new File(imageName);
					if(imageFile.exists()) {
						unencryptedSize = imageFile.length();
						logCachingStatus("Image file found with size: " + unencryptedSize, Level.DEBUG);
					} else {
						logCachingStatus("Could not find image file: " + imageName, Level.ERROR);
						imageSizeExceeded = false;
						spaceNeeded = -1L;
						return -1L;
					}
				}
				
				Long oldCacheSize = 0L;
				EntityWrapper<ImageCacheInfo> db = EntityWrapper.get(ImageCacheInfo.class);
				try {
					List<ImageCacheInfo> imageCacheInfos = db.queryEscape(new ImageCacheInfo());
					for(ImageCacheInfo imageCacheInfo: imageCacheInfos) {
						if(imageCacheInfo.getInCache()) {
							oldCacheSize += imageCacheInfo.getSize();
						}
					}
					db.commit();
				} catch(Exception e) {
					logCachingStatus("Exception calculating used cache capacity. Terminating caching task", Level.ERROR);
					LOG.error("Exception calculating used cache capacity", e);
					spaceNeeded = -1L;
					return -1L;
				} finally {
					db.rollback();
				}
				
				long cacheCapacity = WalrusInfo.getWalrusInfo().getStorageMaxCacheSizeInMB() * WalrusProperties.M;
				if((oldCacheSize + unencryptedSize) > cacheCapacity) { 
					logCachingStatus("Maximum image cache size exceeded when decrypting " + bucketName + "/" + manifestKey + " . Must evict images from cache and retry.", Level.DEBUG);
					failed = true;
					imageSizeExceeded = true;
					//spaceNeeded = unencryptedSize;
					//Old spaceNeeded was the image size itself, it should be the amount needed to be freed, imagesize - freespace in cache.
					spaceNeeded = unencryptedSize - (cacheCapacity - oldCacheSize);
				}
			} catch(Exception ex) {
				logCachingStatus("Caught exception trying to calculate cache usage. Failing task due to: " + ex.getMessage(), Level.ERROR);				
				//try to evict an entry and try again
				failed = true;
			}
			if(failed) {
				if(!imageSizeExceeded) {
					logCachingStatus(" Failed trying to cache image: " + bucketName + "/" + manifestKey + " due to unknown reason.", Level.ERROR);
					try {						
						storageManager.deleteAbsoluteObject(tarredImageName);
						storageManager.deleteAbsoluteObject(imageName);
						logCachingStatus(" Cleaned temporary artifacts for image: " + bucketName + "/" + manifestKey + " on failure cleanup", Level.DEBUG);
					} catch (Exception exception) {
						LOG.error(exception);
					}
				} else {
					logCachingStatus(" Failed trying to cache image: " + bucketName + "/" + manifestKey + " due to not enough available space in cache. Eviction of other images may be required.", null);
				}
				return -1L;
			} else {
				logCachingStatus(" Successfully cached image: " + bucketName + "/" + manifestKey + " size: " + String.valueOf(unencryptedSize), null);
				return unencryptedSize;
			}
		}

		private void notifyWaiters() {
			logCachingStatus(" Notifying waiters for caching of image: " + bucketName + "/" + manifestKey, Level.TRACE);
			WalrusMonitor monitor = imageMessenger.getMonitor(bucketName + "/" + manifestKey);
			synchronized (monitor) {
				imageMessenger.removeMonitor(bucketName + "/" + manifestKey);
				imageCachers.remove(bucketName + manifestKey);			
				monitor.notifyAll();			
			}
			
			//Moved the removeMonitor and cacher remove into the synchronized block from here.
		}

		public void run() {
			//Outermost, to guarantee notification.
			try {			
				try {
					this.myThreadId = Thread.currentThread().getId();
				} catch(final Throwable f) {
					LOG.error("Failed to get thread ID for caching task. Using -1.");
					this.myThreadId = -1L;
				}
			
				logCachingStatus("Initiating caching task for image: " + this.bucketName + "/" + this.manifestKey, null);
				//update status
				//wake up any waiting consumers
				String decryptedImageName = storageManager.getObjectPath(bucketName, decryptedImageKey);
				String imageName = decryptedImageName.substring(0, decryptedImageName.lastIndexOf(".tgz"));
				String tarredImageName = imageName + (".tar");
				String imageKey = decryptedImageKey.substring(0, decryptedImageKey.lastIndexOf(".tgz"));
				Long unencryptedSize;
				int numberOfRetries = 0;
				long backoffTime = WalrusProperties.IMAGE_CACHE_RETRY_BACKOFF_TIME;
				
				while((unencryptedSize = tryToCache(decryptedImageName, tarredImageName, imageName)) < 0) {
					//tryToCache returns -1 on failure. See spaceNeeded for the space needed to finish caching.
					
					try {
						Thread.sleep(backoffTime);
					} catch(InterruptedException ex) {
						logCachingStatus("Terminating cache task due to sleep interruption.", Level.ERROR);
						return;
					}
					backoffTime = 2 * backoffTime;
					if(numberOfRetries++ >= WalrusProperties.IMAGE_CACHE_RETRY_LIMIT) {
						logCachingStatus("Terminating cache task with failure due to retry count exceeded.", Level.ERROR);
						return;
					}
					List<ImageCacheInfo> imageCacheInfos = null;
					EntityWrapper<ImageCacheInfo> db = EntityWrapper.get(ImageCacheInfo.class);
					try {
						ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo();
						searchImageCacheInfo.setInCache(true);
						imageCacheInfos = db.queryEscape(searchImageCacheInfo);					
						if(imageCacheInfos == null || imageCacheInfos.size() == 0) {
							logCachingStatus("Terminating cache task with failure due to insufficient cache space and no images to flush.", Level.ERROR);
							return;
						} else {
							//Sort by use count (embedded into the comparison function for ImageCacheInfo
							Collections.sort(imageCacheInfos);						
						}
					} catch(Exception e) {
						logCachingStatus("Exception checking image cache metadata:" + e.getMessage(), Level.ERROR);
						LOG.error("Exception checking image cache metadata:" + e.getMessage(), e);
					} finally {
						db.rollback();						
					}
					
					try {
						//Check spaceNeeded -- set by tryToCache as the image size
						if(spaceNeeded > 0) {
							ArrayList<ImageCacheInfo> imagesToFlush = new ArrayList<ImageCacheInfo>();
							//Changed spaceNeeded semantics in 'tryToCache' to properly present the actual space needed to be freed
							long tryToFree = spaceNeeded;
							for(ImageCacheInfo imageCacheInfo : imageCacheInfos) {
								if(tryToFree <= 0) {
									break;
								}
								long imageSize = imageCacheInfo.getSize();
								tryToFree -= imageSize;
								imagesToFlush.add(imageCacheInfo);
							}
							if(imagesToFlush.size() == 0) {
								//No images to flush, cannot fit image.
								logCachingStatus("Unable to flush any existing images. None found.", null);
								return;
							}
							
							if(tryToFree > 0){
								//Flushing images will not free enough space. Abort without actually flushing.
								logCachingStatus("Unabled to free enough cache space for image. Aborting without flushing any images. Needed additional " + tryToFree + " bytes", null);
								return;
							} else {
								//Flush the images to make space.
								logCachingStatus("Flushing cached images to make space for new image", Level.DEBUG);
								for(ImageCacheInfo imageCacheInfo : imagesToFlush) {
									flushCachedImage(imageCacheInfo.getBucketName(), imageCacheInfo.getManifestName());
								}
							}
						} else {
							//SpaceNeeded is negative or zero. -1 is uninitialized, but tryToCache returned failure. There was space, but still failure.
							//logCachingStatus("Terminating cache task with failure due to not enough cache space and cannot flush enough images to make space.", null);
							logCachingStatus("Terminating cache task with failure not related to size", null);
							return;
						}
					} catch(Exception ex) {
						logCachingStatus(" Unable to flush previously cached image: " + ex.getMessage(), Level.ERROR);
						LOG.error("Unable to flush previously cached image.", ex);
					}
					
				} //end try attempt while-loop
				
				try {
					logCachingStatus(" Cleaning up temporary image artifacts. decryptedImage " + decryptedImageName + " and tarred image:" + tarredImageName, Level.DEBUG); 
					storageManager.deleteAbsoluteObject(decryptedImageName);
					storageManager.deleteAbsoluteObject(tarredImageName);
					
					EntityWrapper<ImageCacheInfo> db = EntityWrapper.get(ImageCacheInfo.class);
					try {
						ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
						List<ImageCacheInfo> foundImageCacheInfos = db.queryEscape(searchImageCacheInfo);
						if(foundImageCacheInfos.size() > 0) {
							ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
							foundImageCacheInfo.setImageName(imageKey);
							foundImageCacheInfo.setInCache(true);
							foundImageCacheInfo.setSize(unencryptedSize);
							db.commit();
						} else {
							db.rollback();
							logCachingStatus(" Terminating caching with failure. Could not expand image" + decryptedImageName, null);
						}					
					} finally {
						db.rollback();
					}
				} catch (Exception ex) {
					LOG.error("Terminating with failure on exception", ex);
					logCachingStatus(" Terminating with failure Exception: " + ex.getMessage(), Level.ERROR);
				}
			} finally {
				//Ensure this is always called on exit
				notifyWaiters();
			}
		}
	}
	
	/**
	 * Returns true if a caching task is in progress. False otherwise. Does not consider
	 * if the object is cached, just in progress.
	 * @param bucket
	 * @param key
	 * @return
	 */
	private boolean cachingInProgress(String bucket, String key) {
		return imageCachers.containsKey(bucket + key);
	}
	
	/**
	 * Deletes all artifacts associated with the image name, tarball, crypt, etc.
	 * Does not ensure exclusive access, so caller must ensure no race conditions
	 * @param imageName
	 */
	private void deleteAllArtifacts(String bucketName, String imageName) {
		String[] suffixes = { "", ".crypt.gz", ".tar", ".tgz" };
		String name = null;
		for(String suffix : suffixes) {
			name = imageName + suffix;
			try {
				//storageManager.deleteAbsoluteObject(name);
				storageManager.deleteObject(bucketName, name);				
			} catch (IOException e) {
				LOG.warn("Failed to delete artifact: " + name);
			}
		}
	}
	
	private String getTaskId(String bucketName, String manifestKey) {
		try {
			ImageCacher c = imageCachers.get(bucketName + manifestKey);
			return String.valueOf(c.myThreadId);
		} catch(Exception e) {
			LOG.error("Failed to find task ID for: " + bucketName + "/" + manifestKey);
			return null;
		}
	}

	/**
	 * Returns a decrypted image file to client or returns an error if it cannot be cached or found.
	 * @param request
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public GetDecryptedImageResponseType getDecryptedImage(GetDecryptedImageType request) throws EucalyptusCloudException {
		GetDecryptedImageResponseType reply = (GetDecryptedImageResponseType) request.getReply();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		Context ctx = Contexts.lookup();
		final String correlationId = ctx.getCorrelationId();		
		Account account = ctx.getAccount();
		final String accountNumber = account.getAccountNumber();
		logWithContext("Processing GetDecryptedImage request for " + bucketName + "/" + objectKey, Level.INFO, correlationId, accountNumber);
		
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo bucketInfo = new BucketInfo(bucketName);
			List<BucketInfo> bucketList = db.queryEscape(bucketInfo);
			if (bucketList.size() > 0) {
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
				List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
				if(objectInfos.size() > 0)  {
					ObjectInfo objectInfo = objectInfos.get(0);

					logWithContext("Found object for caching: " + 
							objectInfo.getBucketName() + "/" + objectInfo.getObjectKey() + " version: " + (objectInfo.getVersionId() == null ? "null" : objectInfo.getVersionId()), null, correlationId, accountNumber);

					//Ensure proper privileges
					if(ctx.hasAdministrativePrivileges() || (
							objectInfo.canRead(account.getAccountNumber()) &&
							Lookups.checkPrivilege(PolicySpec.S3_GETOBJECT,
									PolicySpec.VENDOR_S3,
									PolicySpec.S3_RESOURCE_OBJECT,
									PolicySpec.objectFullName(bucketName, objectKey),
									objectInfo.getOwnerId()))) {
						db.commit();
						EucaSemaphore semaphore = EucaSemaphoreDirectory.getSemaphore(bucketName + "/" + objectKey);
						try { //ensure semaphore release
							try {
								semaphore.acquire();
							} catch(InterruptedException ex) {
								throw new WalrusException("semaphore could not be acquired");
							}
							EntityWrapper<ImageCacheInfo> db2 = EntityWrapper.get(ImageCacheInfo.class);
							try {
								ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, objectKey);
								List<ImageCacheInfo> foundImageCacheInfos = db2.queryEscape(searchImageCacheInfo);
								if(foundImageCacheInfos.size() > 0) {
									ImageCacheInfo imageCacheInfo = foundImageCacheInfos.get(0);
									if(imageCacheInfo.getInCache() && (!storageManager.objectExists(bucketName, imageCacheInfo.getImageName()))) {
										//Remove any and all left-over artifacts from failed cache attempt.
										this.deleteAllArtifacts(bucketName, imageCacheInfo.getImageName());
										db2.delete(imageCacheInfo);
										db2.commit();
										logWithContext("Deleted cache entry: " + bucketName + "/" + objectKey, null, correlationId, accountNumber);
										db2 = EntityWrapper.get(ImageCacheInfo.class);
										foundImageCacheInfos = db2.queryEscape(searchImageCacheInfo);
									}
								}
								if((foundImageCacheInfos.size() == 0) || (!imageCachers.containsKey(bucketName + objectKey))) {
									db2.commit();
									//issue a cache request
									logWithContext("No existing cache entries found or in-progress tasks, initiating caching of image " + bucketName + "/" + objectKey + ".", null, correlationId, accountNumber);
									cacheImage(bucketName, objectKey, account, ctx.hasAdministrativePrivileges(), correlationId);
									//query db again
									db2 = EntityWrapper.get(ImageCacheInfo.class);
									foundImageCacheInfos = db2.queryEscape(searchImageCacheInfo);
								}

								ImageCacheInfo foundImageCacheInfo = null;
								if(foundImageCacheInfos.size() > 0) {
									foundImageCacheInfo = foundImageCacheInfos.get(0);
								}

								db2.commit();

								if((foundImageCacheInfo == null) || (!foundImageCacheInfo.getInCache())) {
									boolean cached = false;
									String taskId = null;
									WalrusMonitor monitor = imageMessenger.getMonitor(bucketName + "/" + objectKey);
									synchronized (monitor) {
										try {
											long lastCheckBytesCached = 0;
											int number_of_tries = 0;
											long totalBytesCached = 0;
											//Wait for the caching task to complete or time-out with no progress.
											while(!(cached = isCached(bucketName, objectKey)) && cachingInProgress(bucketName, objectKey) && number_of_tries <= WalrusProperties.IMAGE_CACHE_WAIT_RETRY_LIMIT) {

												totalBytesCached = checkCachingProgress(bucketName, objectKey); //negative return indicates not found or error
												if(totalBytesCached <= lastCheckBytesCached) {
													number_of_tries++;
												}

												//Set to the larger, actual cache amount cannot decrease, so -1 is removed this way
												lastCheckBytesCached = Math.max(totalBytesCached, lastCheckBytesCached);
												taskId = getTaskId(bucketName, objectKey);
												logWithContext("Caching in progress. Bytes cached so far for image " + bucketName + "/" + objectKey + " :" +  String.valueOf(lastCheckBytesCached) + " caching task ID: " + taskId, Level.DEBUG, correlationId, accountNumber);
												logWithContext("Caching in progress for " + bucketName + "/" + objectKey + " with caching task ID:" + taskId + " Waiting " + WalrusProperties.CACHE_PROGRESS_TIMEOUT + "ms for image to cache (" + 
														number_of_tries + " out of " + WalrusProperties.IMAGE_CACHE_WAIT_RETRY_LIMIT + ")", Level.DEBUG, correlationId, accountNumber);					

												//Wait to be awoken, or timeout.
												monitor.wait(WalrusProperties.CACHE_PROGRESS_TIMEOUT);									
											}
										} catch(Exception ex) {
											logWithContext("Failed on exception while waiting for image cache progress for image: " + bucketName + "/" + objectKey + ". Exception: " + ex.getMessage(), Level.ERROR, correlationId, accountNumber);
											LOG.error("Failed waiting for caching", ex);
											semaphore.release();
											semaphore = null;
											imageMessenger.removeMonitor(bucketName + "/" + objectKey);
											throw new WalrusException("monitor failure");
										}
									} //end synchronized block

									if(!cached) {
										logWithContext("Finished waiting to cache image: " + bucketName + "/" + objectKey + ". Caching not complete", Level.ERROR, correlationId, accountNumber);
										imageMessenger.removeMonitor(bucketName + "/" + objectKey);
										semaphore.release();
										semaphore = null;
										if(!imageCachers.containsKey(bucketName + objectKey)) {
											//caching not in progress. delete image cache row.
											logWithContext("No caching task in progress, so deleting Image Cache Info: " + bucketName + "/" + objectKey, null, correlationId, accountNumber);										
											db2 = EntityWrapper.get(ImageCacheInfo.class);
											try {
												foundImageCacheInfos = db2.queryEscape(searchImageCacheInfo);
												if(foundImageCacheInfos.size() > 0) {
													db2.delete(foundImageCacheInfos.get(0));
												}
												db2.commit();
											} catch(Exception e) {
												logWithContext("Failed to commit delete of cache record. May already be removed: " + e.getMessage(), Level.ERROR, correlationId, accountNumber);
											} finally {
												db2.rollback();
											}
										} else {
											logWithContext("Caching task still in progress: " + bucketName + "/" + objectKey + " . Try run instances request again in a while...", null, correlationId, accountNumber);
										}
										throw new NoSuchEntityException("Caching failure: " + bucketName + "/" + objectKey);
									} else {
										logWithContext("Finished waiting to cache image: " + bucketName + "/" + objectKey + ". Caching completed", null, correlationId, accountNumber);
									}
									//caching may have modified the db. repeat the query
									db2 = EntityWrapper.get(ImageCacheInfo.class);
									try {
										foundImageCacheInfos = db2.queryEscape(searchImageCacheInfo);
										if(foundImageCacheInfos.size() > 0) {
											foundImageCacheInfo = foundImageCacheInfos.get(0);
											foundImageCacheInfo.setUseCount(foundImageCacheInfo.getUseCount() + 1);
											if(!foundImageCacheInfo.getInCache()) {
												logWithContext("Image: " + bucketName + "/" + objectKey + " metadata indicates not in cache. This is unexpected. Returning an error to the client", Level.ERROR, correlationId, accountNumber);
												throw new NoSuchEntityException(objectKey);
											} else {
												logWithContext("Cache check ok for image: " + bucketName + "/" + objectKey + ". Preparing response to client", null, correlationId, accountNumber);
											}
										} else {
											semaphore.release();
											semaphore = null;
											logWithContext("Image metadata not found. Unexpected error. Image: " + bucketName + "/" + objectKey + ". Returning failure to client", Level.ERROR, correlationId, accountNumber);
											throw new NoSuchEntityException(objectKey);
										}
										db2.commit();
									} catch (RollbackException re) {
										logWithContext("Failed to commit ImageCacheInfo UseCount, record may already be updated or removed: " + re.getMessage(), Level.DEBUG, correlationId, accountNumber);
									} finally {
										db2.rollback();
									}
								}

								Long unencryptedSize = foundImageCacheInfo.getSize();
								String imageKey = foundImageCacheInfo.getImageName();						
								reply.setSize(unencryptedSize);
								reply.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
								reply.setEtag("");

								logWithContext("GetDecryptedImage successful for image: " + bucketName + "/" + objectKey + ". Sending image to client", null, correlationId, accountNumber);
								DefaultHttpResponse httpResponse = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.OK ); 
								storageManager.sendObject(request, httpResponse, bucketName, imageKey, unencryptedSize, null, 
										DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN + ".000Z"), 
										objectInfo.getContentType(), objectInfo.getContentDisposition(), request.getIsCompressed(), null, null);                            
								semaphore.release();
								semaphore = null;
								imageMessenger.removeMonitor(bucketName + "/" + objectKey);
							} finally {
								db2.rollback();							
							}
							return reply;
						} finally {
							//Expectation is that if semaphore is released cleanly it will be set to null
							if(semaphore != null) {
								semaphore.release();
							}
						}
					} else {
						logWithContext("GetDecryptedImage failed for image: " + bucketName + "/" + objectKey + ". Access is denied.", null, correlationId, accountNumber);
						throw new AccessDeniedException("Key", objectKey);
					}

				} else {
					logWithContext("GetDecryptedImage failed for image: " + bucketName + "/" + objectKey + ". No such object found.", null, correlationId, accountNumber);
					throw new NoSuchEntityException(objectKey);
				}
			} else {
				logWithContext("GetDecryptedImage failed for image: " + bucketName + "/" + objectKey + ". No such bucket found.", null, correlationId, accountNumber);
				throw new NoSuchBucketException(bucketName);
			}
		} finally {
			db.rollback();
		}
	}

	public CheckImageResponseType checkImage(CheckImageType request) throws EucalyptusCloudException {
		CheckImageResponseType reply = (CheckImageResponseType) request.getReply();
		reply.setSuccess(false);
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		final String correlationId = ctx.getCorrelationId();
		logWithContext("Processing CheckImage request for " + bucketName + "/" + objectKey, Level.INFO, correlationId, account.getAccountNumber());

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		BucketInfo bucket = null;
		try {
			bucket = db.getUniqueEscape(bucketInfo);
		} catch(Exception t) {
			throw new WalrusException("Unable to get bucket", t);
		}

		if (bucket != null) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);
				if(ctx.hasAdministrativePrivileges() || (
						objectInfo.canRead(account.getAccountNumber()) &&
						Lookups.checkPrivilege(PolicySpec.S3_GETOBJECT,
								PolicySpec.VENDOR_S3,
								PolicySpec.S3_RESOURCE_OBJECT,
								PolicySpec.objectFullName(bucketName, objectKey),
								objectInfo.getOwnerId()))) {
					db.commit();
					checkManifest(bucketName, objectKey, account);
					reply.setSuccess(true);
					return reply;
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey);
				}
			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
	}

	public CacheImageResponseType cacheImage(CacheImageType request) throws EucalyptusCloudException {
		CacheImageResponseType reply = (CacheImageResponseType) request.getReply();
		reply.setSuccess(false);
		String bucketName = request.getBucket();
		String manifestKey = request.getKey();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		final String correlationId = ctx.getCorrelationId();
		final String accountNumber = account.getAccountNumber();
		logWithContext("Processing CacheImage request for " + bucketName + "/" + manifestKey, null, correlationId, accountNumber);

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo bucketInfo = new BucketInfo(bucketName);
			List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

			if (bucketList.size() > 0) {
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, manifestKey);
				List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
				if(objectInfos.size() > 0)  {
					ObjectInfo objectInfo = objectInfos.get(0);

					if(ctx.hasAdministrativePrivileges() || (
							objectInfo.canRead(account.getAccountNumber()) &&
							Lookups.checkPrivilege(PolicySpec.S3_GETOBJECT,
									PolicySpec.VENDOR_S3,
									PolicySpec.S3_RESOURCE_OBJECT,
									PolicySpec.objectFullName( bucketName, manifestKey ),
									objectInfo.getOwnerId()))) {
						EntityWrapper<ImageCacheInfo> db2 = EntityWrapper.get(ImageCacheInfo.class);
						try {
							ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
							List<ImageCacheInfo> foundImageCacheInfos = db2.queryEscape(searchImageCacheInfo);
							if(!imageCachers.containsKey(bucketName + manifestKey)) {

								if(foundImageCacheInfos.size() > 0) {
									ImageCacheInfo cacheInfo = foundImageCacheInfos.get(0);
									if(!cacheInfo.getInCache()) {
										//try again
										db2.delete(cacheInfo);
									}
								}
								db2.commit();
								logWithContext("No caching task found for image: " + bucketName + "/" + manifestKey + " Initiating caching task.", Level.DEBUG, correlationId, accountNumber); 
								cacheImage(bucketName, manifestKey, account, Contexts.lookup( ).hasAdministrativePrivileges( ), correlationId);
								reply.setSuccess(true);
							} else {
								logWithContext("Caching in progress for image " + bucketName + "/" + manifestKey + " nothing to do.", null, correlationId, accountNumber); 
								db2.rollback();
							}
						} finally {
							db2.rollback();
						}
						db.commit( );
						return reply;
					} else {
						logWithContext("CacheImage failed for image " + bucketName + "/" + manifestKey + " due to Access Denied.", Level.ERROR, correlationId, accountNumber);  
						throw new AccessDeniedException("Key", manifestKey);
					}

				} else {
					logWithContext("CacheImage failed for image " + bucketName + "/" + manifestKey + " because object not found.", Level.ERROR, correlationId, accountNumber); 
					throw new NoSuchEntityException(manifestKey);

				}
			} else {
				logWithContext("CachImage failed for image " + bucketName + "/" + manifestKey + " because bucket not found.", Level.ERROR, correlationId, accountNumber); 
				throw new NoSuchBucketException(bucketName);
			}
		} finally {
			if(db.isActive()) {
				db.rollback();
			}
		}
	}

	public FlushCachedImageResponseType flushCachedImage(FlushCachedImageType request) throws EucalyptusCloudException {
		FlushCachedImageResponseType reply = (FlushCachedImageResponseType) request.getReply();

		String bucketName = request.getBucket();
		String manifestKey = request.getKey();
		Context ctx = Contexts.lookup();
		final String correlationId = ctx.getCorrelationId();
		Account account = ctx.getAccount();
		logWithContext("Processing FlushCachedImage request for " + bucketName + "/" + manifestKey, Level.INFO, correlationId, account.getAccountNumber() );

		EntityWrapper<ImageCacheInfo> db = EntityWrapper.get(ImageCacheInfo.class);
		try {
			ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
			List<ImageCacheInfo> foundImageCacheInfos = db.queryEscape(searchImageCacheInfo);
			
			if(foundImageCacheInfos.size() > 0) {
				ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
				if(foundImageCacheInfo.getInCache() && (imageCachers.get(bucketName + manifestKey) == null)) {
					//check that there are no operations in progress and then flush cache and delete image file
					db.commit();
					ImageCacheFlusher imageCacheFlusher = new ImageCacheFlusher(bucketName, manifestKey);
					Threads.lookup(Walrus.class, WalrusImageManager.ImageCacheFlusher.class).limitTo(10).submit(imageCacheFlusher);
				} else {
					throw new WalrusException("not in cache");
				}
			} else {
				throw new NoSuchEntityException(bucketName + manifestKey);
			}
			return reply;
		} finally {
			db.rollback();
		}
	}

	public ValidateImageResponseType validateImage(ValidateImageType request) throws EucalyptusCloudException {
		ValidateImageResponseType reply = (ValidateImageResponseType) request.getReply();
		String bucketName = request.getBucket();
		String manifestKey = request.getKey();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		final String correlationId = ctx.getCorrelationId();
		logWithContext("Processing ValidateImage request for " + bucketName + "/" + manifestKey, Level.INFO, correlationId, account.getAccountNumber());
		
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo bucketInfo = new BucketInfo(bucketName);
			List<BucketInfo> bucketList = db.queryEscape(bucketInfo);
			if (bucketList.size() > 0) {
				BucketInfo bucket = bucketList.get(0);
				BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
				ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, manifestKey);
				searchObjectInfo.setDeleted(false);
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
				if (objectInfos.size() > 0) {
					ObjectInfo objectInfo = objectInfos.get(0);
					if (ctx.hasAdministrativePrivileges() || (
							objectInfo.canRead(account.getAccountNumber()) &&
							Lookups.checkPrivilege(PolicySpec.S3_GETOBJECT,
									PolicySpec.VENDOR_S3,
									PolicySpec.S3_RESOURCE_OBJECT,
									PolicySpec.objectFullName(bucketName, manifestKey),
									objectInfo.getOwnerId()))) {
						//validate manifest
						validateManifest(bucketName, manifestKey, account.getAccountNumber());
						db.commit();
					} else {
						throw new AccessDeniedException("Key", manifestKey, logData);
					}
				} else {
					throw new NoSuchEntityException(manifestKey, logData);
				}
			} else {
				throw new NoSuchBucketException(bucketName);
			}			
			return reply;
		} finally {
			db.rollback();
		}
	}
}
