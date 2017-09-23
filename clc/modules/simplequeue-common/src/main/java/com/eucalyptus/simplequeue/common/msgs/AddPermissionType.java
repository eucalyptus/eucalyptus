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
package com.eucalyptus.simplequeue.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpParameterMapping;

public class AddPermissionType extends SimpleQueueMessage implements QueueUrlGetterSetter {

  private String queueUrl;
  private String label;
  @HttpParameterMapping( parameter = "AWSAccountId" )
  private ArrayList<String> awsAccountId = new ArrayList<String>( );
  private ArrayList<String> actionName = new ArrayList<String>( );

  public String getQueueUrl( ) {
    return queueUrl;
  }

  public void setQueueUrl( String queueUrl ) {
    this.queueUrl = queueUrl;
  }

  public String getLabel( ) {
    return label;
  }

  public void setLabel( String label ) {
    this.label = label;
  }

  public ArrayList<String> getAwsAccountId( ) {
    return awsAccountId;
  }

  public void setAwsAccountId( ArrayList<String> awsAccountId ) {
    this.awsAccountId = awsAccountId;
  }

  public ArrayList<String> getActionName( ) {
    return actionName;
  }

  public void setActionName( ArrayList<String> actionName ) {
    this.actionName = actionName;
  }
}
