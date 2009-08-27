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
 *
 * Parts of this code are licensed under the BSD license and carry the following copyright,
 * Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)
 * @author Brian Wellington &lt;bwelling@xbill.org&gt;         
 *
 */

package com.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class UDPHandler extends ConnectionHandler {
    private static Logger LOG = Logger.getLogger( UDPHandler.class );

    DatagramSocket socket;
    public UDPHandler(DatagramSocket s) {
        this.socket = s;
    }

    public void run() {
        try {
            final short udpLength = 512;
            byte [] in = new byte[udpLength];
            DatagramPacket indp = new DatagramPacket(in, in.length);
            DatagramPacket outdp = null;
            while (true) {
                indp.setLength(in.length);
                try {
                    socket.receive(indp);
                }
                catch (InterruptedIOException e) {
                    continue;
                }
                Message query;
                byte [] response = null;
                try {
                    query = new Message(in);
                    response = generateReply(query, in,
                            indp.getLength(),
                            null);
                    if (response == null)
                        continue;
                }
                catch (IOException e) {
                    response = formerrMessage(in);
                }
                if (outdp == null)
                    outdp = new DatagramPacket(response,
                            response.length,
                            indp.getAddress(),
                            indp.getPort());
                else {
                    outdp.setData(response);
                    outdp.setLength(response.length);
                    outdp.setAddress(indp.getAddress());
                    outdp.setPort(indp.getPort());
                }
                socket.send(outdp);
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }

    }
}
