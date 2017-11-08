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
import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AutoScalingNotificationConfiguration {

  @Property
  @Required
  private String topicARN;

  @Property
  @Required
  private ArrayList<String> notificationTypes = Lists.newArrayList( );

  public ArrayList<String> getNotificationTypes( ) {
    return notificationTypes;
  }

  public void setNotificationTypes( ArrayList<String> notificationTypes ) {
    this.notificationTypes = notificationTypes;
  }

  public String getTopicARN( ) {
    return topicARN;
  }

  public void setTopicARN( String topicARN ) {
    this.topicARN = topicARN;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final AutoScalingNotificationConfiguration that = (AutoScalingNotificationConfiguration) o;
    return Objects.equals( getTopicARN( ), that.getTopicARN( ) ) &&
        Objects.equals( getNotificationTypes( ), that.getNotificationTypes( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getTopicARN( ), getNotificationTypes( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "topicARN", topicARN )
        .add( "notificationTypes", notificationTypes )
        .toString( );
  }
}
