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

public class AWSIAMPolicyProperties implements ResourceProperties {

  @Property
  private ArrayList<String> groups = Lists.newArrayList( );

  @Required
  @Property
  private JsonNode policyDocument;

  @Required
  @Property
  private String policyName;

  @Property
  private ArrayList<String> roles = Lists.newArrayList( );

  @Property
  private ArrayList<String> users = Lists.newArrayList( );

  public ArrayList<String> getGroups( ) {
    return groups;
  }

  public void setGroups( ArrayList<String> groups ) {
    this.groups = groups;
  }

  public JsonNode getPolicyDocument( ) {
    return policyDocument;
  }

  public void setPolicyDocument( JsonNode policyDocument ) {
    this.policyDocument = policyDocument;
  }

  public String getPolicyName( ) {
    return policyName;
  }

  public void setPolicyName( String policyName ) {
    this.policyName = policyName;
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
        .add( "groups", groups )
        .add( "policyDocument", policyDocument )
        .add( "policyName", policyName )
        .add( "roles", roles )
        .add( "users", users )
        .toString( );
  }
}
