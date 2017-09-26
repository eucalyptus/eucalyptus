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
package com.eucalyptus.compute.common.network;

import java.util.ArrayList;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class InstanceResourceReportType extends EucalyptusData {

  private ArrayList<String> publicIps = Lists.newArrayList( );
  private ArrayList<String> privateIps = Lists.newArrayList( );
  private ArrayList<String> macs = Lists.newArrayList( );

  public ArrayList<String> getPublicIps( ) {
    return publicIps;
  }

  public void setPublicIps( ArrayList<String> publicIps ) {
    this.publicIps = publicIps;
  }

  public ArrayList<String> getPrivateIps( ) {
    return privateIps;
  }

  public void setPrivateIps( ArrayList<String> privateIps ) {
    this.privateIps = privateIps;
  }

  public ArrayList<String> getMacs( ) {
    return macs;
  }

  public void setMacs( ArrayList<String> macs ) {
    this.macs = macs;
  }
}
