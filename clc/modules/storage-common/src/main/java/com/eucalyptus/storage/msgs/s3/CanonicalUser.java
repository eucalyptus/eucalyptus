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

import java.util.Objects;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class CanonicalUser extends EucalyptusData {

  private String ID;
  private String DisplayName;

  public CanonicalUser( ) {
  }

  public CanonicalUser( String ID, String DisplayName ) {
    this.ID = ID;
    this.DisplayName = DisplayName;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final CanonicalUser that = (CanonicalUser) o;
    return Objects.equals( getID( ), that.getID( ) ) &&
        Objects.equals( getDisplayName( ), that.getDisplayName( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getID( ), getDisplayName( ) );
  }

  public String getID( ) {
    return ID;
  }

  public void setID( String ID ) {
    this.ID = ID;
  }

  public String getDisplayName( ) {
    return DisplayName;
  }

  public void setDisplayName( String DisplayName ) {
    this.DisplayName = DisplayName;
  }
}
