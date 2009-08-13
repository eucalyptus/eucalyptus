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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "system_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class SystemConfiguration {
    @Id
    @GeneratedValue
    @Column( name = "system_info_id" )
    private Long id = -1l;
    @Column( name = "system_info_storage_url" )
    private String storageUrl;
    @Column( name = "system_info_default_kernel" )
    private String defaultKernel;
    @Column( name = "system_info_default_ramdisk" )
    private String defaultRamdisk;
    @Column( name = "system_registration_id" )
    private String registrationId;
    @Column( name = "system_max_user_public_addresses" )
    private Integer maxUserPublicAddresses;
    @Column( name = "system_do_dynamic_public_addresses" )
    private Boolean doDynamicPublicAddresses;
    @Column( name = "system_reserved_public_addresses" )
    private Integer systemReservedPublicAddresses;
    @Column( name = "zero_fill_volumes" )
    private Boolean zeroFillVolumes;
    @Column( name = "dns_domain" )
    private String dnsDomain;
    @Column( name = "nameserver" )
    private String nameserver;
    @Column( name = "ns_address" )
    private String nameserverAddress;

    public SystemConfiguration() {}

    public SystemConfiguration(	final String storageUrl,
                                   final String defaultKernel,
                                   final String defaultRamdisk,
                                   final Integer maxUserPublicAddresses,
                                   final Boolean doDynamicPublicAddresses,
                                   final Integer systemReservedPublicAddresses,
                                   final Boolean zeroFillVolumes,
                                   final String dnsDomain,
                                   final String nameserver,
                                   final String nameserverAddress)
    {
        this.storageUrl = storageUrl;
        this.defaultKernel = defaultKernel;
        this.defaultRamdisk = defaultRamdisk;
        this.maxUserPublicAddresses = maxUserPublicAddresses;
        this.doDynamicPublicAddresses = doDynamicPublicAddresses;
        this.systemReservedPublicAddresses = systemReservedPublicAddresses;
        this.dnsDomain = dnsDomain;
        this.zeroFillVolumes = zeroFillVolumes;
        this.nameserver = nameserver;
        this.nameserverAddress = nameserverAddress;
    }

    public Long getId() {
        return id;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public String getDefaultKernel() {
        return defaultKernel;
    }

    public String getDefaultRamdisk() {
        return defaultRamdisk;
    }

    public void setStorageUrl( final String storageUrl ) {
        this.storageUrl = storageUrl;
    }

    public void setDefaultKernel( final String defaultKernel ) {
        this.defaultKernel = defaultKernel;
    }

    public void setDefaultRamdisk( final String defaultRamdisk ) {
        this.defaultRamdisk = defaultRamdisk;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId( final String registrationId ) {
        this.registrationId = registrationId;
    }

    public Integer getMaxUserPublicAddresses() {
        return maxUserPublicAddresses;
    }

    public void setMaxUserPublicAddresses( final Integer maxUserPublicAddresses ) {
        this.maxUserPublicAddresses = maxUserPublicAddresses;
    }

    public Integer getSystemReservedPublicAddresses() {
        return systemReservedPublicAddresses;
    }

    public void setSystemReservedPublicAddresses( final Integer systemReservedPublicAddresses ) {
        this.systemReservedPublicAddresses = systemReservedPublicAddresses;
    }

    public Boolean isDoDynamicPublicAddresses() {
        return doDynamicPublicAddresses;
    }

    public void setDoDynamicPublicAddresses( final Boolean doDynamicPublicAddresses ) {
        this.doDynamicPublicAddresses = doDynamicPublicAddresses;
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

	public Boolean getZeroFillVolumes() {
		return zeroFillVolumes;
	}

	public void setZeroFillVolumes(Boolean zeroFillVolumes) {
		this.zeroFillVolumes = zeroFillVolumes;
	}
}
