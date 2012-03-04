/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.entities;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;

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
  @Lob
  @Column( name = "metadata_state_change_stack" )
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
    this.stateChangeStack = Logs.isDebug( ) ? Threads.currentStackString( ) : "n/a";
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

  private String getStateChangeStack( ) {
    return this.stateChangeStack;
  }

  private void setStateChangeStack( String stateChangeStack ) {
    this.stateChangeStack = stateChangeStack;
  }

  public STATE getLastState( ) {
    return this.lastState;
  }

  public void setLastState( STATE lastState ) {
    this.lastState = lastState;
  }
  
}
