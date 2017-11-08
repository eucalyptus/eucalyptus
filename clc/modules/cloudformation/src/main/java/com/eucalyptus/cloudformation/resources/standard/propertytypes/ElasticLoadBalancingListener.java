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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class ElasticLoadBalancingListener {

  @Required
  @Property
  private Integer instancePort;

  @Property
  private String instanceProtocol;

  @Required
  @Property
  private Integer loadBalancerPort;

  @Property
  private ArrayList<String> policyNames = Lists.newArrayList( );

  @Required
  @Property
  private String protocol;

  @Property( name = "SSLCertificateId" )
  private String sslCertificateId;

  public Integer getInstancePort( ) {
    return instancePort;
  }

  public void setInstancePort( Integer instancePort ) {
    this.instancePort = instancePort;
  }

  public String getInstanceProtocol( ) {
    return instanceProtocol;
  }

  public void setInstanceProtocol( String instanceProtocol ) {
    this.instanceProtocol = instanceProtocol;
  }

  public Integer getLoadBalancerPort( ) {
    return loadBalancerPort;
  }

  public void setLoadBalancerPort( Integer loadBalancerPort ) {
    this.loadBalancerPort = loadBalancerPort;
  }

  public ArrayList<String> getPolicyNames( ) {
    return policyNames;
  }

  public void setPolicyNames( ArrayList<String> policyNames ) {
    this.policyNames = policyNames;
  }

  public String getProtocol( ) {
    return protocol;
  }

  public void setProtocol( String protocol ) {
    this.protocol = protocol;
  }

  public String getSslCertificateId( ) {
    return sslCertificateId;
  }

  public void setSslCertificateId( String sslCertificateId ) {
    this.sslCertificateId = sslCertificateId;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ElasticLoadBalancingListener that = (ElasticLoadBalancingListener) o;
    return Objects.equals( getInstancePort( ), that.getInstancePort( ) ) &&
        Objects.equals( getInstanceProtocol( ), that.getInstanceProtocol( ) ) &&
        Objects.equals( getLoadBalancerPort( ), that.getLoadBalancerPort( ) ) &&
        Objects.equals( getPolicyNames( ), that.getPolicyNames( ) ) &&
        Objects.equals( getProtocol( ), that.getProtocol( ) ) &&
        Objects.equals( getSslCertificateId( ), that.getSslCertificateId( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getInstancePort( ), getInstanceProtocol( ), getLoadBalancerPort( ), getPolicyNames( ), getProtocol( ), getSslCertificateId( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "instancePort", instancePort )
        .add( "instanceProtocol", instanceProtocol )
        .add( "loadBalancerPort", loadBalancerPort )
        .add( "policyNames", policyNames )
        .add( "protocol", protocol )
        .add( "sslCertificateId", sslCertificateId )
        .toString( );
  }
}
