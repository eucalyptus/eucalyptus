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
package com.eucalyptus.system;

import java.io.File;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;

/**
 *
 */
class Directories {

  static final String PROP_EXEC_PERMISSIONS = "com.eucalyptus.system.dirExec";

  static final boolean DIR_EXEC = Boolean.parseBoolean( System.getProperty( PROP_EXEC_PERMISSIONS, "true" ) );

  static void execPermissions( String command ) throws ScriptExecutionFailedException {
    if ( DIR_EXEC ) {
      Groovyness.exec( command );
    }
  }

  static String getChildPath( String dir, String[] children ) {
    String ret = dir;
    for ( final String child : children ) {
      ret += File.separator + child;
    }
    return ret;
  }
}
