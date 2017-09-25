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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class NodeLogInfo extends EucalyptusData implements Comparable<NodeLogInfo> {

  private String serviceTag;
  private String ccLog = "";
  private String ncLog = "";
  private String httpdLog = "";
  private String axis2Log = "";

  public int compareTo( NodeLogInfo o ) {
    return this.serviceTag.compareTo( o.serviceTag );
  }

  public String getServiceTag( ) {
    return serviceTag;
  }

  public void setServiceTag( String serviceTag ) {
    this.serviceTag = serviceTag;
  }

  public String getCcLog( ) {
    return ccLog;
  }

  public void setCcLog( String ccLog ) {
    this.ccLog = ccLog;
  }

  public String getNcLog( ) {
    return ncLog;
  }

  public void setNcLog( String ncLog ) {
    this.ncLog = ncLog;
  }

  public String getHttpdLog( ) {
    return httpdLog;
  }

  public void setHttpdLog( String httpdLog ) {
    this.httpdLog = httpdLog;
  }

  public String getAxis2Log( ) {
    return axis2Log;
  }

  public void setAxis2Log( String axis2Log ) {
    this.axis2Log = axis2Log;
  }
}
