package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.util.BindingUtil;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
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
