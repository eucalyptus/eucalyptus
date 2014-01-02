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
 ************************************************************************/
package com.eucalyptus.images;

public enum ImportTaskState {
	NEW("active", "active", "pending"), PENDING("active", "active", "pending"),
	DOWNLOADING("active", "active", "pending"), DOWNLOADED("active", "active", "pending"),
	PRE_VALIDATION("active", "active", "pending"), PRE_VALIDATED("active", "active", "pending"),
	CONVERTING("active", "active", "converting"), CONVERTED("active", "completed", "converted"),
	STATIC_VALIDATION("active", "completed", "converted"), STATICLLY_VALIDATED("active", "completed", "converted"),
	UPLOADING("active", "completed", "converted"), UPLOADED("active", "completed", "converted"),
	DYNAMIC_VALIDATION("active", "completed", "converted"), DYNAMICALLY_VALIDATED("active",
			"completed", "converted"), DONE("completed", "completed", "converted"),
	CANSELING("cancelling", "cancelling", ""), CANSELED("cancelled", "cancelled", "");

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
