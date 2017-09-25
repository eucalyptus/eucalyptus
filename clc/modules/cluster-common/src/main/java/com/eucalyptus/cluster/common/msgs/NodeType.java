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

import java.net.URI;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class NodeType extends EucalyptusData {

  private String serviceTag;
  private String iqn;
  private String hypervisor;

  public String toString( ) {
    return "NodeType " + URI.create( serviceTag ).getHost( ) + " " + iqn + " " + hypervisor;
  }

  public String getServiceTag( ) {
    return serviceTag;
  }

  public void setServiceTag( String serviceTag ) {
    this.serviceTag = serviceTag;
  }

  public String getIqn( ) {
    return iqn;
  }

  public void setIqn( String iqn ) {
    this.iqn = iqn;
  }

  public String getHypervisor( ) {
    return hypervisor;
  }

  public void setHypervisor( String hypervisor ) {
    this.hypervisor = hypervisor;
  }
}
