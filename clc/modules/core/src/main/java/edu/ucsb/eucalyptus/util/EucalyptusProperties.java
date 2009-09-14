/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.util;

import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.util.StorageProperties;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.entities.ImageInfo;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public class EucalyptusProperties {

	private static Logger LOG = Logger.getLogger( EucalyptusProperties.class );


	public static boolean disableNetworking = false;
	public static boolean disableBlockStorage = false;

	public static String DEBUG_FSTRING = "[%12s] %s";

	public enum NETWORK_PROTOCOLS {

		tcp, udp, icmp
	}

	public static String NAME_SHORT = "euca2";
	public static String FSTRING = "::[ %-20s: %-50.50s ]::\n";
	public static String IMAGE_MACHINE = "machine";
	public static String IMAGE_KERNEL = "kernel";
	public static String IMAGE_RAMDISK = "ramdisk";
	public static String IMAGE_MACHINE_PREFIX = "emi";
	public static String IMAGE_KERNEL_PREFIX = "eki";
	public static String IMAGE_RAMDISK_PREFIX = "eri";

	public static String getDName( String name ) {
		return String.format( "CN=www.eucalyptus.com, OU=Eucalyptus, O=%s, L=Santa Barbara, ST=CA, C=US", name );
	}

	public static SystemConfiguration getSystemConfiguration() {
		EntityWrapper<SystemConfiguration> confDb = new EntityWrapper<SystemConfiguration>();
		SystemConfiguration conf = null;
		try {
			conf = confDb.getUnique( new SystemConfiguration());
			validateSystemConfiguration(conf);
			confDb.commit();
		}
		catch ( EucalyptusCloudException e ) {
			LOG.warn("Failed to get system configuration. Loading defaults.");
			conf = validateSystemConfiguration(null);
			confDb.add(conf);
			confDb.commit();
		}
		catch (Throwable t) {
			LOG.error("Unable to get system configuration.");
			confDb.rollback();
			return validateSystemConfiguration(null);
		}
		return conf;
	}

	public static String getInternalIpAddress ()
	{
		String ipAddr = null;
		try {
			for( String addr : NetworkUtil.getAllAddresses( ) ) {
				ipAddr = addr;
				break;
			}
		} catch ( SocketException e ) {}
		return ipAddr == null ? "127.0.0.1" : ipAddr;
	}

	private static SystemConfiguration validateSystemConfiguration(SystemConfiguration sysConf) {
		if(sysConf == null) {
			sysConf = new SystemConfiguration();
		}
		if( sysConf.getRegistrationId() == null ) {
			sysConf.setRegistrationId( UUID.randomUUID().toString() );
		}
		if(sysConf.getCloudHost() == null) {
			String ipAddr = getInternalIpAddress ();
			sysConf.setCloudHost(ipAddr);
		}
		if(sysConf.getDefaultKernel() == null) {
			ImageInfo q = new ImageInfo();
			EntityWrapper<ImageInfo> db2 = new EntityWrapper<ImageInfo>();
			q.setImageType( EucalyptusProperties.IMAGE_KERNEL );
			List<ImageInfo> res = db2.query(q);
			if( res.size() > 0 )
				sysConf.setDefaultKernel(res.get(0).getImageId());
			db2.commit( );
		}
		if(sysConf.getDefaultRamdisk() == null) {
			ImageInfo q = new ImageInfo();
			EntityWrapper<ImageInfo> db2 = new EntityWrapper<ImageInfo>();
			q.setImageType( EucalyptusProperties.IMAGE_RAMDISK );
			List<ImageInfo> res = db2.query(q);
			if( res.size() > 0 )
				sysConf.setDefaultRamdisk(res.get(0).getImageId());
			db2.commit( );
		}
		if(sysConf.getDnsDomain() == null) {
			sysConf.setDnsDomain(DNSProperties.DOMAIN);
		}
		if(sysConf.getNameserver() == null) {
			sysConf.setNameserver(DNSProperties.NS_HOST);
		}
		if(sysConf.getNameserverAddress() == null) {
			sysConf.setNameserverAddress(DNSProperties.NS_IP);
		}
		if( sysConf.getMaxUserPublicAddresses() == null ) {
			sysConf.setMaxUserPublicAddresses( 5 );
		}
		if( sysConf.isDoDynamicPublicAddresses() == null ) {
			sysConf.setDoDynamicPublicAddresses( true );
		}
		if( sysConf.getSystemReservedPublicAddresses() == null ) {
			sysConf.setSystemReservedPublicAddresses( 10 );
		}
		if(sysConf.getZeroFillVolumes() == null) {
			sysConf.setZeroFillVolumes(StorageProperties.zeroFillVolumes);
		}
		return sysConf;
	}

	public static String getCloudUrl() {
		try {
			String cloudHost = EucalyptusProperties.getSystemConfiguration( ).getCloudHost( );
			if( cloudHost == null ) {
				for( WalrusConfiguration w : Configuration.getWalrusConfigurations( ) ) {
					if( NetworkUtil.testLocal( w.getHostName( ) ) ) {
						cloudHost = w.getHostName( );
						break;
					}
				}
			}
			if( cloudHost == null ) {
				try {
					cloudHost = NetworkUtil.getAllAddresses( ).get( 0 );
				} catch ( SocketException e ) {}
			}
			return String.format( "http://%s:8773/services/Eucalyptus", cloudHost );
		} catch ( EucalyptusCloudException e ) {
			return "http://127.0.0.1:8773/services/Eucalyptus";
		}
	}

	public static String getWalrusUrl() {
		String cloudHost = getCloudHost( );
		return String.format( "http://%s:8773/services/Walrus", cloudHost == null ? "127.0.0.1" : cloudHost );
	}

	private static String getCloudHost( ) {
		String cloudHost = null;
		try {
			cloudHost = EucalyptusProperties.getSystemConfiguration( ).getCloudHost( );
			if( cloudHost == null ) {
				for( WalrusConfiguration w : Configuration.getWalrusConfigurations( ) ) {
					if( NetworkUtil.testLocal( w.getHostName( ) ) ) {
						cloudHost = w.getHostName( );
						break;
					}
				}
			}
		} catch ( EucalyptusCloudException e ) {
		}
		if( cloudHost == null ) {
			try {
				cloudHost = NetworkUtil.getAllAddresses( ).get( 0 );
			} catch ( SocketException e ) {}
		}
		return cloudHost;
	}

	public enum TokenState {

		preallocate, returned, accepted, submitted, allocated, redeemed;
	}
}
