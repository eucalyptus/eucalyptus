/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common

import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

import java.util.ArrayList;
import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.Filter;
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
