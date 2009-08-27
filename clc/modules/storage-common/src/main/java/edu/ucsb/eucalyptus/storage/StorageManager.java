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
import com.eucalyptus.ws.MappingHttpResponse;

import edu.ucsb.eucalyptus.storage.fs.FileIO;

import java.io.IOException;
import java.util.List;

import org.jboss.netty.channel.Channel;

public interface StorageManager {

    public void checkPreconditions() throws EucalyptusCloudException;

    public boolean bucketExists(String bucket);
    
    public void createBucket(String bucket) throws IOException;

    public long getSize(String bucket, String object);

    public void deleteBucket(String bucket) throws IOException;

    public void createObject(String bucket, String object) throws IOException;

    public void putObject(String bucket, String object, byte[] base64Data, boolean append) throws IOException;

    public FileIO prepareForRead(String bucket, String object) throws Exception;

    public FileIO prepareForWrite(String bucket, String object) throws Exception;

    public int readObject(String bucket, String object, byte[] bytes, long offset) throws IOException;

    public int readObject(String objectPath, byte[] bytes, long offset) throws IOException;

    public void deleteObject(String bucket, String object) throws IOException;

    public void deleteAbsoluteObject(String object) throws IOException;

    public void copyObject(String sourceBucket, String sourceObject, String destinationBucket, String destinationObject) throws IOException;

    public void renameObject(String bucket, String oldName, String newName) throws IOException;

    public String getObjectPath(String bucket, String object);

    public long getObjectSize(String bucket, String object);

	public void sendObject(Channel channel, MappingHttpResponse httpResponse, String bucketName, String objectName, 
			long size, String etag, String lastModified, String contentType, String contentDisposition, Boolean isCompressed);

	public void sendObject(Channel channel, MappingHttpResponse httpResponse, String bucketName, String objectName, 
			long start, long end, long size, String etag, String lastModified, String contentType, String contentDisposition, Boolean isCompressed);

	public void sendHeaders(Channel channel, MappingHttpResponse httpResponse, Long size, String etag,
			String lastModified, String contentType, String contentDisposition);
	
    public void setRootDirectory(String rootDirectory);

    public void deleteSnapshot(String bucket, String snapshotId, String vgName, String lvName, List<String> snapshotSet, boolean removeVg) throws EucalyptusCloudException;

    public String createVolume(String bucket, List<String> snapshotSet, List<String> vgNames, List<String> lvNames, String snapshotId, String snapshotVgName, String snapshotLvName) throws EucalyptusCloudException;
}
