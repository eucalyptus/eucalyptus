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

import java.util.Objects;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class GroupItemType extends EucalyptusData implements Comparable<GroupItemType> {

  private String groupId;
  private String groupName;

  public GroupItemType( ) {
  }

  public GroupItemType( String groupId, String groupName ) {
    this.groupId = groupId;
    this.groupName = groupName;
  }

  public static CompatFunction<GroupItemType, String> groupId( ) {
    return GroupItemType::getGroupId;
  }

  public static CompatFunction<GroupItemType, String> groupName( ) {
    return GroupItemType::getGroupName;
  }

  @Override
  public int compareTo( final GroupItemType o ) {
    return Objects.toString( groupId, "" ).compareTo( Objects.toString( o.groupId, "" ) );
  }

  public String getGroupId( ) {
    return groupId;
  }

  public void setGroupId( String groupId ) {
    this.groupId = groupId;
  }

  public String getGroupName( ) {
    return groupName;
  }

  public void setGroupName( String groupName ) {
    this.groupName = groupName;
  }
}
