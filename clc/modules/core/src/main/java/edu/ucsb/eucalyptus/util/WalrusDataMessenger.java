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
package edu.ucsb.eucalyptus.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;

// A concurrent hash map that holds a map of queues, which can be used for passing data
// Currently, the queues are a LinkedBlockingQueue and producers/consumers do not timeout.

public class WalrusDataMessenger {
    private static Logger LOG = Logger.getLogger( WalrusDataMessenger.class );
    private static final int DATA_QUEUE_SIZE = 3;

    private ConcurrentHashMap<String, ConcurrentHashMap<String,LinkedBlockingQueue<WalrusDataMessage>>> queueMap;
    private ConcurrentHashMap<String, WalrusMonitor> monitorMap;
    private ConcurrentHashMap<String, WalrusSemaphore> semaphoreMap;

    public WalrusDataMessenger() {
        queueMap = new ConcurrentHashMap<String, ConcurrentHashMap<String,LinkedBlockingQueue<WalrusDataMessage>>>();
        monitorMap = new ConcurrentHashMap<String, WalrusMonitor>();
        semaphoreMap = new ConcurrentHashMap<String, WalrusSemaphore>();
    }

    public LinkedBlockingQueue<WalrusDataMessage> getQueue(String key1, String key2) {
        ConcurrentHashMap<String,LinkedBlockingQueue<WalrusDataMessage>> queues = queueMap.putIfAbsent(key1, new ConcurrentHashMap<String, LinkedBlockingQueue<WalrusDataMessage>>());
        if (queues == null) {
            queues = queueMap.get(key1);
        }
        LinkedBlockingQueue<WalrusDataMessage> queue = queues.putIfAbsent(key2, new LinkedBlockingQueue<WalrusDataMessage>(DATA_QUEUE_SIZE));
        if (queue == null) {
            queue = queues.get(key2);
        }
        return queue;
    }

    public synchronized LinkedBlockingQueue<WalrusDataMessage> interruptAllAndGetQueue(String key1, String key2) {
        ConcurrentHashMap<String,LinkedBlockingQueue<WalrusDataMessage>> queues = queueMap.get(key1);
        if(queues != null) {
            for (LinkedBlockingQueue<WalrusDataMessage> queue: queues.values()) {
		try {
                    queue.put(WalrusDataMessage.InterruptTransaction());
		} catch(InterruptedException ex) {
		    LOG.warn(ex, ex);
		    return null;
		}
	
            }
        }
        return getQueue(key1, key2);
    }

    public void removeQueue(String key1, String key2) {
        if(queueMap.containsKey(key1)) {
            ConcurrentHashMap<String, LinkedBlockingQueue<WalrusDataMessage>> queues = queueMap.get(key1);
            if(queues.containsKey(key2)) {
                queues.remove(key2);
            }
        }
    }

    public WalrusMonitor getMonitor(String key) {
        WalrusMonitor monitor = monitorMap.putIfAbsent(key, new WalrusMonitor());
        if (monitor == null) {
            monitor = monitorMap.get(key);
        }
        return monitor;
    }

    public void removeMonitor(String key) {
        if(monitorMap.containsKey(key)) {
            monitorMap.remove(key);
        }
    }

    public WalrusSemaphore getSemaphore(String key) {
        WalrusSemaphore semaphore = semaphoreMap.putIfAbsent(key, new WalrusSemaphore());
        if (semaphore == null) {
            semaphore = semaphoreMap.get(key);
        }
        return semaphore;
    }

    public void removeSemaphore(String key) {
        if(semaphoreMap.containsKey(key)) {
            semaphoreMap.remove(key);
        }
    }
}
