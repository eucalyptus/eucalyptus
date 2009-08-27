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
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.storage;

import com.eucalyptus.util.EucalyptusCloudException;

import java.util.List;

public interface LogicalStorageManager {
    public void initialize();

    public void configure();
    
    public void checkPreconditions() throws EucalyptusCloudException;

    public void reload();

    public void startupChecks();

    public void setStorageInterface(String storageInterface);

    public void cleanVolume(String volumeId);

    public void cleanSnapshot(String volumeId);

    public List<String> createSnapshot(String volumeId, String snapshotId) throws EucalyptusCloudException;

    public List<String> prepareForTransfer(String volumeId, String snapshotId) throws EucalyptusCloudException;

    public void createVolume(String volumeId, int size) throws EucalyptusCloudException;

    public int createVolume(String volumeId, String snapshotId) throws EucalyptusCloudException;

    public void addSnapshot(String snapshotId) throws EucalyptusCloudException;
    
    public void dupVolume(String volumeId, String dupedVolumeId) throws EucalyptusCloudException;
    
    public List<String> getStatus(List<String> volumeSet) throws EucalyptusCloudException;

    public void deleteVolume(String volumeId) throws EucalyptusCloudException;

    public void deleteSnapshot(String snapshotId) throws EucalyptusCloudException;

    public List<String> getVolume(String volumeId) throws EucalyptusCloudException;

    public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException;

    public List<String> getSnapshotValues(String snapshotId) throws EucalyptusCloudException;

    public int getSnapshotSize(String snapshotId) throws EucalyptusCloudException;    
}
