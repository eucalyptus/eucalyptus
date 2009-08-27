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

import edu.ucsb.eucalyptus.annotation.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class VmAddressMessage extends EucalyptusMessage{}
/** *******************************************************************************/
public class AllocateAddressType extends VmAddressMessage {} //** added 2008-02-01  **/
public class AllocateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;
}
/** *******************************************************************************/
public class ReleaseAddressType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;

  def ReleaseAddressType(final publicIp) {
    this.publicIp = publicIp;
  }

  def ReleaseAddressType() {}
}
public class ReleaseAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  boolean _return;
}
/** *******************************************************************************/
public class DescribeAddressesType extends VmAddressMessage { //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "PublicIp")
  ArrayList<String> publicIpsSet = new ArrayList<String>();
}
public class DescribeAddressesResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  ArrayList<DescribeAddressesResponseItemType> addressesSet = new ArrayList<DescribeAddressesResponseItemType>();
}
/** *******************************************************************************/
public class AssociateAddressType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;
  String instanceId;

  def AssociateAddressType(final publicIp, final instanceId) {
    this.publicIp = publicIp;
    this.instanceId = instanceId;
  }

  def AssociateAddressType() {
  }
}
public class AssociateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  boolean _return;
}
/** *******************************************************************************/
public class DisassociateAddressType extends VmAddressMessage {  //** added 2008-02-01  **/
  String publicIp;
}
public class DisassociateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  boolean _return;
}
/** *******************************************************************************/
public class DescribeAddressesResponseItemType extends EucalyptusData {  //** added 2008-02-01  **/
  String publicIp;
  String instanceId;

  def DescribeAddressesResponseItemType(final publicIp, final instanceId)
  {
    this.publicIp = publicIp;
    this.instanceId = instanceId;
  }

  def DescribeAddressesResponseItemType()
  {
  }

}


