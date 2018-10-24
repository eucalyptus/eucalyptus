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
package com.eucalyptus.imaging.backend;

public enum ImportTaskState {
  NEW("active", "active", "pending"),  // a task is accepted
  PENDING("active","active","pending"), // waiting for processing by worker
  CONVERTING("active", "active", "active"), // being processed by worker
  INSTANTIATING("active", "active", "active"), // image registration and launch for import-instance
  CANCELLING("cancelling", "cancelling", ""), 
  CANCELLED("cancelled", "cancelled", ""),
  COMPLETED("completed", "completed", ""),
  FAILED("cancelled", "cancelled", "failed (internal server error)");
  
  
	private final String externalTaskStateName;
	private final String externalVolumeStateName;
	private final String externalVolumeStatusMessage;

	private ImportTaskState(String externalTaskState, String externalVolumeState,
			String externalVolumeStatus) {
		this.externalTaskStateName = externalTaskState;
		this.externalVolumeStateName = externalVolumeState;
		this.externalVolumeStatusMessage = externalVolumeStatus;
	}

	public String getExternalTaskStateName() {
		return externalTaskStateName;
	}

	public String getExternalVolumeStateName() {
		return externalVolumeStateName;
	}

	public String getExternalVolumeStatusMessage() {
		return externalVolumeStatusMessage;
	}
	
	public static final String STATE_MSG_DONE = "Import task finished successfully";
	public static final String STATE_MSG_FAILED_UNEXPECTED = "Import task failed unexpectedly";
	public static final String STATE_MSG_TASK_INSUFFICIENT_PARAMETERS = "Import task does not have sufficient parameters";
	public static final String STATE_MSG_TASK_EXPIRED = "Import task is expired";
	public static final String STATE_MSG_RUN_FAILURE = "Failed to run instances";
	public static final String STATE_MSG_SNAPSHOT_FAILURE = "Failed to create snapshot";
	public static final String STATE_MSG_REGISTER_FAILURE = "Failed to register image";
	public static final String STATE_MSG_USER_CANCELLATION = "Cancelled by user";
	public static final String STATE_MSG_PENDING_UPLOAD = "Pending import-image upload";
	public static final String STATE_MSG_PENDING_CONVERSION = "Pending conversion";
	public static final String STATE_MSG_IN_CONVERSION = "Converting images";
	public static final String STATE_MSG_CONVERSION_TIMEOUT = "Image conversion timed out";
	public static final String STATE_MSG_LAUNCHING_INSTANCE = "Launching instance";
	public static final String STATE_MSG_CONVERSION_FAILED = "Image conversion failed";
	public static final String STATE_MSG_DOWNLOAD_MANIFEST = "Failed to generate download manifest";
	public static final String STATE_MSG_CREATING_VOLUME = "Creating volumes";
}
