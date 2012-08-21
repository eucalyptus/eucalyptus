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
 ************************************************************************/

package com.eucalyptus.reporting.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import com.eucalyptus.event.Event;
import com.eucalyptus.util.OwnerFullName;

@SuppressWarnings("serial")
public class S3BucketEvent implements Event {

    public enum S3BucketAction {
	BUCKETCREATE, BUCKETDELETE
    }

    private final EventActionInfo<S3BucketAction> actionInfo;
    private final OwnerFullName ownerFullName;
    private final String uuid;
    private final Long size;
    private final String bucketName;

    public static EventActionInfo<S3BucketAction> forS3BucketCreate() {
	return new EventActionInfo<S3BucketAction>(S3BucketAction.BUCKETCREATE);
    }

    public static EventActionInfo<S3BucketAction> forS3BucketDelete() {
	return new EventActionInfo<S3BucketAction>(S3BucketAction.BUCKETDELETE);
    }

    public static S3BucketEvent with(
	    final EventActionInfo<S3BucketAction> actionInfo,
	    final String s3UUID, final OwnerFullName ownerFullName,
	    final Long size, final String bucketName) {

	return new S3BucketEvent(actionInfo, s3UUID, ownerFullName, size,
		bucketName);
    }

    private S3BucketEvent(final EventActionInfo<S3BucketAction> actionInfo,
	    final String uuid, final OwnerFullName ownerFullName,
	    final Long size, final String bucketName) {
	assertThat(actionInfo, notNullValue());
	assertThat(uuid, notNullValue());
	assertThat(ownerFullName.getUserId(), notNullValue());
	assertThat(size, notNullValue());
	assertThat(bucketName, notNullValue());

	this.actionInfo = actionInfo;
	this.ownerFullName = ownerFullName;
	this.uuid = uuid;
	this.size = size;
	this.bucketName = bucketName;

    }

    public EventActionInfo<S3BucketAction> getActionInfo() {
	return actionInfo;
    }

    public OwnerFullName getOwner() {
	return ownerFullName;
    }

    public String getUuid() {
	return uuid;
    }

    public Long getSize() {
	return size;
    }

    public String getBucketName() {
	return bucketName;
    }

    @Override
    public String toString() {
	return "S3BucketEvent [actionInfo=" + actionInfo + ", userId="
		+ ownerFullName.getUserId() + ", uuid=" + uuid + ", size="
		+ size + ", bucketName=" + bucketName + "]";
    }

}
