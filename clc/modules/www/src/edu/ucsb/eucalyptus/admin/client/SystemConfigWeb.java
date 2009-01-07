package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * User: decker
 * Date: Dec 9, 2008
 * Time: 4:08:38 AM
 */
public class SystemConfigWeb implements IsSerializable {
  private String storageUrl;
  private String storagePath;
  private Integer storageMaxBucketsPerUser;
  private Integer storageMaxBucketSizeInMB;
  private Integer storageMaxCacheSizeInMB;
  private String defaultKernelId;
  private String defaultRamdiskId;

  public SystemConfigWeb()
  {
  }

  public SystemConfigWeb( final String storageUrl, final String storagePath, final int storageMaxBucketsPerUser, final int storageMaxBucketSizeInMB, final int storageMaxCacheSizeInMB, final String defaultKernelId, final String defaultRamdiskId )
  {
    this.storageUrl = storageUrl;
    this.storagePath = storagePath;
	this.storageMaxBucketsPerUser = storageMaxBucketsPerUser;
	this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
	this.storageMaxCacheSizeInMB = storageMaxCacheSizeInMB;
    this.defaultKernelId = defaultKernelId;
    this.defaultRamdiskId = defaultRamdiskId;
  }

  public String getStorageUrl()
  {
    return storageUrl;
  }

  public void setStorageUrl( final String storageUrl )
  {
    this.storageUrl = storageUrl;
  }

  public String getStoragePath()
  {
    return storagePath;
  }

  public void setStoragePath( final String storagePath )
  {
    this.storagePath = storagePath;
  }

  public Integer getStorageMaxBucketSizeInMB()
  {
    return storageMaxBucketSizeInMB;
  }

  public void setStorageMaxBucketSizeInMB( final Integer storageMaxBucketSizeInMB )
  {
    this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
  }

  public Integer getStorageMaxBucketsPerUser()
  {
    return storageMaxBucketsPerUser;
  }

  public void setStorageMaxBucketsPerUser( final Integer storageMaxBucketsPerUser )
  {
    this.storageMaxBucketsPerUser = storageMaxBucketsPerUser;
  }

  public Integer getStorageMaxCacheSizeInMB()
  {
    return storageMaxCacheSizeInMB;
  }

  public void setStorageMaxCacheSizeInMB( final Integer storageMaxCacheSizeInMB )
  {
    this.storageMaxCacheSizeInMB = storageMaxCacheSizeInMB;
  }

  public String getDefaultKernelId()
  {
    return defaultKernelId;
  }

  public void setDefaultKernelId( final String defaultKernelId )
  {
    this.defaultKernelId = defaultKernelId;
  }

  public String getDefaultRamdiskId()
  {
    return defaultRamdiskId;
  }

  public void setDefaultRamdiskId( final String defaultRamdiskId )
  {
    this.defaultRamdiskId = defaultRamdiskId;
  }
}
