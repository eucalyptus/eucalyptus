/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package edu.ucsb.eucalyptus.admin.client.extensions.store;


public interface ImageState {

    /**
     * Return the URI for the image which this state refers to.
     *
     * @return the URI for the image this state refers to.
     */
    String getImageUri();

    /**
     * Return the last error message for this image, or null.
     *
     * @return the last error related to requested changes in this image,
     *         or null if no error is known.
     */
    String getErrorMessage();

    /**
     * Return the current status of the image.  Some statuses are transient
     * and in some cases may have their progress tracked via the
     * getProgressPercentage() method.
     *
     * @return constant from the ImageState.Status enum.
     */
    Status getStatus();

    /**
     * Return progress percentage.
     *
     * @return the completion percentage for the on going task, or null
     *         if there's no task in progress.
     */
    Integer getProgressPercentage();

    /**
     * Return the URI to perform the given action.
     *
     * @return URI to perform the requested action.
     */
    String getActionUri(Action action);
    
    /**
     * Return true if the action is available in this state.
     *
     * @return true if this state has the given action URI.
     */
    boolean hasAction(Action action);

    /**
     * Return whether this image is an upgrade to an already installed
     * image.
     *
     * @return true if an older version of this image is installed.
     */
    boolean isUpgrade();

    enum Status {
        UNINSTALLED(false),
        DOWNLOADING(true),
        INSTALLING(true),
        INSTALLED(false),
        UNKNOWN(false);

        private final boolean isTransient;

        Status(boolean isTransient) {
            this.isTransient = isTransient;
        }

        boolean isTransient() {
            return this.isTransient;
        }
    }

    enum Action {
        INSTALL,
        CANCEL,
        CLEAR_ERROR;
    }
}
