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

import java.util.ArrayList;
import com.google.common.collect.Lists;

public class CreateFlowLogsType extends FlowLogMessage {

  private ArrayList<String> resourceId = Lists.newArrayList( );
  private String resourceType;
  private String trafficType;
  private String logGroupName;
  private String deliverLogsPermissionArn;
  private String clientToken;

  public ArrayList<String> getResourceId( ) {
    return resourceId;
  }

  public void setResourceId( ArrayList<String> resourceId ) {
    this.resourceId = resourceId;
  }

  public String getResourceType( ) {
    return resourceType;
  }

  public void setResourceType( String resourceType ) {
    this.resourceType = resourceType;
  }

  public String getTrafficType( ) {
    return trafficType;
  }

  public void setTrafficType( String trafficType ) {
    this.trafficType = trafficType;
  }

  public String getLogGroupName( ) {
    return logGroupName;
  }

  public void setLogGroupName( String logGroupName ) {
    this.logGroupName = logGroupName;
  }

  public String getDeliverLogsPermissionArn( ) {
    return deliverLogsPermissionArn;
  }

  public void setDeliverLogsPermissionArn( String deliverLogsPermissionArn ) {
    this.deliverLogsPermissionArn = deliverLogsPermissionArn;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }
}
