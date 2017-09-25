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

public class SetStackPolicyType extends CloudFormationMessage {

  private String stackName;
  private String stackPolicyBody;
  private String stackPolicyURL;

  public String getStackName( ) {
    return stackName;
  }

  public void setStackName( String stackName ) {
    this.stackName = stackName;
  }

  public String getStackPolicyBody( ) {
    return stackPolicyBody;
  }

  public void setStackPolicyBody( String stackPolicyBody ) {
    this.stackPolicyBody = stackPolicyBody;
  }

  public String getStackPolicyURL( ) {
    return stackPolicyURL;
  }

  public void setStackPolicyURL( String stackPolicyURL ) {
    this.stackPolicyURL = stackPolicyURL;
  }
}
