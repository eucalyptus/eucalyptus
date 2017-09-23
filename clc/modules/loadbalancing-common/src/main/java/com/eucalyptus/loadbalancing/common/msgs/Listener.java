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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Listener extends EucalyptusData {

  private static final long serialVersionUID = 1L;
  private String protocol;
  private Integer loadBalancerPort;
  private String instanceProtocol;
  private Integer instancePort;
  private String SSLCertificateId;

  public Listener( ) {
  }

  public String getProtocol( ) {
    return protocol;
  }

  public void setProtocol( String protocol ) {
    this.protocol = protocol;
  }

  public Integer getLoadBalancerPort( ) {
    return loadBalancerPort;
  }

  public void setLoadBalancerPort( Integer loadBalancerPort ) {
    this.loadBalancerPort = loadBalancerPort;
  }

  public String getInstanceProtocol( ) {
    return instanceProtocol;
  }

  public void setInstanceProtocol( String instanceProtocol ) {
    this.instanceProtocol = instanceProtocol;
  }

  public Integer getInstancePort( ) {
    return instancePort;
  }

  public void setInstancePort( Integer instancePort ) {
    this.instancePort = instancePort;
  }

  public String getSSLCertificateId( ) {
    return SSLCertificateId;
  }

  public void setSSLCertificateId( String SSLCertificateId ) {
    this.SSLCertificateId = SSLCertificateId;
  }
}
