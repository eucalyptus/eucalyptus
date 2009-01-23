/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.util;

import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class ReplyCoordinator {

  private static Logger LOG = Logger.getLogger( ReplyCoordinator.class );
  private static int MAP_CAPACITY = 64;
  private static int MAP_NUM_CONCURRENT = MAP_CAPACITY / 2;
  private static float MAP_BIN_AVG_THRESHOLD = 1.0f;
  private static long MAP_GET_WAIT_MS = 10;
  private static long MAP_SUBMIT_SLEEP_MS = MAP_GET_WAIT_MS;
  private static long MAP_TIMEOUT_MS = 3600000;
  private ConcurrentHashMap<String, EucalyptusMessage> replyMap;
  private ConcurrentHashMap<String,String> waitList;

  public ReplyCoordinator()
  {
    this.replyMap = new ConcurrentHashMap<String, EucalyptusMessage>( MAP_CAPACITY, MAP_BIN_AVG_THRESHOLD, MAP_NUM_CONCURRENT );
    this.waitList = new ConcurrentHashMap<String, String>( MAP_CAPACITY, MAP_BIN_AVG_THRESHOLD, MAP_NUM_CONCURRENT );
  }

  public void putMessage( EucalyptusMessage msg )
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

  public EucalyptusMessage getMessage( String corId )
  {
    long startTime = System.currentTimeMillis();
    this.waitList.put( corId, corId );
    EucalyptusMessage reply = null;
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
//        LOG.error( "TIMEOUT: Requesting client has waited for msec=" + (System.currentTimeMillis()-startTime) );
//        LOG.error( "TIMEOUT: Returning a message to indicate that the system hasn't finished processing yet" );
//        LOG.error( "TIMEOUT: correlationId:" +  corId );
        return new EucalyptusErrorMessageType("Looks like you are going to timeout, but we aren't done processing your request yet. Might be a slow network -- or a bug :(", "Raise the timeout value used by your client software.");
      }
    }
    return reply;
  }
}
