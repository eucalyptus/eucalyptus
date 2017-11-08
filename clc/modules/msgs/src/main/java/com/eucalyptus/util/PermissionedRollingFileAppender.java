/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
