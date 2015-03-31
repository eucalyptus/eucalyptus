/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.template;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseType;
import com.eucalyptus.compute.common.DescribeKeyPairsType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

public class AWSParameterTypeValidationHelper {
  public static List<String> getKeyPairKeyNames(String effectiveUserId) throws Exception {
    List<String> retVal = Lists.newArrayList();
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    DescribeKeyPairsType describeKeyPairsType = MessageHelper.createMessage(DescribeKeyPairsType.class, effectiveUserId);
    DescribeKeyPairsResponseType describeKeyPairsResponseType = AsyncRequests.<DescribeKeyPairsType, DescribeKeyPairsResponseType>sendSync(configuration, describeKeyPairsType);
    if (describeKeyPairsResponseType != null && describeKeyPairsResponseType.getKeySet() != null) {
      for (DescribeKeyPairsResponseItemType describeKeyPairsResponseItemType:describeKeyPairsResponseType.getKeySet()) {
        retVal.add(describeKeyPairsResponseItemType.getKeyName());
      }
    }
    return retVal;
  }

  public static List<String> getSubnetIds(String effectiveUserId) throws Exception {
    List<String> retVal = Lists.newArrayList();
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    DescribeSubnetsType describeSubnetsType = MessageHelper.createMessage(DescribeSubnetsType.class, effectiveUserId);
    DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.<DescribeSubnetsType, DescribeSubnetsResponseType>sendSync(configuration, describeSubnetsType);
    if (describeSubnetsResponseType != null && describeSubnetsResponseType.getSubnetSet() != null && describeSubnetsResponseType.getSubnetSet().getItem() != null) {
      for (SubnetType subnetType:describeSubnetsResponseType.getSubnetSet().getItem()) {
        retVal.add(subnetType.getSubnetId());
      }
    }
    return retVal;
  }

  public static List<String> getSecurityGroupIds(String effectiveUserId) throws Exception {
    List<String> retVal = Lists.newArrayList();
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, effectiveUserId);
    DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
    if (describeSecurityGroupsResponseType != null && describeSecurityGroupsResponseType.getSecurityGroupInfo() != null) {
      for (SecurityGroupItemType securityGroupItemType:describeSecurityGroupsResponseType.getSecurityGroupInfo()) {
        retVal.add(securityGroupItemType.getGroupId());
      }
    }
    return retVal;
  }


  public static void validateParameter(StackEntity.Parameter parameter, TemplateParser.ParameterType parameterType, String effectiveUserId) throws Exception {
    if (parameterType == null) throw new ValidationErrorException("Can not find parameter type for parameter " + parameter.getKey());
    if (parameterType == TemplateParser.ParameterType.AWS_EC2_KeyPair_KeyName ||
      parameterType == TemplateParser.ParameterType.List_AWS_EC2_KeyPair_KeyName) {
      List<String> keyPairNames = AWSParameterTypeValidationHelper.getKeyPairKeyNames(effectiveUserId);
      JsonNode jsonNode = JsonHelper.getJsonNodeFromString(parameter.getJsonValue());
      List<String> valuesToCheck = Lists.newArrayList();
      if (parameterType == TemplateParser.ParameterType.AWS_EC2_KeyPair_KeyName) {
        if (!jsonNode.isValueNode())
          throw new ValidationErrorException("Invalid value for Parameter " + parameter.getKey());
        valuesToCheck.add(jsonNode.asText());
      } else {
        if (!jsonNode.isArray())
          throw new ValidationErrorException("Invalid value for Parameter " + parameter.getKey());
        for (int i = 0; i < jsonNode.size(); i++) {
          JsonNode elementNode = jsonNode.get(i);
          if (!elementNode.isValueNode())
            throw new ValidationErrorException("Invalid value for Parameter " + parameter.getKey());
          valuesToCheck.add(elementNode.asText());
        }
      }
      for (String valueToCheck: valuesToCheck) {
        if (!keyPairNames.contains(valueToCheck)) {
          throw new ValidationErrorException("Invalid value for parameter " + parameter.getKey() + ", No such key pair name '" + valueToCheck + "'");
        }
      }
    } else if (parameterType == TemplateParser.ParameterType.AWS_EC2_SecurityGroup_Id ||
      parameterType == TemplateParser.ParameterType.List_AWS_EC2_SecurityGroup_Id) {
      List<String> securityGroupIds = AWSParameterTypeValidationHelper.getSecurityGroupIds(effectiveUserId);
      JsonNode jsonNode = JsonHelper.getJsonNodeFromString(parameter.getJsonValue());
      List<String> valuesToCheck = Lists.newArrayList();
      if (parameterType == TemplateParser.ParameterType.AWS_EC2_SecurityGroup_Id) {
        if (!jsonNode.isValueNode())
          throw new ValidationErrorException("Invalid value for Parameter " + parameter.getKey());
        valuesToCheck.add(jsonNode.asText());
      } else {
        if (!jsonNode.isArray())
          throw new ValidationErrorException("Invalid value for Parameter " + parameter.getKey());
        for (int i = 0; i < jsonNode.size(); i++) {
          JsonNode elementNode = jsonNode.get(i);
          if (!elementNode.isValueNode())
            throw new ValidationErrorException("Invalid value for Parameter " + parameter.getKey());
          valuesToCheck.add(elementNode.asText());
        }
      }
      for (String valueToCheck: valuesToCheck) {
        if (!securityGroupIds.contains(valueToCheck)) {
          throw new ValidationErrorException("Invalid value for parameter " + parameter.getKey() + ", No such security group id '" + valueToCheck + "'");
        }
      }
    } else if (parameterType == TemplateParser.ParameterType.AWS_EC2_Subnet_Id ||
      parameterType == TemplateParser.ParameterType.List_AWS_EC2_Subnet_Id) {
      List<String> subnetIds = AWSParameterTypeValidationHelper.getSecurityGroupIds(effectiveUserId);
      JsonNode jsonNode = JsonHelper.getJsonNodeFromString(parameter.getJsonValue());
      List<String> valuesToCheck = Lists.newArrayList();
      if (parameterType == TemplateParser.ParameterType.AWS_EC2_Subnet_Id) {
        if (!jsonNode.isValueNode())
          throw new ValidationErrorException("Invalid value for Parameter " + parameter.getKey());
        valuesToCheck.add(jsonNode.asText());
      } else {
        if (!jsonNode.isArray())
          throw new ValidationErrorException("Invalid value for Parameter " + parameter.getKey());
        for (int i = 0; i < jsonNode.size(); i++) {
          JsonNode elementNode = jsonNode.get(i);
          if (!elementNode.isValueNode())
            throw new ValidationErrorException("Invalid value for Parameter " + parameter.getKey());
          valuesToCheck.add(elementNode.asText());
        }
      }
      for (String valueToCheck : valuesToCheck) {
        if (!subnetIds.contains(valueToCheck)) {
          throw new ValidationErrorException("Invalid value for parameter " + parameter.getKey() + ", No such key pair name '" + valueToCheck + "'");
        }
      }
    } else { // we don't care
      ;
    }
  }
}
