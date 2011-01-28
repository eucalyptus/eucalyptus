/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

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
