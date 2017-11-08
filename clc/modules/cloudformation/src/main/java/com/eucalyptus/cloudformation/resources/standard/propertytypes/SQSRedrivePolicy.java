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

public class SQSRedrivePolicy {

  @Required
  @Property( name = "deadLetterTargetArn" )
  private String deadLetterTargetArn;

  @Required
  @Property( name = "maxReceiveCount" )
  private Integer maxReceiveCount;

  public String getDeadLetterTargetArn( ) {
    return deadLetterTargetArn;
  }

  public void setDeadLetterTargetArn( String deadLetterTargetArn ) {
    this.deadLetterTargetArn = deadLetterTargetArn;
  }

  public Integer getMaxReceiveCount( ) {
    return maxReceiveCount;
  }

  public void setMaxReceiveCount( Integer maxReceiveCount ) {
    this.maxReceiveCount = maxReceiveCount;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final SQSRedrivePolicy that = (SQSRedrivePolicy) o;
    return Objects.equals( getDeadLetterTargetArn( ), that.getDeadLetterTargetArn( ) ) &&
        Objects.equals( getMaxReceiveCount( ), that.getMaxReceiveCount( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getDeadLetterTargetArn( ), getMaxReceiveCount( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "deadLetterTargetArn", deadLetterTargetArn )
        .add( "maxReceiveCount", maxReceiveCount )
        .toString( );
  }
}
