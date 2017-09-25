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

public class Group extends EucalyptusData {

  private String uri;

  public Group( ) {
  }

  public Group( String uri ) {
    this.uri = uri;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final Group group = (Group) o;
    return Objects.equals( getUri( ), group.getUri( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getUri( ) );
  }

  public String getUri( ) {
    return uri;
  }

  public void setUri( String uri ) {
    this.uri = uri;
  }
}
