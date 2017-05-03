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
package com.eucalyptus.cluster.common.msgs

import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

/**
 *
 */
class CloudGatherLogMessage extends EucalyptusMessage {

  CloudGatherLogMessage() {
    super();
  }

  CloudGatherLogMessage(EucalyptusMessage msg) {
    super(msg);
  }

  CloudGatherLogMessage(String userId) {
    super(userId);
  }
}

class NodeLogInfo extends EucalyptusData implements Comparable {
  String serviceTag;
  String ccLog = "";
  String ncLog = "";
  String httpdLog = "";
  String axis2Log = "";

  int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeLogInfo) o).serviceTag);
  }
}

class GetLogsType extends CloudGatherLogMessage implements Comparable {
  String serviceTag;

  GetLogsType() {
  }

  GetLogsType(final serviceTag) {
    this.serviceTag = serviceTag;
  }

  int compareTo(Object o) {
    return this.serviceTag.compareTo(((GetLogsType) o).serviceTag);
  }
}

class GetLogsResponseType extends CloudGatherLogMessage {
  NodeLogInfo logs = new NodeLogInfo();
}

class NodeCertInfo extends EucalyptusData implements Comparable {
  String serviceTag;
  String ccCert = "";
  String ncCert = "";

  int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeCertInfo) o).serviceTag);
  }

  @Override
  String toString() {
    return "NodeCertInfo [" +
        "serviceTag='" + serviceTag.replaceAll("services/EucalyptusNC", "") + '\'' +
        ", ccCert='" + ccCert + '\'' +
        ", ncCert='" + ncCert + '\'' +
        ']';
  }
}

class GetKeysType extends CloudGatherLogMessage implements Comparable {
  String serviceTag;

  GetKeysType() {
  }

  GetKeysType(final serviceTag) {
    this.serviceTag = serviceTag;
  }

  int compareTo(Object o) {
    return this.serviceTag.compareTo(((GetKeysType) o).serviceTag);
  }
}

class GetKeysResponseType extends CloudGatherLogMessage {
  NodeCertInfo certs = new NodeCertInfo();
}


