/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.reporting.event;

/**
 * An InstanceCreatedEvent is fired on the CLC when an instance is run.
 */
public class InstanceCreatedEvent extends InstanceEventSupport
{
  /**
   * Constructor for InstanceCreatedEvent.
   */
  public InstanceCreatedEvent( final String uuid,
                               final String instanceId,
                               final String instanceType,
                               final String userId,
                               final String userName,
                               final String accountId,
                               final String accountName,
                               final String clusterName,
                               final String availabilityZone ) {
    super( uuid, instanceId, instanceType, userId, userName, accountId, accountName, clusterName, availabilityZone );
  }

}
