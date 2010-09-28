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
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
package edu.ucsb.eucalyptus.cloud.ws;
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.util.*;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedInput;

import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.auth.crypto.Digest;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
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
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import edu.ucsb.eucalyptus.cloud.BucketLogData;
import edu.ucsb.eucalyptus.cloud.DecryptionFailedException;
import edu.ucsb.eucalyptus.cloud.NoSuchBucketException;
import edu.ucsb.eucalyptus.cloud.NoSuchEntityException;
import edu.ucsb.eucalyptus.cloud.NotAuthorizedException;
import edu.ucsb.eucalyptus.cloud.entities.BucketInfo;
import edu.ucsb.eucalyptus.cloud.entities.ImageCacheInfo;
import edu.ucsb.eucalyptus.cloud.entities.ObjectInfo;
import edu.ucsb.eucalyptus.msgs.CacheImageResponseType;
import edu.ucsb.eucalyptus.msgs.CacheImageType;
import edu.ucsb.eucalyptus.msgs.CheckImageResponseType;
import edu.ucsb.eucalyptus.msgs.CheckImageType;
import edu.ucsb.eucalyptus.msgs.FlushCachedImageResponseType;
import edu.ucsb.eucalyptus.msgs.FlushCachedImageType;
import edu.ucsb.eucalyptus.msgs.GetDecryptedImageResponseType;
import edu.ucsb.eucalyptus.msgs.GetDecryptedImageType;
import edu.ucsb.eucalyptus.msgs.ValidateImageResponseType;
import edu.ucsb.eucalyptus.msgs.ValidateImageType;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.util.EucaSemaphore;
import edu.ucsb.eucalyptus.util.EucaSemaphoreDirectory;
import edu.ucsb.eucalyptus.util.WalrusDataMessenger;
import edu.ucsb.eucalyptus.util.WalrusMonitor;
import edu.ucsb.eucalyptus.util.XMLParser;

public class WalrusImageManager {
	private static Logger LOG = Logger.getLogger( WalrusImageManager.class );
	private static ConcurrentHashMap<String, ImageCacher> imageCachers = new ConcurrentHashMap<String, ImageCacher>();

	private StorageManager storageManager;
	private  WalrusDataMessenger imageMessenger;

	public WalrusImageManager(StorageManager storageManager, WalrusDataMessenger imageMessenger) {
		this.storageManager = storageManager;
		this.imageMessenger = imageMessenger;
	}

	private String decryptImage(String bucketName, String objectKey, String userId, boolean isAdministrator) throws EucalyptusCloudException {
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);


		if (bucketList.size() > 0) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);
				if(objectInfo.canRead(userId)) {
					String objectName = objectInfo.getObjectName();
					File file = new File(storageManager.getObjectPath(bucketName, objectName));
					XMLParser parser = new XMLParser(file);
					//Read manifest
					String imageKey = parser.getValue("//image/name");
					String encryptedKey = parser.getValue("//ec2_encrypted_key");
					String encryptedIV = parser.getValue("//ec2_encrypted_iv");
					String signature = parser.getValue("//signature");


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
							for(User user:Users.listAllUsers( )) {
								for (X509Certificate cert : user.getAllX509Certificates()) {
									if(cert != null)
										verified = canVerifySignature(sigVerifier, cert, signature, verificationString);
									if(verified)
										break;
								}
							}
							if(!verified) {
								X509Certificate cert = SystemCredentialProvider.getCredentialProvider(Component.eucalyptus).getCertificate();
								if(cert != null)
									verified = canVerifySignature(sigVerifier, cert, signature, verificationString);
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
						User user = null;
						try {
							user = Users.lookupUser( userId );
						} catch ( NoSuchUserException e ) {
							throw new AccessDeniedException(userId,e);            
						}         
						try {
							for(X509Certificate cert : user.getAllX509Certificates()) {
								if(cert != null) {
									signatureVerified = canVerifySignature(sigVerifier, cert, signature, verificationString);
								}
								if(signatureVerified)
									break;
							}
						} catch(Exception ex) {
							db.rollback();
							LOG.error(ex, ex);
							throw new DecryptionFailedException("signature verification");
						}
						if(!signatureVerified) {
							try {
								X509Certificate cert = SystemCredentialProvider.getCredentialProvider(Component.eucalyptus).getCertificate();
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
					if(parts == null) 
						throw new DecryptionFailedException("Invalid manifest");
					ArrayList<String> qualifiedPaths = new ArrayList<String>();
					searchObjectInfo = new ObjectInfo();
					searchObjectInfo.setBucketName(bucketName);
					List<ObjectInfo> bucketObjectInfos = dbObject.query(searchObjectInfo);

					for (String part: parts) {
						for(ObjectInfo object : bucketObjectInfos) {
							if(part.equals(object.getObjectKey())) {
								qualifiedPaths.add(storageManager.getObjectPath(bucketName, object.getObjectName()));
							}
						}
					}
					//Assemble parts
					String encryptedImageKey = UUID.randomUUID().toString() + ".crypt.gz";//imageKey + "-" + Hashes.getRandom(5) + ".crypt.gz";
					String encryptedImageName = storageManager.getObjectPath(bucketName, encryptedImageKey);
					String decryptedImageKey = encryptedImageKey.substring(0, encryptedImageKey.lastIndexOf("crypt.gz")) + "tgz";

					String decryptedImageName = storageManager.getObjectPath(bucketName, decryptedImageKey);
					assembleParts(encryptedImageName, qualifiedPaths);
					//Decrypt key and IV

					byte[] key;
					byte[] iv;
					try {
						PrivateKey pk = SystemCredentialProvider.getCredentialProvider(
								Component.eucalyptus ).getPrivateKey();
						Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
						cipher.init(Cipher.DECRYPT_MODE, pk);
						String keyString = new String(cipher.doFinal(Hashes.hexToBytes(encryptedKey)));
						key = Hashes.hexToBytes(keyString);
						String ivString = new String(cipher.doFinal(Hashes.hexToBytes(encryptedIV)));
						iv = Hashes.hexToBytes(ivString);
					} catch(Exception ex) {
						db.rollback();
						LOG.error(ex, ex);
						throw new DecryptionFailedException("AES params");
					}

					//Unencrypt image
					try {
						Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
						IvParameterSpec salt = new IvParameterSpec(iv);
						SecretKey keySpec = new SecretKeySpec(key, "AES");
						cipher.init(Cipher.DECRYPT_MODE, keySpec, salt);
						decryptImage(encryptedImageName, decryptedImageName, cipher);
					} catch (Exception ex) {
						db.rollback();
						LOG.error(ex, ex);
						throw new DecryptionFailedException("decryption failed");
					}
					try {
						storageManager.deleteAbsoluteObject(encryptedImageName);
					} catch (Exception ex) {
						LOG.error(ex);
						throw new EucalyptusCloudException();
					}
					db.commit();
					return decryptedImageKey;
				}
			}
		}
		return null;
	}

	private boolean canVerifySignature(Signature sigVerifier, X509Certificate cert, String signature, String verificationString) throws Exception {
		PublicKey publicKey = cert.getPublicKey();
		sigVerifier.initVerify(publicKey);
		sigVerifier.update((verificationString).getBytes());
		return sigVerifier.verify(Hashes.hexToBytes(signature));
	}


	private void checkManifest(String bucketName, String objectKey, String userId) throws EucalyptusCloudException {
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		BucketInfo bucket = null;
		try {
			bucket = db.getUnique(bucketInfo);
		} catch(Throwable t) {
			throw new EucalyptusCloudException("Unable to get bucket: " + bucketName, t);
		}

		if (bucket != null) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);

				if(objectInfo.canRead(userId)) {
					String objectName = objectInfo.getObjectName();
					File file = new File(storageManager.getObjectPath(bucketName, objectName));
					XMLParser parser = new XMLParser(file);
					//Read manifest
					String encryptedKey = parser.getValue("//ec2_encrypted_key");
					String encryptedIV = parser.getValue("//ec2_encrypted_iv");
					String signature = parser.getValue("//signature");

					String image = parser.getXML("image");
					String machineConfiguration = parser.getXML("machine_configuration");

					User user = null;
					try {
						user = Users.lookupUser( userId );
					} catch ( NoSuchUserException e ) {
						throw new AccessDeniedException(userId,e);            
					}         
					boolean signatureVerified = false;

					Signature sigVerifier;
					try {
						sigVerifier = Signature.getInstance("SHA1withRSA");
					} catch (NoSuchAlgorithmException ex) {
						LOG.error(ex, ex);
						throw new DecryptionFailedException("SHA1withRSA not found");
					}

					try {
						X509Certificate cert = user.getX509Certificate( );
						PublicKey publicKey = cert.getPublicKey();
						sigVerifier.initVerify(publicKey);
						sigVerifier.update((machineConfiguration + image).getBytes());
						signatureVerified = sigVerifier.verify(Hashes.hexToBytes(signature));
					} catch(Exception ex) {
						db.rollback();
						LOG.error(ex, ex);
						throw new DecryptionFailedException("signature verification");
					}

					//check if Eucalyptus signed it
					if(!signatureVerified) {
						try {
							X509Certificate cert = SystemCredentialProvider.getCredentialProvider(Component.eucalyptus).getCertificate();
							PublicKey publicKey = cert.getPublicKey();
							sigVerifier.initVerify(publicKey);
							sigVerifier.update((machineConfiguration + image).getBytes());
							signatureVerified = sigVerifier.verify(Hashes.hexToBytes(signature));
						} catch(Exception ex) {
							db.rollback();
							LOG.error(ex, ex);
							throw new DecryptionFailedException("signature verification");
						}

					}
					if(!signatureVerified) {
						throw new NotAuthorizedException("Invalid signature");
					}
					//Decrypt key and IV

					byte[] key;
					byte[] iv;
					try {
						PrivateKey pk = SystemCredentialProvider.getCredentialProvider(Component.eucalyptus).getPrivateKey();
						Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
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

	private boolean isCached(String bucketName, String manifestKey) {
		EntityWrapper<ImageCacheInfo> db = WalrusControl.getEntityWrapper();
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
		try {
			ImageCacheInfo foundImageCacheInfo = db.getUnique(searchImageCacheInfo);
			db.commit();
			if(foundImageCacheInfo.getInCache())
				return true;
			else
				return false;
		} catch(Exception ex) {
			db.commit();
			return false;
		} 
	}

	private long checkCachingProgress(String bucketName, String manifestKey, long oldBytesRead) {
		EntityWrapper<ImageCacheInfo> db = WalrusControl.getEntityWrapper();
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
		try {
			ImageCacheInfo foundImageCacheInfo = db.getUnique(searchImageCacheInfo);
			String cacheImageKey = foundImageCacheInfo.getImageName().substring(0, foundImageCacheInfo.getImageName().lastIndexOf(".tgz"));
			long objectSize = storageManager.getObjectSize(bucketName, cacheImageKey);
			db.commit();
			if(objectSize > 0) {
				return objectSize - oldBytesRead;
			}
			return oldBytesRead;
		} catch (Exception ex) {
			db.commit();
			return oldBytesRead;
		}
	}

	private synchronized void cacheImage(String bucketName, String manifestKey, String userId, boolean isAdministrator) throws EucalyptusCloudException {

		EntityWrapper<ImageCacheInfo> db = WalrusControl.getEntityWrapper();
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
		List<ImageCacheInfo> imageCacheInfos = db.query(searchImageCacheInfo);
		String decryptedImageKey = null;
		if(imageCacheInfos.size() != 0) {
			ImageCacheInfo icInfo = imageCacheInfos.get(0);
			if(!icInfo.getInCache()) {
				decryptedImageKey = icInfo.getImageName();
			} else {
				db.commit();
				return;
			}
		}
		db.commit();
		//unzip, untar image in the background
		ImageCacher imageCacher = imageCachers.putIfAbsent(bucketName + manifestKey, new ImageCacher(bucketName, manifestKey, decryptedImageKey));
		if(imageCacher == null) {
			if(decryptedImageKey == null) {
				try {
					decryptedImageKey = decryptImage(bucketName, manifestKey, userId, isAdministrator);
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
				db = WalrusControl.getEntityWrapper();
				db.add(foundImageCacheInfo);
				db.commit();
			}
			imageCacher = imageCachers.get(bucketName + manifestKey);
			imageCacher.setDecryptedImageKey(decryptedImageKey);
			imageCacher.start();
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
		EntityWrapper<ImageCacheInfo> db = WalrusControl.getEntityWrapper();
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, objectKey);
		List<ImageCacheInfo> foundImageCacheInfos = db.query(searchImageCacheInfo);

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

	private void validateManifest(String bucketName, String objectKey, String userId) throws EucalyptusCloudException {
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		BucketInfo bucket = null;
		try {
			bucket = db.getUnique(bucketInfo);
		} catch(Throwable t) {
			throw new EucalyptusCloudException("Unable to get bucket: " + bucketName, t);
		}

		if (bucket != null) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);

				if(objectInfo.canRead(userId)) {
					String objectName = objectInfo.getObjectName();
					File file = new File(storageManager.getObjectPath(bucketName, objectName));
					XMLParser parser = new XMLParser(file);
					String image = parser.getXML("image");
					String machineConfiguration = parser.getXML("machine_configuration");
					String verificationString = machineConfiguration + image;
					FileInputStream inStream = null;

					FileInputStream fileInputStream = null;
					try {
						PrivateKey pk = SystemCredentialProvider.getCredentialProvider(Component.eucalyptus).getPrivateKey();
						Signature sigCloud = Signature.getInstance("SHA1withRSA");
						sigCloud.initSign(pk);
						sigCloud.update(verificationString.getBytes());
						String signature = new String(Hashes.bytesToHex(sigCloud.sign()));
						//TODO: refactor
						DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance( ).newDocumentBuilder( );
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
								throw new EucalyptusCloudException(e);
							} finally {
								try {
									inStream.close();
								} catch (IOException e) {
									LOG.error(e);
									throw new EucalyptusCloudException(e);
								}
							}
							String md5 = Hashes.bytesToHex(digest.digest());
							objectInfo.setEtag(md5);
							objectInfo.setSize(totalBytesRead);
						} catch (FileNotFoundException e) {
							LOG.error(e, e);
							throw new EucalyptusCloudException(e);
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
						throw new EucalyptusCloudException("Unable to sign manifest: " + bucketName + "/" + objectKey);
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
		imageCacheFlusher.start();
	}

	private class ImageCacheFlusher extends Thread {
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


	private class ImageCacher extends Thread {

		private String bucketName;
		private String manifestKey;
		private String decryptedImageKey;
		private boolean imageSizeExceeded;
		private long spaceNeeded;

		public ImageCacher(String bucketName, String manifestKey, String decryptedImageKey) {
			this.bucketName = bucketName;
			this.manifestKey = manifestKey;
			this.decryptedImageKey = decryptedImageKey;
			this.imageSizeExceeded = false;

		}

		public void setDecryptedImageKey(String key) {
			this.decryptedImageKey = key;
		}

		private long tryToCache(String decryptedImageName, String tarredImageName, String imageName) {
			Long unencryptedSize = 0L;
			boolean failed = false;
			try {
				if(!imageSizeExceeded) {
					LOG.info("Unzipping image: " + bucketName + "/" + manifestKey);
					unzipImage(decryptedImageName, tarredImageName);
					LOG.info("Untarring image: " + bucketName + "/" + manifestKey);
					unencryptedSize = untarImage(tarredImageName, imageName);
				} else {
					File imageFile = new File(imageName);
					if(imageFile.exists()) {
						unencryptedSize = imageFile.length();
					} else {
						LOG.error("Could not find image: " + imageName);
						imageSizeExceeded = false;
						return -1L;
					}

				}
				Long oldCacheSize = 0L;
				EntityWrapper<ImageCacheInfo> db = WalrusControl.getEntityWrapper();
				List<ImageCacheInfo> imageCacheInfos = db.query(new ImageCacheInfo());
				for(ImageCacheInfo imageCacheInfo: imageCacheInfos) {
					if(imageCacheInfo.getInCache()) {
						oldCacheSize += imageCacheInfo.getSize();
					}
				}
				db.commit();
				if((oldCacheSize + unencryptedSize) > (WalrusInfo.getWalrusInfo().getStorageMaxCacheSizeInMB() * WalrusProperties.M)) {
					LOG.error("Maximum image cache size exceeded when decrypting " + bucketName + "/" + manifestKey);
					failed = true;
					imageSizeExceeded = true;
					spaceNeeded = unencryptedSize;
				}
			} catch(Exception ex) {
				LOG.warn(ex);
				//try to evict an entry and try again
				failed = true;
			}
			if(failed) {
				if(!imageSizeExceeded) {
					try {
						storageManager.deleteAbsoluteObject(tarredImageName);
						storageManager.deleteAbsoluteObject(imageName);
					} catch (Exception exception) {
						LOG.error(exception);
					}
				}
				return -1L;
			}
			LOG.info("Cached image: " + bucketName + "/" + manifestKey + " size: " + String.valueOf(unencryptedSize));
			return unencryptedSize;
		}

		private void notifyWaiters() {
			WalrusMonitor monitor = imageMessenger.getMonitor(bucketName + "/" + manifestKey);
			synchronized (monitor) {
				monitor.notifyAll();
			}
			imageMessenger.removeMonitor(bucketName + "/" + manifestKey);
			imageCachers.remove(bucketName + manifestKey);
		}

		public void run() {
			//update status
			//wake up any waiting consumers
			String decryptedImageName = storageManager.getObjectPath(bucketName, decryptedImageKey);
			String imageName = decryptedImageName.substring(0, decryptedImageName.lastIndexOf(".tgz"));
			String tarredImageName = imageName + (".tar");
			String imageKey = decryptedImageKey.substring(0, decryptedImageKey.lastIndexOf(".tgz"));
			Long unencryptedSize;
			int numberOfRetries = 0;
			while((unencryptedSize = tryToCache(decryptedImageName, tarredImageName, imageName)) < 0) {
				try {
					Thread.sleep(WalrusProperties.IMAGE_CACHE_RETRY_TIMEOUT);
				} catch(InterruptedException ex) {
					notifyWaiters();
					return;
				}
				WalrusProperties.IMAGE_CACHE_RETRY_TIMEOUT = 2* WalrusProperties.IMAGE_CACHE_RETRY_TIMEOUT;
				if(numberOfRetries++ >= WalrusProperties.IMAGE_CACHE_RETRY_LIMIT) {
					notifyWaiters();
					return;
				}
				EntityWrapper<ImageCacheInfo> db = WalrusControl.getEntityWrapper();
				ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo();
				searchImageCacheInfo.setInCache(true);
				List<ImageCacheInfo> imageCacheInfos = db.query(searchImageCacheInfo);
				if(imageCacheInfos.size() == 0) {
					LOG.error("No cached images found to flush. Unable to cache image. Please check the error log and the image cache size.");
					db.rollback();
					notifyWaiters();
					return;
				}
				Collections.sort(imageCacheInfos);
				db.commit();
				try {
					if(spaceNeeded > 0) {
						ArrayList<ImageCacheInfo> imagesToFlush = new ArrayList<ImageCacheInfo>();
						long tryToFree = spaceNeeded;
						for(ImageCacheInfo imageCacheInfo : imageCacheInfos) {
							if(tryToFree <= 0)
								break;
							long imageSize = imageCacheInfo.getSize();
							tryToFree -= imageSize;
							imagesToFlush.add(imageCacheInfo);
						}
						if(imagesToFlush.size() == 0) {
							LOG.error("Unable to flush existing images. Sorry.");
							notifyWaiters();
							return;
						}
						for(ImageCacheInfo imageCacheInfo : imagesToFlush) {
							flushCachedImage(imageCacheInfo.getBucketName(), imageCacheInfo.getManifestName());
						}
					} else {
						LOG.error("Unable to cache image. Unable to flush existing images.");
						notifyWaiters();
						return;
					}
				} catch(Exception ex) {
					LOG.error(ex);
					LOG.error("Unable to flush previously cached image. Please increase your image cache size");
					notifyWaiters();
				}
			}
			try {
				storageManager.deleteAbsoluteObject(decryptedImageName);
				storageManager.deleteAbsoluteObject(tarredImageName);

				EntityWrapper<ImageCacheInfo>db = WalrusControl.getEntityWrapper();
				ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
				List<ImageCacheInfo> foundImageCacheInfos = db.query(searchImageCacheInfo);
				if(foundImageCacheInfos.size() > 0) {
					ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
					foundImageCacheInfo.setImageName(imageKey);
					foundImageCacheInfo.setInCache(true);
					foundImageCacheInfo.setSize(unencryptedSize);
					db.commit();
					//wake up waiters
					notifyWaiters();
				} else {
					db.rollback();
					LOG.error("Could not expand image" + decryptedImageName);
				}
			} catch (Exception ex) {
				LOG.error(ex);
			}
		}
	}

	private void unzipImage(String decryptedImageName, String tarredImageName) throws Exception {
		GZIPInputStream in = new GZIPInputStream(new FileInputStream(new File(decryptedImageName)));
		File outFile = new File(tarredImageName);
		ReadableByteChannel inChannel = Channels.newChannel(in);
		FileOutputStream fileOutputStream = new FileOutputStream(outFile);
		WritableByteChannel outChannel = fileOutputStream.getChannel();

		ByteBuffer buffer = ByteBuffer.allocate(102400);
		try {
			while (inChannel.read(buffer) != -1) {
				buffer.flip();
				outChannel.write(buffer);
				buffer.clear();
			}
		} catch(IOException ex) {
			throw ex;
		} finally {
			outChannel.close();
			fileOutputStream.close();
			inChannel.close();
			in.close();
		}		
	}

	private long untarImage(String tarredImageName, String imageName) throws Exception {
		/*TarInputStream in = new TarInputStream(new FileInputStream(new File(tarredImageName)));
       File outFile = new File(imageName);
       BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));

       TarEntry tEntry = in.getNextEntry();
       assert(!tEntry.isDirectory());

       in.copyEntryContents(out);
       out.close();
       in.close();
       return outFile.length();*/

		//Workaround because TarInputStream is broken
		Tar tarrer = new Tar();
		tarrer.untar(tarredImageName, imageName);
		File outFile = new File(imageName);
		if(outFile.exists())
			return outFile.length();
		else
			throw new EucalyptusCloudException("Could not untar image " + imageName);
	}

	private class StreamConsumer extends Thread
	{
		private InputStream is;
		private File file;

		public StreamConsumer(InputStream is) {
			this.is = is;
		}

		public StreamConsumer(InputStream is, File file) {
			this(is);
			this.file = file;
		}

		public void run()
		{
			BufferedOutputStream outStream = null;
			try
			{
				BufferedInputStream inStream = new BufferedInputStream(is);
				if(file != null) {
					outStream = new BufferedOutputStream(new FileOutputStream(file));
				}
				byte[] bytes = new byte[WalrusProperties.IO_CHUNK_SIZE];
				int bytesRead;
				while((bytesRead = inStream.read(bytes)) > 0) {
					if(outStream != null) {
						outStream.write(bytes, 0, bytesRead);
					}
				}
				if(outStream != null)
					outStream.close();
			} catch (IOException ex)
			{
				if(outStream != null)
					try {
						outStream.close();
					} catch (IOException e) {
						LOG.error(e);
					}
					LOG.error(ex);
			}
		}
	}

	private class Tar {
		public void untar(String tarFile, String outFile) {
			try
			{
				Runtime rt = Runtime.getRuntime();
				Process proc = rt.exec(new String[]{ "/bin/tar", "xfO", tarFile});
				StreamConsumer error = new StreamConsumer(proc.getErrorStream());
				StreamConsumer output = new StreamConsumer(proc.getInputStream(), new File(outFile));
				error.start();
				output.start();
				int exitVal = proc.waitFor();
				output.join();
			} catch (Throwable t) {
				LOG.error(t);
			}
		}
	}

	private void decryptImage(final String encryptedImageName, final String decryptedImageName, final Cipher cipher) throws Exception {
		LOG.info("Decrypting image: " + decryptedImageName);
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(decryptedImageName)));
		File inFile = new File(encryptedImageName);
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));

		int bytesRead = 0;
		byte[] bytes = new byte[8192];

		while((bytesRead = in.read(bytes)) > 0) {
			byte[] outBytes = cipher.update(bytes, 0, bytesRead);
			out.write(outBytes);
		}
		byte[] outBytes = cipher.doFinal();
		out.write(outBytes);
		in.close();
		out.close();
		LOG.info("Done decrypting: " + decryptedImageName);
	}

	private void assembleParts(final String name, List<String> parts) {
		FileOutputStream fileOutputStream = null;
		FileInputStream fileInputStream = null;
		try {
			fileOutputStream = new FileOutputStream(new File(name));
			FileChannel out = fileOutputStream.getChannel();
			for (String partName: parts) {
				fileInputStream = new FileInputStream(new File(partName));
				FileChannel in = fileInputStream.getChannel();
				in.transferTo(0, in.size(), out);
				in.close();
				fileInputStream.close();
			}
			out.close();
			fileOutputStream.close();
		} catch (Exception ex) {
			LOG.error(ex);
		} finally {
			if(fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					LOG.error(e);
				}
			}
			if(fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					LOG.error(e);
				}
			}
		}
	}



	public GetDecryptedImageResponseType getDecryptedImage(GetDecryptedImageType request) throws EucalyptusCloudException {
		GetDecryptedImageResponseType reply = (GetDecryptedImageResponseType) request.getReply();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);
		if (bucketList.size() > 0) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);

				if(objectInfo.canRead(userId) || request.isAdministrator() ) {
					db.commit();
					EucaSemaphore semaphore = EucaSemaphoreDirectory.getSemaphore(bucketName + "/" + objectKey);
					try {
						semaphore.acquire();
					} catch(InterruptedException ex) {
						throw new EucalyptusCloudException("semaphore could not be acquired");
					}
					EntityWrapper<ImageCacheInfo> db2 = WalrusControl.getEntityWrapper();
					ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, objectKey);
					List<ImageCacheInfo> foundImageCacheInfos = db2.query(searchImageCacheInfo);
					if(foundImageCacheInfos.size() > 0) {
						ImageCacheInfo imageCacheInfo = foundImageCacheInfos.get(0);
						if(imageCacheInfo.getInCache() && 
								(!storageManager.objectExists(bucketName, imageCacheInfo.getImageName()))) {
							db2.delete(imageCacheInfo);
							db2.commit();
							db2 = WalrusControl.getEntityWrapper();
							foundImageCacheInfos = db2.query(searchImageCacheInfo);
						}						
					}
					if((foundImageCacheInfos.size() == 0) || 
							(!imageCachers.containsKey(bucketName + objectKey))) {
						db2.commit();
						//issue a cache request
						LOG.info("Image " + bucketName + "/" + objectKey + " not found in cache. Issuing cache request (might take a while...)");
						cacheImage(bucketName, objectKey, userId, request.isAdministrator());
						//query db again
						db2 = WalrusControl.getEntityWrapper();
						foundImageCacheInfos = db2.query(searchImageCacheInfo);
					}
					ImageCacheInfo foundImageCacheInfo = null;
					if(foundImageCacheInfos.size() > 0)
						foundImageCacheInfo = foundImageCacheInfos.get(0);
					db2.commit();
					if((foundImageCacheInfo == null) || 
							(!foundImageCacheInfo.getInCache())) {
						boolean cached = false;
						WalrusMonitor monitor = imageMessenger.getMonitor(bucketName + "/" + objectKey);
						synchronized (monitor) {
							try {
								long bytesCached = 0;
								int number_of_tries = 0;
								do {
									LOG.info("Waiting " + WalrusProperties.CACHE_PROGRESS_TIMEOUT + "ms for image to cache (" + number_of_tries + " out of " + WalrusProperties.IMAGE_CACHE_RETRY_LIMIT + ")");
									monitor.wait(WalrusProperties.CACHE_PROGRESS_TIMEOUT);
									if(isCached(bucketName, objectKey)) {
										cached = true;
										break;
									}
									long newBytesCached = checkCachingProgress(bucketName, objectKey, bytesCached);
									boolean is_caching = (newBytesCached - bytesCached) > 0 ? true : false;

									if (!is_caching && (number_of_tries++ >= WalrusProperties.IMAGE_CACHE_RETRY_LIMIT))
										break;

									bytesCached = newBytesCached;
									if(is_caching) {
										LOG.info("Bytes cached so far for image " + bucketName + "/" + objectKey + " :" +  String.valueOf(bytesCached));
									}
								} while(true);
							} catch(Exception ex) {
								LOG.error(ex);
								semaphore.release();
								throw new EucalyptusCloudException("monitor failure");
							}
						}
						if(!cached) {
							LOG.error("Tired of waiting to cache image: " + bucketName + "/" + objectKey + " giving up");
							semaphore.release();
							throw new EucalyptusCloudException("caching failure");
						}
						//caching may have modified the db. repeat the query
						db2 = WalrusControl.getEntityWrapper();
						foundImageCacheInfos = db2.query(searchImageCacheInfo);
						if(foundImageCacheInfos.size() > 0) {
							foundImageCacheInfo = foundImageCacheInfos.get(0);
							foundImageCacheInfo.setUseCount(foundImageCacheInfo.getUseCount() + 1);
							assert(foundImageCacheInfo.getInCache());
						} else {
							db2.rollback();
							semaphore.release();
							throw new NoSuchEntityException(objectKey);
						}
						db2.commit();
					}

					Long unencryptedSize = foundImageCacheInfo.getSize();
					String imageKey = foundImageCacheInfo.getImageName();
					reply.setSize(unencryptedSize);
					reply.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
					reply.setEtag("");
					DefaultHttpResponse httpResponse = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.OK ); 
					storageManager.sendObject(request, httpResponse, bucketName, imageKey, unencryptedSize, null, 
							DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN + ".000Z"), 
							objectInfo.getContentType(), objectInfo.getContentDisposition(), request.getIsCompressed(), null, null);                            
					semaphore.release();
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

	public CheckImageResponseType checkImage(CheckImageType request) throws EucalyptusCloudException {
		CheckImageResponseType reply = (CheckImageResponseType) request.getReply();
		reply.setSuccess(false);
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		BucketInfo bucket = null;
		try {
			bucket = db.getUnique(bucketInfo);
		} catch(Throwable t) {
			throw new EucalyptusCloudException("Unable to get bucket", t);
		}

		if (bucket != null) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);
				if(objectInfo.canRead(userId)) {
					db.commit();
					checkManifest(bucketName, objectKey, userId);
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
		String userId = request.getUserId();


		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, manifestKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);

				if(objectInfo.canRead(userId)) {
					EntityWrapper<ImageCacheInfo> db2 = WalrusControl.getEntityWrapper();
					ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
					List<ImageCacheInfo> foundImageCacheInfos = db2.query(searchImageCacheInfo);
					db2.commit();
					if((foundImageCacheInfos.size() == 0) || (!imageCachers.containsKey(bucketName + manifestKey))) {
						cacheImage(bucketName, manifestKey, userId, request.isAdministrator());
						reply.setSuccess(true);
					}
					db.commit( );
					return reply;
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", manifestKey);
				}

			} else {
				db.rollback( );
				throw new NoSuchEntityException(manifestKey);

			}
		} else {
			db.rollback( );
			throw new NoSuchBucketException(bucketName);
		}
	}

	public FlushCachedImageResponseType flushCachedImage(FlushCachedImageType request) throws EucalyptusCloudException {
		FlushCachedImageResponseType reply = (FlushCachedImageResponseType) request.getReply();

		String bucketName = request.getBucket();
		String manifestKey = request.getKey();

		EntityWrapper<ImageCacheInfo> db = WalrusControl.getEntityWrapper();
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
		List<ImageCacheInfo> foundImageCacheInfos = db.query(searchImageCacheInfo);

		if(foundImageCacheInfos.size() > 0) {
			ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
			if(foundImageCacheInfo.getInCache() && (imageCachers.get(bucketName + manifestKey) == null)) {
				//check that there are no operations in progress and then flush cache and delete image file
				db.commit();
				ImageCacheFlusher imageCacheFlusher = new ImageCacheFlusher(bucketName, manifestKey);
				imageCacheFlusher.start();
			} else {
				db.rollback();
				throw new EucalyptusCloudException("not in cache");
			}
		} else {
			db.rollback();
			throw new NoSuchEntityException(bucketName + manifestKey);
		}
		return reply;
	}

	public ValidateImageResponseType validateImage(ValidateImageType request) throws EucalyptusCloudException {
		ValidateImageResponseType reply = (ValidateImageResponseType) request.getReply();
		String bucketName = request.getBucket();
		String manifestKey = request.getKey();
		String userId = request.getUserId();
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);
		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, manifestKey);
					searchObjectInfo.setDeleted(false);
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
					if (objectInfos.size() > 0) {
						ObjectInfo objectInfo = objectInfos.get(0);
						if (objectInfo.canRead(userId)) {
							//validate manifest
							validateManifest(bucketName, manifestKey, userId);
							db.commit();
						} else {
							db.rollback();
							throw new AccessDeniedException("Key", manifestKey, logData);
						}
					} else {
						db.rollback();
						throw new NoSuchEntityException(manifestKey, logData);
					}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}

		return reply;
	}
}
