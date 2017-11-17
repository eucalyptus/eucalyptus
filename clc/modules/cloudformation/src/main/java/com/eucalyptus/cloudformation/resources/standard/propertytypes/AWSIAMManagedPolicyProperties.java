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
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSIAMManagedPolicyProperties implements ResourceProperties {

  @Property
  private String description;

  @Property
  private ArrayList<String> groups = Lists.newArrayList( );

  @Property
  private String path;

  @Required
  @Property
  private JsonNode policyDocument;

  @Property
  private ArrayList<String> roles = Lists.newArrayList( );

  @Property
  private ArrayList<String> users = Lists.newArrayList( );

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public ArrayList<String> getGroups( ) {
    return groups;
  }

  public void setGroups( ArrayList<String> groups ) {
    this.groups = groups;
  }

  public String getPath( ) {
    return path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  public JsonNode getPolicyDocument( ) {
    return policyDocument;
  }

  public void setPolicyDocument( JsonNode policyDocument ) {
    this.policyDocument = policyDocument;
  }

  public ArrayList<String> getRoles( ) {
    return roles;
  }

  public void setRoles( ArrayList<String> roles ) {
    this.roles = roles;
  }

  public ArrayList<String> getUsers( ) {
    return users;
  }

  public void setUsers( ArrayList<String> users ) {
    this.users = users;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "description", description )
        .add( "groups", groups )
        .add( "path", path )
        .add( "policyDocument", policyDocument )
        .add( "roles", roles )
        .add( "users", users )
        .toString( );
  }
}
