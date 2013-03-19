/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.loadbalancing;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.eucalyptus.dns.SetResponse;
import com.eucalyptus.dns.TransientPtrZone;
import com.eucalyptus.dns.Zone;
import com.eucalyptus.util.Internets;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

public class LoadBalancerDNSZone extends TransientPtrZone {
	private static Logger LOG = Logger.getLogger( LoadBalancerDNSZone.class );
	private static int ttl               = 604800;
	private static ConcurrentHashMap<String, LBAddresses> lbDNSMap = new ConcurrentHashMap<String, LBAddresses>();
	private static ConcurrentHashMap<InetAddress, String> reverseLBDNSMap = new ConcurrentHashMap<InetAddress, String>();

	public LoadBalancerDNSZone( Name name, Record[] records ) throws IOException {
		super(name,records);
	}

	public static Zone getZone( ) {
		try {
			Name name = getName( );
			long serial = 1;
			long refresh = 86400;
			long retry = ttl;
			long expires = 2419200;
			//This is the negative cache TTL
			long minimum = 600;
			Record soarec = new SOARecord( name, DClass.IN, ttl, name, Name.fromString( "root." + name.toString( ) ), serial,
					refresh, retry, expires, minimum );
			long nsTTL = 604800;
			Record nsrec = new NSRecord( name, DClass.IN, nsTTL,
					Name.fromString( Internets.localHostInetAddress( ).getCanonicalHostName( )+"." ) );
			return new LoadBalancerDNSZone( name, new Record[] { soarec, nsrec } );
		} catch ( Exception e ) {
			LOG.error( e, e );
			return null;
		} 
	}

	public static Name getName( ) throws TextParseException {
		//TODO: "elb." is a placeholder. Modify as necessary.
		String nameString = "elb." + SystemConfiguration.getSystemConfiguration( ).getDnsDomain( ) + ".";
		Name name = Name.fromString( nameString );
		return name;
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.dns.Zone#findRecords(org.xbill.DNS.Name, int)
	 */
	@Override
	public SetResponse findRecords( Name name, int type ) {
		if( name.toString( ).matches(".*\\.elb\\..*") ) {
			try {
				if(type == Type.AAAA)
					return(SetResponse.ofType(SetResponse.SUCCESSFUL));

				String dnsName = name.toString( );
				//grab an IP from map
				InetAddress ip = getNextAddress(dnsName);
				if(ip != null) {
					SetResponse resp = new SetResponse(SetResponse.SUCCESSFUL);
					resp.addRRset( new RRset( new ARecord( name, 1, ttl, ip ) ) );
					return resp;
				} else {
					return super.findRecords( name, type );
				}
			} catch ( Exception e ) {
				return super.findRecords( name, type );
			}
		} else if (name.toString().endsWith(".in-addr.arpa.")) {
			int index = name.toString().indexOf(".in-addr.arpa.");
			Name target = null;
			if ( index > 0 ) {
				String ipString = name.toString().substring(0, index);
				String[] parts = ipString.split("\\.");
				String ipCandidate;
				if (parts.length == 4) {
					ipCandidate = new StringBuffer()
					.append(parts[3]).append(".")
					.append(parts[2]).append(".")
					.append(parts[1]).append(".")
					.append(parts[0]).toString( );		  	
				} else {
					return super.findRecords( name, type );
				}
				InetAddress ip = null;
				try {
					ip = InetAddress.getByName( ipCandidate );
				} catch (UnknownHostException e) {
					LOG.error(e, e);
					return super.findRecords( name, type );
				}
				String targetName = reverseLBDNSMap.get(ip);
				if(targetName != null) {
					try {
						target = new Name(targetName);
					} catch (TextParseException e) {
						LOG.error(e, e);
						return super.findRecords( name, type );
					}
					SetResponse resp = new SetResponse(SetResponse.SUCCESSFUL);
					resp.addRRset( new RRset( new PTRRecord( name, DClass.IN, ttl, target ) ) );
					return resp;
				} else {
					//ask super, in this case TransientPtrZone
					return super.findRecords( name, type );
				}
			} else {
				return super.findRecords( name, type );
			}
		} else {
			return super.findRecords( name, type );
		}
	}

	public static Zone getPtrZone(Name queryName) {
		try {
			String nameString = queryName.toString();
			Name name;
			int index = nameString.indexOf(".in-addr.arpa.");
			if ( index > 0 ) {
				String ipString = nameString.substring(0, index);
				String[] parts = ipString.split("\\.");
				//fix this for v6
				if(parts.length == 4) {
					nameString = nameString.substring(parts[0].length() + 1);
					name = new Name(nameString);    	 
				} else {
					return null;
				}
			} else {
				return null;
			}
			long serial = 1;
			long refresh = 86400;
			long retry = ttl;
			long expires = 2419200;
			//This is the negative cache TTL
			long minimum = 600;
			Record soarec = new SOARecord( name, DClass.IN, ttl, name, Name.fromString( "root." + name.toString( ) ), serial,
					refresh, retry, expires, minimum );
			long nsTTL = 604800;
			Record nsrec = new NSRecord( name, DClass.IN, nsTTL,
					Name.fromString( Internets.localHostInetAddress( ).getCanonicalHostName( ) +".") );

			//testy
			addARecord("hi.elb." + SystemConfiguration.getSystemConfiguration( ).getDnsDomain( ) + ".", InetAddress.getByName("192.168.1.11"));

			return new LoadBalancerDNSZone( name, new Record[] { soarec, nsrec } );
		} catch ( Exception e ) {
			LOG.error( e, e );
			return null;
		}
	}

	public static void addARecord(String name, InetAddress address) {
		if(lbDNSMap.containsKey(name)) {
			lbDNSMap.get(name).addAddress(address);
		} else {
			LBAddresses addrs = new LBAddresses();
			addrs.addAddress(address);
			lbDNSMap.put(name, addrs);
		}
		if(reverseLBDNSMap.containsKey(address)) {
			reverseLBDNSMap.replace(address, name);
		} else {
			reverseLBDNSMap.put(address, name);
		}
	}


	public static void addARecord(String name, List<InetAddress> addresses){
		if(lbDNSMap.containsKey(name)) {
			lbDNSMap.get(name).pruneAddress(addresses);
		} else {
			LBAddresses addrs = new LBAddresses();
			addrs.addAddress(addresses);
			lbDNSMap.put(name, addrs);
		}
		for (InetAddress addr : addresses) {
			if(reverseLBDNSMap.containsKey(addr)) {
				reverseLBDNSMap.replace(addr, name);
			} else {
				reverseLBDNSMap.put(addr, name);
			}
		}
	}

	public static void removeARecord(String name, InetAddress address) {
		if(lbDNSMap.containsKey(name)) {
			lbDNSMap.get(name).deleteAddress(address);
		}
		reverseLBDNSMap.remove(address);
	}

	private static InetAddress getNextAddress(String name) {
		if (lbDNSMap.containsKey(name)) {
			return lbDNSMap.get(name).getNextAddress();
		}
		return null;
	}

	//This is a simple implementation that returns the next 
	//element % size. It is not thread safe and any reduction
	//in the size of the list will reset the marker
	private static class LBAddresses {
		private ArrayList<InetAddress> addresses;
		private int nextMarker;

		public LBAddresses() {
			addresses =  new ArrayList<InetAddress>();
			nextMarker = 0;
		}

		public InetAddress getNextAddress() {
			InetAddress get = addresses.get(nextMarker);
			nextMarker = (nextMarker + 1) % addresses.size();
			return get;
		}

		public void deleteAddress(InetAddress addr) {
			if(addresses.contains(addr)) {
				//reset nextMarker because we don't want out of bounds issues
				//it is best for get to be non blocking and a removal to force
				//a start from the beginning
				nextMarker = 0;
			}
			if(addresses.remove(addr));
		}

		//This is not thread safe. This should not be an issue
		//in the case of LB since there is only one 
		//writer, but be careful
		public void addAddress(InetAddress addr) {
			if(!addresses.contains(addr)) {
				addresses.add(addr);
			}
		}

		public void pruneAddress(List<InetAddress> addr) {
			nextMarker = 0;
			addresses.retainAll(addr);
		}

		public void addAddress(List<InetAddress> addr) {
			nextMarker = 0;
			addresses.addAll(addr);
		}
	}

}
