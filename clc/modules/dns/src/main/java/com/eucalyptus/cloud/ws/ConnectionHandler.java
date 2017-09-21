/*************************************************************************
 * Copyright 1999-2004 Brian Wellington
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud.ws;

import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Credibility;
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
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TSIGRecord;
import org.xbill.DNS.Type;

import com.eucalyptus.dns.Cache;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.dns.DnsResolvers;
import com.google.common.base.Function;
import com.google.common.base.Optional;

public class ConnectionHandler extends Thread {

  private static Logger LOG = Logger.getLogger( ConnectionHandler.class );

	static final int FLAG_DNSSECOK = 1;
	static final int FLAG_SIGONLY = 2;

	Map caches = new ConcurrentHashMap();

	public ConnectionHandler() {
		super( Threads.threadUniqueName( "dns-connection-handler" ) );
	}

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
		if (header.getRcode() != Rcode.NOERROR)
			return errorMessage(query, Rcode.FORMERR);
		if (header.getOpcode() != Opcode.QUERY)
			return errorMessage(query, Rcode.NOTIMP);

		Record queryRecord = query.getQuestion();

		TSIGRecord queryTSIG = query.getTSIG();
		TSIG tsig = null;

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

			if (!Type.isRR(type) && type != Type.ANY)
			  return errorMessage(query, Rcode.NOTIMP);

			 byte rcode = addAnswer(response, name, type, dclass, 0, flags);
			 if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) {
			   if(rcode == Rcode.REFUSED)
			     return errorMessage(query, Rcode.REFUSED);
			   else
			     return errorMessage(query, Rcode.SERVFAIL);
			 }

			 if (queryOPT != null) {
				 int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
				 OPTRecord opt = new OPTRecord((short)4096, rcode, (byte)0, optflags);
				 response.addRecord(opt, Section.ADDITIONAL);
			 }
		}
		response.setTSIG(tsig, Rcode.NOERROR, queryTSIG);
		return response.toWire(maxLength);
	}
	byte
	addAnswer( final Message response, Name name, int type, int dclass,
			int iterations, int flags)
	{
		SetResponse sr = null;
		byte rcode = Rcode.NOERROR;

		if (iterations > 6)
			return Rcode.NOERROR;

		if (type == Type.SIG || type == Type.RRSIG) {
			type = Type.ANY;
			flags |= FLAG_SIGONLY;
		}

		try {
		  sr = DnsResolvers.findRecords( response, new DnsRequest() {
		    @Override public Record getQuery() { return response.getQuestion( ); }
		    @Override public InetAddress getLocalAddress() { return ConnectionHandler.getLocalInetAddress(); }
		    @Override public InetAddress getRemoteAddress() { return ConnectionHandler.getRemoteInetAddress(); }
		  } );

		  if ( sr == null ) {
		    return Rcode.SERVFAIL;
		  } else {
		    if (sr.isDelegation()) {
		      RRset nsRecords = sr.getNS();
		      addRRset(nsRecords.getName(), response, nsRecords,
		          Section.AUTHORITY, flags);
		    }else if (sr.isCNAME()) {
		      CNAMERecord cname = sr.getCNAME();
		      RRset rrset = new RRset(cname);
		      addRRset(name, response, rrset, Section.ANSWER, flags);
		    } else if (sr.isDNAME()) {
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
		      }
		    }

		    if ( sr.isSuccessful( ) ) {
		      if (type == Type.AAAA)
	          response.getHeader().setFlag(Flags.AA);
	        return Rcode.NOERROR;
		    } else if ( sr.isNXDOMAIN( )) {
		      response.getHeader().setRcode(Rcode.NXDOMAIN);
		      return Rcode.NXDOMAIN;
		    } else if (response.getHeader().getRcode() == Rcode.REFUSED) {
		      return Rcode.REFUSED;
		    } else
		      return Rcode.SERVFAIL;
		  }
		} catch ( Exception ex ) {
		  Logger.getLogger( DnsResolvers.class ).error( ex );
		  return Rcode.SERVFAIL;
		}
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
		  if(in != null)
		    header = new Header(in);
		  else
		    header = new Header();
		}
		catch (IOException e) {
			return null;
		}
		return buildErrorMessage(header, Rcode.FORMERR, null);
	}

	@SuppressWarnings( "unchecked" )
	public Cache
	getCache(int dclass) {
		Cache c = (Cache) caches.get(new Integer(dclass));
		if (c == null) {
			c = new Cache(dclass);
			caches.put(new Integer(dclass), c);
		}
		return c;
	}

private static final ThreadLocal<Pair<InetAddress,InetAddress>> localAndRemoteInetAddresses = new ThreadLocal<>();
	private static InetAddress getInetAddress( final Function<Pair<InetAddress,InetAddress>,InetAddress> extractor ) {
		return Optional.fromNullable( localAndRemoteInetAddresses.get( ) ).transform( extractor ).orNull( );
	}
	static InetAddress getLocalInetAddress( ) {
		return getInetAddress( Pair.<InetAddress,InetAddress>left( ) );
	}
	static InetAddress getRemoteInetAddress( ) {
		return getInetAddress( Pair.<InetAddress, InetAddress>right() );
	}
	static void setLocalAndRemoteInetAddresses( InetAddress local, InetAddress remote ) {
		ConnectionHandler.localAndRemoteInetAddresses.set( Pair.pair( local, remote ) );
	}
	static void clearInetAddresses( ) {
		ConnectionHandler.localAndRemoteInetAddresses.remove( );
	}
}
