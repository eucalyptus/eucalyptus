/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.simplequeue;

import static com.eucalyptus.simplequeue.common.policy.SimpleQueuePolicySpec.*;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.simplequeue.common.msgs.SimpleQueueMessage;
import com.eucalyptus.simplequeue.exceptions.AccessDeniedException;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.collect.ImmutableSet;
import org.apache.log4j.Logger;

/**
 *
 */
@ComponentNamed
public class SimpleQueueServiceAdvice extends ServiceAdvice {
  private static final Logger LOG = Logger.getLogger(SimpleQueueServiceAdvice.class);
  @Override
  protected void beforeService( @Nonnull final Object requestObject ) throws Exception {
    if ( requestObject instanceof SimpleQueueMessage ) {
      final SimpleQueueMessage request = (SimpleQueueMessage) requestObject;
      Set<String> validAnonymousOperations = ImmutableSet.of(
        SIMPLEQUEUE_CHANGEMESSAGEVISIBILITY,
        SIMPLEQUEUE_DELETEMESSAGE,
        SIMPLEQUEUE_GETQUEUEATTRIBUTES,
        SIMPLEQUEUE_LISTDEADLETTERSOURCEQUEUES,
        "purgequeue",
        SIMPLEQUEUE_RECEIVEMESSAGE,
        SIMPLEQUEUE_SENDMESSAGE
      );
      boolean anonymous = Principals.isSameUser(Contexts.lookup().getUser(), Principals.nobodyUser());
      if (anonymous && !validAnonymousOperations.contains(RestrictedTypes.getIamActionByMessageType(request))) {
        throw new AccessDeniedException("Access denied");
      }
    } else {
      throw new AccessDeniedException("Access denied");
    }
  }
}
