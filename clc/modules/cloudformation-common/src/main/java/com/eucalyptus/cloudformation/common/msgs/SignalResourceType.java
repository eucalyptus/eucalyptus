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
package com.eucalyptus.cloudformation.common.msgs;

public class SignalResourceType extends CloudFormationMessage {

  private String logicalResourceId;
  private String stackName;
  private String status;
  private String uniqueId;

  public String getLogicalResourceId( ) {
    return logicalResourceId;
  }

  public void setLogicalResourceId( String logicalResourceId ) {
    this.logicalResourceId = logicalResourceId;
  }

  public String getStackName( ) {
    return stackName;
  }

  public void setStackName( String stackName ) {
    this.stackName = stackName;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public String getUniqueId( ) {
    return uniqueId;
  }

  public void setUniqueId( String uniqueId ) {
    this.uniqueId = uniqueId;
  }
}
