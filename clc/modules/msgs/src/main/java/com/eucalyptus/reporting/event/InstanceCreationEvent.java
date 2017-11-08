/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.reporting.event;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;

import com.eucalyptus.event.Event;

public class InstanceCreationEvent implements Event{
    
    private static final long serialVersionUID = 1L;
    
    private String uuid;
    private String instanceId;
    private String instanceType;
    private String userId;
    private String userName;
    private String accountId;
    private String accountName;
    private String availabilityZone;
    
    public InstanceCreationEvent(String uuid, String instanceId,
          String instanceType, String userId, String userName, String accountId,
          String accountName, String availabilityZone) {

      checkParam( uuid, not( isEmptyOrNullString() ) );
      checkParam( instanceId, not( isEmptyOrNullString() ) );
      checkParam( instanceType, not( isEmptyOrNullString() ) );
      checkParam( userId, not( isEmptyOrNullString() ) );
      checkParam( userName, not( isEmptyOrNullString() ) );
      checkParam( accountId, not( isEmptyOrNullString() ) );
      checkParam( accountName, not( isEmptyOrNullString() ) );

      this.uuid = uuid;
      this.instanceId = instanceId;
      this.instanceType = instanceType;
      this.userId = userId;
      this.userName = userName;
      this.accountId = accountId;
      this.accountName = accountName;
      this.availabilityZone = availabilityZone;

    }

    public String getUuid() {
        return uuid;
    }
    public String getInstanceId() {
        return instanceId;
    }
    public String getInstanceType() {
        return instanceType;
    }
    public String getUserId() {
        return userId;
    }
    public String getUserName() {
        return userName;
    }
    public String getAccountId() {
        return accountId;
    }
    public String getAccountName() {
        return accountName;
    }
 
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    @Override
    public String toString() {
	return "InstanceCreationEvent [uuid=" + uuid + ", instanceId="
		+ instanceId + ", instanceType=" + instanceType + ", userId="
		+ userId + ", userName=" + userName + ", accountId="
		+ accountId + ", accountName=" + accountName
		+ ", availabilityZone=" + availabilityZone + "]";
    }

}
