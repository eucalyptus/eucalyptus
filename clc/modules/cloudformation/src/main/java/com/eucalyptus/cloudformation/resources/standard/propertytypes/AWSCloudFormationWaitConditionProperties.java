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

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

public class AWSCloudFormationWaitConditionProperties implements ResourceProperties {

  @Property
  private Integer count;

  @Property
  private String handle;

  @Property
  private Integer timeout;

  public Integer getCount( ) {
    return count;
  }

  public void setCount( Integer count ) {
    this.count = count;
  }

  public String getHandle( ) {
    return handle;
  }

  public void setHandle( String handle ) {
    this.handle = handle;
  }

  public Integer getTimeout( ) {
    return timeout;
  }

  public void setTimeout( Integer timeout ) {
    this.timeout = timeout;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "count", count )
        .add( "handle", handle )
        .add( "timeout", timeout )
        .toString( );
  }
}
