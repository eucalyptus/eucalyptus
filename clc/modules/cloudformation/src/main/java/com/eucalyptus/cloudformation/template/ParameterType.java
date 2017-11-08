package com.eucalyptus.cloudformation.template;

/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
