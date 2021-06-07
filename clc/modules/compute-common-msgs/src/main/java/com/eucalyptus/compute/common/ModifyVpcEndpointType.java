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

import java.util.ArrayList;

public class ModifyVpcEndpointType extends VpcMessage {

  private ArrayList<String> addRouteTableId;
  private String policyDocument;
  private ArrayList<String> removeRouteTableId;
  private Boolean resetPolicy;
  private String vpcEndpointId;

  public ArrayList<String> getAddRouteTableId( ) {
    return addRouteTableId;
  }

  public void setAddRouteTableId( ArrayList<String> addRouteTableId ) {
    this.addRouteTableId = addRouteTableId;
  }

  public String getPolicyDocument( ) {
    return policyDocument;
  }

  public void setPolicyDocument( String policyDocument ) {
    this.policyDocument = policyDocument;
  }

  public ArrayList<String> getRemoveRouteTableId( ) {
    return removeRouteTableId;
  }

  public void setRemoveRouteTableId( ArrayList<String> removeRouteTableId ) {
    this.removeRouteTableId = removeRouteTableId;
  }

  public Boolean getResetPolicy( ) {
    return resetPolicy;
  }

  public void setResetPolicy( Boolean resetPolicy ) {
    this.resetPolicy = resetPolicy;
  }

  public String getVpcEndpointId( ) {
    return vpcEndpointId;
  }

  public void setVpcEndpointId( String vpcEndpointId ) {
    this.vpcEndpointId = vpcEndpointId;
  }
}
