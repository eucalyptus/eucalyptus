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
package com.eucalyptus.cluster.common.msgs;

import java.util.ArrayList;

public class NcMigrateInstancesType extends CloudNodeMessage {

  private String action;
  private String credentials;
  private ArrayList<InstanceType> instances = new ArrayList<InstanceType>( );
  private ArrayList<String> resourceLocation = new ArrayList<String>( );

  public String getAction( ) {
    return action;
  }

  public void setAction( String action ) {
    this.action = action;
  }

  public String getCredentials( ) {
    return credentials;
  }

  public void setCredentials( String credentials ) {
    this.credentials = credentials;
  }

  public ArrayList<InstanceType> getInstances( ) {
    return instances;
  }

  public void setInstances( ArrayList<InstanceType> instances ) {
    this.instances = instances;
  }

  public ArrayList<String> getResourceLocation( ) {
    return resourceLocation;
  }

  public void setResourceLocation( ArrayList<String> resourceLocation ) {
    this.resourceLocation = resourceLocation;
  }
}
