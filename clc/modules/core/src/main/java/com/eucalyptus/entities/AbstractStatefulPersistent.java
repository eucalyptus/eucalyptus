/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.entities;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import org.hibernate.annotations.Type;

@MappedSuperclass
public abstract class AbstractStatefulPersistent<STATE extends Enum<STATE>> extends AbstractPersistent {
  @Transient 
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_state" )
  @Enumerated( EnumType.STRING )
  STATE                     state;
  @Column( name = "metadata_last_state" )
  @Enumerated( EnumType.STRING )
  STATE                     lastState;
  @Column( name = "metadata_state_change_stack" )
  @Type(type="text")
  protected String          stateChangeStack;
  @Column( name = "metadata_display_name" )
  protected String          displayName;
  
  protected AbstractStatefulPersistent( ) {
    super( );
  }
  
  protected AbstractStatefulPersistent( final STATE state, final String displayName ) {
    super( );
    this.state = state;
    this.displayName = displayName;
  }
  
  protected AbstractStatefulPersistent( final String displayName ) {
    super( );
    this.displayName = displayName;
  }
  
  public STATE getState( ) {
    return this.state;
  }
  
  public void setState( final STATE state ) {
    this.stateChangeStack = Logs.isExtrrreeeme( ) ? Threads.currentStackRange( 0, 32 ) : "n/a";
    if ( state != null && this.state != null && !state.equals( this.state ) ) {
      this.lastState = this.state;
    } else if ( state != null && this.state == null ) {
      this.lastState = state;
    } else if ( state == null && this.state != null ) {
      this.lastState = this.state;
    }
    this.state = state;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.displayName == null )
      ? 0
      : this.displayName.hashCode( ) );
    return result;
  }
  
  public String getDisplayName( ) {
    return this.displayName;
  }
  
  public void setDisplayName( final String displayName ) {
    this.displayName = displayName;
  }
  
  public final String getName( ) {
    return this.getDisplayName( );
  }

  public String getStateChangeStack( ) {
    return this.stateChangeStack;
  }

  public void setStateChangeStack( String stateChangeStack ) {
    this.stateChangeStack = stateChangeStack;
  }

  public STATE getLastState( ) {
    return this.lastState;
  }

  public void setLastState( STATE lastState ) {
    this.lastState = lastState;
  }
  
}
