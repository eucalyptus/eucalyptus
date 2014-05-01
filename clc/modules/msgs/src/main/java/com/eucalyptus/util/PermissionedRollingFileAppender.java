/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.apache.log4j.RollingFileAppender;

/**
 *
 */
public class PermissionedRollingFileAppender extends RollingFileAppender {

  /**
   *
   */
  private String permissions;

  /**
   * Get the permission string or null if not set.
   *
   * @return The permission string
   * @see PosixFilePermissions#fromString(String)
   */
  public String getPermissions( ) {
    return permissions;
  }

  /**
   * Set the posix file permissions for the log files.
   *
   * <p>The permissions are 9 characters with rwx for owner, group and other.</p>
   *
   * @param permissions The permissions to use (null for default permissions)
   * @see PosixFilePermissions#fromString(String)
   */
  public void setPermissions( final String permissions ) {
    this.permissions = permissions;
  }

  @Override
  public synchronized void setFile(
      final String fileName,
      final boolean append,
      final boolean bufferedIO,
      final int bufferSize ) throws IOException {
    super.setFile( fileName, append, bufferedIO, bufferSize );
    final File file = new File(fileName);
    final Path path = file.toPath( );
    if ( permissions != null && !permissions.isEmpty( ) ) try {
      Files.setPosixFilePermissions( path, PosixFilePermissions.fromString( permissions ) );
    } catch ( IllegalArgumentException e ) {
      // fallback to secure default
      Files.setPosixFilePermissions( path, PosixFilePermissions.fromString( "rw-------" ) );
    }
  }
}
