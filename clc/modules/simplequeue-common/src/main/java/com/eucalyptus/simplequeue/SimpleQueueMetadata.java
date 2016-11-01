/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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

import java.util.Set;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.simplequeue.common.policy.SimpleQueuePolicySpec;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Strings;
import javaslang.collection.Stream;

/**
 * Created by ethomas on 10/22/14.
 */
@PolicyVendor( SimpleQueuePolicySpec.VENDOR_SIMPLEQUEUE)
public interface SimpleQueueMetadata extends RestrictedType {
  @PolicyResourceType(value = "queue", resourcePolicyActions = {"sqs:sendmessage", "sqs:receivemessage",
      "sqs:deletemessage", "sqs:changemessagevisibility", "sqs:getqueueattributes", "sqs:getqueueurl",
      "sqs:listdeadlettersourcequeues", "sqs:purgequeue" } )
  public interface QueueMetadata extends SimpleQueueMetadata, PolicyRestrictedType {}

  static Set<String> sharedQueueActions( ) {
    return Stream.of( Ats.from( QueueMetadata.class ).get( PolicyResourceType.class ).resourcePolicyActions( ) )
        .map( Strings.substringAfter( "sqs:" ) )
        .toJavaSet( );
  }
}
