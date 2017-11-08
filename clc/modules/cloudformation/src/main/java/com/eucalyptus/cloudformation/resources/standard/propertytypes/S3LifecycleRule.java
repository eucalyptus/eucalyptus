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

public class S3LifecycleRule {

  @Property
  private String expirationDate;

  @Property
  private Integer expirationInDays;

  @Property
  private String id;

  @Property
  private String prefix;

  @Required
  @Property
  private String status;

  @Property
  private S3LifecycleRuleTransition transition;

  public String getExpirationDate( ) {
    return expirationDate;
  }

  public void setExpirationDate( String expirationDate ) {
    this.expirationDate = expirationDate;
  }

  public Integer getExpirationInDays( ) {
    return expirationInDays;
  }

  public void setExpirationInDays( Integer expirationInDays ) {
    this.expirationInDays = expirationInDays;
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

  public S3LifecycleRuleTransition getTransition( ) {
    return transition;
  }

  public void setTransition( S3LifecycleRuleTransition transition ) {
    this.transition = transition;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3LifecycleRule that = (S3LifecycleRule) o;
    return Objects.equals( getExpirationDate( ), that.getExpirationDate( ) ) &&
        Objects.equals( getExpirationInDays( ), that.getExpirationInDays( ) ) &&
        Objects.equals( getId( ), that.getId( ) ) &&
        Objects.equals( getPrefix( ), that.getPrefix( ) ) &&
        Objects.equals( getStatus( ), that.getStatus( ) ) &&
        Objects.equals( getTransition( ), that.getTransition( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getExpirationDate( ), getExpirationInDays( ), getId( ), getPrefix( ), getStatus( ), getTransition( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "expirationDate", expirationDate )
        .add( "expirationInDays", expirationInDays )
        .add( "id", id )
        .add( "prefix", prefix )
        .add( "status", status )
        .add( "transition", transition )
        .toString( );
  }
}
