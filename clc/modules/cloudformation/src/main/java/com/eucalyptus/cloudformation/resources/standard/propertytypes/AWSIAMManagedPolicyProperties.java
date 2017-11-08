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
