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

package com.eucalyptus.images;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.RecoverablePersistenceException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "cloud_image_configuration" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass( root = "cloud.images", description = "Configuration options controlling the handling of registered images (EMI/EKI/ERI)." )
public class ImageConfiguration extends AbstractPersistent {
  @Transient
  private static Logger LOG = Logger.getLogger( ImageConfiguration.class );
  @ConfigurableField( displayName = "default_visibility", description = "The default value used to determine whether or not images are marked 'public' when first registered." )
  @Column( name = "config_image_is_public", nullable = false, columnDefinition = "boolean default true" )
  private Boolean       defaultVisibility;
  
  @ConfigurableField( displayName = "default_kernel_id", description = "The default used for running images which do not have a kernel specified in either the manifest, at register time, or at run-instances time." )
  @Column( name = "config_image_default_kernel_id" )
  private String        defaultKernelId;
  
  @ConfigurableField( displayName = "default_ramdisk_id", description = "The default used for running images which do not have a ramdisk specified in either the manifest, at register time, or at run-instances time." )
  @Column( name = "config_image_default_ramdisk_id" )
  private String        defaultRamdiskId;
  
  public ImageConfiguration( ) {
    super( );
  }
  
  public static void modify( Callback<ImageConfiguration> callback ) throws ExecutionException {
    Transactions.one( new ImageConfiguration( ), callback );
  }
  
  public static ImageConfiguration getInstance( ) {
    ImageConfiguration ret = null;
    try {
      ret = EntityWrapper.get( ImageConfiguration.class ).lookupAndClose( new ImageConfiguration( ) );
    } catch ( NoSuchElementException ex1 ) {
      try {
        ret = EntityWrapper.get( ImageConfiguration.class ).mergeAndCommit( new ImageConfiguration( ) );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        ret = new ImageConfiguration( );
      }
    }
    return ret;
  }
  
  @PrePersist
  protected void initialize( ) {
    if ( this.defaultVisibility == null ) {
      this.defaultVisibility = Boolean.TRUE;
    }
  }
  
  public Boolean getDefaultVisibility( ) {
    return this.defaultVisibility;
  }
  
  public void setDefaultVisibility( Boolean defaultVisibility ) {
    this.defaultVisibility = defaultVisibility;
  }
  
  public String getDefaultKernelId( ) {
    return this.defaultKernelId;
  }
  
  public void setDefaultKernelId( String defaultKernelId ) {
    this.defaultKernelId = defaultKernelId;
  }
  
  public String getDefaultRamdiskId( ) {
    return this.defaultRamdiskId;
  }
  
  public void setDefaultRamdiskId( String defaultRamdiskId ) {
    this.defaultRamdiskId = defaultRamdiskId;
  }
}
