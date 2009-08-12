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
    private String walrusUrl;
    private String bucketsRootDirectory;
    private Integer maxBucketsPerUser;
    private Integer maxBucketSizeInMB;
    private Integer maxCacheSizeInMB;
    private Integer snapshotsTotalInGB;
    private String defaultKernelId;
    private String defaultRamdiskId;
    private Integer maxUserPublicAddresses;
    private Boolean doDynamicPublicAddresses;
    private Integer systemReservedPublicAddresses;
    private Boolean zeroFillVolumes;
    private String dnsDomain;
    private String nameserver;
    private String nameserverAddress;

    public SystemConfigWeb() {}

    public SystemConfigWeb( final String walrusUrl,
                            final String bucketsRootDirectory,
                            final Integer maxBucketsPerUser,
                            final Integer maxBucketSizeInMB,
                            final Integer maxCacheSizeInMB,
                            final Integer snapshotsTotalInGB,
                            final String defaultKernelId,
                            final String defaultRamdiskId,
                            final Integer maxUserPublicAddresses,
                            final Boolean doDynamicPublicAddresses,
                            final Integer systemReservedPublicAddresses,
                            final Boolean zeroFillVolumes,
                            final String dnsDomain,
                            final String nameserver,
                            final String nameserverAddress)
    {
        this.walrusUrl = walrusUrl;
        this.bucketsRootDirectory = bucketsRootDirectory;
        this.maxBucketsPerUser = maxBucketsPerUser;
        this.maxBucketSizeInMB = maxBucketSizeInMB;
        this.maxCacheSizeInMB = maxCacheSizeInMB;
        this.snapshotsTotalInGB = snapshotsTotalInGB;
        this.defaultKernelId = defaultKernelId;
        this.defaultRamdiskId = defaultRamdiskId;
        this.dnsDomain = dnsDomain;
        this.nameserver = nameserver;
        this.nameserverAddress = nameserverAddress;
        this.maxUserPublicAddresses = maxUserPublicAddresses;
        this.systemReservedPublicAddresses = systemReservedPublicAddresses;
        this.doDynamicPublicAddresses = doDynamicPublicAddresses;
        this.zeroFillVolumes = zeroFillVolumes;
    }

  public String getWalrusUrl()
    {
        return walrusUrl;
    }

    public void setWalrusUrl( final String walrusUrl )
    {
        this.walrusUrl = walrusUrl;
    }

    public String getBucketsRootDirectory()
    {
        return bucketsRootDirectory;
    }

    public void setBucketsRootDirectory( final String bucketsRootDirectory )
    {
        this.bucketsRootDirectory = bucketsRootDirectory;
    }

    public Integer getMaxBucketSizeInMB()
    {
        return maxBucketSizeInMB;
    }

    public void setMaxBucketSizeInMB( final Integer maxBucketSizeInMB )
    {
        this.maxBucketSizeInMB = maxBucketSizeInMB;
    }

    public Integer getMaxBucketsPerUser()
    {
        return maxBucketsPerUser;
    }

    public void setMaxBucketsPerUser( final Integer maxBucketsPerUser )
    {
        this.maxBucketsPerUser = maxBucketsPerUser;
    }

    public Integer getMaxCacheSizeInMB()
    {
        return maxCacheSizeInMB;
    }

    public void setMaxCacheSizeInMB( final Integer maxCacheSizeInMB )
    {
        this.maxCacheSizeInMB = maxCacheSizeInMB;
    }

    public Integer getSnapshotsTotalInGB()
    {
        return snapshotsTotalInGB;
    }

    public void setSnapshotsTotalInGB( final Integer snapshotsTotalInGB )
    {
        this.snapshotsTotalInGB = snapshotsTotalInGB;
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

	public Boolean getZeroFillVolumes() {
		return zeroFillVolumes;
	}

	public void setZeroFillVolumes(Boolean zeroFillVolumes) {
		this.zeroFillVolumes = zeroFillVolumes;
	}    
}
