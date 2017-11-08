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
package com.eucalyptus.cloudformation.template;

import com.eucalyptus.cloudformation.AccessDeniedException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeAvailabilityZonesResponseType;
import com.eucalyptus.compute.common.DescribeAvailabilityZonesType;
import com.eucalyptus.compute.common.DescribeImagesResponseType;
import com.eucalyptus.compute.common.DescribeImagesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseType;
import com.eucalyptus.compute.common.DescribeKeyPairsType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetIdSetItemType;
import com.eucalyptus.compute.common.SubnetIdSetType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.compute.common.VpcIdSetItemType;
import com.eucalyptus.compute.common.VpcIdSetType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.eucalyptus.cloudformation.template.ParameterType.*;

public class AWSParameterTypeValidationHelper {

  public enum ValidParameterFamilyValues {
    AVAILABILITY_ZONE_NAMES (AWS_EC2_AvailabilityZone_Name, List_AWS_EC2_AvailabilityZone_Name) {
      @Override
      public List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException {
        List<String> retVal = Lists.newArrayList();
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        try {
          DescribeAvailabilityZonesType describeAvailabilityZonesType = MessageHelper.createMessage(DescribeAvailabilityZonesType.class, effectiveUserId);
          describeAvailabilityZonesType.setAvailabilityZoneSet(Lists.newArrayList(valuesToCheck));
          DescribeAvailabilityZonesResponseType describeAvailabilityZonesResponseType = AsyncRequests.sendSync(configuration, describeAvailabilityZonesType);
          if (describeAvailabilityZonesResponseType != null && describeAvailabilityZonesResponseType.getAvailabilityZoneInfo() != null) {
            for (ClusterInfoType clusterInfoType:describeAvailabilityZonesResponseType.getAvailabilityZoneInfo()) {
              retVal.add(clusterInfoType.getZoneName());
            }
          }
        } catch (Exception e) {
          Throwable rootCause = Throwables.getRootCause(e);
          throw new AccessDeniedException("Unable to access availability zones.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
        }
        return retVal;
      }
    },
    KEY_PAIR_NAMES (AWS_EC2_KeyPair_KeyName, List_AWS_EC2_KeyPair_KeyName) {
      @Override
      public List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException {
        List<String> retVal = Lists.newArrayList();
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        try {
          DescribeKeyPairsType describeKeyPairsType = MessageHelper.createMessage(DescribeKeyPairsType.class, effectiveUserId);
          describeKeyPairsType.setKeySet(Lists.newArrayList(valuesToCheck));
          DescribeKeyPairsResponseType describeKeyPairsResponseType = AsyncRequests.sendSync(configuration, describeKeyPairsType);
          if (describeKeyPairsResponseType != null && describeKeyPairsResponseType.getKeySet() != null) {
            for (DescribeKeyPairsResponseItemType describeKeyPairsResponseItemType:describeKeyPairsResponseType.getKeySet()) {
              retVal.add(describeKeyPairsResponseItemType.getKeyName());
            }
          }
        } catch (Exception e) {
          Throwable rootCause = Throwables.getRootCause(e);
          throw new AccessDeniedException("Unable to access keypairs.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
        }
        return retVal;
      }
    },
    IMAGE_IDS (AWS_EC2_Image_Id, List_AWS_EC2_Image_Id) {
      @Override
      public List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException {
        List<String> retVal = Lists.newArrayList();
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        try {
          DescribeImagesType describeImagesType = MessageHelper.createMessage(DescribeImagesType.class, effectiveUserId);
          describeImagesType.setImagesSet(Lists.newArrayList(valuesToCheck));
          DescribeImagesResponseType describeImagesResponseType = AsyncRequests.sendSync(configuration, describeImagesType);
          if (describeImagesResponseType != null && describeImagesResponseType.getImagesSet() != null) {
            for (ImageDetails imageDetails: describeImagesResponseType.getImagesSet()) {
              retVal.add(imageDetails.getImageId());
            }
          }
        } catch (Exception e) {
          Throwable rootCause = Throwables.getRootCause(e);
          throw new AccessDeniedException("Unable to access image ids.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
        }
        return retVal;
      }
    },
    INSTANCE_IDS (AWS_EC2_Instance_Id, List_AWS_EC2_Instance_Id) {
      @Override
      public List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException {
        List<String> retVal = Lists.newArrayList();
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        try {
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, effectiveUserId);
          describeInstancesType.setInstancesSet(Lists.newArrayList(valuesToCheck));
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync(configuration, describeInstancesType);
          if (describeInstancesResponseType != null && describeInstancesResponseType.getReservationSet() != null) {
            for (ReservationInfoType reservationInfoType: describeInstancesResponseType.getReservationSet()) {
              if (reservationInfoType != null && reservationInfoType.getInstancesSet() != null) {
                for (RunningInstancesItemType runningInstancesItemType: reservationInfoType.getInstancesSet()) {
                  retVal.add(runningInstancesItemType.getInstanceId());
                }
              }
            }
          }
        } catch (Exception e) {
          Throwable rootCause = Throwables.getRootCause(e);
          throw new AccessDeniedException("Unable to access instance ids.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
        }
        return retVal;
      }
    },
    SUBNET_IDS (AWS_EC2_Subnet_Id, List_AWS_EC2_Subnet_Id) {
      @Override
      public List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException {
        List<String> retVal = Lists.newArrayList();
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        try {
          DescribeSubnetsType describeSubnetsType = MessageHelper.createMessage(DescribeSubnetsType.class, effectiveUserId);
          SubnetIdSetType subnetSet = new SubnetIdSetType();
          ArrayList<SubnetIdSetItemType> item = Lists.newArrayList(
            Collections2.transform(valuesToCheck, new Function<String, SubnetIdSetItemType>() {
              @Nullable
              @Override
              public SubnetIdSetItemType apply(@Nullable String subnetId) {
                SubnetIdSetItemType subnetIdSetItemType = new SubnetIdSetItemType();
                subnetIdSetItemType.setSubnetId(subnetId);
                return subnetIdSetItemType;
              }
            })
          );
          subnetSet.setItem(item);
          describeSubnetsType.setSubnetSet(subnetSet);
          DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.sendSync(configuration, describeSubnetsType);
          if (describeSubnetsResponseType != null && describeSubnetsResponseType.getSubnetSet() != null && describeSubnetsResponseType.getSubnetSet().getItem() != null) {
            for (SubnetType subnetType:describeSubnetsResponseType.getSubnetSet().getItem()) {
              retVal.add(subnetType.getSubnetId());
            }
          }
        } catch (Exception e) {
          Throwable rootCause = Throwables.getRootCause(e);
          throw new AccessDeniedException("Unable to access subnet ids.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
        }
        return retVal;
      }
    },
    VOLUME_IDS (AWS_EC2_Volume_Id, List_AWS_EC2_Volume_Id) {
      @Override
      public List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException {
        List<String> retVal = Lists.newArrayList();
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        try {
          DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, effectiveUserId);
          describeVolumesType.setVolumeSet(Lists.newArrayList(valuesToCheck));
          DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.sendSync(configuration, describeVolumesType);
          if (describeVolumesResponseType != null && describeVolumesResponseType.getVolumeSet() != null && describeVolumesResponseType.getVolumeSet() != null) {
            for (Volume volume: describeVolumesResponseType.getVolumeSet()) {
              retVal.add(volume.getVolumeId());
            }
          }
        } catch (Exception e) {
          Throwable rootCause = Throwables.getRootCause(e);
          throw new AccessDeniedException("Unable to access volume ids.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
        }
        return retVal;
      }
    },
    VPC_IDS (AWS_EC2_VPC_Id, List_AWS_EC2_VPC_Id) {
      @Override
      public List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException {
        List<String> retVal = Lists.newArrayList();
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        try {
          DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, effectiveUserId);
          VpcIdSetType vpcSet = new VpcIdSetType();
          ArrayList<VpcIdSetItemType> item = Lists.newArrayList(
            Collections2.transform(valuesToCheck, new Function<String, VpcIdSetItemType>() {
              @Nullable
              @Override
              public VpcIdSetItemType apply(@Nullable String vpcId) {
                VpcIdSetItemType vpcIdSetItemType = new VpcIdSetItemType();
                vpcIdSetItemType.setVpcId(vpcId);
                return vpcIdSetItemType;
              }
            })
          );
          vpcSet.setItem(item);
          describeVpcsType.setVpcSet(vpcSet);
          DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.sendSync(configuration, describeVpcsType);
          if (describeVpcsResponseType != null && describeVpcsResponseType.getVpcSet() != null && describeVpcsResponseType.getVpcSet().getItem() != null) {
            for (VpcType vpcType:describeVpcsResponseType.getVpcSet().getItem()) {
              retVal.add(vpcType.getVpcId());
            }
          }
        } catch (Exception e) {
          Throwable rootCause = Throwables.getRootCause(e);
          throw new AccessDeniedException("Unable to access VPC ids.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
        }
        return retVal;
      }
    },
    SECURITY_GROUP_IDS (AWS_EC2_SecurityGroup_Id, List_AWS_EC2_SecurityGroup_Id) {
      @Override
      public List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException {
        List<String> retVal = Lists.newArrayList();
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        try {
          DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, effectiveUserId);
          describeSecurityGroupsType.setSecurityGroupIdSet(Lists.newArrayList(valuesToCheck));
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);
          if (describeSecurityGroupsResponseType != null && describeSecurityGroupsResponseType.getSecurityGroupInfo() != null) {
            for (SecurityGroupItemType securityGroupItemType:describeSecurityGroupsResponseType.getSecurityGroupInfo()) {
              retVal.add(securityGroupItemType.getGroupId());
            }
          }
        } catch (Exception e) {
          Throwable rootCause = Throwables.getRootCause(e);
          throw new AccessDeniedException("Unable to access security groups.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
        }
        return retVal;
      }
    },
    SECURITY_GROUP_NAMES (AWS_EC2_SecurityGroup_GroupName, List_AWS_EC2_SecurityGroup_GroupName) {
      @Override
      public List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException {
        List<String> retVal = Lists.newArrayList();
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        try {
          DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, effectiveUserId);
          describeSecurityGroupsType.setSecurityGroupSet(Lists.newArrayList(valuesToCheck));
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);
          if (describeSecurityGroupsResponseType != null && describeSecurityGroupsResponseType.getSecurityGroupInfo() != null) {
            for (SecurityGroupItemType securityGroupItemType:describeSecurityGroupsResponseType.getSecurityGroupInfo()) {
              retVal.add(securityGroupItemType.getGroupName());
            }
          }
        } catch (Exception e) {
          Throwable rootCause = Throwables.getRootCause(e);
          throw new AccessDeniedException("Unable to access security groups.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
        }
        return retVal;
      }
    };
    private final ParameterType singularType;
    private final ParameterType listType;
    ValidParameterFamilyValues(ParameterType singularType, ParameterType listType) {
      this.singularType = singularType;
      this.listType = listType;
    }
    public boolean matchesSingularType(ParameterType parameterType) {
      return singularType == parameterType;
    }
    public boolean matchesListType(ParameterType parameterType) {
      return listType == parameterType;
    }
    public abstract List<String> getValidValuesFromValueSet(String effectiveUserId, Collection<String> valuesToCheck) throws AccessDeniedException;
  }

  private static boolean matchAndValidateTypeFamily(StackEntity.Parameter parameter,
                                                    ParameterType parameterType, String effectiveUserId,
                                                    ValidParameterFamilyValues validParameterFamilyValues) throws Exception {
    boolean matchesSingular = validParameterFamilyValues.matchesSingularType(parameterType);
    boolean matchesList = validParameterFamilyValues.matchesListType(parameterType);
    boolean matches = matchesSingular || matchesList;
    if (matches) {
      JsonNode jsonNode = JsonHelper.getJsonNodeFromString(parameter.getJsonValue());
      Collection<String> valuesToCheck = Sets.newHashSet();
      if (matchesSingular) {
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
      List<String> validValues = validParameterFamilyValues.getValidValuesFromValueSet(effectiveUserId, valuesToCheck);
      for (String valueToCheck : valuesToCheck) {
        if (!validValues.contains(valueToCheck)) {
          throw new ValidationErrorException("Parameter validation failed: parameter value " + valueToCheck + " for parameter name " + parameter.getKey() + " is not an allowed value for the parameter type.");
        }
      }
    }
    return matches;
  }


  public static void validateParameter(StackEntity.Parameter parameter, ParameterType parameterType, String effectiveUserId) throws Exception {
    if (parameterType == null) throw new ValidationErrorException("Can not find parameter type for parameter " + parameter.getKey());
    for (ValidParameterFamilyValues validParameterFamilyValues:ValidParameterFamilyValues.values()) {
      if (matchAndValidateTypeFamily(parameter, parameterType, effectiveUserId, validParameterFamilyValues)) {
        break; // found the one to check against.
      }
    }
  }
}
