/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
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
