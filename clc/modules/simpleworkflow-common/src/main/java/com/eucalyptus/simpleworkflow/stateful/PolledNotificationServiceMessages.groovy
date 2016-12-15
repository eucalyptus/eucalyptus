/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
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
@GroovyAddClassUUID
package com.eucalyptus.simpleworkflow.stateful

import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.simpleworkflow.common.stateful.PolledNotifications
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@ComponentMessage(PolledNotifications.class)
class PolledNotificationServiceMessage extends BaseMessage {
}

class NotifyType extends PolledNotificationServiceMessage {
  String channel
  String details
}

class NotifyResponseType extends PolledNotificationServiceMessage {
}

class PollForNotificationType extends PolledNotificationServiceMessage {
  String channel
  Long timeout
}

class PollForNotificationResponseType extends PolledNotificationServiceMessage {
  Boolean notified
  String details
}
