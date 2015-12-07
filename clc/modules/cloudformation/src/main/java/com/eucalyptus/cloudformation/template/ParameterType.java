package com.eucalyptus.cloudformation.template;

/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 ************************************************************************/
public enum ParameterType {
  String("String"),
  Number("Number"),
  CommaDelimitedList("CommaDelimitedList"),
  AWS_EC2_AvailabilityZone_Name("AWS::EC2::AvailabilityZone::Name"),
  AWS_EC2_Image_Id("AWS::EC2::Image::Id"),
  AWS_EC2_Instance_Id("AWS::EC2::Instance::Id"),
  AWS_EC2_KeyPair_KeyName("AWS::EC2::KeyPair::KeyName"),
  AWS_EC2_SecurityGroup_Id("AWS::EC2::SecurityGroup::Id"),
  AWS_EC2_SecurityGroup_GroupName("AWS::EC2::SecurityGroup::GroupName"),
  AWS_EC2_Subnet_Id("AWS::EC2::Subnet::Id"),
  AWS_EC2_Volume_Id("AWS::EC2::Volume::Id"),
  AWS_EC2_VPC_Id("AWS::EC2::VPC::Id"),
  List_String("List<String>"),
  List_Number("List<Number>"),
  List_AWS_EC2_AvailabilityZone_Name("List<AWS::EC2::AvailabilityZone::Name>"),
  List_AWS_EC2_Image_Id("List<AWS::EC2::Image::Id>"),
  List_AWS_EC2_Instance_Id("List<AWS::EC2::Instance::Id>"),
  List_AWS_EC2_KeyPair_KeyName("List<AWS::EC2::KeyPair::KeyName>"),
  List_AWS_EC2_SecurityGroup_Id("List<AWS::EC2::SecurityGroup::Id>"),
  List_AWS_EC2_SecurityGroup_GroupName("List<AWS::EC2::SecurityGroup::GroupName>"),
  List_AWS_EC2_Subnet_Id("List<AWS::EC2::Subnet::Id>"),
  List_AWS_EC2_VPC_Id("List<AWS::EC2::VPC::Id>"),
  List_AWS_EC2_Volume_Id("List<AWS::EC2::Volume::Id>");

  private final String displayValue;
  ParameterType(String displayValue) {
    this.displayValue = displayValue;
  }

  public static ParameterType displayValueOf(String typeStr) {
    for (ParameterType parameterType: values()) {
      if (parameterType.displayValue.equals(typeStr)) return parameterType;
    }
    throw new IllegalArgumentException("No such ParameterType " + typeStr);
  }

  public static String[] displayValues() {
    String[] displayValues = new String[values().length];
    int ctr = 0;

    for (ParameterType parameterType: values()) {
      displayValues[ctr++] = parameterType.displayValue;
    }
    return displayValues;
  }


}
