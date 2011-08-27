package com.eucalyptus.records;

import java.util.ArrayList;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Entity;

@Entity
@javax.persistence.Entity
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
    if ( Logs.isExtrrreeeme() ) {
      if ( callerStack != null && callerStack.getFileName( ) != null ) {
        this.caller = String.format( "   [%s.%s.%s]", callerStack.getFileName( ).replaceAll( "\\.\\w*\\b", "" ), callerStack.getMethodName( ),
                                     callerStack.getLineNumber( ) );
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
    if ( Logs.isExtrrreeeme() ) {
      String leadIn = String.format( "%s %s %s ",
                                        ( this.getUserId( ) != null
                                          ? this.getUserId( )
                                          : "" ),
                                         ( this.getCorrelationId( ) != null
                                             ? this.getCorrelationId( )
                                             : "" ),
                                       this.getType( ) );
      StringBuilder ret = new StringBuilder( );
      ret.append( leadIn ).append( ":" ).append( this.getCaller( ) );
      for ( Object o : this.getOthers( ) ) {
        if ( o == null ) continue;
        if ( BaseRecord.NEXT.equals( o ) ) {
          ret.append( leadIn );
        }
        ret.append( " " ).append( o.toString( ) );
      }
      return ret.toString( ).trim( );
      
    } else {
      return ( this.caller != null
        ? super.toString( ) + ":" + this.caller
        : super.toString( ) );
    }
  }

  @Override
  public Record info( ) {
    return super.info( );
  }

  @Override
  public Record error( ) {
    return super.error( );
  }

  @Override
  public Record trace( ) {
    return super.trace( );
  }

  @Override
  public Record debug( ) {
    return super.debug( );
  }

  @Override
  public Record extreme( ) {
    return super.extreme( );
  }

  @Override
  public Record exhaust( ) {
    return super.exhaust( );
  }

  @Override
  public Record warn( ) {
    return super.warn( );
  }

  @Override
  public Record next( ) {
    return super.next( );
  }

  @Override
  public Record append( Object... obj ) {
    return super.append( obj );
  }

  @Override
  public ArrayList getOthers( ) {
    return super.getOthers( );
  }

  @Override
  public String getLead( ) {
    return super.getLead( );
  }

  @Override
  public Date getTimestamp( ) {
    return super.getTimestamp( );
  }

  @Override
  public void setTimestamp( Date timestamp ) {
    super.setTimestamp( timestamp );
  }

  @Override
  public EventType getType( ) {
    return super.getType( );
  }

  @Override
  public void setType( EventType type ) {
    super.setType( type );
  }

  @Override
  public EventClass getEventClass( ) {
    return super.getEventClass( );
  }

  @Override
  public void setClazz( EventClass clazz ) {
    super.setClazz( clazz );
  }

  @Override
  public String getCreator( ) {
    return super.getCreator( );
  }

  @Override
  public void setCreator( String creator ) {
    super.setCreator( creator );
  }

  @Override
  public String getCodeLocation( ) {
    return super.getCodeLocation( );
  }

  @Override
  public void setCodeLocation( String codeLocation ) {
    super.setCodeLocation( codeLocation );
  }

  @Override
  public String getUserId( ) {
    return super.getUserId( );
  }

  @Override
  public void setUserId( String userId ) {
    super.setUserId( userId );
  }

  @Override
  public String getCorrelationId( ) {
    return super.getCorrelationId( );
  }

  @Override
  public String getExtra( ) {
    return super.getExtra( );
  }

  @Override
  public void setExtra( String extra ) {
    super.setExtra( extra );
  }

  @Override
  public void setCorrelationId( String correlationId ) {
    super.setCorrelationId( correlationId );
  }

  @Override
  public Record withDetails( String userId, String primaryInfo, String key, String value ) {
    return super.withDetails( userId, primaryInfo, key, value );
  }

  @Override
  public Record withDetails( String key, String value ) {
    return super.withDetails( key, value );
  }

  @Override
  public int hashCode( ) {
    return super.hashCode( );
  }

  @Override
  public boolean equals( Object obj ) {
    return super.equals( obj );
  }
  
}
