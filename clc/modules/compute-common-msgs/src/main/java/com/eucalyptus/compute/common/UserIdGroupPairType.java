/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class UserIdGroupPairType extends EucalyptusData {

  @HttpParameterMapping( parameter = "UserId" )
  private String sourceUserId;
  @HttpParameterMapping( parameter = "GroupName" )
  private String sourceGroupName;
  @HttpParameterMapping( parameter = "GroupId" )
  private String sourceGroupId;

  public UserIdGroupPairType( ) {
  }

  public UserIdGroupPairType( String userId, String groupName, String groupId ) {
    this.sourceUserId = userId;
    this.sourceGroupName = groupName;
    this.sourceGroupId = groupId;
  }

  public String getSourceUserId( ) {
    return sourceUserId;
  }

  public void setSourceUserId( String sourceUserId ) {
    this.sourceUserId = sourceUserId;
  }

  public String getSourceGroupName( ) {
    return sourceGroupName;
  }

  public void setSourceGroupName( String sourceGroupName ) {
    this.sourceGroupName = sourceGroupName;
  }

  public String getSourceGroupId( ) {
    return sourceGroupId;
  }

  public void setSourceGroupId( String sourceGroupId ) {
    this.sourceGroupId = sourceGroupId;
  }
}
