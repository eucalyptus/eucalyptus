package com.eucalyptus.objectstorage.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.util.Md5Utils;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;

/**
 * Wrapper class around an AmazonS3Client to provide some convenience functions for simple get/put of strings
 * Also provides methods for refreshing the endpoint in case of failure etc
 */
public class EucaS3Client extends DelegatingAmazonS3<AmazonS3Client> implements AutoCloseable {

  protected EucaS3Client( AWSCredentials credentials, boolean useHttps ) {
    super( GenericS3ClientFactory.getS3Client( credentials, useHttps ) );
  }

  protected EucaS3Client( AWSCredentialsProvider credentialsProvider, boolean useHttps ) {
    super( GenericS3ClientFactory.getS3Client( credentialsProvider, useHttps ) );
  }

  /**
   * Finds a new OSG to use for the endpoint. Use this method
   * in case of failure due to an OSG failing and becoming unavailable.
   */
  public void refreshEndpoint( ) throws NoSuchElementException {
    refreshEndpoint( false );
  }

  public void refreshEndpoint( boolean usePublicDns ) throws NoSuchElementException {
    String path = GenericS3ClientFactory.getRandomOSGUri( usePublicDns ).toASCIIString( );
    if ( path.endsWith( "/" ) ) path = path.substring( 0, (int) path.length( ) - 1 );
    setEndpoint( path );
  }

  public String getObjectContent( final String bucket, final String key, int maximumSize ) throws IOException {
    final byte[] buffer = new byte[ 10 * 1024 ];//10k buffer
    final ByteArrayOutputStream contentBytes = new ByteArrayOutputStream( buffer.length );
    try ( InputStream contentStream = getObject( bucket, key ).getObjectContent( ) ) {
      int readBytes;
      while ( ( readBytes = contentStream.read( buffer ) ) > 0 ) {
        contentBytes.write( buffer, 0, readBytes );
        if ( contentBytes.size( ) > maximumSize ) {
          throw new IOException( "Maximum size exceeded for " + bucket + "/" + key );
        }
      }
    }
    try {
      return contentBytes.toString( StandardCharsets.UTF_8.name( ) );
    } catch ( UnsupportedEncodingException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  /**
   * Returns the etag, verifies content by md5
   */
  public String putObjectContent( String bucket, String key, String content, Map<String, String> metadata ) {
    final byte[] contentBytes = content.getBytes( StandardCharsets.UTF_8 );
    final byte[] md5 = Md5Utils.computeMD5Hash( contentBytes );
    final ObjectMetadata objMetadata = new ObjectMetadata( );
    if ( metadata != null ) {
      metadata.forEach( objMetadata::addUserMetadata );
    }
    String base64Md5 = Base64.encode( md5 );
    objMetadata.setContentMD5( base64Md5 );
    ByteArrayInputStream contentStream = new ByteArrayInputStream( contentBytes );
    PutObjectResult result = putObject( bucket, key, contentStream, objMetadata );
    return result.getETag( );
  }

  public void close( ) {
    withDelegate( AmazonWebServiceClient::shutdown );
  }
}
