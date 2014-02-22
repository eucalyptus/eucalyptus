/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.imaging.manifest;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.security.PrivateKey;

import javax.crypto.Cipher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Signatures;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.ListBucketType;
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.util.ChannelBufferStreamingInputStream;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.XMLParser;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.base.Function;

public class DownloadManifestFactory {
	private static Logger LOG = Logger.getLogger( DownloadManifestFactory.class );
	
	private final static String uuid = Signatures.SHA256withRSA.trySign( Eucalyptus.class, "download-manifests".getBytes());
	public static String DOWNLOAD_MANIFEST_BUCKET_NAME = (uuid != null ? uuid.substring(0, 6) : "system") + "-download-manifests";
	private static int DEFAULT_EXPIRE_TIME_HR = 3;

	public static String generateDownloadManifest(final ImageManifestFile baseManifest, final PrivateKey keyToUse,
      final String manifestName) throws DownloadManifestException {
	  return generateDownloadManifest(baseManifest, keyToUse, manifestName, DEFAULT_EXPIRE_TIME_HR);
	}
	/**
	 * Generates download manifest based on bundle manifest and puts in into system owned bucket
	 * @param baseManifestLocation location of the base manifest file
	 * @param keyToUse private key that used for encryption
	 * @param manifestName name for generated manifest file
	 * @param expirationHours expiration policy in hours for self-signed URLs
	 * @param manifestType what kind of manifest 
	 * @return Self-signed URL that can be used to download generated manifest
	 * @throws InvalidMetadataException
	 */
	public static String generateDownloadManifest(final ImageManifestFile baseManifest, final PrivateKey keyToUse,
			final String manifestName, int expirationHours) throws DownloadManifestException {
		try {
			//prepare to do pre-signed urls
			AccessKey adminAccessKey = Accounts.lookupSystemAdmin().getKeys().get(0);
			AWSCredentials myCredentials = new BasicAWSCredentials(adminAccessKey.getAccessKey(),
					adminAccessKey.getSecretKey());
			AmazonS3 s3Client = new AmazonS3Client(myCredentials);
			ServiceConfiguration OSLocation = Topology.lookup( ObjectStorage.class );
			s3Client.setEndpoint(OSLocation.getUri().getScheme() + "://" + OSLocation.getUri().getAuthority());
			
			java.util.Date expiration = new java.util.Date();
			long msec = expiration.getTime() + 1000 * 60 * 60 * expirationHours;
			expiration.setTime(msec);
			
			final String manifest = baseManifest.getManifest();
			if (manifest == null) {
				throw new DownloadManifestException("Can't generate download manifest from null base manifest");
			}
			final Document inputSource;
			final XPath xpath;
			Function<String, String> xpathHelper;
			DocumentBuilder builder = XMLParser.getDocBuilder( );
			inputSource = builder.parse( new ByteArrayInputStream( manifest.getBytes( ) ) );
			if ( !"manifest".equals(inputSource.getDocumentElement().getNodeName()) ) {
				LOG.error("Expected image manifest. Got " + nodeToString(inputSource, false) );
				throw new InvalidBaseManifestException("Base manifest does not have manifest element");
			}

			StringBuilder signatureSrc = new StringBuilder();
			Document manifestDoc = builder.newDocument();
			Element root = (Element) manifestDoc.createElement("manifest");
			manifestDoc.appendChild(root);
			Element el = manifestDoc.createElement("version");
			el.appendChild(manifestDoc.createTextNode("2014-01-14"));
			signatureSrc.append(nodeToString(el, false));
			root.appendChild(el);
			el = manifestDoc.createElement("file-format");
			el.appendChild(manifestDoc.createTextNode( baseManifest.getManifestType().getFileType().toString() ));
			root.appendChild(el);
			signatureSrc.append(nodeToString(el, false));
			
			xpath = XPathFactory.newInstance( ).newXPath();
			xpathHelper = new Function<String, String>( ) {
				@Override
				public String apply( String input ) {
					try {
						return ( String ) xpath.evaluate( input, inputSource, XPathConstants.STRING );
					} catch ( XPathExpressionException ex ) {
						return null;
					}
				}
			};

			// extract keys
			//TODO: move this?
			if (baseManifest.getManifestType().getFileType() == FileType.BUNDLE) {
				String encryptedKey = xpathHelper.apply( "//ec2_encrypted_key" );
				String encryptedIV = xpathHelper.apply( "//ec2_encrypted_iv" );
				EncryptedKey encryptKey = reEncryptKey(new EncryptedKey(encryptedKey, encryptedIV), keyToUse);
				el = manifestDoc.createElement("bundle");
				Element key = manifestDoc.createElement("encrypted-key");
				key.appendChild(manifestDoc.createTextNode(encryptKey.getKey()));
				Element iv = manifestDoc.createElement("encrypted-iv");
				iv.appendChild(manifestDoc.createTextNode(encryptKey.getIV()));
				el.appendChild(key);
				el.appendChild(iv);
				root.appendChild(el);
				signatureSrc.append(nodeToString(el, false));
			}

			el = manifestDoc.createElement("image");
			String bundleSize = xpathHelper.apply( baseManifest.getManifestType().getSizePath() );
			if (bundleSize == null) {
				throw new InvalidBaseManifestException("Base manifest does not have size element");
			}
			Element size = manifestDoc.createElement("size");
			size.appendChild(manifestDoc.createTextNode(bundleSize));		
			el.appendChild(size);
			
			Element partsEl = manifestDoc.createElement("parts");
			el.appendChild(partsEl);
			//parts
			NodeList parts = ( NodeList ) xpath.evaluate( baseManifest.getManifestType().getPartsPath(), inputSource, XPathConstants.NODESET );
			if (parts == null) {
				throw new InvalidBaseManifestException("Base manifest does not have parts");
			}
			String bucketBase = "services/objectstorage/";
			if (baseManifest.getManifestType().signPartUrl()) {
				bucketBase += baseManifest.getBaseBucket();
			}
			for(int i=0; i<parts.getLength();i++){
				Node part = parts.item(i);
				String partIndex = part.getAttributes().getNamedItem("index").getNodeValue();
				String partKey = ((Node) xpath.evaluate(baseManifest.getManifestType().getPartUrlElement(),
						part, XPathConstants.NODE)).getTextContent();
				String partDownloadUrl = partKey;
				if (baseManifest.getManifestType().signPartUrl()) {
					GeneratePresignedUrlRequest generatePresignedUrlRequest = 
						new GeneratePresignedUrlRequest(bucketBase, partKey, HttpMethod.GET);
					generatePresignedUrlRequest.setExpiration(expiration);
					URL s = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
					partDownloadUrl = s.toString();
				}
				Element aPart = manifestDoc.createElement("part");
				Element getUrl = manifestDoc.createElement("get-url");
				getUrl.appendChild(manifestDoc.createTextNode(partDownloadUrl));
				aPart.setAttribute("index", partIndex);
				aPart.appendChild(getUrl);
				partsEl.appendChild(aPart);
			}
			root.appendChild(el);
			signatureSrc.append(nodeToString(el, false));
			String signatureData = signatureSrc.toString();
			Element signature = manifestDoc.createElement("signature");
			signature.setAttribute("algorithm", "RSA-SHA256");
			signature.appendChild(manifestDoc.createTextNode( Signatures.SHA256withRSA.trySign(Eucalyptus.class,
					signatureData.getBytes()) ));
			root.appendChild(signature);
			String downloadManifest = nodeToString(manifestDoc, true);
			//TODO: move this ?
			createManifestsBucket();
			putManifestData(DOWNLOAD_MANIFEST_BUCKET_NAME, manifestName, downloadManifest);
			// generate pre-sign url for download manifest
			GeneratePresignedUrlRequest generatePresignedUrlRequest =
					new GeneratePresignedUrlRequest("services/objectstorage/" + DOWNLOAD_MANIFEST_BUCKET_NAME,
							manifestName, HttpMethod.GET);
			generatePresignedUrlRequest.setExpiration(expiration);
			URL s = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
			return String.format("%s://imaging@%s%s?%s", s.getProtocol(), s.getAuthority(), s.getPath(), s.getQuery());
		} catch(Exception ex) {
			LOG.error("Got an error", ex);
			throw new DownloadManifestException("Can't generate download manifest");
		}
	}
	
	private static final String nodeToString(Node node, boolean addDeclaration) throws Exception {
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		if (!addDeclaration)
			tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		Writer out = new StringWriter();
		tf.transform(new DOMSource(node), new StreamResult(out));
		return out.toString();
	}
	
	private static class EncryptedKey {
		final String key;
		final String IV;
		public EncryptedKey(String key, String IV){
			this.key = key;
			this.IV = IV;
		}
		public String getKey() { return key; }
		public String getIV() { return IV; }
	}

	private static EncryptedKey reEncryptKey(EncryptedKey in, PrivateKey keyToUse) throws Exception {
		byte[] key;
		byte[] iv;
		// Decrypt key and IV with Eucalyptus
		PrivateKey pk = SystemCredentials.lookup(Eucalyptus.class ).getPrivateKey();
		Cipher cipher = Ciphers.RSA_PKCS1.get();
		cipher.init(Cipher.DECRYPT_MODE, pk);
		String keyString = new String(cipher.doFinal(Hashes.hexToBytes(in.getKey())));
		key = Hashes.hexToBytes(keyString);
		String ivString = new String(cipher.doFinal(Hashes.hexToBytes(in.getIV())));
		iv = Hashes.hexToBytes(ivString);
		//Encrypt key and IV with NC
		cipher.init(Cipher.ENCRYPT_MODE, keyToUse);
		keyString = Hashes.bytesToHex(cipher.doFinal(key));
		ivString = Hashes.bytesToHex(cipher.doFinal(iv));
		return new EncryptedKey(keyString, ivString);
	}
	
	private static void putManifestData( String bucketName, String objectName, String data ) throws EucalyptusCloudException {
		PutObjectResponseType reply = null;
		try {
			PutObjectType msg = new PutObjectType();
			msg.setBucket(bucketName);
			msg.setKey(objectName);
			msg.setContentLength(String.valueOf(data.length()));
	        ChannelBufferStreamingInputStream cbsis = new ChannelBufferStreamingInputStream(ChannelBuffers.copiedBuffer(data.getBytes()));
	        msg.setData(cbsis);
			msg.regarding( );
			reply = AsyncRequests.sendSync( Topology.lookup( ObjectStorage.class ), msg );
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to put manifest file: " + bucketName + "/" + objectName, e );
		}
		LOG.debug( "Added manifest to " + bucketName + "/" + objectName + ": " + reply.getStatusMessage() );
	}
	
	/**
	 * Creates system owned bucket to store download manifest files
	 * @throws EucalyptusCloudException
	 */
	public static void createManifestsBucket() throws EucalyptusCloudException {
		CreateBucketResponseType reply;
		if (checkManifestsBucket())
			return;
		try {
			CreateBucketType msg = new CreateBucketType(DOWNLOAD_MANIFEST_BUCKET_NAME);
			msg.setBucket(DOWNLOAD_MANIFEST_BUCKET_NAME);
	        //TODO: what should be here?
			AccessControlList acl = new AccessControlList();
			msg.setAccessControlList(acl);
			msg.regarding( );
			reply = AsyncRequests.sendSync( Topology.lookup( ObjectStorage.class ), msg );
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to create backet " + DOWNLOAD_MANIFEST_BUCKET_NAME, e );
		}
		LOG.debug( "Created bucket " + DOWNLOAD_MANIFEST_BUCKET_NAME + ": " + reply.getStatusMessage() );
	}

	private static boolean checkManifestsBucket() {
		try {
			ListBucketType msg = new ListBucketType();
			msg.setBucket(DOWNLOAD_MANIFEST_BUCKET_NAME);
			msg.regarding( );
			AsyncRequests.sendSync( Topology.lookup( ObjectStorage.class ), msg );
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
