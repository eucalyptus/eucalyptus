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
package com.eucalyptus.simplequeue;

import static com.eucalyptus.simplequeue.common.policy.SimpleQueuePolicySpec.*;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingMessage;
import com.eucalyptus.autoscaling.common.policy.AutoScalingPolicySpec;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceAdvice;
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
