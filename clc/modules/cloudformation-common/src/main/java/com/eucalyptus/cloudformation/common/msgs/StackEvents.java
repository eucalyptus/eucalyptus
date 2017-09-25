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

public class StackEvents extends EucalyptusData {

  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "member" )
  private ArrayList<StackEvent> member = new ArrayList<StackEvent>( );

  public StackEvents( ) {
  }

  public StackEvents( StackEvent stackEvent ) {
    member.add( stackEvent );
  }

  @Override
  public String toString( ) {
    return "StackEvents [member=" + member + "]";
  }

  public ArrayList<StackEvent> getMember( ) {
    return member;
  }

  public void setMember( ArrayList<StackEvent> member ) {
    this.member = member;
  }
}
