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

package com.eucalyptus.cloud.ws;

import com.eucalyptus.util.DNSProperties;

import edu.ucsb.eucalyptus.cloud.entities.*;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.dns.ServiceZone;
import com.eucalyptus.dns.TransientZone;
import com.eucalyptus.dns.Zone;
import com.eucalyptus.entities.EntityWrapper;

import org.xbill.DNS.Address;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.RRset;
import org.xbill.DNS.DClass;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneManager {
	private static ConcurrentHashMap<Name, Zone> zones = new ConcurrentHashMap<Name, Zone>();
	private static Logger LOG = Logger.getLogger( ZoneManager.class );

	public static Zone getZone(String name) {
		try {
			return getZone(new Name(name));
					//zones.get(new Name(name));
		} catch(Exception ex) {
			LOG.error(ex);
		}
		return null;
	}

	public static Zone getZone(Name name) {

		if ( Bootstrap.isFinished( ) ) {
			if(name.toString().endsWith("in-addr.arpa.")) {
				//create new transient zone to handle reverse lookups
				return TransientZone.getPtrZone(name);
			} else {
				try {
					if(!ZoneManager.zones.containsKey(TransientZone.getExternalName())){
						ZoneManager.registerZone(TransientZone.getExternalName( ),TransientZone.getInstanceExternalZone());
					} else if(!ZoneManager.zones.containsKey(TransientZone.getInternalName())){
						ZoneManager.registerZone(TransientZone.getInternalName(),TransientZone.getInstanceInternalZone());
					} else if(!ZoneManager.zones.containsKey(ServiceZone.getName())){
						ZoneManager.registerZone(ServiceZone.getName(),ServiceZone.getZone());
					}	
				} catch(Exception e) {
					LOG.debug( e, e );
				}
			}
			return zones.get(name);
		} else {
			return null;
		}
	}

	public static void registerZone( Name name, Zone z ) {
		zones.putIfAbsent( name, z );
	}

	public static void addZone(ZoneInfo zoneInfo, SOARecordInfo soaRecordInfo, NSRecordInfo nsRecordInfo) {
		try {
			String nameString = zoneInfo.getName();
			Name name =  Name.fromString(nameString);
			long soaTTL = soaRecordInfo.getTtl();
			long serial = soaRecordInfo.getSerialNumber();
			long refresh = soaRecordInfo.getRefresh();
			long retry = soaRecordInfo.getRetry();
			long expires = soaRecordInfo.getExpires();
			long minimum = soaRecordInfo.getMinimum();
			Record soarec = new SOARecord(name, DClass.IN, soaTTL, name, Name.fromString("root." + name.toString()), serial, refresh, retry, expires, minimum);
			long nsTTL = nsRecordInfo.getTtl();
			Record nsrec = new NSRecord(name, DClass.IN, nsTTL, Name.fromString(nsRecordInfo.getTarget()));
			Zone zone = new Zone(name, new Record[]{soarec, nsrec});
			zones.putIfAbsent(name, zone);
		} catch(Exception ex) {
			LOG.error(ex);
		}
	}

	public static void addRecord(ARecordInfo arecInfo, boolean multiRec) {
		try {
			ARecord arecord = new ARecord(Name.fromString(arecInfo.getName()), DClass.IN, arecInfo.getTtl(), Address.getByAddress(arecInfo.getAddress()));
			addRecord(arecInfo.getZone(), arecord, multiRec);
		} catch(Exception ex) {
			LOG.error(ex);
		}
	}

	public static void addRecord(String nameString, Record record, boolean multiRec) {
		Zone zone = getZone(nameString);
		if(zone == null) {
			try {
				Record[] records = new Record[1];
				records[0] = record;
				Name name =  Name.fromString(nameString);
				long soaTTL = 604800;
				long serial = 1;
				long refresh = 604800;
				long retry = 86400;
				long expires = 2419200;
				long minimum = 604800;
				Record soarec = new SOARecord(name, DClass.IN, soaTTL, name, Name.fromString("root." + nameString), serial, refresh, retry, expires, minimum);
				long nsTTL = soaTTL;
				String nsHost = DNSProperties.NS_HOST + ".";
				Name nsName = Name.fromString(nsHost);
				Record nsrec = new NSRecord(name, DClass.IN, nsTTL, nsName);
				ARecord nsARecord = new ARecord(nsName, DClass.IN, nsTTL, Address.getByAddress(DNSProperties.NS_IP));
			
				zone = new Zone(name, new Record[]{soarec, nsrec, nsARecord, record});
				zone =  zones.putIfAbsent(name, zone);
				if(zone == null) {
					zone = zones.get(name);
					EntityWrapper<ZoneInfo> db = EntityWrapper.get(ZoneInfo.class);
					ZoneInfo zoneInfo = new ZoneInfo(nameString);
					db.add(zoneInfo);
					EntityWrapper<SOARecordInfo> dbSOA = db.recast(SOARecordInfo.class);
					SOARecordInfo soaRecordInfo = new SOARecordInfo();
					soaRecordInfo.setName(nameString);
					soaRecordInfo.setRecordclass(DClass.IN);
					soaRecordInfo.setNameserver(nameString);
					soaRecordInfo.setAdmin("root." + nameString);
					soaRecordInfo.setZone(nameString);
					soaRecordInfo.setSerialNumber(serial);
					soaRecordInfo.setTtl(soaTTL);
					soaRecordInfo.setExpires(expires);
					soaRecordInfo.setMinimum(minimum);
					soaRecordInfo.setRefresh(refresh);
					soaRecordInfo.setRetry(retry);
					dbSOA.add(soaRecordInfo);

					EntityWrapper<NSRecordInfo> dbNS = db.recast(NSRecordInfo.class);
					NSRecordInfo nsRecordInfo = new NSRecordInfo();
					nsRecordInfo.setName(nameString);
					nsRecordInfo.setZone(nameString);
					nsRecordInfo.setRecordClass(DClass.IN);
					nsRecordInfo.setTarget(nsHost);
					nsRecordInfo.setTtl(nsTTL);
					dbNS.add(nsRecordInfo);

					EntityWrapper<ARecordInfo> dbARecord = db.recast(ARecordInfo.class);
					ARecordInfo aRecordInfo = new ARecordInfo();
					aRecordInfo.setName(nsHost);
					aRecordInfo.setAddress(DNSProperties.NS_IP);
					aRecordInfo.setTtl(nsTTL);
					aRecordInfo.setZone(nameString);
					aRecordInfo.setRecordclass(DClass.IN);
					dbARecord.add(aRecordInfo);

					db.commit();
				}
			} catch(Exception ex) {
				LOG.error(ex);
			}
		} else {
			if(multiRec || (zone.findExactMatch(record.getName(), record.getType()) == null)) {
				zone.addRecord(record);
			}
		}
	}

	public static void updateARecord(String zoneName, ARecord record) {
		try {
			Zone zone = getZone(zoneName);
			if(zone == null)
				return;
			RRset rrSet = zone.findExactMatch(record.getName(), record.getDClass());
			if(rrSet != null) {
				Iterator<Record> rrIterator = rrSet.rrs();
				Record recordToRemove = null;
				while(rrIterator.hasNext()) {
					Record rec = rrIterator.next();
					if(rec.getName().equals(record.getName())) {
						recordToRemove = rec;            
					}
				}
				if(recordToRemove != null) 
					zone.removeRecord(recordToRemove);
				zone.addRecord(record);
				//now change the persistent store
				EntityWrapper<ARecordInfo> db = EntityWrapper.get(ARecordInfo.class);
				ARecordInfo arecInfo = new ARecordInfo();
				arecInfo.setZone(zoneName);
				arecInfo.setName(record.getName().toString());
				ARecordInfo foundARecInfo = db.getUnique(arecInfo);
				foundARecInfo.setName(record.getName().toString());
				InetAddress address = record.getAddress();
				if(address != null)
					foundARecInfo.setAddress(address.toString());
				foundARecInfo.setRecordclass(record.getDClass());
				foundARecInfo.setTtl(record.getTTL());
				db.commit();
			}
		} catch(Exception ex) {
			LOG.error(ex);
		}
	}


	public static void updateCNAMERecord(String zoneName, CNAMERecord record) {
		try {
			Zone zone = getZone(zoneName);
			if(zone == null)
				return;
			RRset rrSet = zone.findExactMatch(record.getName(), record.getDClass());
			if(rrSet != null) {
				Iterator<Record> rrIterator = rrSet.rrs();
				Record recordToRemove = null;
				while(rrIterator.hasNext()) {
					Record rec = rrIterator.next();
					if(rec.getName().equals(record.getName())) {
						recordToRemove = rec;
					}
				}
				if(recordToRemove != null)
					zone.removeRecord(recordToRemove);
				zone.addRecord(record);
				//now change the persistent store
				EntityWrapper<CNAMERecordInfo> db = EntityWrapper.get(CNAMERecordInfo.class);
				CNAMERecordInfo cnameRecordInfo = new CNAMERecordInfo();
				cnameRecordInfo.setZone(zoneName);
				cnameRecordInfo.setName(record.getName().toString());
				CNAMERecordInfo foundCNAMERecInfo = db.getUnique(cnameRecordInfo);
				foundCNAMERecInfo.setName(record.getName().toString());
				foundCNAMERecInfo.setAlias(record.getAlias().toString());
				foundCNAMERecInfo.setRecordclass(record.getDClass());
				foundCNAMERecInfo.setTtl(record.getTTL());
				db.commit();
			}
		} catch(Exception ex) {
			LOG.error(ex);
		}
	}
	
	public static void deleteARecord(String zoneName, ARecord record){
		try {
			Zone zone = getZone(zoneName);
			if(zone == null)
				return;
			RRset rrSet = zone.findExactMatch(record.getName(), record.getDClass());
			if(rrSet != null) {
				Iterator<Record> rrIterator = rrSet.rrs();
				Record recordToRemove = null;
				while(rrIterator.hasNext()) {
					final Record rec = rrIterator.next();
					if(rec instanceof ARecord){
						ARecord aRec = (ARecord)rec;
						if(aRec.getName().equals(record.getName()) &&
								aRec.getAddress().equals(record.getAddress())){
							recordToRemove = rec;	
						}
					}
				}
				if(recordToRemove != null)
					zone.removeRecord(recordToRemove);
			}
		} catch(Exception ex) {
			LOG.error(ex);
		}    
	}

	public static void deleteRecord(String zoneName, Record record) {
		try {
			Zone zone = getZone(zoneName);
			if(zone == null)
				return;
			RRset rrSet = zone.findExactMatch(record.getName(), record.getDClass());
			if(rrSet != null) {
				Iterator<Record> rrIterator = rrSet.rrs();
				Record recordToRemove = null;
				while(rrIterator.hasNext()) {
					Record rec = rrIterator.next();
					if(rec.getName().equals(record.getName())) {
						recordToRemove = rec;
					}
				}
				if(recordToRemove != null)
					zone.removeRecord(recordToRemove);
			}
		} catch(Exception ex) {
			LOG.error(ex);
		}        
	}

	public static void deleteZone(String zoneName) {
		try {
			zones.remove(new Name(zoneName));
		} catch(Exception ex) {
			LOG.error(ex);
		}
	}

}
