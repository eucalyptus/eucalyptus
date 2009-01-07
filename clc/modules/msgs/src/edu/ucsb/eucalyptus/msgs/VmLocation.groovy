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
public class ClusterInfoType extends EucalyptusData {  //** added 2008-02-01  **/
  public ClusterInfoType(){}
  public ClusterInfoType(String zoneName, String zoneState){this.zoneName = zoneName; this.zoneState = zoneState;}
  String zoneName;
  String zoneState;
}