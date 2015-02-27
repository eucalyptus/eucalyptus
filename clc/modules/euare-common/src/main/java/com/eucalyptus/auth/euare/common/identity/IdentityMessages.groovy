/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
@GroovyAddClassUUID
package com.eucalyptus.auth.euare.common.identity

import com.eucalyptus.component.annotation.ComponentMessage
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@ComponentMessage(Identity.class)
class IdentityMessage extends BaseMessage {
}

class DescribePrincipalType extends IdentityMessage {
  String accessKeyId
}

class DescribePrincipalResponseType extends IdentityMessage {
  DescribePrincipalResult describePrincipalResult
}

class DescribePrincipalResult extends EucalyptusData {
  Principal principal
}

class Principal extends EucalyptusData {
  String arn
  String userId
  String roleId
  String accountAlias
  String passwordHash
  ArrayList<AccessKey> accessKeys
  ArrayList<Certificate> certificates
}

class AccessKey extends EucalyptusData {
  String accessKeyId
  String secretAccessKey
}

class Certificate extends EucalyptusData {
  String certificateId
  String certificateBody
}
