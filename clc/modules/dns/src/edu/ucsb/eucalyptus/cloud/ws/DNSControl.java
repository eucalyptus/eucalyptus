/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2009, Eucalyptus Systems, Inc.
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
 * Author: Neil Soman neil@eucalyptus.com
 */

package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;
import org.xbill.DNS.*;
import org.xbill.DNS.Address;
import edu.ucsb.eucalyptus.msgs.UpdateARecordResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateARecordType;
import edu.ucsb.eucalyptus.msgs.AddZoneResponseType;
import edu.ucsb.eucalyptus.msgs.AddZoneType;
import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.util.DNSProperties;

import java.net.UnknownHostException;
import java.util.List;

public class DNSControl {

    private static Logger LOG = Logger.getLogger( DNSControl.class );
    static {
        initializeDNS();
    }

    private static void initializeUDP() {
        try {
            UDPListener udpListener = new UDPListener(Address.getByAddress(DNSProperties.ADDRESS), DNSProperties.PORT);
            udpListener.start();
        } catch(UnknownHostException ex) {
            LOG.error(ex);
        }
    }

    private static void initializeTCP() {
        try {
            TCPListener tcpListener = new TCPListener(Address.getByAddress(DNSProperties.ADDRESS), DNSProperties.PORT);
            tcpListener.start();
        } catch(UnknownHostException ex) {
            LOG.error(ex);
        }
    }

    private static void populateRecords() {
        try {
            EntityWrapper<ZoneInfo> db = new EntityWrapper<ZoneInfo>();
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
        } catch(EucalyptusCloudException ex) {
            LOG.error(ex);
        }
    }

    private static void initializeDNS() {
        initializeUDP();
        initializeTCP();
        populateRecords();
    }

    public DNSControl() {}

    public UpdateARecordResponseType UpdateARecord(UpdateARecordType request)  throws EucalyptusCloudException {
        UpdateARecordResponseType reply = (UpdateARecordResponseType) request.getReply();
        String zone = request.getZone();
        String name = request.getName();
        String address = request.getAddress();
        long ttl = request.getTtl();
        /*if(!request.isAdministrator()) {
            throw new AccessDeniedException(name);
        } */
        EntityWrapper<ARecordInfo> db = new EntityWrapper<ARecordInfo>();
        ARecordInfo aRecordInfo = new ARecordInfo();
        aRecordInfo.setName(name);
        List<ARecordInfo> arecords = db.query(aRecordInfo);
        if(arecords.size() > 0) {
            aRecordInfo = arecords.get(0);
            if(zone != aRecordInfo.getZone()) {
                db.rollback();
                throw new EucalyptusCloudException("Sorry, record already associated with a zone. Remove the record and try again.");
            }
            aRecordInfo.setAddress(address);
            aRecordInfo.setTtl(ttl);
            //update record
            try {
                ARecord arecord = new ARecord(new Name(name), DClass.IN, ttl, Address.getByAddress(address));
                ZoneManager.updateRecord(zone, arecord);
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

    public AddZoneResponseType AddZone(AddZoneType request) throws EucalyptusCloudException {
        AddZoneResponseType reply = (AddZoneResponseType) request.getReply();
        String name = request.getName();
        if(!request.isAdministrator()) {
            throw new AccessDeniedException(name);
        }

        return reply;
    }
}
