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
package com.eucalyptus.compute.common;

import java.util.Collection;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class RouteTableType extends EucalyptusData implements VpcTagged {

  private String routeTableId;
  private String vpcId;
  private RouteSetType routeSet;
  private RouteTableAssociationSetType associationSet;
  private PropagatingVgwSetType propagatingVgwSet;
  private ResourceTagSetType tagSet;

  public RouteTableType( ) {
  }

  public RouteTableType( final String routeTableId, final String vpcId, final Collection<RouteType> routes, final Collection<RouteTableAssociationType> associations ) {
    this.routeTableId = routeTableId;
    this.vpcId = vpcId;
    this.routeSet = new RouteSetType( routes );
    this.associationSet = new RouteTableAssociationSetType( associations );
    this.propagatingVgwSet = new PropagatingVgwSetType( );
  }

  public static CompatFunction<RouteTableType, String> id( ) {
    return new CompatFunction<RouteTableType, String>( ) {
      @Override
      public String apply( final RouteTableType routeTableType ) {
        return routeTableType.getRouteTableId( );
      }
    };
  }

  public String getRouteTableId( ) {
    return routeTableId;
  }

  public void setRouteTableId( String routeTableId ) {
    this.routeTableId = routeTableId;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public RouteSetType getRouteSet( ) {
    return routeSet;
  }

  public void setRouteSet( RouteSetType routeSet ) {
    this.routeSet = routeSet;
  }

  public RouteTableAssociationSetType getAssociationSet( ) {
    return associationSet;
  }

  public void setAssociationSet( RouteTableAssociationSetType associationSet ) {
    this.associationSet = associationSet;
  }

  public PropagatingVgwSetType getPropagatingVgwSet( ) {
    return propagatingVgwSet;
  }

  public void setPropagatingVgwSet( PropagatingVgwSetType propagatingVgwSet ) {
    this.propagatingVgwSet = propagatingVgwSet;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
