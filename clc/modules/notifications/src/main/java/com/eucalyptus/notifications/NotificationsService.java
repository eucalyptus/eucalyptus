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

package com.eucalyptus.notifications;

import com.eucalyptus.notifications.AddPermissionResponseType;
import com.eucalyptus.notifications.AddPermissionType;
import com.eucalyptus.notifications.ConfirmSubscriptionResponseType;
import com.eucalyptus.notifications.ConfirmSubscriptionType;
import com.eucalyptus.notifications.CreateTopicResponseType;
import com.eucalyptus.notifications.CreateTopicType;
import com.eucalyptus.notifications.DeleteTopicResponseType;
import com.eucalyptus.notifications.DeleteTopicType;
import com.eucalyptus.notifications.GetTopicAttributesResponseType;
import com.eucalyptus.notifications.GetTopicAttributesType;
import com.eucalyptus.notifications.ListSubscriptionsByTopicResponseType;
import com.eucalyptus.notifications.ListSubscriptionsByTopicType;
import com.eucalyptus.notifications.ListSubscriptionsResponseType;
import com.eucalyptus.notifications.ListSubscriptionsType;
import com.eucalyptus.notifications.ListTopicsResponseType;
import com.eucalyptus.notifications.ListTopicsType;
import com.eucalyptus.notifications.PublishResponseType;
import com.eucalyptus.notifications.PublishType;
import com.eucalyptus.notifications.RemovePermissionResponseType;
import com.eucalyptus.notifications.RemovePermissionType;
import com.eucalyptus.notifications.SetTopicAttributesResponseType;
import com.eucalyptus.notifications.SetTopicAttributesType;
import com.eucalyptus.notifications.SubscribeResponseType;
import com.eucalyptus.notifications.SubscribeType;
import com.eucalyptus.notifications.UnsubscribeResponseType;
import com.eucalyptus.notifications.UnsubscribeType;


public class NotificationsService {
  public ConfirmSubscriptionResponseType confirmSubscription(ConfirmSubscriptionType request) {
    ConfirmSubscriptionResponseType reply = request.getReply( );
    return reply;
  }

  public GetTopicAttributesResponseType getTopicAttributes(GetTopicAttributesType request) {
    GetTopicAttributesResponseType reply = request.getReply( );
    return reply;
  }

  public SubscribeResponseType subscribe(SubscribeType request) {
    SubscribeResponseType reply = request.getReply( );
    return reply;
  }

  public SetTopicAttributesResponseType setTopicAttributes(SetTopicAttributesType request) {
    SetTopicAttributesResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteTopicResponseType deleteTopic(DeleteTopicType request) {
    DeleteTopicResponseType reply = request.getReply( );
    return reply;
  }

  public RemovePermissionResponseType removePermission(RemovePermissionType request) {
    RemovePermissionResponseType reply = request.getReply( );
    return reply;
  }

  public ListSubscriptionsResponseType listSubscriptions(ListSubscriptionsType request) {
    ListSubscriptionsResponseType reply = request.getReply( );
    return reply;
  }

  public AddPermissionResponseType addPermission(AddPermissionType request) {
    AddPermissionResponseType reply = request.getReply( );
    return reply;
  }

  public CreateTopicResponseType createTopic(CreateTopicType request) {
    CreateTopicResponseType reply = request.getReply( );
    return reply;
  }

  public ListTopicsResponseType listTopics(ListTopicsType request) {
    ListTopicsResponseType reply = request.getReply( );
    return reply;
  }

  public UnsubscribeResponseType unsubscribe(UnsubscribeType request) {
    UnsubscribeResponseType reply = request.getReply( );
    return reply;
  }

  public ListSubscriptionsByTopicResponseType listSubscriptionsByTopic(ListSubscriptionsByTopicType request) {
    ListSubscriptionsByTopicResponseType reply = request.getReply( );
    return reply;
  }

  public PublishResponseType publish(PublishType request) {
    PublishResponseType reply = request.getReply( );
    return reply;
  }

}
