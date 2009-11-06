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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
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

    /**
     * Return the image's EMI, if available.
     *
     * @return EMI for the referenced image, or null.
     */
    String getEMI();

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
