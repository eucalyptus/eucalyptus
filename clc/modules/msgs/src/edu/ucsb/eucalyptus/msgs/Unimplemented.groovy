package edu.ucsb.eucalyptus.msgs

public class UnimplementedMessage extends EucalyptusMessage {}
/** *******************************************************************************/
public class DescribeReservedInstancesOfferingsType extends UnimplementedMessage {
//:: reservedInstancesOfferingsSet/item/reservedInstancesOfferingId :://
  ArrayList<String> instanceIds = new ArrayList<String>();
  String instanceType;
  String availabilityZone;
  String productDescription;
}
public class DescribeReservedInstancesOfferingsResponseType extends UnimplementedMessage {
  ArrayList<String> reservedInstancesOfferingsSet = new ArrayList<String>();
}
public class DescribeReservedInstancesType extends UnimplementedMessage {
//:: reservedInstancesSet/item/reservedInstancesId :://
  ArrayList<String> instanceIds = new ArrayList<String>();
}
public class DescribeReservedInstancesResponseType extends UnimplementedMessage {
  ArrayList<String> reservedInstancesSet = new ArrayList<String>();
}
/** *******************************************************************************/
