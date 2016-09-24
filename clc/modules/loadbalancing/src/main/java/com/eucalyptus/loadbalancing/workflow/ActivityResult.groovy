/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
//@GroovyAddClassUUID
package com.eucalyptus.loadbalancing.workflow;
import java.util.List;
import java.util.Set;
import com.eucalyptus.loadbalancing.common.msgs.Listener
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.google.common.collect.Lists;;

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

public class AccessLogPolicyActivityResult {
  String roleName = null;
  String policyName = null;
  boolean shouldRollback = true;
}