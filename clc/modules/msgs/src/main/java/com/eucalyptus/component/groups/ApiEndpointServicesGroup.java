package com.eucalyptus.component.groups;

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;

import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.system.Ats;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.util.techpreview.TechPreviews;

/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
@FaultLogPrefix( "cloud" )
@Partition( value = { ApiEndpointServicesGroup.class }, manyToOne = true )
@Description( "The service group of all user-facing API endpoint services" )
public class ApiEndpointServicesGroup extends ServiceGroup {
  private static Logger LOG = Logger.getLogger( ApiEndpointServicesGroup.class );
  
  public ApiEndpointServicesGroup( ) {
    super( "User-API" );
  }
  
  @Override
  public boolean apply( @Nullable ComponentId componentId ) {
    return componentId != null &&
        Ats.from( componentId ).has( PublicService.class ) &&
        componentId.isRegisterable( ) &&
        !TechPreviews.isTechPreview( componentId );
  }
  
  @Entity
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  @PersistenceContext( name = "eucalyptus_config" )
  @ComponentPart( ApiEndpointServicesGroup.class )
  @DiscriminatorValue( "ApiEndpointGroup" )
  public static class ApiEndpointGroup extends BaseServiceGroupConfiguration implements Serializable {
    
    public ApiEndpointGroup( String groupName, String hostName ) {
      super( groupName, hostName );
    }
    
    public ApiEndpointGroup( ) {
      
    }
  }
  
  @ComponentPart( ApiEndpointServicesGroup.class )
  public static class ApiEndpointServiceBuilder extends BaseServiceGroupBuilder<ApiEndpointGroup> {
    
    @Override
    public ApiEndpointGroup newInstance( String partition, String name, String host, Integer port ) {
      return new ApiEndpointGroup( name, host );
    }
    
    @Override
    public ApiEndpointGroup newInstance( ) {
      return new ApiEndpointGroup( );
    }
  }
}
