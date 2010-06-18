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
package edu.ucsb.eucalyptus.cloud.ws;
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.util.StreamConsumer;

import org.apache.log4j.Logger;


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
        byte[] bytes = new byte[102400];
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
