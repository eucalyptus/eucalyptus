/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.upgrade;

import java.io.File;
import org.apache.log4j.Logger;

/**
 * Any Class implementing this interface will be registered as a candidate for usage during the upgrade procedure.
 */
public interface UpgradeScript {
  /**
   * Indicates whether this upgrade script handles upgrades from version <tt>from</tt> to version <tt>to</tt>.
   * 
   * @param from - Version string of installation <b>from</b> which the upgrade is occuring.
   * @param to - Version string of installation <b>to</b> which the upgrade is occuring.
   * @see <tt>$EUCALYPTUS/etc/eucalyptus/eucalyptus-version</tt>
   * @return true - if this script should be executed when upgrading
   */
  public Boolean accepts( String from, String to );
  
  /**
   * Perform the upgrade from <tt>oldEucaHome</tt> to <tt>newEucaHome</tt>
   * 
   * @param oldEucaHome
   * @param newEucaHome
   */
  public void upgrade( File oldEucaHome, File newEucaHome );
  
  public int getPriority();

  public void setLogger(Logger log);
}
