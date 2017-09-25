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
package com.eucalyptus.auth.euare.common.msgs;

import java.util.ArrayList;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ListEntitiesForPolicyResultType extends EucalyptusData {

  private Boolean isTruncated;
  private String marker;
  private ArrayList<PolicyGroup> policyGroups = Lists.newArrayList( );
  private ArrayList<PolicyRole> policyRoles = Lists.newArrayList( );
  private ArrayList<PolicyUser> policyUsers = Lists.newArrayList( );

  public Boolean getIsTruncated( ) {
    return isTruncated;
  }

  public void setIsTruncated( Boolean isTruncated ) {
    this.isTruncated = isTruncated;
  }

  public String getMarker( ) {
    return marker;
  }

  public void setMarker( String marker ) {
    this.marker = marker;
  }

  public ArrayList<PolicyGroup> getPolicyGroups( ) {
    return policyGroups;
  }

  public void setPolicyGroups( ArrayList<PolicyGroup> policyGroups ) {
    this.policyGroups = policyGroups;
  }

  public ArrayList<PolicyRole> getPolicyRoles( ) {
    return policyRoles;
  }

  public void setPolicyRoles( ArrayList<PolicyRole> policyRoles ) {
    this.policyRoles = policyRoles;
  }

  public ArrayList<PolicyUser> getPolicyUsers( ) {
    return policyUsers;
  }

  public void setPolicyUsers( ArrayList<PolicyUser> policyUsers ) {
    this.policyUsers = policyUsers;
  }
}
