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

public class DescribeNetworkInterfaceAttributeResponseType extends VpcMessage {

  private String networkInterfaceId;
  private NullableAttributeValueType description;
  private AttributeBooleanValueType sourceDestCheck;
  private GroupSetType groupSet;
  private NetworkInterfaceAttachmentType attachment;

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public NullableAttributeValueType getDescription( ) {
    return description;
  }

  public void setDescription( NullableAttributeValueType description ) {
    this.description = description;
  }

  public AttributeBooleanValueType getSourceDestCheck( ) {
    return sourceDestCheck;
  }

  public void setSourceDestCheck( AttributeBooleanValueType sourceDestCheck ) {
    this.sourceDestCheck = sourceDestCheck;
  }

  public GroupSetType getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( GroupSetType groupSet ) {
    this.groupSet = groupSet;
  }

  public NetworkInterfaceAttachmentType getAttachment( ) {
    return attachment;
  }

  public void setAttachment( NetworkInterfaceAttachmentType attachment ) {
    this.attachment = attachment;
  }
}
