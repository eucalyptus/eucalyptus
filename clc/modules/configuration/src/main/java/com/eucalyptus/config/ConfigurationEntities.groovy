package com.eucalyptus.config;

import javax.persistence.Entity;
import javax.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
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

@Entity
@Table( name = "config_clusters" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ClusterConfiguration implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "config_cluster_id" )
  String id;
  @Column( name = "config_cluster_name", unique=true )
  String clusterName;
  @Column( name = "config_cluster_hostname", unique=true )
  String hostName;
  @Column( name = "config_cluster_port" )
  Integer port;
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/axis2/services/EucalyptusCC";
  public ClusterConfiguration(){}
  public String getUri() {
    return this.getProtocol() + "://" + this.getHost() + ":" + this.getPort() + this.getServicePath();
  }
  public String getInsecureUri() {
    return this.getProtocol() + "://" + this.getHost() + ":" + this.getPort() + this.getInsecureServicePath();
  }
  public String getServicePath() {
    return DEFAULT_SERVICE_PATH;
  }

  public ClusterConfiguration( String clusterName, String hostName, Integer port ) {
    this.clusterName = clusterName;
    this.hostName = hostName;
    this.port = port;
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

