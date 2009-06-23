/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */
package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

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
    private String dnsDomain;
    private String nameserver;
    private String nameserverAddress;

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
                            final Integer systemReservedPublicAddresses,
                            final String dnsDomain,
                            final String nameserver,
                            final String nameserverAddress)
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
        this.dnsDomain = dnsDomain;
        this.nameserver = nameserver;
        this.nameserverAddress = nameserverAddress;
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

    public String getDnsDomain() {
        return dnsDomain;
    }

    public void setDnsDomain(String dnsDomain) {
        this.dnsDomain = dnsDomain;
    }

    public String getNameserver() {
        return nameserver;
    }

    public void setNameserver(String nameserver) {
        this.nameserver = nameserver;
    }

    public String getNameserverAddress() {
        return nameserverAddress;
    }

    public void setNameserverAddress(String nameserverAddress) {
        this.nameserverAddress = nameserverAddress;

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
