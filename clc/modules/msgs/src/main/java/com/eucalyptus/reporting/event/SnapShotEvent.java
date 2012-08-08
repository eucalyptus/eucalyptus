package com.eucalyptus.reporting.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import com.eucalyptus.event.Event;
import com.eucalyptus.util.OwnerFullName;


@SuppressWarnings("serial")
public class SnapShotEvent implements Event {

    public enum SnapShotAction {
	SNAPSHOTCREATE, SNAPSHOTDELETE
    };

    public static class ActionInfo {
	private final SnapShotAction action;

	private ActionInfo(final SnapShotAction action) {
	    assertThat(action, notNullValue());
	    this.action = action;
	}

	public SnapShotAction getAction() {
	    return action;
	}

	public String toString() {
	    return String.format("[action:%s]", getAction());
	}
    }

    private final ActionInfo actionInfo;
    private final String uuid;
    private final Long sizeGB;
    private final OwnerFullName ownerFullName;
    private final String displayName;

    public static ActionInfo forSnapShotCreate() {
	return new ActionInfo(SnapShotAction.SNAPSHOTCREATE);
    }

    public static ActionInfo forSnapShotDelete() {
	return new ActionInfo(SnapShotAction.SNAPSHOTDELETE);
    }

    public static SnapShotEvent with(final ActionInfo actionInfo,
	    final String uuid, final long sizeGB, final OwnerFullName ownerFullName,
	    final String displayName) {

	return new SnapShotEvent(actionInfo, uuid, sizeGB, ownerFullName, displayName);
    }

    private SnapShotEvent(ActionInfo actionInfo, String uuid, long sizeGB,
	    OwnerFullName ownerFullName , String displayName) {

	assertThat(actionInfo, notNullValue());
	assertThat(uuid, notNullValue());
	assertThat(sizeGB, notNullValue());
	assertThat(displayName, notNullValue());
	assertThat(ownerFullName.getUserId(), notNullValue());
	this.actionInfo = actionInfo;
	this.uuid = uuid;
	this.sizeGB = sizeGB;
	this.ownerFullName = ownerFullName;
	this.displayName = displayName;
    }

    public String getDisplayName() {

	return displayName;
    }

    public Long getSizeGB() {
	return sizeGB;
    }

    public OwnerFullName getOwnerFullName() {
	return ownerFullName;
    }

    public ActionInfo getActionInfo() {
	return actionInfo;
    }

    public String getUuid() {
	return uuid;
    }

    public Long getTimeInMs() {
	return System.currentTimeMillis();
    }

    public boolean requiresReliableTransmission() {
	return false;
    }

    @Override
    public String toString() {
	return "SnapShotEvent [actionInfo=" + actionInfo + ", uuid=" + uuid
		+ ", sizeGB=" + sizeGB + "," + "ownerName=" + ownerFullName.getUserName() + " displayName="
		+ displayName + "]";
    }

}
