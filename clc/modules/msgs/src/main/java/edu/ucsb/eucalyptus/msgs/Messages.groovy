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
package edu.ucsb.eucalyptus.msgs;

import org.jboss.netty.handler.codec.http.HttpResponseStatus

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.component.id.ComponentService
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.system.Threads
import com.eucalyptus.util.Exceptions

import edu.ucsb.eucalyptus.cloud.VirtualBootRecord
import com.eucalyptus.binding.HttpEmbedded


public class ComponentType extends EucalyptusData {
  String component;
  String name;
  String uri;
  public ComponentType( String component, String name, String uri ) {
    this.component = component;
    this.name = name;
    this.uri = uri;
  }
  public ComponentType( ) {
  }
}

public class ComponentProperty extends EucalyptusData {
  private String type;
  private String displayName;
  private String value;
  private String qualifiedName;
 
  public ComponentProperty() {}
 
  public ComponentProperty(String type, String displayName, String value, String qualifiedName) {
    this.type = type;
    this.displayName = displayName;
    this.value = value;
    this.qualifiedName = qualifiedName;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public String getQualifiedName() {
    return qualifiedName;
  }
  public void setQualifiedName(String qualifiedName) {
    this.qualifiedName = qualifiedName;
  }
  public String getDisplayName() {
    return displayName;
  }
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }
}

public class StorageStateType extends BaseMessage{
  private String name;
  def StorageStateType() {
  }
  
  def StorageStateType(final name) {
    this.name = name;
  }
}

public class WalrusStateType extends BaseMessage{
  private String name;
  
  def WalrusStateType() {
  }
  
  def WalrusStateType(final name) {
    this.name = name;
  }
}

/**
 * GRZE:WARN: anything inheriting from this is (and /should be/) treated as in the 'ec2' vendor namespace as far as the IAM implementation is concerned. 
 * There is no reason to annotate /any/ message which inherits from this class:
 * - to get vendor namespace use ComponentId.getVendorName()
 * - to get action use 
 */
@ComponentMessage(Eucalyptus.class)
public class EucalyptusMessage extends BaseMessage implements Cloneable, Serializable {
  
  public EucalyptusMessage() {
    super();
  }
  
  public EucalyptusMessage( EucalyptusMessage msg ) {
    this();
    regarding(msg);
    regardingUserRequest(msg);
    this.userId = msg.userId;
    this.effectiveUserId = msg.effectiveUserId;
    this.correlationId = msg.correlationId;
  }
  
  public  EucalyptusMessage(final String userId) {
    this();
    this.userId = userId;
    this.effectiveUserId = userId;
  }
  
  public MetaClass getMetaClass() {
    return metaClass;
  }
}

@ComponentMessage(Eucalyptus.class)
public class ExceptionResponseType extends BaseMessage {
  String source = "not available";
  String message = "not available";
  String requestType = "not available";
  Throwable exception;
  String error = "not available";
  HttpResponseStatus httpStatus = HttpResponseStatus.BAD_REQUEST;
  public ExceptionResponseType() {
  }
  public ExceptionResponseType( BaseMessage msg, String message, Throwable exception ) {
    this( msg, message, HttpResponseStatus.BAD_REQUEST, exception );
  }
  public ExceptionResponseType( BaseMessage msg, String message, HttpResponseStatus httpStatus, Throwable exception ) {
    this( msg, msg?.getClass()?.getSimpleName(), message, httpStatus, exception )
  }
  public ExceptionResponseType( BaseMessage msg, String requestType, String message, HttpResponseStatus httpStatus, Throwable exception ) {
    super( msg );
    this.httpStatus = httpStatus;
    this.source = exception.getClass( ).getCanonicalName( );
    this.message = message?:exception.getMessage()?:exception.getClass()
    this.requestType = requestType
    this.exception = exception;
    if( this.exception != null ) {
      this.error = Exceptions.string( exception );
    } else {
      this.error = Threads.currentStackString( );
    }
    this.set_return(false);
  }
}

public class EucalyptusErrorMessageType extends EucalyptusMessage {
  
  String source;
  String message;
  String requestType = "not available";
  Throwable exception;
  
  public EucalyptusErrorMessageType() {
  }
  
  public EucalyptusErrorMessageType(String source, String message) {
    this.source = source;
    this.message = message;
  }
  
  public EucalyptusErrorMessageType(String source, BaseMessage msg, String message) {
    this(source, message);
    regardingUserRequest( msg );
    this.requestType = msg != null ? msg.getClass().getSimpleName() : this.requestType;
  }
  
  public String toString() {
    return String.format("SERVICE: %s PROBLEM: %s MSG-TYPE: %s", this.source, this.message, this.requestType);
  }
}

public class EucalyptusData implements BaseData {
  public MetaClass getMetaClass() {
    return metaClass;
  }
  
  public String toString() {
    return this.getProperties().toMapString();
  }
  
  public Object clone(){
    return super.clone();
  }
}
/** *******************************************************************************/

public class VmTypeInfo extends EucalyptusData implements Cloneable {
  String name;
  Integer memory;
  Integer disk;
  Integer cores;
  String rootDeviceName;
  @HttpEmbedded(multiple=true)
  ArrayList<VirtualBootRecord> virtualBootRecord = new ArrayList<VirtualBootRecord>();
  def VmTypeInfo(){
  }

  def VmTypeInfo(String name, Integer memory, Integer disk, Integer cores, String rootDevice ) {
    this.name = name;
    this.memory = memory;
    this.disk = disk;
    this.cores = cores;
    this.rootDeviceName = rootDevice;
  }

  public VmTypeInfo child( ) {
    VmTypeInfo child = new VmTypeInfo( this.name, this.memory, this.disk, this.cores, this.rootDeviceName );
    child.virtualBootRecord.addAll( this.virtualBootRecord.collect{ VirtualBootRecord it -> it.clone() } );
    return child;
  }

  @Override
  public String toString() {
    return "VmTypeInfo ${name} mem=${memory} disk=${disk} cores=${cores}";
  }

  public String dump() {
    StringBuilder sb = new StringBuilder();
    sb.append("VmTypeInfo ${name} mem=${memory} disk=${disk} cores=${cores} rootDeviceName=${rootDeviceName} ");
    for (VirtualBootRecord vbr : this.virtualBootRecord) {
      sb.append("{VirtualBootRecord deviceName=").append(vbr.getGuestDeviceName())
          .append(" resourceLocation=").append(vbr.resourceLocation)
          .append(" size=").append(vbr.size).append("} ");
    }
    return sb.toString();
  }

  public void setEbsRoot( String imageId, String iqn, Long sizeBytes ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, size : sizeBytes, resourceLocation : "${iqn}", guestDeviceName : this.rootDeviceName, type : "ebs" ) );//TODO:GRZE: folow up on the iqn://
  }

  public void setRoot( String imageId, String location, Long sizeBytes ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, size : sizeBytes, resourceLocation : location, guestDeviceName : this.rootDeviceName, type : "machine" ) );
  }

  public void setKernel( String imageId, String location, Long sizeBytes ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, size : sizeBytes, resourceLocation : location, type : "kernel" ) );
  }

  public void setRamdisk( String imageId, String location, Long sizeBytes ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, size : sizeBytes, resourceLocation : location, type : "ramdisk" ) );
  }

  protected void setSwap( String deviceName, Long sizeBytes ) {
    this.virtualBootRecord.add( new VirtualBootRecord( guestDeviceName : deviceName, size : sizeBytes, type : "swap", format : "swap" ) );
  }

  public void setEphemeral( Integer index, String deviceName, Long sizeBytes, String formatName ) {
    this.virtualBootRecord.add( new VirtualBootRecord( guestDeviceName : deviceName, size : sizeBytes, type : "ephemeral", format : formatName ) );
  }

  public VirtualBootRecord lookupRoot( ) throws NoSuchElementException {
    VirtualBootRecord ret;
    if (( ret = this.virtualBootRecord.find{ VirtualBootRecord vbr -> vbr.type == "machine" })==null ) {
      ret = this.virtualBootRecord.find{ VirtualBootRecord vbr -> vbr.type == "ebs" && ( vbr.guestDeviceName == this.rootDeviceName || vbr.guestDeviceName == "xvda" ) };
    }
    if( ret != null ) {
      return ret;
    } else {
      throw new NoSuchElementException( "Failed to find virtual boot record of type machine among: " + this.virtualBootRecord.collect{it.dump()}.toString() );
    }
  }
  public VirtualBootRecord lookupKernel( ) throws NoSuchElementException {
    VirtualBootRecord ret;
    if (( ret = this.virtualBootRecord.find{ VirtualBootRecord vbr -> vbr.type == "kernel" })==null ) {
      throw new NoSuchElementException( "Failed to find virtual boot record of type kernel among: " + this.virtualBootRecord.collect{it.dump()}.toString() );
    } else {
      return ret;
    }
  }
  public VirtualBootRecord lookupRamdisk( ) throws NoSuchElementException {
    VirtualBootRecord ret;
    if (( ret = this.virtualBootRecord.find{ VirtualBootRecord vbr -> vbr.type == "ramdisk" })==null ) {
      throw new NoSuchElementException( "Failed to find virtual boot record of type ramdisk among: " + this.virtualBootRecord.collect{it.dump()}.toString() );
    } else {
      return ret;
    }
  }
}

public class CloudGatherLogMessage extends EucalyptusMessage {

  public CloudGatherLogMessage( ) {
    super( );
  }

  public CloudGatherLogMessage( EucalyptusMessage msg ) {
    super( msg );
  }

  public CloudGatherLogMessage( String userId ) {
    super( userId );
  }
}

public class GetLogsType extends CloudGatherLogMessage implements Comparable {
  String serviceTag;
  def GetLogsType(){
  }
  def GetLogsType(final serviceTag) {
    this.serviceTag = serviceTag;
  }
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((GetLogsType)o).serviceTag);
  }
}
public class GetLogsResponseType extends CloudGatherLogMessage {
  NodeLogInfo logs = new NodeLogInfo();
}
public class GetKeysType extends CloudGatherLogMessage implements Comparable {
  String serviceTag;
  def GetKeysType(){
  }
  def GetKeysType(final serviceTag) {
    this.serviceTag = serviceTag;
  }
  
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((GetKeysType)o).serviceTag);
  }
}
public class GetKeysResponseType extends CloudGatherLogMessage {
  NodeCertInfo certs = new NodeCertInfo();
}

public class NodeCertInfo extends EucalyptusData implements Comparable {
  String serviceTag;
  String ccCert = "";
  String ncCert = "";
  
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeCertInfo)o).serviceTag);
  }
  
  @Override
  public String toString() {
    return "NodeCertInfo [" +
    "serviceTag='" + serviceTag.replaceAll("services/EucalyptusNC","") + '\'' +
    ", ccCert='" + ccCert + '\'' +
    ", ncCert='" + ncCert + '\'' +
    ']';
  }
}

public class NodeLogInfo extends EucalyptusData implements Comparable {
  String serviceTag;
  String ccLog = "";
  String ncLog = "";
  String httpdLog = "";
  String axis2Log = "";
  
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeLogInfo)o).serviceTag);
  }
}

public class StatEventRecord extends BaseMessage {
  
  protected String service = "Eucalyptus";
  protected String version = "Unknown";
  
  def StatEventRecord(final String service, final String version) {
    this.service = service;
    this.version = version;
  }
  
  def StatEventRecord() {
  }
  
  public String toString() {
    return String.format("%s", this.service);
  }
}

@ComponentMessage(ComponentService.class)
public class ComponentMessageType extends BaseMessage {
  String component;
  String host;
  String name;
  
  def ComponentMessageType() {
  }
  
  def ComponentMessageType(String component) {
    this.component = component;
  }
}

public class ComponentMessageResponseType extends BaseMessage {
  def ComponentMessageResponseType() {
  }
}

public class ErrorDetail extends EucalyptusData {
  String type
  Integer code
  String message
  String stackTrace
  public ErrorDetail() {  }
}

@ComponentMessage(Eucalyptus.class) // not strictly correct as this is used for other components also
public class ErrorResponse extends BaseMessage {
  String requestId
  ArrayList<ErrorDetail> error = new ArrayList<ErrorDetail>( )

  ErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.getAt(0)?.type} error (${error?.getAt(0)?.code}): ${error?.getAt(0)?.message}"
  }
}

