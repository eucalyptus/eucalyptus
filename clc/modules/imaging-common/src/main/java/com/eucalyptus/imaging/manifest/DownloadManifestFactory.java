/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.imaging.manifest;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

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
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.imaging.common.UrlValidator;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Signatures;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.XMLParser;
import com.google.common.base.Function;


public class DownloadManifestFactory {
  private static Logger LOG = Logger.getLogger(DownloadManifestFactory.class);

  private final static String uuid = Signatures.SHA256withRSA.trySign(
      Eucalyptus.class, "download-manifests".getBytes());
  public static String DOWNLOAD_MANIFEST_BUCKET_NAME = (uuid != null ? uuid
      .substring(0, 6) : "system") + "-download-manifests-v2";
  private static String DOWNLOAD_MANIFEST_PREFIX = "DM-";
  private static String MANIFEST_EXPIRATION = "expire";
  private static int DEFAULT_EXPIRE_TIME_HR = 3;
  private static int TOKEN_REFRESH_MINS = 60;
  private static HashMap<String, ReentrantLock> manifestLocks = new HashMap<String, ReentrantLock>();

  private static class S3ClientFactory implements PoolableObjectFactory<EucaS3Client> {

    @Override
    public void destroyObject(EucaS3Client obj) throws Exception {
      obj.close();
    }

    @Override
    public EucaS3Client makeObject() throws Exception {
      return EucaS3ClientFactory.getEucaS3Client(
          new SecurityTokenAWSCredentialsProvider(
              Accounts.lookupSystemAccountByAlias( AccountIdentifiers.AWS_EXEC_READ_SYSTEM_ACCOUNT ),
          (int) ( TimeUnit.HOURS.toSeconds( DEFAULT_EXPIRE_TIME_HR ) + TimeUnit.MINUTES.toSeconds( TOKEN_REFRESH_MINS ) ),
          (int) ( TimeUnit.HOURS.toSeconds( DEFAULT_EXPIRE_TIME_HR ) ) ) );
    }

    @Override
    public void activateObject(EucaS3Client client) throws Exception {
    }

    @Override
    public void passivateObject(EucaS3Client client) throws Exception {
    }

    @Override
    public boolean validateObject(EucaS3Client client) {
      return true;
    }
  }

  // pool with up to 10 clients, a wait for client time up to 5 min and at most 2 idle clients
  private static GenericObjectPool<EucaS3Client> s3ClientsPool =
      new GenericObjectPool<EucaS3Client>(new S3ClientFactory(), 10,
          GenericObjectPool.WHEN_EXHAUSTED_BLOCK, 5 * 60 * 1000, 2);

  private static ReentrantLock getLock(String manifestName) {
    synchronized(manifestLocks) {
      if (manifestLocks.get(manifestName) != null)
        return manifestLocks.get(manifestName);
      else {
        ReentrantLock lock = new ReentrantLock();
        manifestLocks.put(manifestName, lock);
        return lock;
      }
    }
  }

  public static class ManifestLocksEventListener implements EventListener<ClockTick> {
    private static long lastCleanUp = System.currentTimeMillis();
    private static long CLEANUP_INTERVAL = 5 * 60 * 1000L;

    public static void register( ) {
      Listeners.register( ClockTick.class, new ManifestLocksEventListener() );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if (lastCleanUp + CLEANUP_INTERVAL < System.currentTimeMillis()) {
        synchronized(manifestLocks) {
          Iterator<Entry<String, ReentrantLock>> itr = manifestLocks.entrySet().iterator();
          while (itr.hasNext()) {
            Map.Entry<String, ReentrantLock> entry = itr.next();
            if (entry.getValue().tryLock()) {
              entry.getValue().unlock();
              itr.remove();
            }
          }
        }
        lastCleanUp = System.currentTimeMillis();
      }
    }
  }

  public static String generateDownloadManifest(
      final ImageManifestFile baseManifest, final PublicKey keyToUse,
      final String manifestName, boolean urlForNc)
      throws DownloadManifestException {
    return generateDownloadManifest(baseManifest, keyToUse, manifestName,
        DEFAULT_EXPIRE_TIME_HR, urlForNc);
  }

  /**
   * Generates download manifest based on bundle manifest and puts in into
   * system owned bucket
   *
   * @param baseManifest
   *          the base manifest
   * @param keyToUse
   *          public key that used for encryption
   * @param manifestName
   *          name for generated manifest file
   * @param expirationHours
   *          expiration policy in hours for pre-signed URLs
   * @param urlForNc
   *          indicates if urs are constructed for NC use
   * @return pre-signed URL that can be used to download generated manifest
   * @throws DownloadManifestException
   */
  public static String generateDownloadManifest(
      final ImageManifestFile baseManifest, final PublicKey keyToUse,
      final String manifestName, int expirationHours, boolean urlForNc)
      throws DownloadManifestException {
    EucaS3Client s3Client = null;
    try ( final LockResource manifestLock = LockResource.lock(getLock(manifestName)) ) {
      try {
        s3Client = s3ClientsPool.borrowObject();
      } catch (Exception ex) {
        throw new DownloadManifestException("Can't borrow s3Client from the pool");
      }
      // prepare to do pre-signed urls
      if (!urlForNc)
        s3Client.refreshEndpoint(true);
      else
        s3Client.refreshEndpoint();

      Date expiration = new Date();
      long msec = expiration.getTime() + 1000 * 60 * 60 * expirationHours;
      expiration.setTime(msec);

      // check if download-manifest already exists
      if (objectExist(s3Client, DOWNLOAD_MANIFEST_BUCKET_NAME,
          DOWNLOAD_MANIFEST_PREFIX + manifestName)) {
        LOG.debug("Manifest '" + (DOWNLOAD_MANIFEST_PREFIX + manifestName)
            + "' is already created and has not expired. Skipping creation");
        URL s = s3Client
            .generatePresignedUrl(DOWNLOAD_MANIFEST_BUCKET_NAME,
                DOWNLOAD_MANIFEST_PREFIX + manifestName, expiration,
                HttpMethod.GET);
        return String.format("%s://imaging@%s%s?%s", s.getProtocol(),
            s.getAuthority(), s.getPath(), s.getQuery());
      } else {
        LOG.debug("Manifest '" + (DOWNLOAD_MANIFEST_PREFIX + manifestName)
            + "' does not exist");
      }

      UrlValidator urlValidator = new UrlValidator();

      final String manifest = baseManifest.getManifest();
      if (manifest == null) {
        throw new DownloadManifestException(
            "Can't generate download manifest from null base manifest");
      }
      final Document inputSource;
      final XPath xpath;
      Function<String, String> xpathHelper;
      DocumentBuilder builder = XMLParser.getDocBuilder();
      inputSource = builder
          .parse(new ByteArrayInputStream(manifest.getBytes()));
      if (!"manifest".equals(inputSource.getDocumentElement().getNodeName())) {
        LOG.error("Expected image manifest. Got "
            + nodeToString(inputSource, false));
        throw new InvalidBaseManifestException(
            "Base manifest does not have manifest element");
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
      el.appendChild(manifestDoc.createTextNode(baseManifest.getManifestType()
          .getFileType().toString()));
      root.appendChild(el);
      signatureSrc.append(nodeToString(el, false));

      xpath = XPathFactory.newInstance().newXPath();
      xpathHelper = new Function<String, String>() {
        @Override
        public String apply(String input) {
          try {
            return (String) xpath.evaluate(input, inputSource,
                XPathConstants.STRING);
          } catch (XPathExpressionException ex) {
            return null;
          }
        }
      };

      // extract keys
      // TODO: move this?
      if (baseManifest.getManifestType().getFileType() == FileType.BUNDLE) {
        String encryptedKey = xpathHelper
            .apply("/manifest/image/ec2_encrypted_key");
        String encryptedIV = xpathHelper
            .apply("/manifest/image/ec2_encrypted_iv");
        String size = xpathHelper.apply("/manifest/image/size");
        EncryptedKey encryptKey = reEncryptKey(new EncryptedKey(encryptedKey,
            encryptedIV), keyToUse);
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
      String bundleSize = xpathHelper.apply(baseManifest.getManifestType()
          .getSizePath());
      if (bundleSize == null) {
        throw new InvalidBaseManifestException(
            "Base manifest does not have size element");
      }
      Element size = manifestDoc.createElement("size");
      size.appendChild(manifestDoc.createTextNode(bundleSize));
      el.appendChild(size);

      Element partsEl = manifestDoc.createElement("parts");
      el.appendChild(partsEl);
      // parts
      NodeList parts = (NodeList) xpath.evaluate(baseManifest.getManifestType()
          .getPartsPath(), inputSource, XPathConstants.NODESET);
      if (parts == null) {
        throw new InvalidBaseManifestException(
            "Base manifest does not have parts");
      }

      for (int i = 0; i < parts.getLength(); i++) {
        Node part = parts.item(i);
        String partIndex = part.getAttributes().getNamedItem("index")
            .getNodeValue();
        String partKey = ((Node) xpath.evaluate(baseManifest.getManifestType()
            .getPartUrlElement(), part, XPathConstants.NODE)).getTextContent();
        String partDownloadUrl = partKey;
        if (baseManifest.getManifestType().signPartUrl()) {
          final String bucket = baseManifest.getBaseBucket( );
          final String prefix = baseManifest.getPrefix( );
          final String key = prefix.isEmpty( ) ? partKey : prefix + "/" + partKey;
          GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
              bucket, key, HttpMethod.GET);
          generatePresignedUrlRequest.setExpiration(expiration);
          URL s = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
          partDownloadUrl = s.toString();
        } else {
          // validate url per EUCA-9144
          if (!urlValidator.isEucalyptusUrl(partDownloadUrl))
            throw new DownloadManifestException(
                "Some parts in the manifest are not stored in the OS. Its location is outside Eucalyptus:"
                    + partDownloadUrl);
        }
        Node digestNode = null;
        if (baseManifest.getManifestType().getDigestElement() != null)
          digestNode = ((Node) xpath.evaluate(baseManifest.getManifestType()
            .getDigestElement(), part, XPathConstants.NODE));
        Element aPart = manifestDoc.createElement("part");
        Element getUrl = manifestDoc.createElement("get-url");
        getUrl.appendChild(manifestDoc.createTextNode(partDownloadUrl));
        aPart.setAttribute("index", partIndex);
        aPart.appendChild(getUrl);
        if (digestNode != null) {
          NamedNodeMap nm = digestNode.getAttributes();
          if (nm == null)
            throw new DownloadManifestException("Some parts in manifest don't have digest's verification algorithm");
          Element digest = manifestDoc.createElement("digest");
          digest.setAttribute("algorithm", nm.getNamedItem("algorithm").getTextContent());
          digest.appendChild(manifestDoc.createTextNode(digestNode.getTextContent()));
          aPart.appendChild(digest);
        }
        partsEl.appendChild(aPart);
      }
      root.appendChild(el);
      signatureSrc.append(nodeToString(el, false));
      String signatureData = signatureSrc.toString();
      Element signature = manifestDoc.createElement("signature");
      signature.setAttribute("algorithm", "RSA-SHA256");
      signature.appendChild(manifestDoc.createTextNode(Signatures.SHA256withRSA
          .trySign(Eucalyptus.class, signatureData.getBytes())));
      root.appendChild(signature);
      String downloadManifest = nodeToString(manifestDoc, true);
      // TODO: move this ?
      createManifestsBucketIfNeeded(s3Client);
      putManifestData(s3Client, DOWNLOAD_MANIFEST_BUCKET_NAME,
          DOWNLOAD_MANIFEST_PREFIX + manifestName, downloadManifest, expiration);
      // generate pre-sign url for download manifest
      URL s = s3Client.generatePresignedUrl(DOWNLOAD_MANIFEST_BUCKET_NAME,
          DOWNLOAD_MANIFEST_PREFIX + manifestName, expiration, HttpMethod.GET);
      return String.format("%s://imaging@%s%s?%s", s.getProtocol(),
          s.getAuthority(), s.getPath(), s.getQuery());
    } catch (Exception ex) {
      LOG.error("Got an error", ex);
      throw new DownloadManifestException("Can't generate download manifest");
    } finally {
      if (s3Client != null)
        try {
          s3ClientsPool.returnObject(s3Client);
        } catch (Exception e) {
          // sad, but let's not break instances run
          LOG.warn("Could not return s3Client to the pool");
        }
    }
  }

  private static final String nodeToString(Node node, boolean addDeclaration)
      throws Exception {
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

    public EncryptedKey(String key, String IV) {
      this.key = key;
      this.IV = IV;
    }

    public String getKey() {
      return key;
    }

    public String getIV() {
      return IV;
    }
  }

  private static EncryptedKey reEncryptKey(EncryptedKey in, PublicKey keyToUse)
      throws Exception {
    // Decrypt key and IV with Eucalyptus
    PrivateKey pk = SystemCredentials.lookup(Eucalyptus.class).getPrivateKey();
    Cipher cipher = Ciphers.RSA_PKCS1.get();
    cipher
        .init(Cipher.DECRYPT_MODE, pk, Crypto.getSecureRandomSupplier().get());
    byte[] key = cipher.doFinal(Hashes.hexToBytes(in.getKey()));
    byte[] iv = cipher.doFinal(Hashes.hexToBytes(in.getIV()));
    // Encrypt key and IV with NC
    cipher.init(Cipher.ENCRYPT_MODE, keyToUse, Crypto.getSecureRandomSupplier()
        .get());
    return new EncryptedKey(Hashes.bytesToHex(cipher.doFinal(key)),
        Hashes.bytesToHex(cipher.doFinal(iv)));
  }

  private static void putManifestData(@Nonnull EucaS3Client s3Client,
      String bucketName, String objectName, String data, Date expiration)
      throws EucalyptusCloudException {
    int retries = 3;
    long backoffTime = 500L; // 1 second to start.
    for (int i = 0; i < retries; i++) {
      try {
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put(MANIFEST_EXPIRATION, Long.toString(expiration.getTime()));
        String etag = s3Client.putObjectContent(bucketName, objectName, data,
            metadata);
        LOG.debug("Added manifest to " + bucketName + "/" + objectName
            + " Etag: " + etag);
        return;
      } catch (AmazonClientException e) {
        LOG.warn("Upload error while trying to upload manifest data. Attempt: "
            + String.valueOf((i + 1)) + " of " + String.valueOf(retries), e);
      } catch (Exception e) {
        LOG.warn(
            "Non-upload error while trying to upload manifest data. Attempt: "
                + String.valueOf((i + 1)) + " of " + String.valueOf(retries), e);
      }

      try {
        Thread.sleep(backoffTime);
      } catch (InterruptedException e) {
        LOG.warn("Interrupted during backoff sleep for upload.", e);
        throw new EucalyptusCloudException(e);
      }

      s3Client.refreshEndpoint(); // try another OSG if more than one.
      backoffTime *= 2;
    }
    throw new EucalyptusCloudException("Failed to put manifest file: "
        + bucketName + "/" + objectName + ". Exceeded retry limit");
  }

  private static boolean objectExist(@Nonnull EucaS3Client s3Client,
      String bucketName, String objectName) throws EucalyptusCloudException {
    try {
      ObjectMetadata metadata = s3Client.getObjectMetadata(
          bucketName, objectName);
      if (metadata == null || metadata.getUserMetadata() == null)
        return false;
      Map<String, String> userData = metadata.getUserMetadata();
      String expire = userData.get(MANIFEST_EXPIRATION);
      if (expire == null) {
        return false;
      } else {
        Long currentTime = (new Date()).getTime();
        Long expireTime = Long.parseLong(expire);
        return expireTime > currentTime;
      }
    } catch (Exception ex) {
      return false;
    }
  }

  /**
   * Creates system owned bucket to store download manifest files if needed
   *
   * @throws EucalyptusCloudException
   */
  private static void createManifestsBucketIfNeeded(
      @Nonnull EucaS3Client s3Client) throws EucalyptusCloudException {
    try {
      s3Client.getBucketAcl(DOWNLOAD_MANIFEST_BUCKET_NAME);
    } catch (AmazonServiceException e1) {
      try {
        s3Client.createBucket(DOWNLOAD_MANIFEST_BUCKET_NAME);
      } catch (Exception e) {
        LOG.error("Error creating manifest bucket "
            + DOWNLOAD_MANIFEST_BUCKET_NAME, e);
        throw new EucalyptusCloudException("Failed to create bucket "
            + DOWNLOAD_MANIFEST_BUCKET_NAME, e);
      }
    }
    BucketLifecycleConfiguration config = s3Client
        .getBucketLifecycleConfiguration(DOWNLOAD_MANIFEST_BUCKET_NAME);
    if (config.getRules() == null
        || config.getRules().size() != 1
        || config.getRules().get(0).getExpirationInDays() != 1
        || !"enabled".equalsIgnoreCase(config.getRules().get(0).getStatus())
        || !DOWNLOAD_MANIFEST_PREFIX.equals(config.getRules().get(0)
            .getPrefix())) {
      try {
        BucketLifecycleConfiguration lc = new BucketLifecycleConfiguration();
        BucketLifecycleConfiguration.Rule expireRule = new BucketLifecycleConfiguration.Rule();
        expireRule.setId("Manifest Expiration Rule");
        expireRule.setPrefix(DOWNLOAD_MANIFEST_PREFIX);
        expireRule.setStatus("Enabled");
        expireRule.setExpirationInDays(1);
        lc = lc.withRules(expireRule);
        s3Client.setBucketLifecycleConfiguration(DOWNLOAD_MANIFEST_BUCKET_NAME, lc);
      } catch (Exception e) {
        throw new EucalyptusCloudException(
            "Failed to set bucket lifecycle on bucket "
                + DOWNLOAD_MANIFEST_BUCKET_NAME, e);
      }
    }
    LOG.debug("Created bucket for download-manifests "
        + DOWNLOAD_MANIFEST_BUCKET_NAME);
  }
}
