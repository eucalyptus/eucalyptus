/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
//@GroovyAddClassUUID
package com.eucalyptus.loadbalancing.workflow;
import java.util.List;
import java.util.Set;
import com.eucalyptus.loadbalancing.common.msgs.Listener
import com.fasterxml.jackson.annotation.JsonAutoDetect

@JsonAutoDetect(isGetterVisibility = JsonAutoDetect.Visibility.NONE, 
  getterVisibility = JsonAutoDetect.Visibility.ANY)
public class SecurityGroupSetupActivityResult {
  String createdGroupName = null;
  String createdGroupId = null;
  String groupName = null;
  String groupId = null;
  String groupOwnerAccountId = null;
  boolean shouldRollback = true;
}

public class AuthorizeSSLCertificateActivityResult {
  String roleName = null;
  List<String> policyNames = null;
}

public class AuthorizeIngressRuleActivityResult {
  List<Listener> listeners = null;
}

public class CreateTagActivityResult {
  String tagKey = null;
  String tagValue = null;
  String securityGroup = null;
}

public class AutoscalingGroupSetupActivityResult {
  Set<String> groupNames = null;
  Set<String> launchConfigNames = null;
  Set<String> createdGroupNames = null;
  Set<String> createdLaunchConfigNames = null;
  Integer numVMsPerZone = null;
}

@JsonAutoDetect(isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.ANY)
public class AccessLogPolicyActivityResult {
  String roleName = null;
  String policyName = null;
  boolean shouldRollback = true;
}