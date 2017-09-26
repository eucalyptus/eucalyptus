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
package com.eucalyptus.objectstorage.exceptions.s3;

public class InvalidAddressingHeaderException extends S3Exception {

  public InvalidAddressingHeaderException( ) {
    super( S3ErrorCodeStrings.InvalidAddressingHeader, "You must specify the Anonymous role.", null );
  }

  public InvalidAddressingHeaderException( String resource, String message ) {
    this( );
    this.setResource( resource );
    this.setMessage( message );
  }
}
