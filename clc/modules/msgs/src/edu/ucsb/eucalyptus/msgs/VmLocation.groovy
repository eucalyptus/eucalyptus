package edu.ucsb.eucalyptus.msgs

import edu.ucsb.eucalyptus.annotation.HttpParameterMapping

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

