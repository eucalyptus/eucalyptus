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

import com.eucalyptus.util.OwnerFullName;

@SuppressWarnings("serial")
public class VolumeEvent implements Event {
    public enum VolumeAction {
	VOLUMECREATE, VOLUMEDELETE, VOLUMEATTACH, VOLUMEDETACH
    };

    public static class ActionInfo {
	private final VolumeAction action;

	private ActionInfo(final VolumeAction action) {
	    assertThat(action, notNullValue());
	    this.action = action;
	}

	public VolumeAction getAction() {
	    return action;
	}

	public String toString() {
	    return String.format("[action:%s]", getAction());
	}
    }

    public static class InstanceActionInfo extends ActionInfo {
	private final String instanceUuid;
	private final String instanceId;

	private InstanceActionInfo(final VolumeAction action,
		final String instanceUuid, final String instanceId) {
	    super(action);
	    assertThat(instanceUuid, notNullValue());
	    assertThat(instanceId, notNullValue());
	    this.instanceUuid = instanceUuid;
	    this.instanceId = instanceId;
	}

	public String getInstanceUuid() {
	    return instanceUuid;
	}

	public String getInstanceId() {
	    return instanceId;
	}

	public String toString() {
	    return String.format("[action:%s,instanceUuid:%s,instanceId:%s]",
		    getAction(), getInstanceUuid(), getInstanceId());
	}
    }

    private final ActionInfo actionInfo;
    private final String uuid;
    private final long sizeGB;
    private final String ownerId;
    private final String ownerName;
    private final String accountId;
    private final String accountName;
    private final String displayName;
    private final String availabilityZone;

    public static ActionInfo forVolumeCreate() {
	return new ActionInfo(VolumeAction.VOLUMECREATE);
    }

    public static ActionInfo forVolumeDelete() {
	return new ActionInfo(VolumeAction.VOLUMEDELETE);
    }

    public static InstanceActionInfo forVolumeAttach(final String instanceUuid,
	    final String instanceId) {
	return new InstanceActionInfo(VolumeAction.VOLUMEATTACH, instanceUuid,
		instanceId);
    }

    public static InstanceActionInfo forVolumeDetach(final String instanceUuid,
	    final String instanceId) {
	return new InstanceActionInfo(VolumeAction.VOLUMEDETACH, instanceUuid,
		instanceId);
    }

    public static VolumeEvent with(final ActionInfo actionInfo,
	    final String uuid, final long sizeGB, final OwnerFullName owner,
	    final String displayName, final String availabilityZone) {

	return new VolumeEvent(actionInfo, uuid, sizeGB, owner.getUserId(),
		owner.getUserName(), owner.getAccountNumber(),
		owner.getAccountName(), displayName, availabilityZone);
    }

    private VolumeEvent(ActionInfo actionInfo, String uuid, long sizeGB,
	    String ownerId, String ownerName, String accountId,
	    String accountName, String displayName, String availabilityZone) {

	assertThat(actionInfo, notNullValue());
	assertThat(uuid, notNullValue());
	assertThat(sizeGB, notNullValue());
	assertThat(ownerId, notNullValue());
	assertThat(accountId, notNullValue());
	assertThat(accountName, notNullValue());
	assertThat(displayName, notNullValue());
	assertThat(availabilityZone, notNullValue());
	this.actionInfo = actionInfo;
	this.uuid = uuid;
	this.sizeGB = sizeGB;
	this.ownerId = ownerId;
	this.ownerName = ownerName;
	this.accountId = accountId;
	this.accountName = accountName;
	this.displayName = displayName;
	this.availabilityZone = availabilityZone;
    }

    public String getDisplayName() {

	return displayName;
    }

    public long getSizeGB() {
	return sizeGB;
    }

    public String getOwnerId() {
	return ownerId;
    }

    public String getOwnerName() {
	return ownerName;
    }

    public String getAccountId() {
	return accountId;
    }

    public String getAccountName() {
	return accountName;
    }

    public ActionInfo getActionInfo() {
	return actionInfo;
    }

    public String getUuid() {
	return uuid;
    }

    public String getAvailabilityZone() {
	return availabilityZone;
    }

    @Override
    public boolean requiresReliableTransmission() {
	return true;
    }

    @Override
    public String toString() {
	return "VolumeEvent [actionInfo=" + actionInfo + ", uuid=" + uuid
		+ ", sizeGB=" + sizeGB + ", ownerId=" + ownerId
		+ ", ownerName=" + ownerName + ", accountId=" + accountId
		+ ", accountName=" + accountName + ", displayName="
		+ displayName + ", availabilityZone=" + availabilityZone + "]";
    }

}
