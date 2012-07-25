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

import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.address.Address;
import groovy.sql.Sql;
import com.eucalyptus.records.BaseRecord;

EntityWrapper db = EntityWrapper.get( BaseRecord.class );
Sql sql = new Sql( db.getSession( ).connection( ) )
def groups = [:]
def accountedFor = new TreeSet()
List userResults = new ArrayList()
Users.listAllUsers().each{ User user ->
  def u = new UserAddressData() {{
      userName = user.getName() 
    }
  };
  def query = "SELECT\n" + 
    "  crt.record_correlation_id as addr_id, \n" + 
    "  crt.record_user_id as user_id, \n" + 
    "  UNIX_TIMESTAMP(del.record_timestamp)*1000 as dealloc_time, UNIX_TIMESTAMP(crt.record_timestamp)*1000 as alloc_time,\n" + 
    "  TRIM(REPLACE(crt.record_extra,':size:',' ')) as volume_size\n" + 
    "FROM eucalyptus_records.records_logs as del, eucalyptus_records.records_logs as crt \n" + 
    "WHERE  \n" + 
    "  crt.record_user_id LIKE '${u.userName}' AND crt.record_extra LIKE '%USER%' \n" + 
    "  AND crt.record_type LIKE 'ADDRESS_ASSIGN' \n" + 
    "  AND UNIX_TIMESTAMP(crt.record_timestamp)*1000 < ${notAfter} \n" +
    "  AND UNIX_TIMESTAMP(del.record_timestamp)*1000 > ${notBefore} \n" +
    "  AND crt.record_correlation_id=del.record_correlation_id AND ( del.record_type LIKE 'ADDRESS_ASSIGN' OR del.record_type LIKE 'ADDRESS_UNASSIGN' )\n" + 
    "ORDER BY dealloc_time DESC;"
  db.query( a ).each{ Address addr ->
    def volId = it.addr_id;
    u.volumeCount++
    Long startTime = ( it.alloc_time > notBefore ) ? it.alloc_time : notBefore;
    Long endTime = ( it.dealloc_time < notAfter ) ? it.dealloc_time : notAfter;
    Integer volSize = new Integer( it.volume_size );
    if( it.alloc_time > notBefore ) {
      u.volumeGigabytesAllocated += volSize
    }
    if( it.dealloc_time < notAfter ) {
      u.volumeGigabytesDeleted += volSize
    }
    def time = (volSize * (endTime - startTime) )
    u.volumeTimeSeconds += time
    time = time/(1000.0*60.0*60.0)
    u.volumeTime += Math.ceil( time )
    println "==> ${it.addr_id} ${time} ${new Date(it.alloc_time)} ${new Date(it.dealloc_time)}"
  }
  for( Group group : Groups.lookupUserGroups( user ) ) {
    def g = new GroupAddressData();
    g.groupName = group.getName();
    
    if( groups.containsKey( group.getName() ) ) {
      g = groups.get( group.getName() );
    } else {
      groups.put( group.getName(), g );
      groupResults.add( g );
    }
    g.metaClass.properties.findAll{ !it.name.startsWith("group") && it.name!="metaClass"&&it.name!="class"  }.each {
      g[it.name]+=u[it.name]
    }
  }
  results.add( u )
}
db?.commit()
println results
def class GroupAddressData {
  String groupName;
  Integer allocCount = 0;
  Integer assignCount = 0;
  Integer systemCount = 0;
  Integer allocTime = 0;
  Integer assignTime = 0;
  Integer systemTime = 0;
}
def class UserAddressData {
  String userName;
  Integer allocCount = 0;
  Integer assignCount = 0;
  Integer systemCount = 0;
  Integer allocTime = 0;
  Integer assignTime = 0;
  Integer systemTime = 0;
}
