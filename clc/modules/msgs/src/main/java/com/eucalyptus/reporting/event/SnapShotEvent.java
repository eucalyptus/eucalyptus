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
    private final Long sizeGB;
    private final OwnerFullName ownerFullName;
    private final String snapshotId;
    private final String uuid;
    
    public static ActionInfo forSnapShotCreate() {
	return new ActionInfo(SnapShotAction.SNAPSHOTCREATE);
    }

    public static ActionInfo forSnapShotDelete() {
	return new ActionInfo(SnapShotAction.SNAPSHOTDELETE);
    }

    public static SnapShotEvent with(final ActionInfo actionInfo, String snapShotUUID, String snapshotId,
	    final OwnerFullName ownerFullName,
	    final long sizeGB) {
	
	return new SnapShotEvent(actionInfo, snapShotUUID, snapshotId, ownerFullName, sizeGB );
    }

    private SnapShotEvent(ActionInfo actionInfo, String uuid, String snapshotId, OwnerFullName ownerFullName , long sizeGB) {

	assertThat(actionInfo, notNullValue());
	assertThat(uuid, notNullValue());
	assertThat(sizeGB, notNullValue());
	assertThat(ownerFullName.getUserId(), notNullValue());
	assertThat(snapshotId, notNullValue());
	this.actionInfo = actionInfo;
	this.sizeGB = sizeGB;
	this.ownerFullName = ownerFullName;
	this.snapshotId = snapshotId;
	this.uuid = uuid;
    }

    public String getSnapshotId() {

	return snapshotId;
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

    public String getUUID() {
	return uuid;
    }

    @Override
    public String toString() {
	return "SnapShotEvent [actionInfo=" + actionInfo + ", sizeGB=" + sizeGB
		+ ", userName=" + ownerFullName.getUserName() + ", snapshotId="
		+ snapshotId + ", uuid=" + uuid + "]";
    }    
}
