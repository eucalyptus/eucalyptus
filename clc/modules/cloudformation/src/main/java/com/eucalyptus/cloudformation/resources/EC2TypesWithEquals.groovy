/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.resources

import com.eucalyptus.compute.common.CidrIpType
import com.eucalyptus.compute.common.IpPermissionType
import com.eucalyptus.compute.common.UserIdGroupPairType
import com.google.common.collect.Lists
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by ethomas on 2/8/16.
 */

  @EqualsAndHashCode
  @ToString(includeNames=true)
  public class IpPermissionTypeWithEquals {
    String ipProtocol;
    Integer fromPort;
    Integer toPort;
    ArrayList<UserIdGroupPairTypeWithEquals> groups = new ArrayList<UserIdGroupPairTypeWithEquals>();
    ArrayList<CidrIpTypeWithEquals> ipRanges = new ArrayList<CidrIpTypeWithEquals>();

    IpPermissionTypeWithEquals(IpPermissionType ipPermissionType) {
      this.ipProtocol = ipPermissionType.getIpProtocol();
      this.fromPort = ipPermissionType.getFromPort();
      this.toPort = ipPermissionType.getToPort();
      this.groups = convertGroups(ipPermissionType.getGroups());
      this.ipRanges = convertRanges(ipPermissionType.getIpRanges());
    }

    static Collection<IpPermissionTypeWithEquals> getNonNullCollection(Collection<IpPermissionType> ipPermissionTypes) {
      if (ipPermissionTypes == null) return Collections.emptyList();
      Collection<IpPermissionTypeWithEquals> retVal = Lists.newArrayList();
      for (IpPermissionType ipPermissionType: ipPermissionTypes) {
        retVal.add(new IpPermissionTypeWithEquals(ipPermissionType));
      }
      return retVal;
    }

    private ArrayList<UserIdGroupPairTypeWithEquals> convertGroups(ArrayList<UserIdGroupPairType> groups) {
      if (groups == null) return null;
      ArrayList<UserIdGroupPairTypeWithEquals> newGroups = Lists.newArrayList();
      for (UserIdGroupPairType group: groups) {
        newGroups.add(new UserIdGroupPairTypeWithEquals(group));
      }
      return newGroups;
    }

    private ArrayList<UserIdGroupPairType> convertGroupsBack(ArrayList<UserIdGroupPairTypeWithEquals> groups) {
      if (groups == null) return null;
      ArrayList<UserIdGroupPairType> newGroups = Lists.newArrayList();
      for (UserIdGroupPairTypeWithEquals group: groups) {
        newGroups.add(group.getUserIdGroupPairType());
      }
      return newGroups;
    }

    private ArrayList<CidrIpTypeWithEquals> convertRanges(ArrayList<CidrIpType> ranges) {
      if (ranges == null) return null;
      ArrayList<CidrIpTypeWithEquals> newRanges = Lists.newArrayList();
      for (CidrIpType group: ranges) {
        newRanges.add(new CidrIpTypeWithEquals(group));
      }
      return newRanges;
    }

    private ArrayList<CidrIpType> convertRangesBack(ArrayList<CidrIpTypeWithEquals> ranges) {
      if (ranges == null) return null;
      ArrayList<CidrIpType> newRanges = Lists.newArrayList();
      for (CidrIpTypeWithEquals group: ranges) {
        newRanges.add(group.getCidrIpType());
      }
      return newRanges;
    }

    public IpPermissionType getIpPermissionType() {
      IpPermissionType ipPermissionType = new IpPermissionType();
      ipPermissionType.setIpProtocol(ipProtocol);
      ipPermissionType.setFromPort(fromPort);
      ipPermissionType.setToPort(toPort);
      ipPermissionType.setGroups(convertGroupsBack(groups));
      ipPermissionType.setIpRanges(convertRangesBack(ipRanges));
      return ipPermissionType;
    }

  }

  @EqualsAndHashCode
  @ToString(includeNames=true)
  public class UserIdGroupPairTypeWithEquals {
    String sourceUserId;
    String sourceGroupName;
    String sourceGroupId;


    public UserIdGroupPairTypeWithEquals(UserIdGroupPairType userIdGroupPairType) {
      this.sourceUserId = userIdGroupPairType.getSourceUserId();
      this.sourceGroupName = userIdGroupPairType.getSourceGroupName();
      this.sourceGroupId = userIdGroupPairType.getSourceGroupId();
    }

    public UserIdGroupPairType getUserIdGroupPairType() {
      UserIdGroupPairType userIdGroupPairType = new UserIdGroupPairType();
      userIdGroupPairType.setSourceUserId(sourceUserId);
      userIdGroupPairType.setSourceGroupName(sourceGroupName);
      userIdGroupPairType.setSourceGroupId(sourceGroupId);
      return userIdGroupPairType;
    }
  }

  @EqualsAndHashCode
  @ToString(includeNames=true)
  public class CidrIpTypeWithEquals {
    String cidrIp;

    public CidrIpTypeWithEquals(CidrIpType cidrIpType) {
      this.cidrIp = cidrIpType.getCidrIp();
    }

    public CidrIpType getCidrIpType() {
      CidrIpType cidrIpType = new CidrIpType();
      cidrIpType.setCidrIp(cidrIp);
      return cidrIpType;
    }
  }

