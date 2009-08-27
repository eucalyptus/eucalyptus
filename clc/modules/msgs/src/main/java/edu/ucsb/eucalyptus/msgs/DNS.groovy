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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package edu.ucsb.eucalyptus.msgs
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */
public class DNSResponseType extends EucalyptusMessage {
  def DNSResponseType() {}
}

public class DNSRequestType extends EucalyptusMessage {

  def DNSRequestType() {}
}

public class UpdateARecordType extends DNSRequestType {
  String zone;
  String name;
  String address;
  long ttl;

  def UpdateARecordType() {}

  def UpdateARecordType(String zone, String name, String address, long ttl) {
      this.zone = zone;
      this.name = name;
      this.address = address;
      this.ttl = ttl;
  }
}

public class UpdateARecordResponseType extends DNSResponseType {
  def UpdateARecordResponseType() {}
}

public class RemoveARecordType extends DNSRequestType {
  String zone;
  String name;

  def RemoveARecordType() {}

  def RemoveARecordType(String zone, String name) {
      this.zone = zone;
      this.name = name;
  }
}

public class RemoveARecordResponseType extends DNSResponseType {
  def RemoveARecordResponseType() {}
}

public class UpdateCNAMERecordType extends DNSRequestType {
  String zone;
  String name;
  String alias;
  long ttl;

  def UpdateCNAMERecordType() {}

  def UpdateCNAMERecordType(String zone, String name, String alias, long ttl) {
      this.zone = zone;
      this.name = name;
      this.alias = alias;
      this.ttl = ttl;
  }
}

public class UpdateCNAMERecordResponseType extends DNSResponseType {
  def UpdateCNAMERecordResponseType() {}
}

public class RemoveCNAMERecordType extends DNSRequestType {
  String zone;
  String name;

  def RemoveCNAMERecordType() {}

  def RemoveCNAMERecordType(String zone, String name) {
      this.zone = zone;
      this.name = name;
  }
}

public class RemoveCNAMERecordResponseType extends DNSResponseType {
  def RemoveCNAMERecordResponseType() {}
}

public class AddZoneType extends DNSRequestType {
  String name;
  def AddZoneType() {}
  def AddZoneType(String name) {
    this.name = name;
  }
}

public class AddZoneResponseType extends DNSResponseType {
}

public class DeleteZoneType extends DNSRequestType {
  String name;
  def DeleteZoneType() {}
  def DeleteZoneType(String name) {
    this.name = name;
  }
}

public class DeleteZoneResponseType extends DNSResponseType {
}
