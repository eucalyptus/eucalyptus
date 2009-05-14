package edu.ucsb.eucalyptus.msgs
/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2009, Eucalyptus Systems, Inc.
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Neil Soman neil@eucalyptus.com
 */
public class DNSResponseType extends EucalyptusMessage {
  def DNSResponseType() {}
}

public class DNSRequestType extends EucalyptusMessage {

  def DNSRequestType() {}
}

public class UpdateARecordType extends DNSRequestType {
  String zone;
  String name;
  String address;
  long ttl;

  def UpdateARecordType() {}

  def UpdateARecordType(String zone, String name, String address, long ttl) {
      this.zone = zone;
      this.name = name;
      this.address = address;
      this.ttl = ttl;
  }
}

public class UpdateARecordResponseType extends DNSResponseType {
  def UpdateARecordResponseType() {}
}

public class RemoveARecordType extends DNSRequestType {
  String zone;
  String name;

  def RemoveARecordType() {}

  def RemoveARecordType(String zone, String name) {
      this.zone = zone;
      this.name = name;
  }
}

public class RemoveARecordResponseType extends DNSResponseType {
  def RemoveARecordResponseType() {}
}

public class UpdateCNAMERecordType extends DNSRequestType {
  String zone;
  String name;
  String alias;
  long ttl;

  def UpdateCNAMERecordType() {}

  def UpdateCNAMERecordType(String zone, String name, String alias, long ttl) {
      this.zone = zone;
      this.name = name;
      this.alias = alias;
      this.ttl = ttl;
  }
}

public class UpdateCNAMERecordResponseType extends DNSResponseType {
  def UpdateCNAMERecordResponseType() {}
}

public class RemoveCNAMERecordType extends DNSRequestType {
  String zone;
  String name;

  def RemoveCNAMERecordType() {}

  def RemoveCNAMERecordType(String zone, String name) {
      this.zone = zone;
      this.name = name;
  }
}

public class RemoveCNAMERecordResponseType extends DNSResponseType {
  def RemoveCNAMERecordResponseType() {}
}

public class AddZoneType extends DNSRequestType {
  String name;
  def AddZoneType() {}
  def AddZoneType(String name) {
    this.name = name;
  }
}

public class AddZoneResponseType extends DNSResponseType {
}