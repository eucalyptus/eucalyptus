/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
@GroovyAddClassUUID
package com.eucalyptus.cluster.common.msgs

import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

/**
 *
 */
class CloudGatherLogMessage extends CloudClusterMessage {

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


