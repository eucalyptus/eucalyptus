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
  NEW("active", "active", "pending"),
  PENDING("active","active","pending"),
  CONVERTING("active", "active", "active"),
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
}
