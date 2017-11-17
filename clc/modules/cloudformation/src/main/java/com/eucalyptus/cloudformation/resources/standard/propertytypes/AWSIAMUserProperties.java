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
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSIAMUserProperties implements ResourceProperties {

  @Property
  private String path;

  @Property
  private ArrayList<String> groups = Lists.newArrayList( );

  @Property
  private LoginProfile loginProfile;

  @Property
  private ArrayList<String> managedPolicyArns = Lists.newArrayList( );

  @Property
  private ArrayList<EmbeddedIAMPolicy> policies = Lists.newArrayList( );

  @Property
  private String userName;

  public ArrayList<String> getGroups( ) {
    return groups;
  }

  public void setGroups( ArrayList<String> groups ) {
    this.groups = groups;
  }

  public LoginProfile getLoginProfile( ) {
    return loginProfile;
  }

  public void setLoginProfile( LoginProfile loginProfile ) {
    this.loginProfile = loginProfile;
  }

  public ArrayList<String> getManagedPolicyArns( ) {
    return managedPolicyArns;
  }

  public void setManagedPolicyArns( ArrayList<String> managedPolicyArns ) {
    this.managedPolicyArns = managedPolicyArns;
  }

  public String getPath( ) {
    return path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  public ArrayList<EmbeddedIAMPolicy> getPolicies( ) {
    return policies;
  }

  public void setPolicies( ArrayList<EmbeddedIAMPolicy> policies ) {
    this.policies = policies;
  }

  public String getUserName( ) {
    return userName;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "path", path )
        .add( "groups", groups )
        .add( "loginProfile", loginProfile )
        .add( "managedPolicyArns", managedPolicyArns )
        .add( "policies", policies )
        .add( "userName", userName )
        .toString( );
  }
}
