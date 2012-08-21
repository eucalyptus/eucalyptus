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
import com.eucalyptus.reporting.event.EventActionInfo;
import com.eucalyptus.util.OwnerFullName;

@SuppressWarnings("serial")
public class S3ObjectEvent implements Event  {

    public enum S3ObjectAction {
	OBJECTGET, OBJECTCREATE, OBJECTDELETE
    }

	  private final EventActionInfo<S3ObjectAction> actionInfo;
	  private final OwnerFullName ownerFullName;
	  private final String uuid;
	  private final Long size;
	  private final String bucketName;
	  private final String objectName;
	  
	  public static EventActionInfo<S3ObjectAction> forS3ObjectPut() {
	      return new EventActionInfo<S3ObjectAction>( S3ObjectAction.OBJECTCREATE);
	  }

	  public static EventActionInfo<S3ObjectAction> forS3ObjectGet() {
	    return new EventActionInfo<S3ObjectAction>(S3ObjectAction.OBJECTGET);
	  }
	  
	  public static EventActionInfo<S3ObjectAction> forS3ObjectDelete() {
            return new EventActionInfo<S3ObjectAction>(S3ObjectAction.OBJECTDELETE);
          }
		  
	  
	  public static S3ObjectEvent with( final EventActionInfo<S3ObjectAction> actionInfo,
	                                    final String s3UUID,
	                                    final OwnerFullName ownerFullName,
	                                    final Long size,
	                                    final String bucketName,
	                                    final String objectName) {

	    return new S3ObjectEvent( actionInfo, s3UUID, ownerFullName, size, bucketName, objectName );
	  }

	  private S3ObjectEvent( final EventActionInfo<S3ObjectAction> actionInfo,
	                         final String uuid,
	                         final OwnerFullName ownerFullName,
	                         final Long size,
	                         final String bucketName,
	                         final String objectName) {
	    assertThat(actionInfo, notNullValue());
	    assertThat(uuid, notNullValue());
	    assertThat(ownerFullName.getUserId(), notNullValue());
	    assertThat(size, notNullValue());
	    assertThat(bucketName, notNullValue());
	    assertThat(objectName, notNullValue());
	    this.actionInfo = actionInfo;
	    this.ownerFullName = ownerFullName;
	    this.uuid = uuid;
	    this.size = size;
	    this.bucketName = bucketName;
	    this.objectName = objectName;
	    
	  }
	  
	public EventActionInfo<S3ObjectAction> getActionInfo() {
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

	public String getObjectName() {
	    return objectName;
	}

	@Override
	public String toString() {
	    return "S3ObjectEvent [actionInfo=" + actionInfo
		    + ", ownerFullName=" + ownerFullName + ", uuid=" + uuid
		    + ", size=" + size + ", bucketName=" + bucketName
		    + ", objectName=" + objectName + "]";
	}
	

}
