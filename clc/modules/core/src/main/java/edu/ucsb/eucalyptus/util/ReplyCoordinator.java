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
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.util;

import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class ReplyCoordinator {

  private static Logger LOG = Logger.getLogger( ReplyCoordinator.class );
  private static int MAP_CAPACITY = 64;
  private static int MAP_NUM_CONCURRENT = MAP_CAPACITY / 2;
  private static float MAP_BIN_AVG_THRESHOLD = 1.0f;
  private static long MAP_GET_WAIT_MS = 10;
  private static long MAP_SUBMIT_SLEEP_MS = MAP_GET_WAIT_MS;
  private long MAP_TIMEOUT_MS = 15000;
  private ConcurrentHashMap<String, BaseMessage> replyMap;
  private ConcurrentHashMap<String,String> waitList;

  public ReplyCoordinator()
  {
    this.MAP_TIMEOUT_MS = 15000;
    this.replyMap = new ConcurrentHashMap<String, BaseMessage>( MAP_CAPACITY, MAP_BIN_AVG_THRESHOLD, MAP_NUM_CONCURRENT );
    this.waitList = new ConcurrentHashMap<String, String>( MAP_CAPACITY, MAP_BIN_AVG_THRESHOLD, MAP_NUM_CONCURRENT );
  }
  public ReplyCoordinator( long user_timeout )
  {
    this();
    this.MAP_TIMEOUT_MS = user_timeout;
  }


  public void putMessage( BaseMessage msg )
  {
    long startTime = System.currentTimeMillis();
    String msgCorId = msg.getCorrelationId();
    //:: what if the requesting thread hasnt entered the wait list yet? :://
    //:: - try to wait for the requesting thread to join the list :://
    //:: -> if joins, place the reply  :://
    //:: -> fail gracefully after a predefined timeout :://
    while( !this.waitList.containsKey( msgCorId ) && (System.currentTimeMillis()-startTime) < MAP_TIMEOUT_MS )
      try
      {
        Thread.sleep( MAP_SUBMIT_SLEEP_MS );
      }
      catch ( InterruptedException e ){}

    if( this.waitList.containsKey( msgCorId ) )
    {
      String corId = this.waitList.get(msgCorId);
      synchronized(corId)
      {
        this.replyMap.put( corId, msg );
        corId.notifyAll();
      }
    }
    else
    {
      LOG.error( "TIMEOUT: A message was returned by the system but the requesting client cannot be found, most likely timed out. ");
      LOG.error( "TIMEOUT: Waited for msec=" + ( System.currentTimeMillis() - startTime  ) );
      LOG.error( "TIMEOUT: Message:" + msg  );
    }
  }

  public BaseMessage getMessage( String corId )
  {
    long startTime = System.currentTimeMillis();
    this.waitList.put( corId, corId );
    BaseMessage reply = null;
    synchronized ( corId )
    {
      while ( !this.replyMap.containsKey( corId ) && (System.currentTimeMillis() - startTime ) < MAP_TIMEOUT_MS )
        try
        {
          corId.wait( MAP_GET_WAIT_MS );
        }
        catch ( InterruptedException e ) {}
      this.waitList.remove( corId );
      if( this.replyMap.containsKey( corId ) )
        reply = this.replyMap.remove( corId );
      else
      {
        LOG.error( "TIMEOUT: Requesting client has waited for msec=" + (System.currentTimeMillis()-startTime) );
        LOG.error( "TIMEOUT: Returning a message to indicate that the system hasn't finished processing yet" );
        LOG.error( "TIMEOUT: correlationId:" +  corId );
        return new EucalyptusErrorMessageType("Looks like you are going to timeout, but we aren't done processing your request yet. Might be a slow network -- or a bug :(", "Raise the timeout value used by your client software.");
      }
    }
    return reply;
  }
}
