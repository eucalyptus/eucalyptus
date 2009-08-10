package com.eucalyptus.ws.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

import com.eucalyptus.ws.HttpException;


public class HttpUtils {

  public static String readLine( ChannelBuffer buffer, int maxLineLength ) throws HttpException {
    StringBuilder sb = new StringBuilder( 64 );
    int lineLength = 0;
    while ( true ) {
      byte nextByte = buffer.readByte( );
      if ( nextByte == HttpUtils.CR ) {
        nextByte = buffer.readByte( );
        if ( nextByte == HttpUtils.LF ) { return sb.toString( ); }
      } else if ( nextByte == HttpUtils.LF ) {
        return sb.toString( );
      } else {
        if ( lineLength >= maxLineLength ) { throw new HttpException( "HTTP input line longer than " + maxLineLength + " bytes." ); }
        lineLength++;
        sb.append( ( char ) nextByte );
      }
    }
  }

  public static final byte SP = 32;
  public static final byte HT = 9;
  public static final byte CR = 13;
  public static final byte EQUALS = 61;
  public static final byte LF = 10;
  public static final byte[] CRLF = new byte[] { CR, LF };
  public static final byte COLON = 58;
  public static final byte SEMICOLON = 59;
  public static final byte COMMA = 44;
  public static final byte DOUBLE_QUOTE = '"';
  public static final String DEFAULT_CHARSET = "UTF-8";

}
