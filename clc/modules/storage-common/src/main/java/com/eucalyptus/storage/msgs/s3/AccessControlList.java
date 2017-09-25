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
package com.eucalyptus.storage.msgs.s3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AccessControlList extends EucalyptusData {

  private ArrayList<Grant> grants = new ArrayList<Grant>( );

  @SuppressWarnings( "unchecked" )
  @Override
  public boolean equals( Object o ) {
    if ( o == null || !( o instanceof AccessControlList ) ) {
      return false;
    }


    AccessControlList other = (AccessControlList) o;
    //Do an unordered comparison, sort clones of each first
    List<Grant> myGrants = (List<Grant>) this.grants.clone( );
    Collections.sort( myGrants );
    List<Grant> otherGrants = (List<Grant>) other.grants.clone( );
    Collections.sort( otherGrants );
    return myGrants.equals( otherGrants );
  }

  @SuppressWarnings( "unchecked" )
  @Override
  public int hashCode( ) {
    List<Grant> myGrants = (List<Grant>) this.grants.clone( );
    Collections.sort( myGrants );
    return Objects.hash( myGrants );
  }

  public ArrayList<Grant> getGrants( ) {
    return grants;
  }

  public void setGrants( ArrayList<Grant> grants ) {
    this.grants = grants;
  }
}
