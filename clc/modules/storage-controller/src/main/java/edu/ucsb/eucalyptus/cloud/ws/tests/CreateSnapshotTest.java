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

package edu.ucsb.eucalyptus.cloud.ws.tests;

import edu.ucsb.eucalyptus.cloud.ws.BlockStorage;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotType;
import com.eucalyptus.util.WalrusProperties;
import junit.framework.TestCase;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;

import com.eucalyptus.auth.Hashes;

import java.util.Date;

public class CreateSnapshotTest extends TestCase {

    static BlockStorage blockStorage;

    public void testCreateSnapshot() throws Throwable {

        String userId = "admin";

        String volumeId = "vol-Xj-6F2zFUTOAYQxx";
        String snapshotId = "snap-" + Hashes.getRandom(10);

        CreateStorageSnapshotType createSnapshotRequest = new CreateStorageSnapshotType();

        createSnapshotRequest.setUserId(userId);
        createSnapshotRequest.setVolumeId(volumeId);
        createSnapshotRequest.setSnapshotId(snapshotId);
        CreateStorageSnapshotResponseType createSnapshotResponse = blockStorage.CreateStorageSnapshot(createSnapshotRequest);
        System.out.println(createSnapshotResponse);

        while(true);
    }

    public void testTransferSnapshot() throws Throwable {
        String volumeId = "vol-yCqCbrweuVviYQxx";
        String snapshotId = "snap-zVl2kZJmjhxnEg..";
        String dupSnapshotId = "snap-zVl2kZJmjhxnEg...SrZ5iA..";

        blockStorage.transferSnapshot(volumeId, snapshotId, dupSnapshotId, true);
        while(true);
    }

    public void testSendDummy() throws Throwable {
        HttpClient httpClient = new HttpClient();
        String addr = System.getProperty(WalrusProperties.URL_PROPERTY) + "/meh/ttt.wsl?gg=vol&hh=snap";

        HttpMethodBase method = new PutMethod(addr);
        method.setRequestHeader("Authorization", "Euca");
        method.setRequestHeader("Date", (new Date()).toString());
        method.setRequestHeader("Expect", "100-continue");

        httpClient.executeMethod(method);
        String responseString = method.getResponseBodyAsString();
        System.out.println(responseString);
        method.releaseConnection();
    }

    public void testGetSnapshotInfo() throws Throwable {
        HttpClient httpClient = new HttpClient();
        String addr = System.getProperty(WalrusProperties.URL_PROPERTY) + "/snapset-FuXLn1MUHJ66BkK0/snap-zVl2kZJmjhxnEg..";

        HttpMethodBase method = new GetMethod(addr);
        method.setRequestHeader("Authorization", "Euca");
        method.setRequestHeader("Date", (new Date()).toString());
        method.setRequestHeader("Expect", "100-continue");
        method.setRequestHeader("EucaOperation", "GetSnapshotInfo");
        httpClient.executeMethod(method);
        String responseString = method.getResponseBodyAsString();
        System.out.println(responseString);
        method.releaseConnection();         
    }

    public void setUp() {
        blockStorage = new BlockStorage();
        BlockStorage.initialize();
    }

}
