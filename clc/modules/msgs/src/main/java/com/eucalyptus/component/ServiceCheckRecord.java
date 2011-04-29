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
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.util.Date;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.component.ServiceChecks.Severity;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.util.Exceptions;

public class ServiceCheckRecord extends AbstractPersistent {
  private final Severity severity;
  private final String   uuid;
  private final String   message;
  private final String   correlationId;
  private final String   serviceFullName;
  private final String   serviceName;
  private final String   serviceHost;
  private final String   stackTrace;
  private final Date     timestamp;
  
  ServiceCheckRecord( CheckException ex ) {
    this( ex.getCorrelationId( ), ex );
  }
  
  ServiceCheckRecord( String correlationId, CheckException ex ) {
    this.uuid = ex.getUuid( );
    this.timestamp = ex.getTimestamp( );
    this.severity = ex.getSeverity( );
    this.message = ex.getMessage( );
    this.serviceFullName = ex.getConfig( ).getFullName( ).toString( );
    this.serviceName = ex.getConfig( ).getName( );
    this.serviceHost = ex.getConfig( ).getHostName( );
    String tempCorrelationId = correlationId;
    if ( tempCorrelationId == null ) {
      try {
        tempCorrelationId = Contexts.lookup( ).getCorrelationId( );
      } catch ( IllegalContextAccessException ex1 ) {
        tempCorrelationId = this.uuid;
      }
    }
    this.correlationId = tempCorrelationId;
    this.stackTrace = Exceptions.string( ex );
  }
  
  public Severity getSeverity( ) {
    return this.severity;
  }
  
  public String getMessage( ) {
    return this.message;
  }
  
  public String getCorrelationId( ) {
    return this.correlationId;
  }
  
  public String getServiceName( ) {
    return this.serviceName;
  }
  
  public String getServiceHost( ) {
    return this.serviceHost;
  }
  
  public String getStackTrace( ) {
    return this.stackTrace;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "ServiceCheckRecord " ).append( this.severity )
           .append( " " ).append( this.serviceFullName )
           .append( " name=" ).append( this.serviceName )
           .append( " host=" ).append( this.serviceHost )
           .append( " uuid=" ).append( this.uuid )
           .append( " correlationId=" ).append( this.correlationId )
           .append( " timestamp=" ).append( this.timestamp )
           .append( " message=" ).append( this.message )
           .append( " stackTrace=" ).append( this.stackTrace );
    return builder.toString( );
  }
  
  public String getUuid( ) {
    return this.uuid;
  }
  
  public String getServiceFullName( ) {
    return this.serviceFullName;
  }
  
  public Date getTimestamp( ) {
    return this.timestamp;
  }
  
}
