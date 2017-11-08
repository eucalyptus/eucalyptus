/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common

import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpEmbedded;

public class VmBundleMessage extends ComputeMessage {
  
  public VmBundleMessage( ) {
    super( );
  }
  
  public VmBundleMessage( ComputeMessage msg ) {
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
}
public class BundleInstanceResponseType extends VmBundleMessage {
  BundleTask task;
}

public class CancelBundleTaskType extends VmBundleMessage {
  String bundleId;
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
    this.state = "pending";
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
