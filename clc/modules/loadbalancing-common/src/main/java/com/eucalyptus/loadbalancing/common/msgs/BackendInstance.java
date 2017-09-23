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
package com.eucalyptus.loadbalancing.common.msgs;

import javax.annotation.Nonnull;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class BackendInstance extends EucalyptusData {

  private static final long serialVersionUID = 1L;
  @Nonnull
  @LoadBalancingMessageValidation.FieldRegex( LoadBalancingMessageValidation.FieldRegexValue.LOAD_BALANCER_INSTANCE_ID_OPTIONAL_STATUS )
  private String instanceId;
  @Nonnull
  private String instanceIpAddress;
  private Boolean reportHealthCheck;

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getInstanceIpAddress( ) {
    return instanceIpAddress;
  }

  public void setInstanceIpAddress( String instanceIpAddress ) {
    this.instanceIpAddress = instanceIpAddress;
  }

  public Boolean getReportHealthCheck( ) {
    return reportHealthCheck;
  }

  public void setReportHealthCheck( Boolean reportHealthCheck ) {
    this.reportHealthCheck = reportHealthCheck;
  }
}
