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
 * A metadata operation could not be completed. Either the db failed
 * or something prevented the update from committing. This is not a conflict or
 * state exception
 */
public class MetadataOperationFailureException extends ObjectStorageInternalException {

  public MetadataOperationFailureException( ) {
  }

  public MetadataOperationFailureException( String msg ) {
    super( msg );
  }

  public MetadataOperationFailureException( Throwable cause ) {
    super( cause );
  }

  public MetadataOperationFailureException( String msg, Throwable cause ) {
    super( msg, cause );
  }
}
