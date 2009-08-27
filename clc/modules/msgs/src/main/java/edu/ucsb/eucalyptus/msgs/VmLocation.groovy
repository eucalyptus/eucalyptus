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
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.msgs

import edu.ucsb.eucalyptus.annotation.HttpParameterMapping;
public class ClusterMessage extends EucalyptusMessage{}
/** *******************************************************************************/
public class DescribeAvailabilityZonesType extends ClusterMessage { //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "ZoneName")
  ArrayList<String> availabilityZoneSet = new ArrayList<String>();
}
public class DescribeAvailabilityZonesResponseType extends ClusterMessage { //** added 2008-02-01  **/
  ArrayList<ClusterInfoType> availabilityZoneInfo = new ArrayList<ClusterInfoType>();
}
/** *******************************************************************************/
public class DescribeRegionsType extends ClusterMessage { //** added 2008-12-01  **/
  @HttpParameterMapping (parameter = "Region")
  ArrayList<String> regions = new ArrayList<String>();
}
public class DescribeRegionsResponseType extends ClusterMessage { //** added 2008-12-01  **/
  ArrayList<RegionInfoType> regionInfo = new ArrayList<ClusterInfoType>();
}
/** *******************************************************************************/
public class ClusterInfoType extends EucalyptusData {  //** added 2008-02-01  **/
  public ClusterInfoType(){}
  public ClusterInfoType(String zoneName, String zoneState){this.zoneName = zoneName; this.zoneState = zoneState;}
  String zoneName;
  String zoneState;
}
public class RegionInfoType extends EucalyptusData {  //** added 2008-12-01  **/
  public RegionInfoType(){}
  public RegionInfoType(final String regionName, final String regionEndpoint) {
    this.regionName = regionName;
    this.regionEndpoint = regionEndpoint;
  }
  String regionName;
  String regionEndpoint;
}


