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

package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.TableDisplay;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class VmTypeWebBackend {
  
  private static final Logger LOG = Logger.getLogger( VmTypeWebBackend.class );
  
  public static final String NAME = "Name";
  public static final String CPU = "CPU";
  public static final String DISK = "Disk";
  public static final String MEMORY = "Memory";
  
  // Common fields
  public static final ArrayList<SearchResultFieldDesc> COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, NAME, false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CPU, "CPUs", false, "10%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( MEMORY, "Memory (MB)", false, "10%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( DISK, "Disk (GB)", false, "60%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
  }
  
  /**
   * @return VmType info for display
   */
  public static List<SearchResultRow> getVmTypes( ) {
    List<SearchResultRow> rows = Lists.newArrayList( );
    for ( VmType v : VmTypes.list( ) ) {
      SearchResultRow row = new SearchResultRow( );
      serializeVmType( v, row );
      rows.add( row );
    }
    return rows;
  }

  private static void serializeVmType( VmType v, SearchResultRow row ) {
    row.addField( v.getName( ) );
    row.addField( v.getCpu( ) == null ? "" : v.getCpu( ).toString( ) );
    row.addField( v.getMemory( ) == null ? "" : v.getMemory( ).toString( ) );
    row.addField( v.getDisk( ) == null ? "" : v.getDisk( ).toString( ) );
  }
  
  /**
   * Update VmType by UI input.
   * 
   * @param row
   * @throws EucalyptusServiceException
   */
  public static void setVmType( SearchResultRow row ) throws EucalyptusServiceException {
    VmType input = deserializeVmType( row );
    if ( input == null ) {
      throw new EucalyptusServiceException( "Invalid input" );
    }
    Set<VmType> newVms = Sets.newTreeSet( );
    for ( VmType v : VmTypes.list( ) ) {
      if ( v.getName( ).equals( input.getName( ) ) ) {
        newVms.add( input );
      } else {
        newVms.add( v );
      }
    }
    try {
      VmTypes.update( newVms );
    } catch ( EucalyptusCloudException e ) {
      LOG.error( "Failed to update VmType for row " + row, e );
      throw new EucalyptusServiceException( e.getMessage( ), e );
    }
  }

  private static VmType deserializeVmType( SearchResultRow row ) {
    int i = 0;
    String name = row.getField( i++ );
    Integer cpu = null;
    try {
      cpu = Integer.parseInt( row.getField( i++ ) );
      if ( cpu <= 0 ) {
        throw new IllegalArgumentException( "Can not have negative or zero value for CPU" );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to parse cpu value from UI input for " + name, e );
      return null;
    }
    Integer memory = null;
    try {
      memory = Integer.parseInt( row.getField( i++ ) );
      if ( memory <= 0 ) {
        throw new IllegalArgumentException( "Can not have negative or zero value for memory" );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to parse memory value from UI input for " + name, e );
      return null;
    }
    Integer disk = null;
    try {
      disk = Integer.parseInt( row.getField( i++ ) );
      if ( disk <= 0 ) {
        throw new IllegalArgumentException( "Can not have negative or zero value for disk" );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to parse disk value from UI input for " + name, e );
      return null;
    }
    return VmType.create( name, cpu, disk, memory );
  }
}
