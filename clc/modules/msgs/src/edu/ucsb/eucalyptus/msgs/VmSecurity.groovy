package edu.ucsb.eucalyptus.msgs

import edu.ucsb.eucalyptus.annotation.HttpParameterMapping
import edu.ucsb.eucalyptus.annotation.HttpEmbedded

public class VmSecurityMessage extends EucalyptusMessage{}
/** *******************************************************************************/
public class AuthorizeSecurityGroupIngressResponseType extends VmSecurityMessage {
  boolean _return;
}
public class AuthorizeSecurityGroupIngressType extends VmSecurityMessage {
  @HttpParameterMapping(parameter="UserId")
  String groupUserId;
  String groupName;
  @HttpEmbedded
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}
/** *******************************************************************************/
public class CreateSecurityGroupResponseType extends VmSecurityMessage {
  boolean _return;
}
public class CreateSecurityGroupType extends VmSecurityMessage {
  String groupName;
  String groupDescription;
}
/** *******************************************************************************/
public class DeleteSecurityGroupResponseType extends VmSecurityMessage {
  boolean _return;
}
public class DeleteSecurityGroupType extends VmSecurityMessage {
  String groupName;
}
/** *******************************************************************************/
public class RevokeSecurityGroupIngressResponseType extends VmSecurityMessage {
  boolean _return;
}
public class RevokeSecurityGroupIngressType extends VmSecurityMessage {
  @HttpParameterMapping(parameter="UserId")
  String groupUserId;
  String groupName;
  @HttpEmbedded
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}
/** *******************************************************************************/
public class DescribeSecurityGroupsResponseType extends VmSecurityMessage {
  ArrayList<SecurityGroupItemType> securityGroupInfo = new ArrayList<SecurityGroupItemType>();
}
public class DescribeSecurityGroupsType extends VmSecurityMessage {
  @HttpParameterMapping (parameter = "GroupName")
  ArrayList<String> securityGroupSet = new ArrayList<String>();
}
public class SecurityGroupItemType extends EucalyptusData {
  String ownerId;
  String groupName;
  String groupDescription;
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}

public class IpPermissionType extends EucalyptusData {
  String ipProtocol;
  int fromPort;
  int toPort;
  @HttpEmbedded
  ArrayList<UserIdGroupPairType> groups = new ArrayList<UserIdGroupPairType>();
  @HttpParameterMapping(parameter = "CidrIp")
  ArrayList<String> ipRanges = new ArrayList<String>();

  def IpPermissionType(){}

  def IpPermissionType(final ipProtocol, final fromPort, final toPort )
  {
    this.ipProtocol = ipProtocol;
    this.fromPort = fromPort;
    this.toPort = toPort;
  }

}
public class UserIdGroupPairType extends EucalyptusData {
  @HttpParameterMapping(parameter = "SourceSecurityGroupOwnerId")
  String userId;
  @HttpParameterMapping(parameter = "SourceSecurityGroupName")
  String groupName;

  def UserIdGroupPairType(){}

  def UserIdGroupPairType(final userId, final groupName)
  {
    this.userId = userId;
    this.groupName = groupName;
  }



}
