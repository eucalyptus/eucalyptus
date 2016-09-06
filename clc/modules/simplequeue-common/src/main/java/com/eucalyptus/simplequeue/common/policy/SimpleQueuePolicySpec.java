/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simplequeue.common.policy;

/**
 *
 */
public interface SimpleQueuePolicySpec {

  String VENDOR_SIMPLEQUEUE = "sqs"; // not a mistake, sqs is the vendor

  //Simple Queue actions, based on WSDL (http://queue.amazonaws.com/doc/2012-11-05/QueueService.wsdl)
  String SIMPLEQUEUE_ADDPERMISSION = "addpermission";
  String SIMPLEQUEUE_CHANGEMESSAGEVISIBILITY = "changemessagevisibility";
  String SIMPLEQUEUE_CHANGEMESSAGEVISIBILITYBATCH = "changemessagevisibilitybatch";
  String SIMPLEQUEUE_CREATEQUEUE = "createqueue";
  String SIMPLEQUEUE_DELETEMESSAGE = "deletemessage";
  String SIMPLEQUEUE_DELETEMESSAGEBATCH = "deletemessagebatch";
  String SIMPLEQUEUE_DELETEQUEUE = "deletequeue";
  String SIMPLEQUEUE_GETQUEUEATTRIBUTES = "getqueueattributes";
  String SIMPLEQUEUE_GETQUEUEURL = "getqueueurl";
  String SIMPLEQUEUE_LISTDEADLETTERSOURCEQUEUES = "listdeadlettersourcequeues";
  String SIMPLEQUEUE_LISTQUEUES = "listqueues";
  String SIMPLEQUEUE_RECEIVEMESSAGE = "receivemessage";
  String SIMPLEQUEUE_REMOVEPERMISSION = "removepermission";
  String SIMPLEQUEUE_SENDMESSAGE = "sendmessage";
  String SIMPLEQUEUE_SENDMESSAGEBATCH = "sendmessagebatch";
  String SIMPLEQUEUE_SETQUEUEATTRIBUTES = "setqueueattributes";
}
