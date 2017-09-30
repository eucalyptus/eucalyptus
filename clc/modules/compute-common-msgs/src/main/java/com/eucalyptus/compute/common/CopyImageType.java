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

public class CopyImageType extends VmImageMessage {

  private String sourceRegion;
  private String sourceImageId;
  private String name;
  private String description;
  private String clientToken;

  public String getSourceRegion( ) {
    return sourceRegion;
  }

  public void setSourceRegion( String sourceRegion ) {
    this.sourceRegion = sourceRegion;
  }

  public String getSourceImageId( ) {
    return sourceImageId;
  }

  public void setSourceImageId( String sourceImageId ) {
    this.sourceImageId = sourceImageId;
  }

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }
}
