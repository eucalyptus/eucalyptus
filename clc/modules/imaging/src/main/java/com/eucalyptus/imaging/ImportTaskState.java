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
 ************************************************************************/
package com.eucalyptus.imaging;

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

	public static ImportTaskState fromString(String input) {
		if (input == null)
			throw new IllegalArgumentException("Input can't be null");
		return ImportTaskState.valueOf(input.toUpperCase().replaceAll("-", "_"));
	}

	public boolean validTransition(ImportTaskState newState) {
		// there is one valid back transition
		if (newState == NEW && this == PENDING)
			return true;
		return newState.ordinal() > this.ordinal();
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
