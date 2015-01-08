/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

/**
 * Added 2010-11-15
 */
@GroovyAddClassUUID
package edu.ucsb.eucalyptus.msgs;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpEmbedded;

public class VmImportMessage extends EucalyptusMessage {
}
public class VmImportResponseMessage extends VmImportMessage {
}
/*********************************************************************************/
public class ImportInstanceType extends VmImportMessage {
  String description;
  ImportInstanceLaunchSpecification launchSpecification;
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "DiskImage")
  ArrayList<DiskImage> diskImageSet = new ArrayList<DiskImage>();
  Boolean keepPartialImports;
  String platform;
  public ImportInstance() {}
}
public class ImportInstanceResponseType extends VmImportResponseMessage {
  ConversionTask conversionTask;
  public ImportInstanceResponse() {}
}
public class ImportInstanceLaunchSpecification extends EucalyptusData {
  String architecture;
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "GroupName")
  ArrayList<String> groupName = new ArrayList<String>();
  @HttpEmbedded
  UserData userData;
  String instanceType;
  InstancePlacement placement;
  MonitoringInstance monitoring;
  String subnetId;
  String instanceInitiatedShutdownBehavior;
  String privateIpAddress;
  String keyName;
  public ImportInstanceLaunchSpecification() {}
}

public class DiskImage extends EucalyptusData {
  DiskImageDetail image;
  String description;
  DiskImageVolume volume;
  public DiskImage() {}
}
public class UserData extends EucalyptusData {
  @HttpParameterMapping( parameter="UserData" )
  String data;
  String version = "1.0"
  String encoding = "base64"
  public UserData() {}
}
public class InstancePlacement extends EucalyptusData {
  String availabilityZone;
  String groupName;
  public InstancePlacement() {}
}
public class MonitoringInstance extends EucalyptusData {
  Boolean enabled;
  public MonitoringInstance() {}
}
/*********************************************************************************/
public class DescribeConversionTasksType extends VmImportMessage {
  @HttpParameterMapping (parameter = "ConversionTaskId")
  ArrayList<String> conversionTaskIdSet = new ArrayList<String>();
  public DescribeConversionTasks() {}
}
public class DescribeConversionTasksResponseType extends VmImportResponseMessage {
  ArrayList<ConversionTask> conversionTasks = new ArrayList<ConversionTask>();
  public DescribeConversionTasksResponse() {}
}
/*********************************************************************************/
public class ImportVolumeType extends VmImportMessage {
  String availabilityZone;
  DiskImageDetail image;
  String description;
  DiskImageVolume volume;
  public ImportVolume() {}
}
public class ImportVolumeResponseType extends VmImportResponseMessage {
  ConversionTask conversionTask;
  public ImportVolumeResponse() {}
}
public class DiskImageDetail extends EucalyptusData {
  String format;
  Long bytes;
  String importManifestUrl;
  public DiskImageDetail() {}
}
public class DiskImageVolume extends EucalyptusData {
  Integer size;
  public DiskImageVolume() {}
}
/*********************************************************************************/
public class CancelConversionTaskType extends VmImportMessage {
  String conversionTaskId;
  public CancelConversionTask() {}
}
public class CancelConversionTaskResponseType extends VmImportResponseMessage {
  Boolean _return;
  public CancelConversionTaskResponse() {}
}
/*********************************************************************************/
public class ImportResourceTag extends EucalyptusData {
  String key;
  String value;
  public ImportResourceTag() {}
  public ImportResourceTag( String key, String value ) {
    this.key = key;
    this.value = value;
  }
  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("key", key);
    obj.put("value", value);
    return obj;
  }
  ImportResourceTag(JSONObject obj) {
	key = obj.optString("key");
	value = obj.optString("value");
  }
}

public class ConversionTask extends EucalyptusData {
  String conversionTaskId;
  String expirationTime;
  ImportVolumeTaskDetails importVolume;
  ImportInstanceTaskDetails importInstance;
  String state;
  String statusMessage;
  
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "ResourceTag")
  ArrayList<ImportResourceTag> resourceTagSet = new ArrayList<ImportResourceTag>();
  public ConversionTask() {}
  public ConversionTask(JSONObject obj) {
    conversionTaskId = obj.optString("conversionTaskId");
    expirationTime = obj.optString("expirationTime");
    JSONObject importDetails = obj.optJSONObject("importVolume");
    if (importDetails != null)
      importVolume = new ImportVolumeTaskDetails(importDetails);
    importDetails = obj.optJSONObject("importInstance");
    if (importDetails != null)
      importInstance = new ImportInstanceTaskDetails(importDetails);
    state = obj.optString("state", null);
    statusMessage = obj.optString("statusMessage", null);
    JSONArray arr = obj.optJSONArray("resourceTagSet");
    if (arr != null) {
      for(int i=0;i<arr.size(); i++)
        resourceTagSet.add(new ImportResourceTag(arr.getJSONObject(i)));
    } else {
      JSONObject res = obj.optJSONObject("resourceTagSet");
      if (res!=null)
        resourceTagSet.add(new ImportResourceTag(res));
    }
  }
    
  @Override
  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("conversionTaskId", conversionTaskId);
    obj.put("expirationTime", expirationTime);
	if (importVolume != null)
      obj.put("importVolume", importVolume.toJSON())
	if (importInstance != null)
      obj.put("importInstance", importInstance.toJSON())
    obj.put("state", state);
    obj.put("statusMessage", statusMessage);
    for(ImportResourceTag tag:resourceTagSet)
      obj.accumulate("resourceTagSet", tag.toJSON());
    return obj;
  }
}

public class ImportVolumeTaskDetails extends EucalyptusData {
  Long bytesConverted;
  String availabilityZone;
  String description;
  DiskImageDescription image;
  DiskImageVolumeDescription volume;
  public ImportVolumeTaskDetails() {}
  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("bytesConverted", bytesConverted);
    obj.put("availabilityZone", availabilityZone);
    obj.put("description", description);
	if (image != null)
      obj.put("image", image.toJSON());
	if (volume != null)
      obj.put("volume", volume.toJSON());
    return obj;
  }
  public ImportVolumeTaskDetails(JSONObject obj) {
    if (obj != null) {
	  bytesConverted = obj.optLong("bytesConverted");
	  availabilityZone = obj.optString("availabilityZone", null);
	  description = obj.optString("description", null);
	  JSONObject diskDescription = obj.optJSONObject("image");
	  if (diskDescription != null)
		image = new DiskImageDescription(diskDescription);
	  diskDescription = obj.optJSONObject("volume");
	  if (diskDescription != null)
		volume = new DiskImageVolumeDescription(diskDescription);
    }
  }
}

public class ImportInstanceTaskDetails extends EucalyptusData {
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "Volume")
  ArrayList<ImportInstanceVolumeDetail> volumes = new ArrayList<ImportInstanceVolumeDetail>();
  String instanceId;
  String platform;
  String description;
  public ImportInstanceTaskDetails() {}
  public ImportInstanceTaskDetails( String instanceId, String platform, String description, ArrayList<ImportInstanceVolumeDetail> volumes ) {
    this.volumes = volumes;
    this.instanceId = instanceId;
    this.platform = platform;
    this.description = description;
  }
  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
	for(ImportInstanceVolumeDetail vol:volumes)
      obj.accumulate("volumes", vol.toJSON());
    obj.put("instanceId", instanceId);
    obj.put("platform", platform);
	obj.put("description", description);
    return obj;
  }
  public ImportInstanceTaskDetails(JSONObject obj) {
    if (obj != null){
	  description = obj.optString("description", null);
	  instanceId = obj.optString("instanceId", null);
	  platform = obj.optString("platform", null);
	  JSONArray arr = obj.optJSONArray("volumes");
	  if (arr != null) {
	    for(int i=0;i<arr.size(); i++)
		  volumes.add(new ImportInstanceVolumeDetail(arr.getJSONObject(i)));
	  } else {
	    JSONObject vol = obj.optJSONObject("volumes");
		if (vol!=null)
		  volumes.add(new ImportInstanceVolumeDetail(vol));
	  }
    }
  }
}

public class ImportInstanceVolumeDetail extends EucalyptusData {
  Long bytesConverted;
  String availabilityZone;
  DiskImageDescription image;
  String description;
  DiskImageVolumeDescription volume;
  String status;
  String statusMessage;
  public ImportInstanceVolumeDetail() {}
  public ImportInstanceVolumeDetail( String status, String statusMessage, Long bytesConverted, String availabilityZone, 
      String description, DiskImageDescription image, DiskImageVolumeDescription volume) {
    this.bytesConverted = bytesConverted;
    this.availabilityZone = availabilityZone;
    this.description = description;
    this.status = status;
    this.statusMessage = statusMessage;
    this.image = image;
    this.volume = volume;
  }
  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("bytesConverted", bytesConverted);
    obj.put("availabilityZone", availabilityZone);
	obj.put("image", image.toJSON());
	obj.put("description", description);
	obj.put("volume", volume.toJSON());
	obj.put("status", status);
	obj.put("statusMessage", statusMessage);
    return obj;
  }
  public ImportInstanceVolumeDetail(JSONObject obj) {
    if (obj != null){
	  bytesConverted = obj.optLong("bytesConverted");
	  availabilityZone = obj.optString("availabilityZone", null);
	  description = obj.optString("description", null);
	  JSONObject diskDescription = obj.optJSONObject("image");
	  if (diskDescription != null)
		image = new DiskImageDescription(diskDescription);
	  diskDescription = obj.optJSONObject("volume");
	  if (diskDescription != null)
		volume = new DiskImageVolumeDescription(diskDescription);
	  status = obj.optString("status", null);
	  statusMessage = obj.optString("statusMessage", null);
    }
  }
}

public class DiskImageDescription extends EucalyptusData {
  String format;
  Long size;
  String importManifestUrl;
  String checksum;
  public DiskImageDescription() {}
  public DiskImageDescription( String format, Long size, String importManifestUrl, String checksum ) {
    this.format = format;
    this.size = size;
    this.importManifestUrl = importManifestUrl;
    this.checksum = checksum;
  }
  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("format", format);
    obj.put("size", size);
    obj.put("importManifestUrl", importManifestUrl);
    obj.put("checksum", checksum);
    return obj;
  }
  public DiskImageDescription(JSONObject obj) {
    if (obj != null) {
      format = obj.optString("format", null);
	  size = obj.optLong("size");
	  importManifestUrl = obj.optString("importManifestUrl", null);
	  checksum = obj.optString("checksum", null);
    }
  }
}

public class DiskImageVolumeDescription extends EucalyptusData {
  Integer size;
  String id;
  public DiskImageVolumeDescription() {}
  public DiskImageVolumeDescription( Integer size, String id ) {
    this.size = size;
    this.id = id;
  }
  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("size", size);
    obj.put("id", id);
    return obj;
  }
  public DiskImageVolumeDescription(JSONObject obj) {
    if (obj != null) {
	  size = obj.optInt("size");
	  id = obj.optString("id", null);
    }
  }
}