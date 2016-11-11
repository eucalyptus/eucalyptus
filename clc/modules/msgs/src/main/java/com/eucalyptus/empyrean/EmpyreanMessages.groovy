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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.empyrean

import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.util.HasSideEffect
import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import groovy.transform.Canonical

public class AnonymousMessage extends BaseMessage implements Cloneable, Serializable {
}
@ComponentMessage(Empyrean.class)
public class EmpyreanMessage extends BaseMessage implements Cloneable, Serializable {
}
public class ModifyServiceType extends EmpyreanMessage {
  String name;
  String state;
}
public class ModifyServiceResponseType extends EmpyreanMessage implements HasSideEffect {
}
public class ServiceTransitionType extends EmpyreanMessage  {
  ArrayList<ServiceId> services = new ArrayList<ServiceId>();
}
public class StartServiceType extends ServiceTransitionType {
}
public class StartServiceResponseType extends ServiceTransitionType {
}
public class StopServiceType extends ServiceTransitionType {
}
public class StopServiceResponseType extends ServiceTransitionType {
}
public class DestroyServiceType extends ServiceTransitionType {
}
public class DestroyServiceResponseType extends ServiceTransitionType {
}
public class EnableServiceType extends ServiceTransitionType {
}
public class EnableServiceResponseType extends ServiceTransitionType implements HasSideEffect {
}
public class DisableServiceType extends ServiceTransitionType {
}
public class DisableServiceResponseType extends ServiceTransitionType implements HasSideEffect {
}
public class ServiceId extends EucalyptusData {
  String uuid;/** UUID of the registration **/
  String partition;/** The resource partition name **/
  String name;/** The registration name **/
  String type;/** name of the ComponentId **/
  String fullName;/** full name of the registration **/
  ArrayList<String> uris = new ArrayList<String>( );
  String uri;
  String host
  public String getUri( ) {
    return ( uris.isEmpty( ) ? "none" : uris.get( 0 ) );
  }
  public void setUri( String uri ) {
    this.uris.remove( uri );
    this.uris.add(0, uri);
    this.uri = uri;
    this.host = null
  }
  public void setServiceUri( URI serviceUri ) {
    String uri = serviceUri.toASCIIString( )
    this.uris.remove( uri );
    this.uris.add(0, uri);
    this.uri = uri;
    this.host = serviceUri.host
  }
}

// For better mapping account ARNs to user-readable names
public class ServiceAccount extends EucalyptusData {
  String accountName
  String accountNumber
  String accountCanonicalId

  public ServiceAccount(){
  }

  public ServiceAccount(String name, String number, String canonicalId) {
    this.accountName = name
    this.accountNumber = number
    this.accountCanonicalId = canonicalId
  }
}

public class ServiceStatusType extends EucalyptusData {
    ServiceId serviceId;
    String localState;/** one of DISABLED, PRIMORDIAL, INITIALIZED, LOADED, RUNNING, STOPPED, PAUSED **/
    Integer localEpoch;
    ArrayList<String> details = new ArrayList<String>( );
    ArrayList<ServiceStatusDetail> statusDetails = new ArrayList<ServiceStatusDetail>( );
    ArrayList<ServiceAccount> serviceAccounts = new ArrayList<ServiceAccount>();

    @Override
    public String toString( ) {
        return "${this.serviceId.fullName} ${this.localState} ${this.localEpoch} ${this.statusDetails} ${this.serviceAccounts}";
    }
}
public class ServiceStatusDetail extends EucalyptusData {
  String   severity;
  String   uuid;
  String   message;
  String   serviceFullName;
  String   serviceName;
  String   serviceHost;
  String   stackTrace;
  String   timestamp;
  @Override
  public String toString( ) {
    return "${this.timestamp} ${this.severity} ${this.serviceFullName} ${this.serviceName} ${this.serviceHost} ${this.message}";
  }
}
class Filter extends EucalyptusData {
  String name
  @HttpParameterMapping (parameter = "Value")
  ArrayList<String> values = Lists.newArrayList( )
}
public class DescribeServicesType extends ServiceTransitionType {
  Boolean listAll;
  Boolean listInternal;
  Boolean listUserServices;
  Boolean showEvents;
  Boolean showEventStacks;
  String byServiceType;
  String byHost;
  String byState;
  String byPartition;
  @HttpParameterMapping (parameter = "ServiceName")
  ArrayList<String> serviceNames = new ArrayList( );
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded(multiple = true)
  ArrayList<Filter> filters = Lists.newArrayList( )
}
public class DescribeServicesResponseType extends EmpyreanMessage {
  ArrayList<ServiceStatusType> serviceStatuses = new ArrayList<ServiceStatusType>( );
}

class DescribeServiceCertificatesType extends EmpyreanMessage {
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded(multiple = true)
  ArrayList<Filter> filters = Lists.newArrayList( )
  String format // pem / der (b64)
  String fingerprintDigest // 'SHA-1', 'SHA-224', 'SHA-256', 'SHA-384', 'SHA-512'
}

class DescribeServiceCertificatesResponseType extends EmpyreanMessage {
  ArrayList<ServiceCertificateType> serviceCertificates = Lists.newArrayList( );
}

class ServiceCertificateType extends EucalyptusData {
  String serviceType
  String certificateUsage
  String certificateFormat
  String certificate
  String certificateFingerprint
  String certificateFingerprintDigest
}
