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

import edu.ucsb.eucalyptus.cloud.ws.WalrusControl;
import edu.ucsb.eucalyptus.msgs.*;
import junit.framework.TestCase;

import java.util.ArrayList;

import com.eucalyptus.auth.Hashes;

public class ObjectTest extends TestCase {

	static WalrusControl bukkit;
	public void testObject() throws Throwable {

		String bucketName = "halo" + Hashes.getRandom(6);
		String objectName = "key" + Hashes.getRandom(6);
		String userId = "admin";

		CreateBucketType createBucketRequest = new CreateBucketType(bucketName);
		createBucketRequest.setBucket(bucketName);
		createBucketRequest.setUserId(userId);
        createBucketRequest.setEffectiveUserId("eucalyptus");
		AccessControlListType acl = new AccessControlListType();
		createBucketRequest.setAccessControlList(acl);
		CreateBucketResponseType reply = bukkit.CreateBucket(createBucketRequest);
		System.out.println(reply);

		PutObjectInlineType putObjectRequest = new PutObjectInlineType();
		putObjectRequest.setBucket(bucketName);
		putObjectRequest.setKey(objectName);
        String data = "hi here is some data";
		putObjectRequest.setContentLength(String.valueOf(data.length()));
		putObjectRequest.setBase64Data(data);
		putObjectRequest.setUserId(userId);
        ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
        MetaDataEntry metaDataEntry = new MetaDataEntry();
        metaDataEntry.setName("mammalType");
        metaDataEntry.setValue("walrus");
        metaData.add(metaDataEntry);
        putObjectRequest.setMetaData(metaData);
        PutObjectInlineResponseType putObjectReply = bukkit.PutObjectInline(putObjectRequest);
		System.out.println(putObjectReply);

        ListBucketType listBucketRequest = new ListBucketType();
        listBucketRequest.setBucket(bucketName);
        listBucketRequest.setUserId(userId);
        ListBucketResponseType listBucketReply = bukkit.ListBucket(listBucketRequest);
        System.out.println(listBucketReply);        

        GetObjectAccessControlPolicyType acpRequest = new GetObjectAccessControlPolicyType();
		acpRequest.setBucket(bucketName);
        acpRequest.setKey(objectName);
        acpRequest.setUserId(userId);
		GetObjectAccessControlPolicyResponseType acpResponse = bukkit.GetObjectAccessControlPolicy(acpRequest);
		System.out.println(acpResponse);

        GetObjectType getObjectRequest = new GetObjectType();
        getObjectRequest.setUserId(userId);
        getObjectRequest.setBucket(bucketName);
        getObjectRequest.setKey(objectName);
        getObjectRequest.setGetData(true);
        getObjectRequest.setGetMetaData(true);
        getObjectRequest.setInlineData(true);
        GetObjectResponseType getObjectReply = bukkit.GetObject(getObjectRequest);
        System.out.println(getObjectReply);

        DeleteObjectType deleteObjectRequest = new DeleteObjectType();
		deleteObjectRequest.setBucket(bucketName);
		deleteObjectRequest.setKey(objectName);
		deleteObjectRequest.setUserId(userId);
		DeleteObjectResponseType deleteObjectReply = bukkit.DeleteObject(deleteObjectRequest);
		System.out.println(deleteObjectReply);

		DeleteBucketType deleteBucketRequest = new DeleteBucketType();
		deleteBucketRequest.setUserId(userId);
		deleteBucketRequest.setBucket(bucketName);
		DeleteBucketResponseType deleteResponse = bukkit.DeleteBucket(deleteBucketRequest);
		System.out.println(deleteResponse);
	}

    public void setUp() {
        bukkit = new WalrusControl();
   }        
}
