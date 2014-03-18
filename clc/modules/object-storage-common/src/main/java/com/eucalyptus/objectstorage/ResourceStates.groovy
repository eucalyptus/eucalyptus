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

package com.eucalyptus.objectstorage

public enum BucketState {
    creating, extant, deleting
}

/**
 * creating - The metadata is in a transitional state. The resource is being created. Records in this state for too long
 * may be interpreted as failed and GCd. Objects in this state are not part of the user-visible system yet.
 * mpu_pending - This object is a Multipart-Upload with a valid uploadId but not finalized or committed yet. No data, but
 * this is a user-visible entity: a valid UploadId.
 * extant - The object and its data, exists and is part of the system.
 * deleting - The object is logically ready for removal. It may be GCd at any time in the future.
 */
public enum ObjectState {
    creating, mpu_pending, extant, deleting
}
