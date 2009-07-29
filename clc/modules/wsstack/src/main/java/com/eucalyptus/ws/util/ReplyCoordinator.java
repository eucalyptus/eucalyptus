package com.eucalyptus.ws.util;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class ReplyCoordinator {

  private static Logger                                LOG                   = Logger.getLogger( ReplyCoordinator.class );
  private static int                                   MAP_CAPACITY          = 64;
  private static int                                   MAP_NUM_CONCURRENT    = MAP_CAPACITY / 2;
  private static float                                 MAP_BIN_AVG_THRESHOLD = 1.0f;
  private static long                                  MAP_GET_WAIT_MS       = 10;
  private static long                                  MAP_SUBMIT_SLEEP_MS   = MAP_GET_WAIT_MS;
  private long                                         MAP_TIMEOUT_MS        = 15000;
  private ConcurrentHashMap<String, EucalyptusMessage> replyMap;
  private ConcurrentHashMap<String, String>            waitList;

  public ReplyCoordinator( ) {
    this.MAP_TIMEOUT_MS = 15000;
    this.replyMap = new ConcurrentHashMap<String, EucalyptusMessage>( MAP_CAPACITY, MAP_BIN_AVG_THRESHOLD, MAP_NUM_CONCURRENT );
    this.waitList = new ConcurrentHashMap<String, String>( MAP_CAPACITY, MAP_BIN_AVG_THRESHOLD, MAP_NUM_CONCURRENT );
  }

  public ReplyCoordinator( long user_timeout ) {
    this( );
    this.MAP_TIMEOUT_MS = user_timeout;
  }

  public void putMessage( EucalyptusMessage msg ) {
    long startTime = System.currentTimeMillis( );
    String msgCorId = msg.getCorrelationId( );
    while ( !this.waitList.containsKey( msgCorId ) && ( System.currentTimeMillis( ) - startTime ) < MAP_TIMEOUT_MS ) {
      try {
        Thread.sleep( MAP_SUBMIT_SLEEP_MS );
      } catch ( InterruptedException e ) {
      }
    }

    if ( this.waitList.containsKey( msgCorId ) ) {
      String corId = this.waitList.get( msgCorId );
      synchronized ( corId ) {
        this.replyMap.put( corId, msg );
        corId.notifyAll( );
      }
    } else {
      LOG.error( "TIMEOUT: A message was returned by the system but the requesting client cannot be found, most likely timed out. " );
      LOG.error( "TIMEOUT: Waited for msec=" + ( System.currentTimeMillis( ) - startTime ) );
      LOG.error( "TIMEOUT: Message:" + msg );
    }
  }

  public EucalyptusMessage getMessage( String corId ) {
    long startTime = System.currentTimeMillis( );
    this.waitList.put( corId, corId );
    EucalyptusMessage reply = null;
    synchronized ( corId ) {
      while ( !this.replyMap.containsKey( corId ) && ( System.currentTimeMillis( ) - startTime ) < MAP_TIMEOUT_MS )
        try {
          corId.wait( MAP_GET_WAIT_MS );
        } catch ( InterruptedException e ) {
        }
      this.waitList.remove( corId );
      if ( this.replyMap.containsKey( corId ) ) reply = this.replyMap.remove( corId );
      else {
        LOG.error( "TIMEOUT: Requesting client has waited for msec=" + ( System.currentTimeMillis( ) - startTime ) );
        LOG.error( "TIMEOUT: Returning a message to indicate that the system hasn't finished processing yet" );
        LOG.error( "TIMEOUT: correlationId:" + corId );
        return new EucalyptusErrorMessageType( "Looks like you are going to timeout, but we aren't done processing your request yet. Might be a slow network -- or a bug :(", "Raise the timeout value used by your client software." );
      }
    }
    return reply;
  }
}
