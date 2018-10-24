/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.records;

import java.util.ArrayList;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;

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
