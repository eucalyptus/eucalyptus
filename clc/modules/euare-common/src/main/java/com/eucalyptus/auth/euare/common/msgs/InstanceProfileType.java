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
package com.eucalyptus.auth.euare.common.msgs;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class InstanceProfileType extends EucalyptusData {

  private String path;
  private String instanceProfileName;
  private String instanceProfileId;
  private String arn;
  private Date createDate;
  private RoleListType roles;

  public String getPath( ) {
    return path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  public String getInstanceProfileName( ) {
    return instanceProfileName;
  }

  public void setInstanceProfileName( String instanceProfileName ) {
    this.instanceProfileName = instanceProfileName;
  }

  public String getInstanceProfileId( ) {
    return instanceProfileId;
  }

  public void setInstanceProfileId( String instanceProfileId ) {
    this.instanceProfileId = instanceProfileId;
  }

  public String getArn( ) {
    return arn;
  }

  public void setArn( String arn ) {
    this.arn = arn;
  }

  public Date getCreateDate( ) {
    return createDate;
  }

  public void setCreateDate( Date createDate ) {
    this.createDate = createDate;
  }

  public RoleListType getRoles( ) {
    return roles;
  }

  public void setRoles( RoleListType roles ) {
    this.roles = roles;
  }
}
