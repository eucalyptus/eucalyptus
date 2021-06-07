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
