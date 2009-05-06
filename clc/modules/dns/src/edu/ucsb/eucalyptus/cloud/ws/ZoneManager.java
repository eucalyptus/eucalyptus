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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;
import org.xbill.DNS.Zone;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.RRset;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

public class ZoneManager {
    private static ConcurrentHashMap<String, Zone> zones;
    private static Logger LOG = Logger.getLogger( ZoneManager.class );


    public static Zone addZone(String name, Zone zone) {
        return zones.putIfAbsent(name, zone);
    }

    public static Zone getZone(String name) {
        try {
            return zones.putIfAbsent(name, new Zone(new Name(name), new Record[0]));
        } catch(Exception ex) {
            LOG.error(ex);
        }
        return null;
    }

    public static void addRecord(String name, Record record) {
        Zone zone = getZone(name);
        zone.addRecord(record);
    }

    public static void updateRecord(String zoneName, Record record) {
        try {
            Zone zone = getZone(zoneName);
            RRset rrSet = zone.findExactMatch(record.getName(), record.getDClass());
            Iterator<Record> rrIterator = rrSet.rrs();
            while(rrIterator.hasNext()) {
                Record rec = rrIterator.next();
                if(rec.getName().equals(record.getName())) {
                    zone.removeRecord(rec);
                }
            }
            zone.addRecord(record);
        } catch(Exception ex) {
            LOG.error(ex);
        }

    }
}