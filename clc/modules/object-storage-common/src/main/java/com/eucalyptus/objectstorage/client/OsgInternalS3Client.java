/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.client;

import java.util.Date;

import javax.annotation.Nonnull;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;

/**
 * A convenience wrapper for an AWS Java SDK S3 Client that sets default timeouts etc, options, etc
 *
 * This is specifically as needed for the OSG's internal use to various backends
 */
public class OsgInternalS3Client {
  private static final int CONNECTION_TIMEOUT_MS = 500; // 500ms connection timeout, fail fast
  private static final int OSG_SOCKET_TIMEOUT_MS = 10 * 1000; // 10 sec socket timeout if no data
  private static final int OSG_MAX_CONNECTIONS = 512; // Lots of connections since this is for the whole OSG

  private S3ClientOptions ops;
  private AmazonS3Client s3Client;
  private ClientConfiguration clientConfig;
  private Date instantiated;
  private volatile String endpoint;
  private volatile AWSCredentials currentCredentials;

  public OsgInternalS3Client(AWSCredentials credentials, String endpoint, boolean https, boolean useDns) {
    update(credentials, endpoint, https, useDns);
  }

  public void setUsePathStyle(boolean usePathStyle) {
    ops.setPathStyleAccess(usePathStyle);
    s3Client.setS3ClientOptions(ops);
  }

  public AmazonS3Client getS3Client() {
    return s3Client;
  }

  public synchronized String getS3Endpoint() {
    return this.endpoint;
  }

  public synchronized void setS3Endpoint(String s3Endpoint) {
    this.endpoint = s3Endpoint;
    s3Client.setEndpoint(s3Endpoint);
  }

  /**
   * Basically an .equals() call for AWSCreds
   * 
   * @param c1
   * @param c2
   * @return
   */
  private static boolean credentialsEqual(AWSCredentials c1, AWSCredentials c2) {
    return (c1 == null && c2 == null)
        || (c1 == null || c2 == null)
        || (c1.getAWSSecretKey() != null && c1.getAWSSecretKey().equals(c2.getAWSSecretKey()) && c1.getAWSAccessKeyId() != null && c1
            .getAWSAccessKeyId().equals(c2.getAWSAccessKeyId()));
  }

  private synchronized void initializeNewClient(@Nonnull AWSCredentials credentials, @Nonnull String endpoint, @Nonnull Boolean https,
      @Nonnull Boolean useDns) {
    ClientConfiguration config = new ClientConfiguration();
    config.setConnectionTimeout(CONNECTION_TIMEOUT_MS); // very short timeout
    config.setSocketTimeout(OSG_SOCKET_TIMEOUT_MS);
    config.setUseReaper(true);
    config.setMaxConnections(OSG_MAX_CONNECTIONS);
    Protocol protocol = https ? Protocol.HTTPS : Protocol.HTTP;
    config.setProtocol(protocol);
    this.clientConfig = config;
    this.s3Client = new AmazonS3Client(credentials, config);
    this.s3Client.addRequestHandler( new RequestHandler2( ) {
      @Override
      public void beforeRequest( final Request<?> request ) {
        if ( request.getOriginalRequest( ) instanceof InitiateMultipartUploadRequest ) {
          request.addHeader( "Content-Length", "0" );
        }
      }

      @Override
      public void afterResponse( final Request<?> request, final Response<?> response ) {
      }

      @Override
      public void afterError( final Request<?> request, final Response<?> response, final Exception e ) {
      }
    } );
    this.ops = new S3ClientOptions().withPathStyleAccess(!useDns);
    this.s3Client.setS3ClientOptions(ops);
    this.instantiated = new Date();
    this.currentCredentials = credentials;
    this.setS3Endpoint(endpoint);
  }

  public void update(@Nonnull AWSCredentials credentials, @Nonnull String latestEndpoint, @Nonnull Boolean https, @Nonnull Boolean useDns) {
    // Credentials changes require new client instance (for now)
    if (this.s3Client == null || !credentialsEqual(this.currentCredentials, credentials)) {
      this.initializeNewClient(credentials, latestEndpoint, https, useDns);
      return;
    }

    // They have opposite semantics, so any equality means it must be updated
    if (this.ops.isPathStyleAccess() == useDns) {
      this.ops.setPathStyleAccess(!useDns);
      this.s3Client.setS3ClientOptions(this.ops);
    }

    Protocol tmpProto = https ? Protocol.HTTPS : Protocol.HTTP;
    if (!tmpProto.equals(this.clientConfig.getProtocol())) {
      this.clientConfig.setProtocol(tmpProto);
    }

    if (this.getS3Endpoint() == null || !this.getS3Endpoint().equals(latestEndpoint)) {
      this.setS3Endpoint(latestEndpoint);
    }
  }

  public Date getInstantiated() {
    return instantiated;
  }
}
