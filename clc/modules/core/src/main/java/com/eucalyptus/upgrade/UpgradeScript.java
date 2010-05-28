package com.eucalyptus.upgrade;

import java.io.File;

/**
 * Any Class implementing this interface will be registered as a candidate for usage during the upgrade procedure.
 * 
 * @author decker
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
}