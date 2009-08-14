package com.eucalyptus.util;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

public class EucaLayout extends PatternLayout {
  public static int LINE_BYTES = 100;
  static {
    try {
      LINE_BYTES = Integer.parseInt( System.getenv( "COLUMNS" ) );
    } catch ( NumberFormatException e ) {}
  }
  public static String PATTERN = "%d{HH:mm:ss} %5p %-20.20c{1} | %-100.100m%n";
  private String CONTINUATION = "%m%n";
  private PatternLayout continuation = null;
  

  public EucaLayout( ) {
    super( PATTERN );
    
  }

  public EucaLayout( String pattern ) {
    super( PATTERN );
    
  }

  @Override
  public String format( LoggingEvent event ) {
    String[] messages = event.getRenderedMessage( ).split( "\n" );
    StringBuffer sb = new StringBuffer( );
    boolean con = false;
    for( int i = 0; i < messages.length; i++ ) {
//      String message= messages[i];
      String substring= messages[i];
//      while ( message.length( ) > 0 ) {
//        int rb = LINE_BYTES>message.length( )?message.length( ):LINE_BYTES;
//        String substring = message.substring( 0, rb );
//        message = message.substring( rb );
        LoggingEvent n = new LoggingEvent( event.getFQNOfLoggerClass( ), event.getLogger( ), 
                                           event.getTimeStamp( ), event.getLevel( ), 
                                           substring, event.getThreadName( ), 
                                           event.getThrowableInformation( ), null, null, null );
        sb.append( (!con)?super.format( n ):continuation.format( n ) );
        if(continuation==null) {
          continuation = new PatternLayout(sb.toString( ).split( "\\|" )[0].replaceAll( ".", " " )+"| "+CONTINUATION);
        }
        con = true;        
//      }      
    }
    return sb.toString( );
  }
}
