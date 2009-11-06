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
 *
 * Parts of this code are licensed under the BSD license and carry the following copyright,
 * Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)
 * @author Brian Wellington &lt;bwelling@xbill.org&gt;

 */

package com.eucalyptus.cloud.ws;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TSIGRecord;
import org.xbill.DNS.Type;

import com.eucalyptus.dns.SetResponse;
import com.eucalyptus.dns.Zone;
import com.eucalyptus.dns.Cache;

public class ConnectionHandler extends Thread {

	static final int FLAG_DNSSECOK = 1;
	static final int FLAG_SIGONLY = 2;

	Map caches = new ConcurrentHashMap();
	//Map TSIGs;

	byte []
	      generateReply(Message query, byte [] in, int length, Socket s)
	throws IOException
	{
		Header header;
		boolean badversion;
		int maxLength;
		boolean sigonly;
		SetResponse sr;
		int flags = 0;

		header = query.getHeader();
		if (header.getFlag(Flags.QR))
			return null;
		if (header.getRcode() != Rcode.NOERROR)
			return errorMessage(query, Rcode.FORMERR);
		if (header.getOpcode() != Opcode.QUERY)
			return errorMessage(query, Rcode.NOTIMP);

		Record queryRecord = query.getQuestion();

		TSIGRecord queryTSIG = query.getTSIG();
		TSIG tsig = null;
		/*  if (queryTSIG != null) {
            tsig = (TSIG) TSIGs.get(queryTSIG.getName());
            if (tsig == null ||
                    tsig.verify(query, in, length, null) != Rcode.NOERROR)
                return formerrMessage(in);
        }*/

		OPTRecord queryOPT = query.getOPT();
		if (queryOPT != null && queryOPT.getVersion() > 0)
			badversion = true;

		if (s != null)
			maxLength = 65535;
		else if (queryOPT != null)
			maxLength = Math.max(queryOPT.getPayloadSize(), 512);
		else
			maxLength = 512;

		if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0)
			flags = FLAG_DNSSECOK;

		Message response = new Message(query.getHeader().getID());
		response.getHeader().setFlag(Flags.QR);
		if (query.getHeader().getFlag(Flags.RD))
			response.getHeader().setFlag(Flags.RD);
		if(queryRecord != null) {
			response.addRecord(queryRecord, Section.QUESTION);

			Name name = queryRecord.getName();
			int type = queryRecord.getType();
			int dclass = queryRecord.getDClass();

			/*        if (type == Type.AXFR && s != null)
            return doAXFR(name, query, tsig, queryTSIG, s);
			 */    if (!Type.isRR(type) && type != Type.ANY)
				 return errorMessage(query, Rcode.NOTIMP);

			 byte rcode = addAnswer(response, name, type, dclass, 0, flags);
			 if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN)
				 return errorMessage(query, rcode);

			 addAdditional(response, flags);

			 if (queryOPT != null) {
				 int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
				 OPTRecord opt = new OPTRecord((short)4096, rcode, (byte)0,
						 optflags);
				 response.addRecord(opt, Section.ADDITIONAL);
			 }
		}
		response.setTSIG(tsig, Rcode.NOERROR, queryTSIG);
		return response.toWire(maxLength);
	}

	public Zone
	findBestZone(Name name) {
		Zone foundzone = null;
		foundzone = (Zone) ZoneManager.getZone(name);
		if (foundzone != null)
			return foundzone;
		int labels = name.labels();
		for (int i = 1; i < labels; i++) {
			Name tname = new Name(name, i);
			foundzone = (Zone) ZoneManager.getZone(tname);
			if (foundzone != null)
				return foundzone;
		}
		return null;
	}

	public RRset
	findExactMatch(Name name, int type, int dclass, boolean glue) {
		Zone zone = findBestZone(name);
		if (zone != null)
			return zone.findExactMatch(name, type);
		else {
			RRset [] rrsets;
			Cache cache = getCache(dclass);
			if (glue)
				rrsets = cache.findAnyRecords(name, type);
			else
				rrsets = cache.findRecords(name, type);
			if (rrsets == null)
				return null;
			else
				return rrsets[0]; /* not quite right */
		}
	}

	private void
	addGlue(Message response, Name name, int flags) {
		RRset a = findExactMatch(name, Type.A, DClass.IN, true);
		if (a == null)
			return;
		addRRset(name, response, a, Section.ADDITIONAL, flags);
	}

	private void
	addAdditional2(Message response, int section, int flags) {
		Record [] records = response.getSectionArray(section);
		for (int i = 0; i < records.length; i++) {
			Record r = records[i];
			Name glueName = r.getAdditionalName();
			if (glueName != null)
				addGlue(response, glueName, flags);
		}
	}

	private final void
	addAdditional(Message response, int flags) {
		addAdditional2(response, Section.ANSWER, flags);
		addAdditional2(response, Section.AUTHORITY, flags);
	}

	byte
	addAnswer(Message response, Name name, int type, int dclass,
			int iterations, int flags)
	{
		SetResponse sr;
		byte rcode = Rcode.NOERROR;

		if (iterations > 6)
			return Rcode.NOERROR;

		if (type == Type.SIG || type == Type.RRSIG) {
			type = Type.ANY;
			flags |= FLAG_SIGONLY;
		}

		Zone zone = findBestZone(name);
		if (zone != null)
			sr = zone.findRecords(name, type);
		else {
			Cache cache = getCache(dclass);
			sr = cache.lookupRecords(name, type, Credibility.NORMAL);
		}

		if (sr.isUnknown()) {
			addCacheNS(response, getCache(dclass), name);
		}
		if (sr.isNXDOMAIN()) {
			response.getHeader().setRcode(Rcode.NXDOMAIN);
			if (zone != null) {
				addSOA(response, zone);
				if (iterations == 0)
					response.getHeader().setFlag(Flags.AA);
			}
			rcode = Rcode.NXDOMAIN;
		}
		else if (sr.isNXRRSET()) {
			if (zone != null) {
				addSOA(response, zone);
				if (iterations == 0)
					response.getHeader().setFlag(Flags.AA);
			}
		}
		else if (sr.isDelegation()) {
			RRset nsRecords = sr.getNS();
			addRRset(nsRecords.getName(), response, nsRecords,
					Section.AUTHORITY, flags);
		}
		else if (sr.isCNAME()) {
			CNAMERecord cname = sr.getCNAME();
			RRset rrset = new RRset(cname);
			addRRset(name, response, rrset, Section.ANSWER, flags);
			if (zone != null && iterations == 0)
				response.getHeader().setFlag(Flags.AA);
			rcode = addAnswer(response, cname.getTarget(),
					type, dclass, iterations + 1, flags);
		}
		else if (sr.isDNAME()) {
			DNAMERecord dname = sr.getDNAME();
			RRset rrset = new RRset(dname);
			addRRset(name, response, rrset, Section.ANSWER, flags);
			Name newname;
			try {
				newname = name.fromDNAME(dname);
			}
			catch (NameTooLongException e) {
				return Rcode.YXDOMAIN;
			}
			if(newname != null) {
				rrset = new RRset(new CNAMERecord(name, dclass, 0, newname));
				addRRset(name, response, rrset, Section.ANSWER, flags);
				if (zone != null && iterations == 0)
					response.getHeader().setFlag(Flags.AA);
				rcode = addAnswer(response, newname, type, dclass,
						iterations + 1, flags);
			}
		}
		else if (sr.isSuccessful()) {
			RRset [] rrsets = sr.answers();
			if(rrsets != null) {
				for (int i = 0; i < rrsets.length; i++)
					addRRset(name, response, rrsets[i],
							Section.ANSWER, flags);
			}
			if (zone != null) {
				addNS(response, zone, flags);
				if (iterations == 0)
					response.getHeader().setFlag(Flags.AA);
			}
			else
				addCacheNS(response, getCache(dclass), name);
		}
		return rcode;
	}


	private final void
	addSOA(Message response, Zone zone) {
		response.addRecord(zone.getSOA(), Section.AUTHORITY);
	}

	private final void
	addNS(Message response, Zone zone, int flags) {
		RRset nsRecords = zone.getNS();
		addRRset(nsRecords.getName(), response, nsRecords,
				Section.AUTHORITY, flags);
	}

	private final void
	addCacheNS(Message response, Cache cache, Name name) {
		SetResponse sr = cache.lookupRecords(name, Type.NS, Credibility.HINT);
		if (!sr.isDelegation())
			return;
		RRset nsRecords = sr.getNS();
		Iterator it = nsRecords.rrs();
		while (it.hasNext()) {
			Record r = (Record) it.next();
			response.addRecord(r, Section.AUTHORITY);
		}
	}


	byte []
	      doAXFR(Name name, Message query, TSIG tsig, TSIGRecord qtsig, Socket s) {
		Zone zone = (Zone) ZoneManager.getZone(name);
		boolean first = true;
		if (zone == null)
			return errorMessage(query, Rcode.REFUSED);
		Iterator it = zone.AXFR();
		try {
			DataOutputStream dataOut;
			dataOut = new DataOutputStream(s.getOutputStream());
			int id = query.getHeader().getID();
			while (it.hasNext()) {
				RRset rrset = (RRset) it.next();
				Message response = new Message(id);
				Header header = response.getHeader();
				header.setFlag(Flags.QR);
				header.setFlag(Flags.AA);
				addRRset(rrset.getName(), response, rrset,
						Section.ANSWER, FLAG_DNSSECOK);
				if (tsig != null) {
					tsig.applyStream(response, qtsig, first);
					qtsig = response.getTSIG();
				}
				first = false;
				byte [] out = response.toWire();
				dataOut.writeShort(out.length);
				dataOut.write(out);
			}
		}
		catch (IOException ex) {
			System.out.println("AXFR failed");
		}
		try {
			s.close();
		}
		catch (IOException ex) {
		}
		return null;
	}

	void
	addRRset(Name name, Message response, RRset rrset, int section, int flags) {
		for (int s = 1; s <= section; s++)
			if (response.findRRset(name, rrset.getType(), s))
				return;
		if ((flags & FLAG_SIGONLY) == 0) {
			Iterator it = rrset.rrs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild())
					r = r.withName(name);
				response.addRecord(r, section);
			}
		}
		if ((flags & (FLAG_SIGONLY | FLAG_DNSSECOK)) != 0) {
			Iterator it = rrset.sigs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild())
					r = r.withName(name);
				response.addRecord(r, section);
			}
		}
	}

	byte []
	      buildErrorMessage(Header header, int rcode, Record question) {
		Message response = new Message();
		response.setHeader(header);
		for (int i = 0; i < 4; i++)
			response.removeAllRecords(i);
		if (rcode == Rcode.SERVFAIL)
			response.addRecord(question, Section.QUESTION);
		header.setRcode(rcode);
		return response.toWire();
	}

	public byte []
	             errorMessage(Message query, int rcode) {
		return buildErrorMessage(query.getHeader(), rcode,
				query.getQuestion());
	}

	public byte []
	             formerrMessage(byte [] in) {
		Header header;
		try {
			header = new Header(in);
		}
		catch (IOException e) {
			return null;
		}
		return buildErrorMessage(header, Rcode.FORMERR, null);
	}

	public Cache
	getCache(int dclass) {
		Cache c = (Cache) caches.get(new Integer(dclass));
		if (c == null) {
			c = new Cache(dclass);
			caches.put(new Integer(dclass), c);
		}
		return c;
	}
}
