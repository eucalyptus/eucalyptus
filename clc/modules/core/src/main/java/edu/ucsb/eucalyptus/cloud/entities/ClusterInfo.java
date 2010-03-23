/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@PersistenceContext(name="eucalyptus_general")
@Table( name = "clusters" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ClusterInfo {

  @Id
  @GeneratedValue
  @Column( name = "cluster_id" )
  private Long id = -1l;
  @Column( name = "cluster_host" )
  private String host;
  @Column( name = "cluster_port" )
  private Integer port;
  @Column( name = "cluster_name" )
  private String name;
  @Column( name = "cluster_protocol" )
  private String protocol;
  @Column( name = "cluster_path" )
  private String servicePath;
  @Column( name = "cluster_enabled" )
  private Boolean enabled;


  private static String DEFAULT_SERVICE_PATH = "/axis2/services/EucalyptusCC";
  private static String DEFAULT_PROTOCOL = "http";

  public ClusterInfo(){}

  public ClusterInfo( final String name )
  {
    this.name = name;
  }

  public ClusterInfo( final String name, final String host, final Integer port )
  {
    this();
    this.host = host;
    this.port = port;
    this.name = name;
    this.protocol = DEFAULT_PROTOCOL;
    this.servicePath = DEFAULT_SERVICE_PATH;
    this.enabled = true;
  }

  public Long getId()
  {
    return id;
  }

  public Boolean getEnabled()
  {
    return enabled;
  }

  public void setEnabled( final Boolean enabled )
  {
    this.enabled = enabled;
  }

  public String getHost()
  {
    return host;
  }

  public void setHost( final String host )
  {
    this.host = host;
  }

  public int getPort()
  {
    return port;
  }

  public void setPort( final int port )
  {
    this.port = port;
  }

  public String getName()
  {
    return name;
  }

  public void setName( final String name )
  {
    this.name = name;
  }

  public String getProtocol()
  {
    return protocol;
  }

  public void setProtocol( final String protocol )
  {
    this.protocol = protocol;
  }

  public String getInsecureServicePath()
  {
    return servicePath.replaceAll(".C$","GL");
  }

  public String getServicePath()
  {
    return servicePath;
  }

  public void setServicePath( final String servicePath )
  {
    this.servicePath = servicePath;
  }

  public String getUri()
  {
    return this.getProtocol() + "://" + this.getHost() + ":" + this.getPort() + this.getServicePath();
  }
  public String getInsecureUri()
  {
    return this.getProtocol() + "://" + this.getHost() + ":" + this.getPort() + this.getInsecureServicePath();
  }

  @Override
  public boolean equals( Object o )
  {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    ClusterInfo that = ( ClusterInfo ) o;

    if ( !name.equals( that.name ) ) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    return name.hashCode();
  }

  @Override
  public String toString()
  {
    return this.getUri();
  }

  public static ClusterInfo byName( String name )
  {
    return new ClusterInfo(name);
  }

}
