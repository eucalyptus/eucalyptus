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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Lists;

@Entity
@PersistenceContext( name = "eucalyptus_records" )
@Table( name = "records" )
@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
@DiscriminatorColumn( name = "record_class", discriminatorType = DiscriminatorType.STRING )
@DiscriminatorValue( value = "base" )
public class BaseRecord extends AbstractPersistent implements Serializable, Record {
  @Transient
  private static Logger                     LOG    = Logger.getLogger( BaseRecord.class );
  @Column( name = "record_timestamp" )
  private Date                              timestamp;
  @Column( name = "record_type" )
  @Enumerated( EnumType.STRING )
  private EventType                         type;
  @Column( name = "record_class" )
  @Enumerated( EnumType.STRING )
  private EventClass                        eventClass;
  @Column( name = "record_creator" )
  private String                            creator;
  @Column( name = "record_code_location" )
  private String                            codeLocation;
  @Column( name = "record_user_id" )
  private String                            userId;
  @Column( name = "record_correlation_id" )
  private String                            correlationId;
  @Type(type="text")
  @Column( name = "record_extra" )
  private String                            extra;
  @Column( name = "record_level" )
  @Enumerated( EnumType.STRING )
  private RecordLevel                       level;
  @Transient
  private ArrayList                         others = Lists.newArrayList( );
  @Transient
  private static final String               ISNULL = "NULL";
  @Transient
  protected static final String             NEXT   = "\n";
  @Transient
  private transient String                  lead;
  @Transient
  private Class                             realCreator;
  @Transient
  private static BlockingQueue<EventRecord> trace  = new LinkedBlockingDeque<EventRecord>( );
  @Transient
  private static BlockingQueue<EventRecord> debug  = new LinkedBlockingDeque<EventRecord>( );
  @Transient
  private static BlockingQueue<EventRecord> info   = new LinkedBlockingDeque<EventRecord>( );
  @Transient
  private static BlockingQueue<EventRecord> warn   = new LinkedBlockingDeque<EventRecord>( );
  @Transient
  private static BlockingQueue<EventRecord> error  = new LinkedBlockingDeque<EventRecord>( );
  @Transient
  private static BlockingQueue<EventRecord> fatal  = new LinkedBlockingDeque<EventRecord>( );
  
  public BaseRecord( EventType type, EventClass clazz, Class creator, StackTraceElement codeLocation, String userId, String correlationId, String other ) {
    this.type = type;
    this.eventClass = clazz;
    this.realCreator = creator;
    this.creator = creator != null
      ? creator.getSimpleName( )
      : "";
    this.codeLocation = codeLocation != null
      ? codeLocation.toString( )
      : "";
    this.userId = userId;
    this.correlationId = correlationId;
    this.timestamp = new Date( );
    this.extra = other;
    this.others.add( other );
  }
  
  public BaseRecord( ) {}
  
  public Record info( ) {
    this.level = RecordLevel.INFO;
    Logger.getLogger( this.realCreator ).info( this );
    return this;
  }
  
  public Record error( ) {
    this.level = RecordLevel.ERROR;
    Logger.getLogger( this.realCreator ).error( this );
    return this;
  }
  
  public Record trace( ) {
    this.level = RecordLevel.TRACE;
    Logger.getLogger( this.realCreator ).trace( this );
    return this;
  }
  
  public Record debug( ) {
    this.level = RecordLevel.DEBUG;
    Logger.getLogger( this.realCreator ).debug( this );
    return this;
  }
  
  @Override
  public Record extreme( ) {
    this.level = RecordLevel.TRACE;
    Logs.extreme( ).trace( this );
    return this;
  }
  
  @Override
  public Record exhaust( ) {
    this.level = RecordLevel.TRACE;
    Logs.exhaust( ).trace( this );
    return this;
  }

  public Record warn( ) {
    this.level = RecordLevel.WARN;
    Logger.getLogger( this.realCreator ).warn( this );
    return this;
  }
  
  public Record next( ) {
    this.extra = "";
    for ( Object o : this.others ) {
      if ( o == null ) continue;
      this.extra += ":" + o.toString( );
    }
    Record newThis = new LogFileRecord( this.eventClass, this.type, this.realCreator, null, this.userId, this.correlationId, "" );
    return newThis;
  }
  
  public Record append( Object... obj ) {
    for ( Object o : obj ) {
      this.others.add( o == null
        ? ISNULL
        : "" + o );
    }
    this.extra = "";
    for ( Object o : this.others ) {
      if ( o == null ) continue;
      this.extra += ":" + o.toString( );
    }
    return this;
  }
  
  public ArrayList getOthers( ) {
    return this.others;
  }
  
  public static String getIsnull( ) {
    return ISNULL;
  }
  
  public static String getNext( ) {
    return NEXT;
  }
  
  public String getLead( ) {
    return this.lead;
  }
  
  private String leadIn( ) {
    return lead == null
      ? ( lead = String.format( ":%010d:%s:%s:%s:%s:", this.getTimestamp( ).getTime( ), this.getCreator( ),
                                                  ( ( this.correlationId != null )
                                                    ? this.correlationId
                                                    : "" ), ( ( this.userId != null )
                                                    ? this.userId
                                                    : "" ),
                                                  this.type ) )
      : lead;
  }
  
  public String toString( ) {
    String ret = this.leadIn( );
    for ( Object o : this.others ) {
      if ( o == null ) continue;
      ret += ":" + o.toString( );
    }
    return ret.replaceAll( "::*", ":" ).replaceAll( NEXT, NEXT + this.leadIn( ) );
  }
  
  public Date getTimestamp( ) {
    return this.timestamp;
  }
  
  public void setTimestamp( Date timestamp ) {
    this.timestamp = timestamp;
  }
  
  public EventType getType( ) {
    return this.type;
  }
  
  public void setType( EventType type ) {
    this.type = type;
  }
  
  public EventClass getEventClass( ) {
    return this.eventClass;
  }
  
  public void setClazz( EventClass clazz ) {
    this.eventClass = clazz;
  }
  
  public String getCreator( ) {
    return this.creator;
  }
  
  public void setCreator( String creator ) {
    this.creator = creator;
  }
  
  public String getCodeLocation( ) {
    return this.codeLocation;
  }
  
  public void setCodeLocation( String codeLocation ) {
    this.codeLocation = codeLocation;
  }
  
  public String getUserId( ) {
    return this.userId;
  }
  
  public void setUserId( String userId ) {
    this.userId = userId;
  }
  
  public String getCorrelationId( ) {
    return this.correlationId;
  }
  
  public String getExtra( ) {
    return this.extra;
  }
  
  public void setExtra( String extra ) {
    this.extra = extra;
  }
  
  public void setCorrelationId( String correlationId ) {
    this.correlationId = correlationId;
  }
  
  public Record withDetails( String userId, String primaryInfo, String key, String value ) {
    this.userId = userId;
    this.correlationId = primaryInfo;
    return this.withDetails( key, value );
  }
  
  public Record withDetails( String key, String value ) {
    this.others.clear( );
    this.others.add( key );
    this.others.add( value );
    this.info( );
    return this.next( );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.eventClass == null )
      ? 0
      : this.eventClass.hashCode( ) );
    result = prime * result + ( ( this.codeLocation == null )
      ? 0
      : this.codeLocation.hashCode( ) );
    result = prime * result + ( ( this.correlationId == null )
      ? 0
      : this.correlationId.hashCode( ) );
    result = prime * result + ( ( this.creator == null )
      ? 0
      : this.creator.hashCode( ) );
    result = prime * result + ( ( this.timestamp == null )
      ? 0
      : this.timestamp.hashCode( ) );
    result = prime * result + ( ( this.type == null )
      ? 0
      : this.type.hashCode( ) );
    result = prime * result + ( ( this.userId == null )
      ? 0
      : this.userId.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    BaseRecord other = ( BaseRecord ) obj;
    if ( this.eventClass == null ) {
      if ( other.eventClass != null ) return false;
    } else if ( !this.eventClass.equals( other.eventClass ) ) return false;
    if ( this.codeLocation == null ) {
      if ( other.codeLocation != null ) return false;
    } else if ( !this.codeLocation.equals( other.codeLocation ) ) return false;
    if ( this.correlationId == null ) {
      if ( other.correlationId != null ) return false;
    } else if ( !this.correlationId.equals( other.correlationId ) ) return false;
    if ( this.creator == null ) {
      if ( other.creator != null ) return false;
    } else if ( !this.creator.equals( other.creator ) ) return false;
    if ( this.timestamp == null ) {
      if ( other.timestamp != null ) return false;
    } else if ( !this.timestamp.equals( other.timestamp ) ) return false;
    if ( this.type == null ) {
      if ( other.type != null ) return false;
    } else if ( !this.type.equals( other.type ) ) return false;
    if ( this.userId == null ) {
      if ( other.userId != null ) return false;
    } else if ( !this.userId.equals( other.userId ) ) return false;
    return true;
  }
  
}
