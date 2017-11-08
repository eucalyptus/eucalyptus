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

public class S3LifecycleRuleTransition {

  @Required
  @Property
  private String storageClass;

  @Property
  private String transitionDate;

  @Property
  private Integer transitionInDays;

  public String getStorageClass( ) {
    return storageClass;
  }

  public void setStorageClass( String storageClass ) {
    this.storageClass = storageClass;
  }

  public String getTransitionDate( ) {
    return transitionDate;
  }

  public void setTransitionDate( String transitionDate ) {
    this.transitionDate = transitionDate;
  }

  public Integer getTransitionInDays( ) {
    return transitionInDays;
  }

  public void setTransitionInDays( Integer transitionInDays ) {
    this.transitionInDays = transitionInDays;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3LifecycleRuleTransition that = (S3LifecycleRuleTransition) o;
    return Objects.equals( getStorageClass( ), that.getStorageClass( ) ) &&
        Objects.equals( getTransitionDate( ), that.getTransitionDate( ) ) &&
        Objects.equals( getTransitionInDays( ), that.getTransitionInDays( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getStorageClass( ), getTransitionDate( ), getTransitionInDays( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "storageClass", storageClass )
        .add( "transitionDate", transitionDate )
        .add( "transitionInDays", transitionInDays )
        .toString( );
  }
}
