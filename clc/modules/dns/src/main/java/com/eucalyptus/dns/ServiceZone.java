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

package com.eucalyptus.dns;

import java.io.IOException;
import java.net.InetAddress;
import org.apache.log4j.Logger;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.objectstorage.WalrusManager;
import com.eucalyptus.objectstorage.util.WalrusProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

public class ServiceZone extends Zone {
  private static Logger LOG = Logger.getLogger( ServiceZone.class );
  private static int ttl               = 604800;
  
  public ServiceZone( Name name, Record[] records ) throws IOException {
    super(name,records);
  }

  public static Zone getZone( ) {
    try {
      Name name = getName( );
      Name host = Name.fromString( "root." + name.toString( ) );
      Name admin = Name.fromString( Internets.localHostInetAddress( ).getCanonicalHostName( ) + "." + name.toString( ) );
      Name target = Name.fromString( Internets.localHostInetAddress( ).getCanonicalHostName( ) + "." );
      long serial = 1;
      long refresh = 86400;
      long retry = ttl;
      long expires = 2419200;
      //This is the negative cache TTL
      long minimum = 600;
      Record soarec = new SOARecord( name, DClass.IN, ttl, host, admin, serial, refresh, retry, expires, minimum );
      long nsTTL = 604800;
      Record nsrec = new NSRecord( name, DClass.IN, nsTTL, target);
      return new ServiceZone( name, new Record[] { soarec, nsrec } );
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    } 
  }

  public static Name getName( ) throws TextParseException {
    String nameString = SystemConfiguration.getSystemConfiguration( ).getDnsDomain( ) + ".";
    nameString = nameString.startsWith(".") ? nameString.replaceFirst("\\.", "") : nameString;
    Name name = Name.fromString( nameString );
    return name;
  }

  /* (non-Javadoc)
 * @see com.eucalyptus.dns.Zone#findRecords(org.xbill.DNS.Name, int)
 */
@Override
  public SetResponse findRecords( Name name, int type ) {
	if(type == Type.AAAA)
		return(SetResponse.ofType(SetResponse.SUCCESSFUL));

    if (name.toString().startsWith("eucalyptus.") || 
    	    (name.toString().startsWith("euare.")) || 
    		(name.toString().startsWith("tokens.")) ||
    		(name.toString().startsWith("autoscaling.")) ||
    		(name.toString().startsWith("cloudwatch.")) ||
    		(name.toString().startsWith("loadbalancing."))) {
        SetResponse resp = new SetResponse(SetResponse.SUCCESSFUL);        
		try {
			InetAddress cloudIp = Topology.lookup( Eucalyptus.class ).getInetAddress( );
	        if (cloudIp != null) {
	          resp.addRRset( new RRset( new ARecord( name, 1, 20/*ttl*/, cloudIp ) ) );
	        }
	        return resp;
		} catch (Exception e) {
            return super.findRecords( name, type );
		}
    } else if (name.toString().startsWith("walrus.") || name.toString().matches(".*\\.walrus\\..*")) {
    	//Walrus. Handles both bucket subdomains and service itself.
    	//Fix for EUCA-8367 - don't check if bucket is valid, otherwise it will break bucket-creation when
    	// using virtual-hosted bucket names.
    	SetResponse resp = new SetResponse(SetResponse.SUCCESSFUL);
        InetAddress walrusIp = null;
          try {
		    walrusIp = WalrusProperties.getWalrusAddress();
          } catch (EucalyptusCloudException e) {
        	LOG.error(e);
        	return super.findRecords( name, type );
          }
		  resp.addRRset( new RRset( new ARecord( name, 1, 20/*ttl*/, walrusIp ) ) );
		  return resp;
	} else {
      return super.findRecords( name, type );
    }
  }
  
}