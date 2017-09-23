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
package com.eucalyptus.simplequeue.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.auth.policy.annotation.PolicyAction;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.simplequeue.common.policy.SimpleQueuePolicySpec;

@PolicyAction( vendor = SimpleQueuePolicySpec.VENDOR_SIMPLEQUEUE, action = SimpleQueuePolicySpec.SIMPLEQUEUE_CHANGEMESSAGEVISIBILITY )
public class ChangeMessageVisibilityBatchType extends SimpleQueueMessage implements QueueUrlGetterSetter {

  private String queueUrl;
  @HttpEmbedded( multiple = true )
  private ArrayList<ChangeMessageVisibilityBatchRequestEntry> changeMessageVisibilityBatchRequestEntry = new ArrayList<ChangeMessageVisibilityBatchRequestEntry>( );

  public String getQueueUrl( ) {
    return queueUrl;
  }

  public void setQueueUrl( String queueUrl ) {
    this.queueUrl = queueUrl;
  }

  public ArrayList<ChangeMessageVisibilityBatchRequestEntry> getChangeMessageVisibilityBatchRequestEntry( ) {
    return changeMessageVisibilityBatchRequestEntry;
  }

  public void setChangeMessageVisibilityBatchRequestEntry( ArrayList<ChangeMessageVisibilityBatchRequestEntry> changeMessageVisibilityBatchRequestEntry ) {
    this.changeMessageVisibilityBatchRequestEntry = changeMessageVisibilityBatchRequestEntry;
  }
}
