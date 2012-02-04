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

package com.eucalyptus.address;

import java.util.NoSuchElementException;
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
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.Logs;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "cloud_address_configuration" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass( root = "cloud.addresses",
                    description = "Configuration options controlling the handling of public/elastic addresses." )
public class AddressingConfiguration extends AbstractPersistent {
  private static final long serialVersionUID = 1L;
  
  @Transient
  private static Logger     LOG              = Logger.getLogger( AddressingConfiguration.class );
  
  @ConfigurableField( displayName = "dynamic_public_addressing",
                      description = "Public addresses are assigned to instances by the system as available.",
                      changeListener = DynamicAddressingListener.class )
  @Column( name = "config_addr_do_dynamic_public_addresses",
           nullable = false,
           columnDefinition = "boolean default true" )
  private Boolean           doDynamicPublicAddresses;
  
  @ConfigurableField( displayName = "static_address_pool",
                      changeListener = PropertyChangeListeners.IsPositiveInteger.class,
                      description = "Public addresses are assigned to instances by the system only from a pool of reserved instances whose size is determined by this value." )
  @Column( name = "config_addr_reserved_public_addresses" )
  private Integer           systemReservedPublicAddresses;
  
  @ConfigurableField( displayName = "address_orphan_count",
                      changeListener = PropertyChangeListeners.IsPositiveInteger.class,
                      description = "Number of times an orphaned address is reported by a cluster before it is reclaimed by the system." )
  @Column( name = "config_addr_orphan_ticks", nullable = false )
  private Integer           maxKillOrphans;
  
  @ConfigurableField( displayName = "address_orphan_grace",
                      changeListener = PropertyChangeListeners.IsPositiveInteger.class,
                      description = "Time after the last recorded state change where an orphaned address will not be modified by the system (minutes)." )
  @Column( name = "config_addr_orphan_grace", nullable = false )
  private Integer           orphanGrace;
  
  public static class DynamicAddressingListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      AddressingConfiguration.getInstance( ).doDynamicPublicAddresses = ( Boolean ) newValue;
    }
  };
  
  public AddressingConfiguration( ) {
    super( );
  }
  
  public static AddressingConfiguration getInstance( ) {
    AddressingConfiguration ret = null;
    try {
      ret = EntityWrapper.get( AddressingConfiguration.class ).lookupAndClose( new AddressingConfiguration( ) );
    } catch ( final NoSuchElementException ex1 ) {
      try {
        ret = EntityWrapper.get( AddressingConfiguration.class ).mergeAndCommit( new AddressingConfiguration( ) );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        ret = new AddressingConfiguration( );
      }
    }
    return ret;
  }
  
  @PrePersist
  protected void initialize( ) {
    if ( this.doDynamicPublicAddresses == null ) {
      this.doDynamicPublicAddresses = Boolean.TRUE;
    }
    if ( this.systemReservedPublicAddresses == null ) {
      this.systemReservedPublicAddresses = 0;
    }
    if ( this.maxKillOrphans == null ) {
      this.maxKillOrphans = 360;
    }
    if ( this.orphanGrace == null ) {
      this.orphanGrace = 360;
    }
  }
  
  public Boolean getDoDynamicPublicAddresses( ) {
    return this.doDynamicPublicAddresses;
  }
  
  public void setDoDynamicPublicAddresses( final Boolean doDynamicPublicAddresses ) {
    this.doDynamicPublicAddresses = doDynamicPublicAddresses;
  }
  
  public Integer getSystemReservedPublicAddresses( ) {
    return this.systemReservedPublicAddresses;
  }
  
  public void setSystemReservedPublicAddresses( final Integer systemReservedPublicAddresses ) {
    this.systemReservedPublicAddresses = systemReservedPublicAddresses;
  }
  
  public Integer getMaxKillOrphans( ) {
    return this.maxKillOrphans;
  }
  
  public void setMaxKillOrphans( Integer maxKillOrphans ) {
    this.maxKillOrphans = maxKillOrphans;
  }
  
  public Integer getOrphanGrace( ) {
    return this.orphanGrace;
  }
  
  public void setOrphanGrace( Integer orphanGrace ) {
    this.orphanGrace = orphanGrace;
  }
}
