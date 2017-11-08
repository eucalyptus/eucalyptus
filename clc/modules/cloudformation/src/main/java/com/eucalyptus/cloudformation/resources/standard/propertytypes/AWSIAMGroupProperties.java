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
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSIAMGroupProperties implements ResourceProperties {

  @Property
  private String groupName;

  @Property
  private String path;

  @Property
  private ArrayList<String> managedPolicyArns = Lists.newArrayList( );

  @Property
  private ArrayList<EmbeddedIAMPolicy> policies = Lists.newArrayList( );

  public String getGroupName( ) {
    return groupName;
  }

  public void setGroupName( String groupName ) {
    this.groupName = groupName;
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

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "groupName", groupName )
        .add( "path", path )
        .add( "managedPolicyArns", managedPolicyArns )
        .add( "policies", policies )
        .toString( );
  }
}
