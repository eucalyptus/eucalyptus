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
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.cloud.ws;

import com.eucalyptus.config.Configuration;
import com.eucalyptus.dns.TransientZone;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.DNSProperties;

import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.msgs.*;
import org.apache.log4j.Logger;
import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.util.List;

public class DNSControl {

	private static Logger LOG = Logger.getLogger( DNSControl.class );

	public static <T> EntityWrapper<T> getEntityWrapper( ) {
		return new EntityWrapper<T>( DNSProperties.DB_NAME );
	}

	private static void initializeUDP() throws Exception {
		try {
			UDPListener udpListener = new UDPListener(Address.getByAddress(DNSProperties.ADDRESS), DNSProperties.PORT);
			udpListener.start();
		} catch(UnknownHostException ex) {
			LOG.error(ex);
			throw ex;
		}
	}

	private static void initializeTCP() throws Exception {
		try {
			TCPListener tcpListener = new TCPListener(Address.getByAddress(DNSProperties.ADDRESS), DNSProperties.PORT);
			tcpListener.start();
		} catch(UnknownHostException ex) {
			LOG.error(ex);
			throw ex;
		}
	}

	public static void populateRecords() {
		DNSProperties.update();
		EntityWrapper<ZoneInfo> db = DNSControl.getEntityWrapper();
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
				ZoneManager.addRecord(aRecInfo);
			}
			try {
				ZoneManager.registerZone( TransientZone.getExternalName( ), TransientZone.getInstanceExternalZone( ) );
				ZoneManager.registerZone( TransientZone.getInternalName( ), TransientZone.getInstanceInternalZone( ) );
			} catch ( Throwable e ) {
				LOG.debug( e, e );
			}
			db.commit();
		} catch(EucalyptusCloudException ex) {		
			db.rollback();
			LOG.error(ex);
		}
	}

	public static void initialize() throws Exception {
		try {
			initializeUDP();
			initializeTCP();
		} catch(Exception ex) {
			LOG.error("DNS could not be initialized. Please resolve the problem or run with euca.dns.disable");
			throw ex;
		}
	}

	public DNSControl() {}

	public UpdateARecordResponseType UpdateARecord(UpdateARecordType request)  throws EucalyptusCloudException {
		UpdateARecordResponseType reply = (UpdateARecordResponseType) request.getReply();
		String zone = request.getZone() + DNSProperties.DOMAIN + ".";
		String name = request.getName()  + DNSProperties.DOMAIN + ".";
		String address = request.getAddress();
		long ttl = request.getTtl();
		EntityWrapper<ARecordInfo> db = DNSControl.getEntityWrapper();
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
				ZoneManager.addRecord(zone, record);
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
		String zone = request.getZone()  + DNSProperties.DOMAIN + ".";
		String name = request.getName()  + DNSProperties.DOMAIN + ".";
		String address = request.getAddress();
		EntityWrapper<ARecordInfo> db = DNSControl.getEntityWrapper();
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
		if(!request.isAdministrator()) {
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
		EntityWrapper<CNAMERecordInfo> db = DNSControl.getEntityWrapper();
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
				ZoneManager.addRecord(zone, record);
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
		EntityWrapper<CNAMERecordInfo> db = DNSControl.getEntityWrapper();
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
		if(!request.isAdministrator()) {
			throw new AccessDeniedException(name);
		}
		EntityWrapper<ZoneInfo> db = DNSControl.getEntityWrapper();
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
