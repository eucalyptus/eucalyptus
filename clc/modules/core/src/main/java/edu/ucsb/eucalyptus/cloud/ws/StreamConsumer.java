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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.ws;

import com.eucalyptus.util.WalrusProperties;

import java.io.*;

public class StreamConsumer extends Thread {
    private InputStream is;
    private File file;
    private String returnValue;

    public StreamConsumer(InputStream is) {
        this.is = is;
        returnValue = "";
    }

    public StreamConsumer(InputStream is, File file) {
        this(is);
        this.file = file;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public void run() {
        try {
            BufferedInputStream inStream = new BufferedInputStream(is);
            BufferedOutputStream outStream = null;
            if (file != null) {
                outStream = new BufferedOutputStream(new FileOutputStream(file));
            }
            byte[] bytes = new byte[WalrusProperties.IO_CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = inStream.read(bytes)) > 0) {
                returnValue += new String(bytes, 0, bytesRead);
                if (outStream != null) {
                    outStream.write(bytes, 0, bytesRead);
                }
            }
            if (outStream != null)
                outStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
