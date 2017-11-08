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
  String RESOURCE_TYPE ="queue"; // Placeholder for ARN (arn has no type)
}
