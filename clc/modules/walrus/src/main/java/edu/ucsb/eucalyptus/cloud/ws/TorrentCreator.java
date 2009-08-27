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
package edu.ucsb.eucalyptus.cloud.ws;
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

import com.eucalyptus.util.WalrusProperties;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Hashes;

import java.io.*;

public class TorrentCreator {
    private static Logger LOG = Logger.getLogger( TorrentCreator.class );
    private String torrentFilePath;
    private String filePath;
    private String trackerUrl;
    private String objectKey;
    private String objectName;
    private final String NAME_TAG = "name";

    public TorrentCreator(String filePath, String objectKey, String objectName, String torrentFilePath, String trackerUrl) {
        this.filePath = filePath;
        this.torrentFilePath = torrentFilePath;
        this.trackerUrl = trackerUrl;
        this.objectKey = objectKey;
        this.objectName = objectName;
    }

    private void makeTorrent() {
        new File(WalrusProperties.TRACKER_DIR).mkdirs();
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(new String[]{WalrusProperties.TORRENT_CREATOR_BINARY, filePath, trackerUrl, "--target", torrentFilePath});
            StreamConsumer error = new StreamConsumer(proc.getErrorStream());
            StreamConsumer output = new StreamConsumer(proc.getInputStream());
            error.start();
            output.start();
            int pid = proc.waitFor();
            output.join();
            String errValue = error.getReturnValue();
            String outValue = output.getReturnValue();
            if(errValue.length() > 0)
                LOG.warn(errValue);
            if(outValue.length() > 0)
                LOG.warn(outValue);
        } catch (Throwable t) {
            LOG.error(t);
        }
    }

    private void changeName() throws Exception {
        File inFile = new File(torrentFilePath);
        File outFile = new File(torrentFilePath + Hashes.getRandom(6));
        BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(inFile));
        BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(outFile));

        int bytesRead;
        int totalBytesRead = 0;
        byte[] bytes = new byte[1024];
        String inString = "";

        while((bytesRead = inStream.read(bytes)) > 0) {
            inString += new String(bytes, 0, bytesRead);
            totalBytesRead += bytesRead;
        }
        inStream.close();
        int len = inString.length();
        int idx = inString.indexOf(NAME_TAG);
        int lastidx = inString.indexOf(objectName) + objectName.length();

        outStream.write(bytes, 0, idx + NAME_TAG.length());
        outStream.write(new String(objectKey.length() + ":" + objectKey).getBytes());
        outStream.write(bytes, lastidx, totalBytesRead - lastidx);

        outStream.close();
        outFile.renameTo(inFile);
    }

    public void create() throws Exception {
        makeTorrent();
        changeName();
    }
}
