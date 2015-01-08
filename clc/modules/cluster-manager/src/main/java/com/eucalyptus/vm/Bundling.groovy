/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.vm

import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

import java.util.ArrayList;
import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.Filter;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.vm.VmBundleTask.BundleState
import com.eucalyptus.binding.HttpEmbedded;

public class VmBundleMessage extends EucalyptusMessage {
  
  public VmBundleMessage( ) {
    super( );
  }
  
  public VmBundleMessage( EucalyptusMessage msg ) {
    super( msg );
  }
  
  public VmBundleMessage( String userId ) {
    super( userId );
  }
}

public class BundleInstanceType extends VmBundleMessage {
  String instanceId;
  @HttpParameterMapping(parameter="Storage.S3.Bucket")
  String bucket;
  @HttpParameterMapping(parameter="Storage.S3.Prefix")
  String prefix;
  @HttpParameterMapping(parameter="Storage.S3.AWSAccessKeyId")
  String awsAccessKeyId;
  @HttpParameterMapping(parameter="Storage.S3.UploadPolicy")
  String uploadPolicy;
  @HttpParameterMapping(parameter="Storage.S3.UploadPolicySignature")
  String uploadPolicySignature;
  String url;
  String userKey;
}
public class BundleInstanceResponseType extends VmBundleMessage {
  BundleTask task;
}

public class BundleRestartInstanceType extends VmBundleMessage {
  String instanceId;
}
public class BundleRestartInstanceResponseType extends VmBundleMessage {
  BundleTask task;
}

public class CancelBundleTaskType extends VmBundleMessage {
  String bundleId;
  String instanceId;
}
public class CancelBundleTaskResponseType extends VmBundleMessage {
  BundleTask task;
}
public class BundleTaskState extends EucalyptusData {
  String instanceId;
  String state;
}
public class DescribeBundleTasksType extends VmBundleMessage {
  @HttpParameterMapping (parameter = "BundleId")
  ArrayList<String> bundleIds = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeBundleTasksResponseType extends VmBundleMessage {
  ArrayList<BundleTask> bundleTasks = new ArrayList<BundleTask>();
  ArrayList<BundleTaskState> bundleTaskStates = new ArrayList<BundleTaskState>();
}

public class BundleTask extends EucalyptusData {
  String instanceId;
  String bundleId;
  String state;
  Date startTime;
  Date updateTime;
  String progress;
  String bucket;
  String prefix;
  String errorMessage;
  String errorCode;
  public BundleTask() {
  }
  BundleTask( String bundleId, String instanceId, String bucket, String prefix ) {
    this.bundleId = bundleId;
    this.instanceId = instanceId;
    this.bucket = bucket;
    this.prefix = prefix;
    this.state = BundleState.pending.name( );
    this.startTime = new Date();
    this.updateTime = new Date();
    this.progress = "0%";
  }
  BundleTask( String instanceId, String bundleId, String state, Date startTime, Date updateTime, String progress, String bucket, String prefix,
  String errorMessage, String errorCode ) {
    super( );
    this.instanceId = instanceId;
    this.bundleId = bundleId;
    this.state = state;
    this.startTime = startTime;
    this.updateTime = updateTime;
    this.progress = progress;
    this.bucket = bucket;
    this.prefix = prefix;
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
  }
}
