/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.cluster.node;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Transient;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableIdentifier;

/**
 * @todo doc
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
@Entity
@PersistenceContext( name = "eucalyptus_config" )
@ComponentPart( NodeController.class )
@ConfigurableClass( root = "node",
                    alias = "basic",
                    description = "Node Controller Configuration.",
                    singleton = false,
                    deferred = true )
public class NodeControllerConfiguration extends ComponentConfiguration implements Serializable {

  private static String DEFAULT_SERVICE_PATH = "/axis2/services/EucalyptusNC";
  private static Integer DEFAULT_SERVICE_PORT = 8775;

  @Transient
  @ConfigurableIdentifier
  private String        propertyPrefix;
  
  public NodeControllerConfiguration( ) {
    super( );
  }
  
  public NodeControllerConfiguration( String partition, String hostName ) {
    super( partition, hostName, hostName, DEFAULT_SERVICE_PORT, DEFAULT_SERVICE_PATH );
  }
  
  @PostLoad
  private void initOnLoad( ) {//GRZE:HACK:HACK: needed to mark field as @ConfigurableIdentifier
    if ( this.propertyPrefix == null ) {
      this.propertyPrefix = this.getPartition( ).replace( ".", "" ) + "." + this.getName( );
    }
  }
  
  @Override
  public Boolean isVmLocal( ) {
    return false;
  }
  
  @Override
  public Boolean isHostLocal( ) {
    return BootstrapArgs.isCloudController( );
  }

  /**
   * Immutable for now.
   */
  @Override
  public Integer getPort( ) {
    return DEFAULT_SERVICE_PORT;
  }

  /**
   * Immutable for now.
   */
  @Override
  public String getServicePath( ) {
    return DEFAULT_SERVICE_PATH;
  }
  
  public String getPropertyPrefix( ) {
    return this.getPartition( );
  }
  
  public void setPropertyPrefix( String propertyPrefix ) {
    this.setPartition( propertyPrefix );
  }
}
