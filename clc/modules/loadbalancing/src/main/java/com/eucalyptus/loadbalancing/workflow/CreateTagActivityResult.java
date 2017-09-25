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
package com.eucalyptus.loadbalancing.workflow;

public class CreateTagActivityResult {

  private String tagKey = null;
  private String tagValue = null;
  private String securityGroup = null;

  public String getTagKey( ) {
    return tagKey;
  }

  public void setTagKey( String tagKey ) {
    this.tagKey = tagKey;
  }

  public String getTagValue( ) {
    return tagValue;
  }

  public void setTagValue( String tagValue ) {
    this.tagValue = tagValue;
  }

  public String getSecurityGroup( ) {
    return securityGroup;
  }

  public void setSecurityGroup( String securityGroup ) {
    this.securityGroup = securityGroup;
  }
}
