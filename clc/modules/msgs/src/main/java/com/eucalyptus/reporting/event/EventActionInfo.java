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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import java.io.Serializable;

/**
 * Encapsulates information associated with a specific event action.
 */
public class EventActionInfo<E extends Enum<E>> implements Serializable {
  private static final long serialVersionUID = 1L;
  private final E action;

  EventActionInfo( final E action ) {
    assertThat( action, notNullValue() );
    this.action = action;
  }

  public E getAction() {
    return action;
  }

  public String toString() {
    return String.format( "[action:%s]", getAction() );
  }

  /**
   * Information for an action associated with an (VM) instance.
   */
  public static class InstanceEventActionInfo<E extends Enum<E>> extends EventActionInfo<E> {
    private static final long serialVersionUID = 1L;
    private final String instanceUuid;
    private final String instanceId;

    InstanceEventActionInfo( final E action,
                             final String instanceUuid,
                             final String instanceId ) {
      super( action );
      assertThat( instanceUuid, not(isEmptyOrNullString()) );
      assertThat(instanceId, not(isEmptyOrNullString()) );
      this.instanceUuid = instanceUuid;
      this.instanceId = instanceId;
    }

    public String getInstanceUuid() {
      return instanceUuid;
    }

    public String getInstanceId() {
      return instanceId;
    }

    public String toString() {
      return String.format( "[action:%s,instanceUuid:%s,instanceId:%s]",
          getAction(),
          getInstanceUuid(),
          getInstanceId() );
    }
  }
}
