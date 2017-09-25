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

public class NodeCertInfo extends EucalyptusData implements Comparable<NodeCertInfo> {

  private String serviceTag;
  private String ccCert = "";
  private String ncCert = "";

  public int compareTo( NodeCertInfo o ) {
    return this.serviceTag.compareTo( o.serviceTag );
  }

  @Override
  public String toString( ) {
    return "NodeCertInfo [" + "serviceTag='" + serviceTag.replaceAll( "services/EucalyptusNC", "" ) + "\'" + ", ccCert='" + ccCert + "\'" + ", ncCert='" + ncCert + "\'" + "]";
  }

  public String getServiceTag( ) {
    return serviceTag;
  }

  public void setServiceTag( String serviceTag ) {
    this.serviceTag = serviceTag;
  }

  public String getCcCert( ) {
    return ccCert;
  }

  public void setCcCert( String ccCert ) {
    this.ccCert = ccCert;
  }

  public String getNcCert( ) {
    return ncCert;
  }

  public void setNcCert( String ncCert ) {
    this.ncCert = ncCert;
  }
}
