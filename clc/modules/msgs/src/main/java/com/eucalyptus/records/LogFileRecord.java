package com.eucalyptus.records;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import com.eucalyptus.system.LogLevels;

@Entity
@PersistenceContext( name = "eucalyptus_records" )
@Table( name = "records_logs" )
@DiscriminatorValue(value = "base")
public class LogFileRecord extends BaseRecord {
  private static Logger            LOG   = Logger.getLogger( EventRecord.class );
  
  private String                   caller;
  
  LogFileRecord( final String component, final String eventUserId, final String eventCorrelationId, final String eventId, final String other, int distance ) {
    super( EventType.BOGUS, EventClass.ORPHAN, component, Thread.currentThread( ).getStackTrace( )[distance].getClassName( ), eventUserId, eventCorrelationId, other );
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[distance];
    if ( LogLevels.DEBUG ) {
      if ( ste != null && ste.getFileName( ) != null ) {
        this.caller = String.format( "%s.%s.%s", ste.getFileName( ).replaceAll( "\\.\\w*\\b", "" ), ste.getMethodName( ), ste.getLineNumber( ) );
      } else {
        this.caller = "unknown";
      }
    }
  }
  

  public String getCaller( ) {
    return this.caller;
  }

  @Override
  public String toString() {
    return this.caller != null ? super.toString( ) + ":" + this.caller : super.toString( );
  }
  
}