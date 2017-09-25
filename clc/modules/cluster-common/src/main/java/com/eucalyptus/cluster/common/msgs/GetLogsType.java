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
package com.eucalyptus.cluster.common.msgs;

public class GetLogsType extends CloudGatherLogMessage implements Comparable {

  private String serviceTag;

  public GetLogsType( ) {
  }

  public GetLogsType( final String serviceTag ) {
    this.serviceTag = serviceTag;
  }

  public int compareTo( Object o ) {
    return this.serviceTag.compareTo( ( (GetLogsType) o ).serviceTag );
  }

  public String getServiceTag( ) {
    return serviceTag;
  }

  public void setServiceTag( String serviceTag ) {
    this.serviceTag = serviceTag;
  }
}
