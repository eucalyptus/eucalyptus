/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.simplequeue.common;

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
