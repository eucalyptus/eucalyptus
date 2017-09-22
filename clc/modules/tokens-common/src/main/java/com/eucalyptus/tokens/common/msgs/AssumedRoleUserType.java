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
package com.eucalyptus.tokens.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AssumedRoleUserType extends EucalyptusData {
  private String assumedRoleId;
  private String arn;

  public AssumedRoleUserType() {
  }

  public AssumedRoleUserType( String assumedRoleId, String arn ) {
    this.assumedRoleId = assumedRoleId;
    this.arn = arn;
  }

  public String getAssumedRoleId() {
    return assumedRoleId;
  }

  public void setAssumedRoleId( String assumedRoleId ) {
    this.assumedRoleId = assumedRoleId;
  }

  public String getArn() {
    return arn;
  }

  public void setArn( String arn ) {
    this.arn = arn;
  }
}
