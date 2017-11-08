/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.reporting.event;

import static com.eucalyptus.util.Parameters.checkParam;
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
    checkParam( action, notNullValue() );
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
      checkParam( instanceUuid, not( isEmptyOrNullString() ) );
      checkParam( instanceId, not( isEmptyOrNullString() ) );
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
