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
// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package com.eucalyptus.dns;

import java.util.*;

import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.Master;
import org.xbill.DNS.RRset;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.ZoneTransferException;
import org.xbill.DNS.ZoneTransferIn;

/**
 * The Response from a query to Cache.lookupRecords() or Zone.findRecords()
 * @see Cache
 * @see Zone
 *
 * @author Brian Wellington
 */

public class SetResponse {

/**
 * The Cache contains no information about the requested name/type
 */
static final int UNKNOWN	= 0;

/**
 * The Zone does not contain the requested name, or the Cache has
 * determined that the name does not exist.
 */
static final int NXDOMAIN	= 1;

/**
 * The Zone contains the name, but no data of the requested type,
 * or the Cache has determined that the name exists and has no data
 * of the requested type.
 */
static final int NXRRSET	= 2;

/**
 * A delegation enclosing the requested name was found.
 */
static final int DELEGATION	= 3;

/**
 * The Cache/Zone found a CNAME when looking for the name.
 * @see CNAMERecord
 */
static final int CNAME		= 4;

/**
 * The Cache/Zone found a DNAME when looking for the name.
 * @see DNAMERecord
 */
static final int DNAME		= 5;

/**
 * The Cache/Zone has successfully answered the question for the
 * requested name/type/class.
 */
static final int SUCCESSFUL	= 6;

private static final SetResponse unknown = new SetResponse(UNKNOWN);
private static final SetResponse nxdomain = new SetResponse(NXDOMAIN);
private static final SetResponse nxrrset = new SetResponse(NXRRSET);

private int type;
private Object data;

private
SetResponse() {}

SetResponse(int type, RRset rrset) {
	if (type < 0 || type > 6)
		throw new IllegalArgumentException("invalid type");
	this.type = type;
	this.data = rrset;
}

SetResponse(int type) {
	if (type < 0 || type > 6)
		throw new IllegalArgumentException("invalid type");
	this.type = type;
	this.data = null;
}

static SetResponse
ofType(int type) {
	switch (type) {
		case UNKNOWN:
			return unknown;
		case NXDOMAIN:
			return nxdomain;
		case NXRRSET:
			return nxrrset;
		case DELEGATION:
		case CNAME:
		case DNAME:
		case SUCCESSFUL:
			SetResponse sr = new SetResponse();
			sr.type = type;
			sr.data = null;
			return sr;
		default:
			throw new IllegalArgumentException("invalid type");
	}
}

void
addRRset(RRset rrset) {
	if (data == null)
		data = new ArrayList();
	List l = (List) data;
	l.add(rrset);
}

/** Is the answer to the query unknown? */
public boolean
isUnknown() {
	return (type == UNKNOWN);
}

/** Is the answer to the query that the name does not exist? */
public boolean
isNXDOMAIN() {
	return (type == NXDOMAIN);
}

/** Is the answer to the query that the name exists, but the type does not? */
public boolean
isNXRRSET() {
	return (type == NXRRSET);
}

/** Is the result of the lookup that the name is below a delegation? */
public boolean
isDelegation() {
	return (type == DELEGATION);
}

/** Is the result of the lookup a CNAME? */
public boolean
isCNAME() {
	return (type == CNAME);
}

/** Is the result of the lookup a DNAME? */
public boolean
isDNAME() {
	return (type == DNAME);
}

/** Was the query successful? */
public boolean
isSuccessful() {
	return (type == SUCCESSFUL);
}

/** If the query was successful, return the answers */
public RRset []
answers() {
	if (type != SUCCESSFUL)
		return null;
	List l = (List) data;
	return (RRset []) l.toArray(new RRset[l.size()]);
}

/**
 * If the query encountered a CNAME, return it.
 */
public CNAMERecord
getCNAME() {
	return (CNAMERecord)((RRset)data).first();
}

/**
 * If the query encountered a DNAME, return it.
 */
public DNAMERecord
getDNAME() {
	return (DNAMERecord)((RRset)data).first();
}

/**
 * If the query hit a delegation point, return the NS set.
 */
public RRset
getNS() {
	return (RRset)data;
}

/** Prints the value of the SetResponse */
public String
toString() {
	switch (type) {
		case UNKNOWN:		return "unknown";
		case NXDOMAIN:		return "NXDOMAIN";
		case NXRRSET:		return "NXRRSET";
		case DELEGATION:	return "delegation: " + data;
		case CNAME:		return "CNAME: " + data;
		case DNAME:		return "DNAME: " + data;
		case SUCCESSFUL:	return "successful";
		default:		return null;
	}
}

}
