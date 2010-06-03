package com.eucalyptus.records;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import com.eucalyptus.system.LogLevels;

@Entity
@PersistenceContext( name = "eucalyptus_records" )
@Table( name = "records_logs" )
@DiscriminatorValue( value = "base" )
public class LogFileRecord extends BaseRecord {
  private static Logger LOG = Logger.getLogger( EventRecord.class );
  @Column( name = "record_caller" )
  private String        caller;
  
  public LogFileRecord( ) {
    super( );
  }

  public LogFileRecord( EventClass eventClass, EventType type, Class creator, StackTraceElement callerStack, String userId, String correlationId, String other ) {
    super( type, eventClass, creator, callerStack, userId, correlationId, other );
    if ( LogLevels.DEBUG ) {
      if ( callerStack != null && callerStack.getFileName( ) != null ) {
        this.caller = String.format( "%s.%s.%s", callerStack.getFileName( ).replaceAll( "\\.\\w*\\b", "" ), callerStack.getMethodName( ), callerStack.getLineNumber( ) );
      } else {
        this.caller = "unknown";
      }
    }
  }
  
  public String getCaller( ) {
    return this.caller;
  }

  @Override
  public String toString( ) {
    return this.caller != null ? super.toString( ) + ":" + this.caller : super.toString( );
  }
  
}