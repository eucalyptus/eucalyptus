/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.config;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@PersistenceContext( name = "eucalyptus_config" )
@Table( name = "config_system" )
public class System extends AbstractPersistent implements Serializable {

  @Column( name = "config_system_default_kernel" )
  private String defaultKernel;

  @Column( name = "config_system_default_ramdisk" )
  private String defaultRamdisk;
  
  @Column( name = "config_system_registration_id" )
  private String registrationId;

  public String getDefaultKernel( ) {
    return defaultKernel;
  }

  public void setDefaultKernel( String defaultKernel ) {
    this.defaultKernel = defaultKernel;
  }

  public String getDefaultRamdisk( ) {
    return defaultRamdisk;
  }

  public void setDefaultRamdisk( String defaultRamdisk ) {
    this.defaultRamdisk = defaultRamdisk;
  }

  public String getRegistrationId( ) {
    return registrationId;
  }

  public void setRegistrationId( String registrationId ) {
    this.registrationId = registrationId;
  }
}
