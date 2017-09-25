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
package com.eucalyptus.objectstorage.exceptions;

/**
 * The current state of the resource is not compatible with the requested
 * update. Either a state-machine does not allow the transition or some state
 * of the entity prohibits an update
 */
public class IllegalResourceStateException extends ObjectStorageInternalException {

  private String expected;
  private String found;

  public IllegalResourceStateException( ) {
  }

  public IllegalResourceStateException( String msg ) {
    super( msg );
  }

  public IllegalResourceStateException( String msg, Throwable cause ) {
    super( msg, cause );
  }

  public IllegalResourceStateException( String msg, Throwable cause, String expectedState, String foundState ) {
    super( msg, cause );
    expected = expectedState;
    found = foundState;
  }

  public String getExpected( ) {
    return expected;
  }

  public void setExpected( String expected ) {
    this.expected = expected;
  }

  public String getFound( ) {
    return found;
  }

  public void setFound( String found ) {
    this.found = found;
  }
}
