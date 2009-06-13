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
    private Integer storageSnapshotsTotalInGB;
    private Integer storageVolumesTotalInGB;
    private Integer storageMaxVolumeSizeInGB;
    private String storageVolumesPath;
    private String defaultKernelId;
    private String defaultRamdiskId;
    private Integer maxUserPublicAddresses;
    private boolean doDynamicPublicAddresses;
    private Integer systemReservedPublicAddresses;


    public SystemConfigWeb() {}

    public SystemConfigWeb( final String storageUrl,
                            final String storagePath,
                            final Integer storageMaxBucketsPerUser,
                            final Integer storageMaxBucketSizeInMB,
                            final Integer storageMaxCacheSizeInMB,
                            final Integer storageSnapshotsTotalInGB,
                            final Integer storageVolumesTotalInGB,
                            final Integer storageMaxVolumeSizeInGB,
                            final String storageVolumesPath,
                            final String defaultKernelId,
                            final String defaultRamdiskId,
                            final Integer maxUserPublicAddresses,
                            final Boolean doDynamicPublicAddresses,
                            final Integer systemReservedPublicAddresses )
    {
        this.storageUrl = storageUrl;
        this.storagePath = storagePath;
        this.storageMaxBucketsPerUser = storageMaxBucketsPerUser;
        this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
        this.storageMaxCacheSizeInMB = storageMaxCacheSizeInMB;
        this.storageSnapshotsTotalInGB = storageSnapshotsTotalInGB;
        this.storageVolumesTotalInGB = storageVolumesTotalInGB;
        this.storageMaxVolumeSizeInGB = storageMaxVolumeSizeInGB;
        this.storageVolumesPath = storageVolumesPath;
        this.defaultKernelId = defaultKernelId;
        this.defaultRamdiskId = defaultRamdiskId;
        this.maxUserPublicAddresses = maxUserPublicAddresses;
        this.systemReservedPublicAddresses = systemReservedPublicAddresses;
        this.doDynamicPublicAddresses = doDynamicPublicAddresses;
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

    public Integer getStorageSnapshotsTotalInGB()
    {
        return storageSnapshotsTotalInGB;
    }

    public void setStorageSnapshotsTotalInGB( final Integer storageSnapshotsTotalInGB )
    {
        this.storageSnapshotsTotalInGB = storageSnapshotsTotalInGB;
    }

    public Integer getStorageVolumesTotalInGB()
    {
        return storageVolumesTotalInGB;
    }

    public void setStorageVolumesTotalInGB( final Integer storageVolumesTotalInGB )
    {
        this.storageVolumesTotalInGB = storageVolumesTotalInGB;
    }

    public Integer getStorageMaxVolumeSizeInGB()
    {
        return storageMaxVolumeSizeInGB;
    }

    public void setStorageMaxVolumeSizeInGB( final Integer storageMaxVolumeSizeInGB )
    {
        this.storageMaxVolumeSizeInGB = storageMaxVolumeSizeInGB;
    }

    public String getStorageVolumesPath()
    {
        return storageVolumesPath;
    }

    public void setStorageVolumesPath( final String storageVolumesPath )
    {
        this.storageVolumesPath = storageVolumesPath;
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

    public Integer getMaxUserPublicAddresses() {
      return maxUserPublicAddresses;
    }

    public void setMaxUserPublicAddresses( final Integer maxUserPublicAddresses ) {
      this.maxUserPublicAddresses = maxUserPublicAddresses;
    }

    public Boolean isDoDynamicPublicAddresses() {
      return doDynamicPublicAddresses;
    }

    public void setDoDynamicPublicAddresses( final Boolean doDynamicPublicAddresses ) {
      this.doDynamicPublicAddresses = doDynamicPublicAddresses;
    }

    public Integer getSystemReservedPublicAddresses() {
      return systemReservedPublicAddresses;
    }

    public void setSystemReservedPublicAddresses( final Integer systemReservedPublicAddresses ) {
      this.systemReservedPublicAddresses = systemReservedPublicAddresses;
    }
}
