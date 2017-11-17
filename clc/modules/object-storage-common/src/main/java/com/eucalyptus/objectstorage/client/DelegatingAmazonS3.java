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
package com.eucalyptus.objectstorage.client;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.model.analytics.AnalyticsConfiguration;
import com.amazonaws.services.s3.model.inventory.InventoryConfiguration;
import com.amazonaws.services.s3.model.metrics.MetricsConfiguration;
import com.amazonaws.services.s3.waiters.AmazonS3Waiters;

/**
 *
 */
class DelegatingAmazonS3<T extends AmazonS3> implements AmazonS3 {
  private final T delegate;

  public DelegatingAmazonS3( final T delegate ) {
    this.delegate = delegate;
  }

  protected void withDelegate( final Consumer<T> consumer ) {
    consumer.accept( delegate );
  }

  @Override
  public void setEndpoint( final String endpoint ) {
    delegate.setEndpoint( endpoint );
  }

  @Override
  public void setRegion( final Region region ) throws IllegalArgumentException {
    delegate.setRegion( region );
  }

  @Override
  public void setS3ClientOptions( final S3ClientOptions clientOptions ) {
    delegate.setS3ClientOptions( clientOptions );
  }

  @Override
  @Deprecated
  public void changeObjectStorageClass( final String bucketName, final String key, final StorageClass newStorageClass ) throws SdkClientException, AmazonServiceException {
    delegate.changeObjectStorageClass( bucketName, key, newStorageClass );
  }

  @Override
  @Deprecated
  public void setObjectRedirectLocation( final String bucketName, final String key, final String newRedirectLocation ) throws SdkClientException, AmazonServiceException {
    delegate.setObjectRedirectLocation( bucketName, key, newRedirectLocation );
  }

  @Override
  public ObjectListing listObjects( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.listObjects( bucketName );
  }

  @Override
  public ObjectListing listObjects( final String bucketName, final String prefix ) throws SdkClientException, AmazonServiceException {
    return delegate.listObjects( bucketName, prefix );
  }

  @Override
  public ObjectListing listObjects( final ListObjectsRequest listObjectsRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.listObjects( listObjectsRequest );
  }

  @Override
  public ListObjectsV2Result listObjectsV2( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.listObjectsV2( bucketName );
  }

  @Override
  public ListObjectsV2Result listObjectsV2( final String bucketName, final String prefix ) throws SdkClientException, AmazonServiceException {
    return delegate.listObjectsV2( bucketName, prefix );
  }

  @Override
  public ListObjectsV2Result listObjectsV2( final ListObjectsV2Request listObjectsV2Request ) throws SdkClientException, AmazonServiceException {
    return delegate.listObjectsV2( listObjectsV2Request );
  }

  @Override
  public ObjectListing listNextBatchOfObjects( final ObjectListing previousObjectListing ) throws SdkClientException, AmazonServiceException {
    return delegate.listNextBatchOfObjects( previousObjectListing );
  }

  @Override
  public ObjectListing listNextBatchOfObjects( final ListNextBatchOfObjectsRequest listNextBatchOfObjectsRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.listNextBatchOfObjects( listNextBatchOfObjectsRequest );
  }

  @Override
  public VersionListing listVersions( final String bucketName, final String prefix ) throws SdkClientException, AmazonServiceException {
    return delegate.listVersions( bucketName, prefix );
  }

  @Override
  public VersionListing listNextBatchOfVersions( final VersionListing previousVersionListing ) throws SdkClientException, AmazonServiceException {
    return delegate.listNextBatchOfVersions( previousVersionListing );
  }

  @Override
  public VersionListing listNextBatchOfVersions( final ListNextBatchOfVersionsRequest listNextBatchOfVersionsRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.listNextBatchOfVersions( listNextBatchOfVersionsRequest );
  }

  @Override
  public VersionListing listVersions( final String bucketName, final String prefix, final String keyMarker, final String versionIdMarker, final String delimiter, final Integer maxResults ) throws SdkClientException, AmazonServiceException {
    return delegate.listVersions( bucketName, prefix, keyMarker, versionIdMarker, delimiter, maxResults );
  }

  @Override
  public VersionListing listVersions( final ListVersionsRequest listVersionsRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.listVersions( listVersionsRequest );
  }

  @Override
  public Owner getS3AccountOwner( ) throws SdkClientException, AmazonServiceException {
    return delegate.getS3AccountOwner( );
  }

  @Override
  public Owner getS3AccountOwner( final GetS3AccountOwnerRequest getS3AccountOwnerRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getS3AccountOwner( getS3AccountOwnerRequest );
  }

  @Override
  public boolean doesBucketExist( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.doesBucketExist( bucketName );
  }

  @Override
  public HeadBucketResult headBucket( final HeadBucketRequest headBucketRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.headBucket( headBucketRequest );
  }

  @Override
  public List<Bucket> listBuckets( ) throws SdkClientException, AmazonServiceException {
    return delegate.listBuckets( );
  }

  @Override
  public List<Bucket> listBuckets( final ListBucketsRequest listBucketsRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.listBuckets( listBucketsRequest );
  }

  @Override
  public String getBucketLocation( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketLocation( bucketName );
  }

  @Override
  public String getBucketLocation( final GetBucketLocationRequest getBucketLocationRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketLocation( getBucketLocationRequest );
  }

  @Override
  public Bucket createBucket( final CreateBucketRequest createBucketRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.createBucket( createBucketRequest );
  }

  @Override
  public Bucket createBucket( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.createBucket( bucketName );
  }

  @Override
  public Bucket createBucket( final String bucketName, final com.amazonaws.services.s3.model.Region region ) throws SdkClientException, AmazonServiceException {
    return delegate.createBucket( bucketName, region );
  }

  @Override
  public Bucket createBucket( final String bucketName, final String region ) throws SdkClientException, AmazonServiceException {
    return delegate.createBucket( bucketName, region );
  }

  @Override
  public AccessControlList getObjectAcl( final String bucketName, final String key ) throws SdkClientException, AmazonServiceException {
    return delegate.getObjectAcl( bucketName, key );
  }

  @Override
  public AccessControlList getObjectAcl( final String bucketName, final String key, final String versionId ) throws SdkClientException, AmazonServiceException {
    return delegate.getObjectAcl( bucketName, key, versionId );
  }

  @Override
  public AccessControlList getObjectAcl( final GetObjectAclRequest getObjectAclRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getObjectAcl( getObjectAclRequest );
  }

  @Override
  public void setObjectAcl( final String bucketName, final String key, final AccessControlList acl ) throws SdkClientException, AmazonServiceException {
    delegate.setObjectAcl( bucketName, key, acl );
  }

  @Override
  public void setObjectAcl( final String bucketName, final String key, final CannedAccessControlList acl ) throws SdkClientException, AmazonServiceException {
    delegate.setObjectAcl( bucketName, key, acl );
  }

  @Override
  public void setObjectAcl( final String bucketName, final String key, final String versionId, final AccessControlList acl ) throws SdkClientException, AmazonServiceException {
    delegate.setObjectAcl( bucketName, key, versionId, acl );
  }

  @Override
  public void setObjectAcl( final String bucketName, final String key, final String versionId, final CannedAccessControlList acl ) throws SdkClientException, AmazonServiceException {
    delegate.setObjectAcl( bucketName, key, versionId, acl );
  }

  @Override
  public void setObjectAcl( final SetObjectAclRequest setObjectAclRequest ) throws SdkClientException, AmazonServiceException {
    delegate.setObjectAcl( setObjectAclRequest );
  }

  @Override
  public AccessControlList getBucketAcl( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketAcl( bucketName );
  }

  @Override
  public void setBucketAcl( final SetBucketAclRequest setBucketAclRequest ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketAcl( setBucketAclRequest );
  }

  @Override
  public AccessControlList getBucketAcl( final GetBucketAclRequest getBucketAclRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketAcl( getBucketAclRequest );
  }

  @Override
  public void setBucketAcl( final String bucketName, final AccessControlList acl ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketAcl( bucketName, acl );
  }

  @Override
  public void setBucketAcl( final String bucketName, final CannedAccessControlList acl ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketAcl( bucketName, acl );
  }

  @Override
  public ObjectMetadata getObjectMetadata( final String bucketName, final String key ) throws SdkClientException, AmazonServiceException {
    return delegate.getObjectMetadata( bucketName, key );
  }

  @Override
  public ObjectMetadata getObjectMetadata( final GetObjectMetadataRequest getObjectMetadataRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getObjectMetadata( getObjectMetadataRequest );
  }

  @Override
  public S3Object getObject( final String bucketName, final String key ) throws SdkClientException, AmazonServiceException {
    return delegate.getObject( bucketName, key );
  }

  @Override
  public S3Object getObject( final GetObjectRequest getObjectRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getObject( getObjectRequest );
  }

  @Override
  public ObjectMetadata getObject( final GetObjectRequest getObjectRequest, final File destinationFile ) throws SdkClientException, AmazonServiceException {
    return delegate.getObject( getObjectRequest, destinationFile );
  }

  @Override
  public String getObjectAsString( final String bucketName, final String key ) throws AmazonServiceException, SdkClientException {
    return delegate.getObjectAsString( bucketName, key );
  }

  @Override
  public GetObjectTaggingResult getObjectTagging( final GetObjectTaggingRequest getObjectTaggingRequest ) {
    return delegate.getObjectTagging( getObjectTaggingRequest );
  }

  @Override
  public SetObjectTaggingResult setObjectTagging( final SetObjectTaggingRequest setObjectTaggingRequest ) {
    return delegate.setObjectTagging( setObjectTaggingRequest );
  }

  @Override
  public DeleteObjectTaggingResult deleteObjectTagging( final DeleteObjectTaggingRequest deleteObjectTaggingRequest ) {
    return delegate.deleteObjectTagging( deleteObjectTaggingRequest );
  }

  @Override
  public void deleteBucket( final DeleteBucketRequest deleteBucketRequest ) throws SdkClientException, AmazonServiceException {
    delegate.deleteBucket( deleteBucketRequest );
  }

  @Override
  public void deleteBucket( final String bucketName ) throws SdkClientException, AmazonServiceException {
    delegate.deleteBucket( bucketName );
  }

  @Override
  public PutObjectResult putObject( final PutObjectRequest putObjectRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.putObject( putObjectRequest );
  }

  @Override
  public PutObjectResult putObject( final String bucketName, final String key, final File file ) throws SdkClientException, AmazonServiceException {
    return delegate.putObject( bucketName, key, file );
  }

  @Override
  public PutObjectResult putObject( final String bucketName, final String key, final InputStream input, final ObjectMetadata metadata ) throws SdkClientException, AmazonServiceException {
    return delegate.putObject( bucketName, key, input, metadata );
  }

  @Override
  public PutObjectResult putObject( final String bucketName, final String key, final String content ) throws AmazonServiceException, SdkClientException {
    return delegate.putObject( bucketName, key, content );
  }

  @Override
  public CopyObjectResult copyObject( final String sourceBucketName, final String sourceKey, final String destinationBucketName, final String destinationKey ) throws SdkClientException, AmazonServiceException {
    return delegate.copyObject( sourceBucketName, sourceKey, destinationBucketName, destinationKey );
  }

  @Override
  public CopyObjectResult copyObject( final CopyObjectRequest copyObjectRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.copyObject( copyObjectRequest );
  }

  @Override
  public CopyPartResult copyPart( final CopyPartRequest copyPartRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.copyPart( copyPartRequest );
  }

  @Override
  public void deleteObject( final String bucketName, final String key ) throws SdkClientException, AmazonServiceException {
    delegate.deleteObject( bucketName, key );
  }

  @Override
  public void deleteObject( final DeleteObjectRequest deleteObjectRequest ) throws SdkClientException, AmazonServiceException {
    delegate.deleteObject( deleteObjectRequest );
  }

  @Override
  public DeleteObjectsResult deleteObjects( final DeleteObjectsRequest deleteObjectsRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.deleteObjects( deleteObjectsRequest );
  }

  @Override
  public void deleteVersion( final String bucketName, final String key, final String versionId ) throws SdkClientException, AmazonServiceException {
    delegate.deleteVersion( bucketName, key, versionId );
  }

  @Override
  public void deleteVersion( final DeleteVersionRequest deleteVersionRequest ) throws SdkClientException, AmazonServiceException {
    delegate.deleteVersion( deleteVersionRequest );
  }

  @Override
  public BucketLoggingConfiguration getBucketLoggingConfiguration( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketLoggingConfiguration( bucketName );
  }

  @Override
  public BucketLoggingConfiguration getBucketLoggingConfiguration( final GetBucketLoggingConfigurationRequest getBucketLoggingConfigurationRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketLoggingConfiguration( getBucketLoggingConfigurationRequest );
  }

  @Override
  public void setBucketLoggingConfiguration( final SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketLoggingConfiguration( setBucketLoggingConfigurationRequest );
  }

  @Override
  public BucketVersioningConfiguration getBucketVersioningConfiguration( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketVersioningConfiguration( bucketName );
  }

  @Override
  public BucketVersioningConfiguration getBucketVersioningConfiguration( final GetBucketVersioningConfigurationRequest getBucketVersioningConfigurationRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketVersioningConfiguration( getBucketVersioningConfigurationRequest );
  }

  @Override
  public void setBucketVersioningConfiguration( final SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketVersioningConfiguration( setBucketVersioningConfigurationRequest );
  }

  @Override
  public BucketLifecycleConfiguration getBucketLifecycleConfiguration( final String bucketName ) {
    return delegate.getBucketLifecycleConfiguration( bucketName );
  }

  @Override
  public BucketLifecycleConfiguration getBucketLifecycleConfiguration( final GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest ) {
    return delegate.getBucketLifecycleConfiguration( getBucketLifecycleConfigurationRequest );
  }

  @Override
  public void setBucketLifecycleConfiguration( final String bucketName, final BucketLifecycleConfiguration bucketLifecycleConfiguration ) {
    delegate.setBucketLifecycleConfiguration( bucketName, bucketLifecycleConfiguration );
  }

  @Override
  public void setBucketLifecycleConfiguration( final SetBucketLifecycleConfigurationRequest setBucketLifecycleConfigurationRequest ) {
    delegate.setBucketLifecycleConfiguration( setBucketLifecycleConfigurationRequest );
  }

  @Override
  public void deleteBucketLifecycleConfiguration( final String bucketName ) {
    delegate.deleteBucketLifecycleConfiguration( bucketName );
  }

  @Override
  public void deleteBucketLifecycleConfiguration( final DeleteBucketLifecycleConfigurationRequest deleteBucketLifecycleConfigurationRequest ) {
    delegate.deleteBucketLifecycleConfiguration( deleteBucketLifecycleConfigurationRequest );
  }

  @Override
  public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration( final String bucketName ) {
    return delegate.getBucketCrossOriginConfiguration( bucketName );
  }

  @Override
  public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration( final GetBucketCrossOriginConfigurationRequest getBucketCrossOriginConfigurationRequest ) {
    return delegate.getBucketCrossOriginConfiguration( getBucketCrossOriginConfigurationRequest );
  }

  @Override
  public void setBucketCrossOriginConfiguration( final String bucketName, final BucketCrossOriginConfiguration bucketCrossOriginConfiguration ) {
    delegate.setBucketCrossOriginConfiguration( bucketName, bucketCrossOriginConfiguration );
  }

  @Override
  public void setBucketCrossOriginConfiguration( final SetBucketCrossOriginConfigurationRequest setBucketCrossOriginConfigurationRequest ) {
    delegate.setBucketCrossOriginConfiguration( setBucketCrossOriginConfigurationRequest );
  }

  @Override
  public void deleteBucketCrossOriginConfiguration( final String bucketName ) {
    delegate.deleteBucketCrossOriginConfiguration( bucketName );
  }

  @Override
  public void deleteBucketCrossOriginConfiguration( final DeleteBucketCrossOriginConfigurationRequest deleteBucketCrossOriginConfigurationRequest ) {
    delegate.deleteBucketCrossOriginConfiguration( deleteBucketCrossOriginConfigurationRequest );
  }

  @Override
  public BucketTaggingConfiguration getBucketTaggingConfiguration( final String bucketName ) {
    return delegate.getBucketTaggingConfiguration( bucketName );
  }

  @Override
  public BucketTaggingConfiguration getBucketTaggingConfiguration( final GetBucketTaggingConfigurationRequest getBucketTaggingConfigurationRequest ) {
    return delegate.getBucketTaggingConfiguration( getBucketTaggingConfigurationRequest );
  }

  @Override
  public void setBucketTaggingConfiguration( final String bucketName, final BucketTaggingConfiguration bucketTaggingConfiguration ) {
    delegate.setBucketTaggingConfiguration( bucketName, bucketTaggingConfiguration );
  }

  @Override
  public void setBucketTaggingConfiguration( final SetBucketTaggingConfigurationRequest setBucketTaggingConfigurationRequest ) {
    delegate.setBucketTaggingConfiguration( setBucketTaggingConfigurationRequest );
  }

  @Override
  public void deleteBucketTaggingConfiguration( final String bucketName ) {
    delegate.deleteBucketTaggingConfiguration( bucketName );
  }

  @Override
  public void deleteBucketTaggingConfiguration( final DeleteBucketTaggingConfigurationRequest deleteBucketTaggingConfigurationRequest ) {
    delegate.deleteBucketTaggingConfiguration( deleteBucketTaggingConfigurationRequest );
  }

  @Override
  public BucketNotificationConfiguration getBucketNotificationConfiguration( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketNotificationConfiguration( bucketName );
  }

  @Override
  public BucketNotificationConfiguration getBucketNotificationConfiguration( final GetBucketNotificationConfigurationRequest getBucketNotificationConfigurationRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketNotificationConfiguration( getBucketNotificationConfigurationRequest );
  }

  @Override
  public void setBucketNotificationConfiguration( final SetBucketNotificationConfigurationRequest setBucketNotificationConfigurationRequest ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketNotificationConfiguration( setBucketNotificationConfigurationRequest );
  }

  @Override
  public void setBucketNotificationConfiguration( final String bucketName, final BucketNotificationConfiguration bucketNotificationConfiguration ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketNotificationConfiguration( bucketName, bucketNotificationConfiguration );
  }

  @Override
  public BucketWebsiteConfiguration getBucketWebsiteConfiguration( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketWebsiteConfiguration( bucketName );
  }

  @Override
  public BucketWebsiteConfiguration getBucketWebsiteConfiguration( final GetBucketWebsiteConfigurationRequest getBucketWebsiteConfigurationRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketWebsiteConfiguration( getBucketWebsiteConfigurationRequest );
  }

  @Override
  public void setBucketWebsiteConfiguration( final String bucketName, final BucketWebsiteConfiguration configuration ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketWebsiteConfiguration( bucketName, configuration );
  }

  @Override
  public void setBucketWebsiteConfiguration( final SetBucketWebsiteConfigurationRequest setBucketWebsiteConfigurationRequest ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketWebsiteConfiguration( setBucketWebsiteConfigurationRequest );
  }

  @Override
  public void deleteBucketWebsiteConfiguration( final String bucketName ) throws SdkClientException, AmazonServiceException {
    delegate.deleteBucketWebsiteConfiguration( bucketName );
  }

  @Override
  public void deleteBucketWebsiteConfiguration( final DeleteBucketWebsiteConfigurationRequest deleteBucketWebsiteConfigurationRequest ) throws SdkClientException, AmazonServiceException {
    delegate.deleteBucketWebsiteConfiguration( deleteBucketWebsiteConfigurationRequest );
  }

  @Override
  public BucketPolicy getBucketPolicy( final String bucketName ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketPolicy( bucketName );
  }

  @Override
  public BucketPolicy getBucketPolicy( final GetBucketPolicyRequest getBucketPolicyRequest ) throws SdkClientException, AmazonServiceException {
    return delegate.getBucketPolicy( getBucketPolicyRequest );
  }

  @Override
  public void setBucketPolicy( final String bucketName, final String policyText ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketPolicy( bucketName, policyText );
  }

  @Override
  public void setBucketPolicy( final SetBucketPolicyRequest setBucketPolicyRequest ) throws SdkClientException, AmazonServiceException {
    delegate.setBucketPolicy( setBucketPolicyRequest );
  }

  @Override
  public void deleteBucketPolicy( final String bucketName ) throws SdkClientException, AmazonServiceException {
    delegate.deleteBucketPolicy( bucketName );
  }

  @Override
  public void deleteBucketPolicy( final DeleteBucketPolicyRequest deleteBucketPolicyRequest ) throws SdkClientException, AmazonServiceException {
    delegate.deleteBucketPolicy( deleteBucketPolicyRequest );
  }

  @Override
  public URL generatePresignedUrl( final String bucketName, final String key, final Date expiration ) throws SdkClientException {
    return delegate.generatePresignedUrl( bucketName, key, expiration );
  }

  @Override
  public URL generatePresignedUrl( final String bucketName, final String key, final Date expiration, final HttpMethod method ) throws SdkClientException {
    return delegate.generatePresignedUrl( bucketName, key, expiration, method );
  }

  @Override
  public URL generatePresignedUrl( final GeneratePresignedUrlRequest generatePresignedUrlRequest ) throws SdkClientException {
    return delegate.generatePresignedUrl( generatePresignedUrlRequest );
  }

  @Override
  public InitiateMultipartUploadResult initiateMultipartUpload( final InitiateMultipartUploadRequest request ) throws SdkClientException, AmazonServiceException {
    return delegate.initiateMultipartUpload( request );
  }

  @Override
  public UploadPartResult uploadPart( final UploadPartRequest request ) throws SdkClientException, AmazonServiceException {
    return delegate.uploadPart( request );
  }

  @Override
  public PartListing listParts( final ListPartsRequest request ) throws SdkClientException, AmazonServiceException {
    return delegate.listParts( request );
  }

  @Override
  public void abortMultipartUpload( final AbortMultipartUploadRequest request ) throws SdkClientException, AmazonServiceException {
    delegate.abortMultipartUpload( request );
  }

  @Override
  public CompleteMultipartUploadResult completeMultipartUpload( final CompleteMultipartUploadRequest request ) throws SdkClientException, AmazonServiceException {
    return delegate.completeMultipartUpload( request );
  }

  @Override
  public MultipartUploadListing listMultipartUploads( final ListMultipartUploadsRequest request ) throws SdkClientException, AmazonServiceException {
    return delegate.listMultipartUploads( request );
  }

  @Override
  public S3ResponseMetadata getCachedResponseMetadata( final AmazonWebServiceRequest request ) {
    return delegate.getCachedResponseMetadata( request );
  }

  @Override
  public void restoreObject( final RestoreObjectRequest request ) throws AmazonServiceException {
    delegate.restoreObject( request );
  }

  @Override
  public void restoreObject( final String bucketName, final String key, final int expirationInDays ) throws AmazonServiceException {
    delegate.restoreObject( bucketName, key, expirationInDays );
  }

  @Override
  public void enableRequesterPays( final String bucketName ) throws AmazonServiceException, SdkClientException {
    delegate.enableRequesterPays( bucketName );
  }

  @Override
  public void disableRequesterPays( final String bucketName ) throws AmazonServiceException, SdkClientException {
    delegate.disableRequesterPays( bucketName );
  }

  @Override
  public boolean isRequesterPaysEnabled( final String bucketName ) throws AmazonServiceException, SdkClientException {
    return delegate.isRequesterPaysEnabled( bucketName );
  }

  @Override
  public void setBucketReplicationConfiguration( final String bucketName, final BucketReplicationConfiguration configuration ) throws AmazonServiceException, SdkClientException {
    delegate.setBucketReplicationConfiguration( bucketName, configuration );
  }

  @Override
  public void setBucketReplicationConfiguration( final SetBucketReplicationConfigurationRequest setBucketReplicationConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    delegate.setBucketReplicationConfiguration( setBucketReplicationConfigurationRequest );
  }

  @Override
  public BucketReplicationConfiguration getBucketReplicationConfiguration( final String bucketName ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketReplicationConfiguration( bucketName );
  }

  @Override
  public BucketReplicationConfiguration getBucketReplicationConfiguration( final GetBucketReplicationConfigurationRequest getBucketReplicationConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketReplicationConfiguration( getBucketReplicationConfigurationRequest );
  }

  @Override
  public void deleteBucketReplicationConfiguration( final String bucketName ) throws AmazonServiceException, SdkClientException {
    delegate.deleteBucketReplicationConfiguration( bucketName );
  }

  @Override
  public void deleteBucketReplicationConfiguration( final DeleteBucketReplicationConfigurationRequest request ) throws AmazonServiceException, SdkClientException {
    delegate.deleteBucketReplicationConfiguration( request );
  }

  @Override
  public boolean doesObjectExist( final String bucketName, final String objectName ) throws AmazonServiceException, SdkClientException {
    return delegate.doesObjectExist( bucketName, objectName );
  }

  @Override
  public BucketAccelerateConfiguration getBucketAccelerateConfiguration( final String bucketName ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketAccelerateConfiguration( bucketName );
  }

  @Override
  public BucketAccelerateConfiguration getBucketAccelerateConfiguration( final GetBucketAccelerateConfigurationRequest getBucketAccelerateConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketAccelerateConfiguration( getBucketAccelerateConfigurationRequest );
  }

  @Override
  public void setBucketAccelerateConfiguration( final String bucketName, final BucketAccelerateConfiguration accelerateConfiguration ) throws AmazonServiceException, SdkClientException {
    delegate.setBucketAccelerateConfiguration( bucketName, accelerateConfiguration );
  }

  @Override
  public void setBucketAccelerateConfiguration( final SetBucketAccelerateConfigurationRequest setBucketAccelerateConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    delegate.setBucketAccelerateConfiguration( setBucketAccelerateConfigurationRequest );
  }

  @Override
  public DeleteBucketMetricsConfigurationResult deleteBucketMetricsConfiguration( final String bucketName, final String id ) throws AmazonServiceException, SdkClientException {
    return delegate.deleteBucketMetricsConfiguration( bucketName, id );
  }

  @Override
  public DeleteBucketMetricsConfigurationResult deleteBucketMetricsConfiguration( final DeleteBucketMetricsConfigurationRequest deleteBucketMetricsConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.deleteBucketMetricsConfiguration( deleteBucketMetricsConfigurationRequest );
  }

  @Override
  public GetBucketMetricsConfigurationResult getBucketMetricsConfiguration( final String bucketName, final String id ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketMetricsConfiguration( bucketName, id );
  }

  @Override
  public GetBucketMetricsConfigurationResult getBucketMetricsConfiguration( final GetBucketMetricsConfigurationRequest getBucketMetricsConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketMetricsConfiguration( getBucketMetricsConfigurationRequest );
  }

  @Override
  public SetBucketMetricsConfigurationResult setBucketMetricsConfiguration( final String bucketName, final MetricsConfiguration metricsConfiguration ) throws AmazonServiceException, SdkClientException {
    return delegate.setBucketMetricsConfiguration( bucketName, metricsConfiguration );
  }

  @Override
  public SetBucketMetricsConfigurationResult setBucketMetricsConfiguration( final SetBucketMetricsConfigurationRequest setBucketMetricsConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.setBucketMetricsConfiguration( setBucketMetricsConfigurationRequest );
  }

  @Override
  public ListBucketMetricsConfigurationsResult listBucketMetricsConfigurations( final ListBucketMetricsConfigurationsRequest listBucketMetricsConfigurationsRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.listBucketMetricsConfigurations( listBucketMetricsConfigurationsRequest );
  }

  @Override
  public DeleteBucketAnalyticsConfigurationResult deleteBucketAnalyticsConfiguration( final String bucketName, final String id ) throws AmazonServiceException, SdkClientException {
    return delegate.deleteBucketAnalyticsConfiguration( bucketName, id );
  }

  @Override
  public DeleteBucketAnalyticsConfigurationResult deleteBucketAnalyticsConfiguration( final DeleteBucketAnalyticsConfigurationRequest deleteBucketAnalyticsConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.deleteBucketAnalyticsConfiguration( deleteBucketAnalyticsConfigurationRequest );
  }

  @Override
  public GetBucketAnalyticsConfigurationResult getBucketAnalyticsConfiguration( final String bucketName, final String id ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketAnalyticsConfiguration( bucketName, id );
  }

  @Override
  public GetBucketAnalyticsConfigurationResult getBucketAnalyticsConfiguration( final GetBucketAnalyticsConfigurationRequest getBucketAnalyticsConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketAnalyticsConfiguration( getBucketAnalyticsConfigurationRequest );
  }

  @Override
  public SetBucketAnalyticsConfigurationResult setBucketAnalyticsConfiguration( final String bucketName, final AnalyticsConfiguration analyticsConfiguration ) throws AmazonServiceException, SdkClientException {
    return delegate.setBucketAnalyticsConfiguration( bucketName, analyticsConfiguration );
  }

  @Override
  public SetBucketAnalyticsConfigurationResult setBucketAnalyticsConfiguration( final SetBucketAnalyticsConfigurationRequest setBucketAnalyticsConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.setBucketAnalyticsConfiguration( setBucketAnalyticsConfigurationRequest );
  }

  @Override
  public ListBucketAnalyticsConfigurationsResult listBucketAnalyticsConfigurations( final ListBucketAnalyticsConfigurationsRequest listBucketAnalyticsConfigurationsRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.listBucketAnalyticsConfigurations( listBucketAnalyticsConfigurationsRequest );
  }

  @Override
  public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration( final String bucketName, final String id ) throws AmazonServiceException, SdkClientException {
    return delegate.deleteBucketInventoryConfiguration( bucketName, id );
  }

  @Override
  public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration( final DeleteBucketInventoryConfigurationRequest deleteBucketInventoryConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.deleteBucketInventoryConfiguration( deleteBucketInventoryConfigurationRequest );
  }

  @Override
  public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration( final String bucketName, final String id ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketInventoryConfiguration( bucketName, id );
  }

  @Override
  public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration( final GetBucketInventoryConfigurationRequest getBucketInventoryConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.getBucketInventoryConfiguration( getBucketInventoryConfigurationRequest );
  }

  @Override
  public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration( final String bucketName, final InventoryConfiguration inventoryConfiguration ) throws AmazonServiceException, SdkClientException {
    return delegate.setBucketInventoryConfiguration( bucketName, inventoryConfiguration );
  }

  @Override
  public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration( final SetBucketInventoryConfigurationRequest setBucketInventoryConfigurationRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.setBucketInventoryConfiguration( setBucketInventoryConfigurationRequest );
  }

  @Override
  public ListBucketInventoryConfigurationsResult listBucketInventoryConfigurations( final ListBucketInventoryConfigurationsRequest listBucketInventoryConfigurationsRequest ) throws AmazonServiceException, SdkClientException {
    return delegate.listBucketInventoryConfigurations( listBucketInventoryConfigurationsRequest );
  }

  @Override
  public com.amazonaws.services.s3.model.Region getRegion( ) {
    return delegate.getRegion( );
  }

  @Override
  public String getRegionName( ) {
    return delegate.getRegionName( );
  }

  @Override
  public URL getUrl( final String bucketName, final String key ) {
    return delegate.getUrl( bucketName, key );
  }

  @Override
  public AmazonS3Waiters waiters( ) {
    return delegate.waiters( );
  }
}

