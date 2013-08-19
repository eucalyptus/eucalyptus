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

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Address;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.objectstorage.exceptions.AccessDeniedException;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.entities.ARecordAddressInfo;
import edu.ucsb.eucalyptus.cloud.entities.ARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.ARecordNameInfo;
import edu.ucsb.eucalyptus.cloud.entities.CNAMERecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.NSRecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.SOARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.ZoneInfo;
import edu.ucsb.eucalyptus.msgs.AddMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.AddMultiARecordType;
import edu.ucsb.eucalyptus.msgs.AddZoneResponseType;
import edu.ucsb.eucalyptus.msgs.AddZoneType;
import edu.ucsb.eucalyptus.msgs.CreateMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.CreateMultiARecordType;
import edu.ucsb.eucalyptus.msgs.DeleteZoneResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteZoneType;
import edu.ucsb.eucalyptus.msgs.RemoveARecordResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveARecordType;
import edu.ucsb.eucalyptus.msgs.RemoveCNAMERecordResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveCNAMERecordType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiANameResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiANameType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiARecordType;
import edu.ucsb.eucalyptus.msgs.UpdateARecordResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateARecordType;
import edu.ucsb.eucalyptus.msgs.UpdateCNAMERecordResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateCNAMERecordType;

public class DNSControl {

	private static Logger LOG = Logger.getLogger( DNSControl.class );

	static UDPListener udpListener;
	static TCPListener tcpListener;
	private static void initializeUDP() throws Exception {
		try {
			if (udpListener == null) {
				udpListener = new UDPListener(Internets.localHostInetAddress( ), DNSProperties.PORT);
				udpListener.start();
			}
		} catch(SocketException ex) {
			LOG.error(ex);
			throw ex;
		}
	}

	private static void initializeTCP() throws Exception {
		try {
			if (tcpListener == null) {
				tcpListener = new TCPListener(Internets.localHostInetAddress( ), DNSProperties.PORT);
				tcpListener.start();
			}
		} catch(UnknownHostException ex) {
			LOG.error(ex);
			throw ex;
		}
	}

	public static void populateRecords() {
		DNSProperties.update();
		EntityWrapper<ZoneInfo> db = EntityWrapper.get(ZoneInfo.class);
		try {
			ZoneInfo zInfo = new ZoneInfo();
			List<ZoneInfo> zoneInfos = db.query(zInfo);
			for(ZoneInfo zoneInfo : zoneInfos) {
				String name = zoneInfo.getName();
				EntityWrapper<SOARecordInfo> dbSOA = db.recast(SOARecordInfo.class);
				SOARecordInfo searchSOARecordInfo = new SOARecordInfo();
				searchSOARecordInfo.setName(name);
				SOARecordInfo soaRecordInfo = dbSOA.getUnique(searchSOARecordInfo);

				EntityWrapper<NSRecordInfo> dbNS = db.recast(NSRecordInfo.class);
				NSRecordInfo searchNSRecordInfo = new NSRecordInfo();
				searchNSRecordInfo.setName(name);
				NSRecordInfo nsRecordInfo = dbNS.getUnique(searchNSRecordInfo);

				ZoneManager.addZone(zoneInfo, soaRecordInfo, nsRecordInfo);
			}

			EntityWrapper<ARecordInfo> dbARec = db.recast(ARecordInfo.class);
			ARecordInfo searchARecInfo = new ARecordInfo();
			List<ARecordInfo> aRecInfos = dbARec.query(searchARecInfo);
			for(ARecordInfo aRecInfo : aRecInfos) {
				ZoneManager.addRecord(aRecInfo, false);
			}
			db.commit();
		} catch(EucalyptusCloudException ex) {		
			db.rollback();
			LOG.error(ex);
		}
		
		final EntityTransaction db2 = Entities.get( ARecordNameInfo.class );
		try{
			int count = 0;
			List<ARecordNameInfo> multiARec = Entities.query(new ARecordNameInfo(), true);
			if(multiARec != null && multiARec.size()>0){
				for(ARecordNameInfo nameRec : multiARec){
					final Collection<ARecordAddressInfo> addresses = nameRec.getAddresses();
					if(addresses!=null && addresses.size()>0){
						for(final ARecordAddressInfo addrRec : addresses){
							ARecord record = new ARecord(new Name(nameRec.getName()), DClass.IN, nameRec.getTtl(), Address.getByAddress(addrRec.getAddress()));
							ZoneManager.addRecord(nameRec.getZone(), record, true);
							count++;
						}
					}
				}
			}
			LOG.info(String.format("%d DNS records populated from database", count));
			db2.commit();
		}catch(Exception ex){
			db2.rollback();
			LOG.error("Failed to populate the existing DNS records", ex);
		}
	}

	public static void initialize() throws Exception {
		try {
			initializeUDP();
			initializeTCP();
			populateRecords();
		} catch(Exception ex) {
			LOG.error("DNS could not be initialized. Is some other service running on port 53?");
			throw ex;
		}
	}

	public static void stop() throws Exception {
		if (udpListener != null) {
			udpListener.close();
			udpListener = null;
		}
		if (tcpListener != null) {
			tcpListener.close();
			tcpListener = null;
		}
	}
	
	public DNSControl() {}
	
	public CreateMultiARecordResponseType CreateMultiARecord(CreateMultiARecordType request) throws EucalyptusCloudException {
		CreateMultiARecordResponseType reply = (CreateMultiARecordResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		long ttl = request.getTtl();

		// look for the records with the same name
		// create one if not found
		final EntityTransaction db = Entities.get( ARecordNameInfo.class );
		try{
			Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			db.commit();
			throw new EucalyptusCloudException("A record with the same name is found");
		}catch(NoSuchElementException ex){
			ARecordNameInfo newInfo = ARecordNameInfo.newInstance(name, zone,  DClass.IN, ttl);
			Entities.persist(newInfo);
			db.commit();
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed due to database error");
		}
		return reply;
	}
	
	/// add a new name - address mapping
	public AddMultiARecordResponseType AddMultiARecord(AddMultiARecordType request) throws EucalyptusCloudException {
		AddMultiARecordResponseType reply = (AddMultiARecordResponseType) request.getReply();
		// find the exsting ARecordNameInfo; if not throw exception
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		long ttl = request.getTtl();
		String address = request.getAddress();
		
		EntityTransaction db = Entities.get( ARecordNameInfo.class );
		ARecordNameInfo nameInfo = null;
		try{
			nameInfo = Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
			throw new EucalyptusCloudException("No dns record with name="+name+" is found");
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to query dns name record", ex);
		}
		
		// add new address
		// call ZoneManager.addRecord
		db = Entities.get( ARecordAddressInfo.class );
		try{
			List<ARecordAddressInfo> exist = Entities.query(ARecordAddressInfo.named(nameInfo, address));
			if(exist==null || exist.size()<=0){
				ARecord record = new ARecord(new Name(name), DClass.IN, ttl, Address.getByAddress(address));
				ZoneManager.addRecord(zone, record, true);
				ARecordAddressInfo addrInfo = ARecordAddressInfo.newInstance(nameInfo, address);
				Entities.persist(addrInfo);
			}
			db.commit();
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to add the record", ex);
		}
		
		return reply;
	}
	
	/// remove an existing name - address mapping
	/// do nothing if no mapping is found
	public RemoveMultiARecordResponseType RemoveMultiARecord(RemoveMultiARecordType request) throws EucalyptusCloudException {
		RemoveMultiARecordResponseType reply = (RemoveMultiARecordResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		String address = request.getAddress();
		EntityTransaction db = Entities.get( ARecordNameInfo.class );
		ARecordNameInfo nameInfo = null;
		try{
			nameInfo = Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
			return reply;
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to query dns name record", ex);
		}
		
		db = Entities.get( ARecordAddressInfo.class );
		ARecordAddressInfo addrInfo = null;
		// find the existing record
		try{
			addrInfo = Entities.uniqueResult(ARecordAddressInfo.named(nameInfo, address));
			ARecord arecord = new ARecord(Name.fromString(name), DClass.IN, nameInfo.getTtl(), Address.getByAddress(addrInfo.getAddress()));
		
			ZoneManager.deleteARecord(zone, arecord);
			Entities.delete(addrInfo);
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to delete the record");
		}
		
		return reply;
	}
	
	/// remove all name - {address1, address2, ...} mapping
	/// do nothing if no mapping is found
	public RemoveMultiANameResponseType RemoveMultiAName(RemoveMultiANameType request) throws EucalyptusCloudException {
		RemoveMultiANameResponseType reply = (RemoveMultiANameResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";

		EntityTransaction db = Entities.get( ARecordNameInfo.class );
		ARecordNameInfo nameInfo = null;
		List<ARecordAddressInfo> addresses = null;
		try{
			nameInfo = Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			addresses = Lists.newArrayList(nameInfo.getAddresses());
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
			return reply;
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to query dns name record", ex);
		}
		
		// delete the records from zone
		for(ARecordAddressInfo addr : addresses){
			try{
				ARecord arecord = new ARecord(Name.fromString(name), DClass.IN, nameInfo.getTtl(), Address.getByAddress(addr.getAddress()));
				ZoneManager.deleteARecord(zone, arecord);
			}catch(Exception ex){
				throw new EucalyptusCloudException("Failed to delete the record from zone", ex);
			}	
		}
		db = Entities.get( ARecordNameInfo.class );
		try{
			nameInfo = Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			Entities.delete(nameInfo); // deletion will be cascaded to AddressInfo
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to query dns name record", ex);
		}
		
		return reply;
	}	

	public UpdateARecordResponseType UpdateARecord(UpdateARecordType request)  throws EucalyptusCloudException {
		UpdateARecordResponseType reply = (UpdateARecordResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		
		String address = request.getAddress();
		long ttl = request.getTtl();
		EntityWrapper<ARecordInfo> db = EntityWrapper.get(ARecordInfo.class);
		ARecordInfo aRecordInfo = new ARecordInfo();
		aRecordInfo.setName(name);
		aRecordInfo.setAddress(address);
		aRecordInfo.setZone(zone);
		List<ARecordInfo> arecords = db.query(aRecordInfo);
		if(arecords.size() > 0) {
			aRecordInfo = arecords.get(0);
			if(!zone.equals(aRecordInfo.getZone())) {
				db.rollback();
				throw new EucalyptusCloudException("Sorry, record already associated with a zone. Remove the record and try again.");
			}
			aRecordInfo.setAddress(address);
			aRecordInfo.setTtl(ttl);
			//update record
			try {
				ARecord arecord = new ARecord(Name.fromString(name), DClass.IN, ttl, Address.getByAddress(address));
				ZoneManager.updateARecord(zone, arecord);
			} catch(Exception ex) {
				LOG.error(ex);
			}
		}  else {
			try {
				ARecordInfo searchARecInfo = new ARecordInfo();
				searchARecInfo.setZone(zone);
				ARecord record = new ARecord(new Name(name), DClass.IN, ttl, Address.getByAddress(address));
				ZoneManager.addRecord(zone, record, false);
				aRecordInfo = new ARecordInfo();
				aRecordInfo.setName(name);
				aRecordInfo.setAddress(address);
				aRecordInfo.setTtl(ttl);
				aRecordInfo.setZone(zone);
				aRecordInfo.setRecordclass(DClass.IN);
				db.add(aRecordInfo);
			} catch(Exception ex) {
				LOG.error(ex);
			}
		}
		db.commit();
		return reply;
	}

	public RemoveARecordResponseType RemoveARecord(RemoveARecordType request) throws EucalyptusCloudException {
		RemoveARecordResponseType reply = (RemoveARecordResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		String address = request.getAddress();
		EntityWrapper<ARecordInfo> db = EntityWrapper.get(ARecordInfo.class);
		ARecordInfo aRecordInfo = new ARecordInfo();
		aRecordInfo.setName(name);
		aRecordInfo.setZone(zone);
		aRecordInfo.setAddress(address);
		try {
			ARecordInfo foundARecordInfo = db.getUnique(aRecordInfo);
			ARecord arecord = new ARecord(Name.fromString(name), DClass.IN, foundARecordInfo.getTtl(), Address.getByAddress(foundARecordInfo.getAddress()));
			ZoneManager.deleteRecord(zone, arecord);
			db.delete(foundARecordInfo);
			db.commit();

		} catch(Exception ex) {
			db.rollback();
			LOG.error(ex);
		}
		return reply;
	}

	public AddZoneResponseType AddZone(AddZoneType request) throws EucalyptusCloudException {
		AddZoneResponseType reply = (AddZoneResponseType) request.getReply();
		String name = request.getName();
		if(!Contexts.lookup().hasAdministrativePrivileges()) {
			throw new AccessDeniedException(name);
		}

		return reply;
	}

	public UpdateCNAMERecordResponseType UpdateCNAMERecord(UpdateCNAMERecordType request)  throws EucalyptusCloudException {
		UpdateCNAMERecordResponseType reply = (UpdateCNAMERecordResponseType) request.getReply();
		String zone = request.getZone()  + DNSProperties.DOMAIN + ".";
		String name = request.getName()  + DNSProperties.DOMAIN + ".";
		String alias = request.getAlias() + DNSProperties.DOMAIN + ".";
		long ttl = request.getTtl();
		EntityWrapper<CNAMERecordInfo> db = EntityWrapper.get(CNAMERecordInfo.class);
		CNAMERecordInfo cnameRecordInfo = new CNAMERecordInfo();
		cnameRecordInfo.setName(name);
		cnameRecordInfo.setAlias(alias);
		cnameRecordInfo.setZone(zone);
		List<CNAMERecordInfo> cnamerecords = db.query(cnameRecordInfo);
		if(cnamerecords.size() > 0) {
			cnameRecordInfo = cnamerecords.get(0);
			if(!zone.equals(cnameRecordInfo.getZone())) {
				db.rollback();
				throw new EucalyptusCloudException("Sorry, record already associated with a zone. Remove the record and try again.");
			}
			cnameRecordInfo.setAlias(alias);
			cnameRecordInfo.setTtl(ttl);
			//update record
			try {
				CNAMERecord cnameRecord = new CNAMERecord(Name.fromString(name), DClass.IN, ttl, Name.fromString(alias));
				ZoneManager.updateCNAMERecord(zone, cnameRecord);
			} catch(Exception ex) {
				LOG.error(ex);
			}
		}  else {
			try {
				CNAMERecordInfo searchCNAMERecInfo = new CNAMERecordInfo();
				searchCNAMERecInfo.setZone(zone);
				CNAMERecord record = new CNAMERecord(new Name(name), DClass.IN, ttl, Name.fromString(alias));
				ZoneManager.addRecord(zone, record, false);
				cnameRecordInfo = new CNAMERecordInfo();
				cnameRecordInfo.setName(name);
				cnameRecordInfo.setAlias(alias);
				cnameRecordInfo.setTtl(ttl);
				cnameRecordInfo.setZone(zone);
				cnameRecordInfo.setRecordclass(DClass.IN);
				db.add(cnameRecordInfo);
			} catch(Exception ex) {
				LOG.error(ex);
			}
		}
		db.commit();

		return reply;
	}

	public RemoveCNAMERecordResponseType RemoveCNAMERecord(RemoveCNAMERecordType request) throws EucalyptusCloudException {
		RemoveCNAMERecordResponseType reply = (RemoveCNAMERecordResponseType) request.getReply();
		String zone = request.getZone()  + DNSProperties.DOMAIN + ".";
		String name = request.getName()  + DNSProperties.DOMAIN + ".";
		String alias = request.getAlias() + DNSProperties.DOMAIN + ".";
		EntityWrapper<CNAMERecordInfo> db = EntityWrapper.get(CNAMERecordInfo.class);
		CNAMERecordInfo cnameRecordInfo = new CNAMERecordInfo();
		cnameRecordInfo.setName(name);
		cnameRecordInfo.setZone(zone);
		cnameRecordInfo.setAlias(alias);
		try {
			CNAMERecordInfo foundCNAMERecordInfo = db.getUnique(cnameRecordInfo);
			CNAMERecord cnameRecord = new CNAMERecord(Name.fromString(name), DClass.IN, foundCNAMERecordInfo.getTtl(), Name.fromString(foundCNAMERecordInfo.getAlias()));
			ZoneManager.deleteRecord(zone, cnameRecord);
			db.delete(foundCNAMERecordInfo);
			db.commit();
		} catch(Exception ex) {
			db.rollback();
			LOG.error(ex);
		}
		return reply;
	}

	public DeleteZoneResponseType DeleteZone(DeleteZoneType request) throws EucalyptusCloudException {
		DeleteZoneResponseType reply = (DeleteZoneResponseType) request.getReply();
		String name = request.getName();
		if(!Contexts.lookup().hasAdministrativePrivileges()) {
			throw new AccessDeniedException(name);
		}
		EntityWrapper<ZoneInfo> db = EntityWrapper.get(ZoneInfo.class);
		ZoneInfo zoneInfo = new ZoneInfo(name);
		try {
			ZoneInfo foundZoneInfo = db.getUnique(zoneInfo);
			db.delete(foundZoneInfo);
			db.commit();
		} catch(Exception ex) {
			db.rollback();
			LOG.error(ex);
		}
		ZoneManager.deleteZone(name);
		return reply;
	}

}
