/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.component

import java.net.URI
import javax.persistence.Transient
import com.eucalyptus.config.ComponentConfiguration

class EphemeralConfiguration extends ComponentConfiguration {
  private static final long serialVersionUID = 1;
  URI         uri;
  ComponentId c;
  
  public EphemeralConfiguration( ComponentId c, String partition, String name, URI uri ) {
    super( partition, name, uri.getHost( ), uri.getPort( ), uri.getPath( ) );
    this.uri = uri;
    this.c = c;
  }
  
  public ComponentId lookupComponentId( ) {
    return c;
  }
  
  public URI getUri( ) {
    return this.uri;
  }
  
  @Override
  public String getName( ) {
    return super.getName( );
  }
  
  @Override
  public Boolean isVmLocal( ) {
    return super.isVmLocal( );
  }

  @Override
  public Boolean isHostLocal( ) {
    return super.isHostLocal( );
  }

  @Override
  public int compareTo( ServiceConfiguration that ) {
    return super.compareTo( that );
  }
  
  @Override
  public String toString( ) {
    return super.toString( );
  }
  
  @Override
  public int hashCode( ) {
    return super.hashCode( );
  }
  
  @Override
  public boolean equals( Object that ) {
    return super.equals(that) &&
    that instanceof ServiceConfiguration &&
    this.getComponentId().equals(((ServiceConfiguration) that).getComponentId());
    //NOTE: these additional tests are necessary as the super.equals method
    // expects Class type differences between different components
  }
  
  @Override
  public String getPartition( ) {
    return super.getPartition( );
  }
  
  @Override
  public void setPartition( String partition ) {
    super.setPartition( partition );
  }
  
  @Override
  public String getHostName( ) {
    return super.getHostName( );
  }
  
  @Override
  public void setHostName( String hostName ) {
    super.setHostName( hostName );
  }
  
  @Override
  public Integer getPort( ) {
    return super.getPort( );
  }
  
  @Override
  public void setPort( Integer port ) {
    super.setPort( port );
  }
  
  @Override
  public String getServicePath( ) {
    return super.getServicePath( );
  }
  
  @Override
  public void setServicePath( String servicePath ) {
    super.setServicePath( servicePath );
  }
  
  @Override
  public void setName( String name ) {
    super.setName( name );
  }

  public String getSourceHostName( ) {
    return null;
  }

  public void setSourceHostName( String aliasHostName ) {}

}
