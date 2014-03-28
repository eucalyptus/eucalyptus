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
import java.security.PublicKey;

import javax.annotation.Nonnull;
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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Signatures;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.XMLParser;
import com.google.common.base.Function;

public class DownloadManifestFactory {
	private static Logger LOG = Logger.getLogger( DownloadManifestFactory.class );
	
	private final static String uuid = Signatures.SHA256withRSA.trySign( Eucalyptus.class, "download-manifests".getBytes());
	public static String DOWNLOAD_MANIFEST_BUCKET_NAME = (uuid != null ? uuid.substring(0, 6) : "system") + "-download-manifests";
	private static String DOWNLOAD_MANIFEST_PREFIX = "DM-";
	private static int DEFAULT_EXPIRE_TIME_HR = 3;

	public static String generateDownloadManifest(final ImageManifestFile baseManifest, final PublicKey keyToUse,
      final String manifestName) throws DownloadManifestException {
	  return generateDownloadManifest(baseManifest, keyToUse, manifestName, DEFAULT_EXPIRE_TIME_HR);
	}

    public static User getDownloadManifestS3User() throws AuthException {
        return Accounts.lookupSystemAdmin();
    }

	/**
	 * Generates download manifest based on bundle manifest and puts in into system owned bucket
	 * @param baseManifestLocation location of the base manifest file
	 * @param keyToUse public key that used for encryption
	 * @param manifestName name for generated manifest file
	 * @param expirationHours expiration policy in hours for pre-signed URLs
	 * @param manifestType what kind of manifest 
	 * @return pre-signed URL that can be used to download generated manifest
	 * @throws DownloadManifestException
	 */
	public static String generateDownloadManifest(final ImageManifestFile baseManifest, final PublicKey keyToUse,
			final String manifestName, int expirationHours) throws DownloadManifestException {
		try {
			//prepare to do pre-signed urls
            EucaS3Client s3Client = EucaS3ClientFactory.getEucaS3Client(getDownloadManifestS3User());

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
				String encryptedKey = xpathHelper.apply( "/manifest/image/ec2_encrypted_key" );
				String encryptedIV = xpathHelper.apply( "/manifest/image/ec2_encrypted_iv" );
				String size = xpathHelper.apply( "/manifest/image/size" );
				EncryptedKey encryptKey = reEncryptKey(new EncryptedKey(encryptedKey, encryptedIV), keyToUse);
				el = manifestDoc.createElement("bundle");
				Element key = manifestDoc.createElement("encrypted-key");
				key.appendChild(manifestDoc.createTextNode(encryptKey.getKey()));
				Element iv = manifestDoc.createElement("encrypted-iv");
				iv.appendChild(manifestDoc.createTextNode(encryptKey.getIV()));
				el.appendChild(key);
				el.appendChild(iv);
				Element sizeEl = manifestDoc.createElement("unbundled-size");
				sizeEl.appendChild(manifestDoc.createTextNode(size));
				el.appendChild(sizeEl);
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

			for(int i=0; i<parts.getLength();i++){
				Node part = parts.item(i);
				String partIndex = part.getAttributes().getNamedItem("index").getNodeValue();
				String partKey = ((Node) xpath.evaluate(baseManifest.getManifestType().getPartUrlElement(),
						part, XPathConstants.NODE)).getTextContent();
				String partDownloadUrl = partKey;
				if (baseManifest.getManifestType().signPartUrl()) {
					GeneratePresignedUrlRequest generatePresignedUrlRequest = 
						new GeneratePresignedUrlRequest(baseManifest.getBaseBucket(), partKey, HttpMethod.GET);
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
			signature.appendChild(manifestDoc.createTextNode(Signatures.SHA256withRSA.trySign(Eucalyptus.class,
                    signatureData.getBytes())));
			root.appendChild(signature);
			String downloadManifest = nodeToString(manifestDoc, true);
			//TODO: move this ?
			createManifestsBucket(s3Client);
			putManifestData(s3Client, DOWNLOAD_MANIFEST_BUCKET_NAME, DOWNLOAD_MANIFEST_PREFIX + manifestName, downloadManifest);
			// generate pre-sign url for download manifest
			URL s = s3Client.generatePresignedUrl(DOWNLOAD_MANIFEST_BUCKET_NAME,
                    DOWNLOAD_MANIFEST_PREFIX+manifestName, expiration, HttpMethod.GET);
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

	private static EncryptedKey reEncryptKey(EncryptedKey in, PublicKey keyToUse) throws Exception {
		// Decrypt key and IV with Eucalyptus
		PrivateKey pk = SystemCredentials.lookup(Eucalyptus.class).getPrivateKey();
		Cipher cipher = Ciphers.RSA_PKCS1.get();
		cipher.init(Cipher.DECRYPT_MODE, pk);
		byte[] key = cipher.doFinal(Hashes.hexToBytes(in.getKey()));
		byte[] iv = cipher.doFinal(Hashes.hexToBytes(in.getIV()));
		//Encrypt key and IV with NC
		cipher.init(Cipher.ENCRYPT_MODE, keyToUse);
		return new EncryptedKey(Hashes.bytesToHex(cipher.doFinal(key)), Hashes.bytesToHex(cipher.doFinal(iv)));
	}
	
	private static void putManifestData(@Nonnull EucaS3Client s3Client, String bucketName, String objectName, String data ) throws EucalyptusCloudException {
        int retries = 3;
        long backoffTime = 500L; // 1 second to start.
        for(int i = 0 ; i < retries; i++) {
            try {
                String etag = s3Client.putObjectContent(bucketName, objectName, data, null);
                LOG.debug("Added manifest to " + bucketName + "/" + objectName + " Etag: " + etag);
                return;
            } catch (AmazonClientException e) {
                LOG.warn("Upload error while trying to upload manifest data. Attempt: " + String.valueOf((i + 1)) + " of " + String.valueOf(retries), e);
            } catch(Exception e) {
                LOG.warn("Non-upload error while trying to upload manifest data. Attempt: " + String.valueOf((i + 1)) + " of " + String.valueOf(retries), e);
            }

            try {
                Thread.sleep(backoffTime);
            } catch(InterruptedException e) {
                LOG.warn("Interrupted during backoff sleep for upload.", e);
                throw new EucalyptusCloudException(e);
            }

            s3Client.refreshEndpoint(); //try another OSG if more than one.
            backoffTime *= 2;
        }
        throw new EucalyptusCloudException( "Failed to put manifest file: " + bucketName + "/" + objectName + ". Exceeded retry limit");
	}
	
	/**
	 * Creates system owned bucket to store download manifest files
	 * @throws EucalyptusCloudException
	 */
	public static void createManifestsBucket(@Nonnull EucaS3Client s3Client) throws EucalyptusCloudException {
		if (checkManifestsBucket(s3Client))
			return;

        Bucket manifestBucket;
        try {
            manifestBucket = s3Client.createBucket(DOWNLOAD_MANIFEST_BUCKET_NAME);
        } catch(Exception e) {
            LOG.error("Error creating manifest bucket " + DOWNLOAD_MANIFEST_BUCKET_NAME, e);
            throw new EucalyptusCloudException( "Failed to create bucket " + DOWNLOAD_MANIFEST_BUCKET_NAME, e );
        }

        try {
            BucketLifecycleConfiguration lc = new BucketLifecycleConfiguration();
            BucketLifecycleConfiguration.Rule expireRule= new BucketLifecycleConfiguration.Rule();
            expireRule.setId("Manifest Expiration Rule");
            expireRule.setPrefix(DOWNLOAD_MANIFEST_PREFIX);
            expireRule.setStatus("Enabled");
            expireRule.setExpirationInDays(1);
            lc = lc.withRules(expireRule);
            s3Client.setBucketLifecycleConfiguration(manifestBucket.getName(), lc);
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to set bucket lifecycle on bucket " + DOWNLOAD_MANIFEST_BUCKET_NAME, e );
		}
		LOG.debug( "Created bucket for download-manifests " + DOWNLOAD_MANIFEST_BUCKET_NAME);
	}

	private static boolean checkManifestsBucket(EucaS3Client s3Client) {
		try {
            //Since we're using the eucalyptus admin, which has access to all buckets, check the bucket owner explicitly
            AccessControlList acl = s3Client.getBucketAcl(DOWNLOAD_MANIFEST_BUCKET_NAME);
            if(!acl.getOwner().getId().equals(getDownloadManifestS3User().getAccount().getCanonicalId())) {
                //Bucket exists, but is owned by another account
                LOG.warn("Found existence of download manifest bucket: " + DOWNLOAD_MANIFEST_BUCKET_NAME + " but it is owned by another account: " + acl.getOwner().getId() + ", " + acl.getOwner().getDisplayName());
                return false;
            }

            BucketLifecycleConfiguration config = s3Client.getBucketLifecycleConfiguration(DOWNLOAD_MANIFEST_BUCKET_NAME);
            return (config.getRules() != null &&
                    config.getRules().size() == 1 &&
                    config.getRules().get(0).getExpirationInDays() == 1 &&
                    "enabled".equals(config.getRules().get(0).getStatus()) &&
                    DOWNLOAD_MANIFEST_PREFIX.equals(config.getRules().get(0).getPrefix()));
        } catch(AmazonServiceException e) {
            //Expected possible path if doesn't exist.
            return false;
        } catch(Exception e) {
            LOG.warn("Unexpected error checking for download manifest bucket", e);
            return false;
        }
	}
}
