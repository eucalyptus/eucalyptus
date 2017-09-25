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
package com.eucalyptus.cloudformation.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class StackResources extends EucalyptusData {

  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "member" )
  private ArrayList<StackResource> member = new ArrayList<StackResource>( );

  public StackResources( ) {
  }

  public StackResources( StackResource stackResource ) {
    member.add( stackResource );
  }

  @Override
  public String toString( ) {
    return "StackResources [member=" + member + "]";
  }

  public ArrayList<StackResource> getMember( ) {
    return member;
  }

  public void setMember( ArrayList<StackResource> member ) {
    this.member = member;
  }
}
