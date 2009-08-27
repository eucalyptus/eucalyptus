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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package com.eucalyptus.config;

import javax.persistence.Entity;
import javax.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.FetchType;
import javax.persistence.CascadeType;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;
import org.hibernate.sql.Alias;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

@MappedSuperclass
public abstract class AbstractPersistent implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "id" )
  String id;
  @Version
  @Column(name = "version")
  Integer version = 0;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update_timestamp")
  Date lastUpdate;
}

@MappedSuperclass
public abstract class ComponentConfiguration extends AbstractPersistent implements Serializable {
  @Column( name = "config_component_name", unique=true )
  String name;
  @Column( name = "config_component_hostname" )
  String hostName;
  @Column( name = "config_component_port" )
  Integer port;  
  @Column( name = "config_component_service_path" )
  String servicePath;  

  public ComponentConfiguration( ) {}
  public ComponentConfiguration( String name, String hostName, Integer port, String servicePath ) {
    this.name = name;
    this.hostName = hostName;
    this.port = port;
    this.servicePath = servicePath;
  }

  public String getUri() {
    return "http://" + this.getHost() + ":" + this.getPort() + this.getServicePath();
  }
}

@Entity
@Table( name = "config_clusters" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ClusterConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/axis2/services/EucalyptusCC";
  @Transient
  private static String INSECURE_SERVICE_PATH = "/axis2/services/EucalyptusGL";

  public ClusterConfiguration( ) {}
  public ClusterConfiguration( String name, String hostName, Integer port ) {
    super( name, hostName, port, DEFAULT_SERVICE_PATH );
  }
  public String getInsecureServicePath() {
    return INSECURE_SERVICE_PATH;
  }
  public String getInsecureUri() {
    return "http://" + this.getHost() + ":" + this.getPort() + INSECURE_SERVICE_PATH;
  }

  public static ClusterConfiguration byClusterName( String name ) {
    ClusterConfiguration c = new ClusterConfiguration( );
    c.setClusterName(name);
    return c;
  }
  public static ClusterConfiguration byHostName( String hostName ) {
    ClusterConfiguration c = new ClusterConfiguration( );
    c.setHostName(hostName);
    return c;
  }
}

@Entity
@Table( name = "config_sc" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class StorageControllerConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/services/Storage";
  public StorageControllerConfiguration( ) {}
  public StorageControllerConfiguration( String name, String hostName, Integer port ) {
    super( name, hostName, port, DEFAULT_SERVICE_PATH );
  }
}
@Entity
@Table( name = "config_walrus" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class WalrusConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/services/Walrus";
  public WalrusConfiguration( ) {
  }
  public WalrusConfiguration( String name, String hostName, Integer port ) {
    super( name, hostName, port, DEFAULT_SERVICE_PATH );
  }
}


@Entity
@Table( name = "config_system" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class System implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "config_system_id" )
  String id
  @Column( name = "config_system_default_kernel" )
  String defaultKernel
  @Column( name = "config_system_default_ramdisk" )
  String defaultRamdisk
  @Column( name = "config_system_registration_id" )
  String registrationId
}

