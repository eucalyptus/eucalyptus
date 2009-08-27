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
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.cloud.ws;

import com.eucalyptus.util.DNSProperties;
import org.apache.log4j.Logger;
import org.xbill.DNS.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class TCPHandler extends ConnectionHandler {
    private static Logger LOG = Logger.getLogger( TCPHandler.class );
    Socket socket;
    public TCPHandler(Socket s) {
        this.socket = s;
    }

    public void run() {
        try {
            int inputLength;
            DataInputStream inStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            inputLength = inStream.readUnsignedShort();
            if(inputLength > DNSProperties.MAX_MESSAGE_SIZE) {
                LOG.error("Maximum message size exceeded. Ignoring request.");
            }
            byte[] inBytes = new byte[inputLength];
            inStream.readFully(inBytes);
            Message query;
            byte [] response = null;
            try {
                query = new Message(inBytes);
                response = generateReply(query, inBytes, inBytes.length, socket);
                if (response == null)
                    return;
            }
            catch (IOException exception) {
                LOG.error(exception);
            }
            outStream.writeShort(response.length);
            outStream.write(response);
        } catch(IOException ex) {
            LOG.error(ex);
        }
    }


}
