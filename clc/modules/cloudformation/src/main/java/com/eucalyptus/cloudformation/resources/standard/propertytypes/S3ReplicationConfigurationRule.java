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

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class S3ReplicationConfigurationRule {

  @Required
  @Property
  private S3ReplicationConfigurationRulesDestination destination;

  @Property
  private String id;

  @Required
  @Property
  private String prefix;

  @Required
  @Property
  private String status;

  public S3ReplicationConfigurationRulesDestination getDestination( ) {
    return destination;
  }

  public void setDestination( S3ReplicationConfigurationRulesDestination destination ) {
    this.destination = destination;
  }

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }

  public String getPrefix( ) {
    return prefix;
  }

  public void setPrefix( String prefix ) {
    this.prefix = prefix;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3ReplicationConfigurationRule that = (S3ReplicationConfigurationRule) o;
    return Objects.equals( getDestination( ), that.getDestination( ) ) &&
        Objects.equals( getId( ), that.getId( ) ) &&
        Objects.equals( getPrefix( ), that.getPrefix( ) ) &&
        Objects.equals( getStatus( ), that.getStatus( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getDestination( ), getId( ), getPrefix( ), getStatus( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "destination", destination )
        .add( "id", id )
        .add( "prefix", prefix )
        .add( "status", status )
        .toString( );
  }
}
