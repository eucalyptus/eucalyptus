package com.eucalyptus.reporting.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import com.eucalyptus.util.OwnerFullName;

@SuppressWarnings("serial")
public class SnapShotEvent implements Event {

    public enum SnapShotAction { SNAPSHOTCREATE, SNAPSHOTDELETE };

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
    private final String sizeGB;
    private final String ownerId;
    private final String ownerName;
    private final String accountId;
    private final String accountName;
    private final String displayName;
    
    public static SnapShotEvent with(final ActionInfo actionInfo,
	    final String uuid, final String sizeGB, final OwnerFullName owner,
	    final String displayName) {

	return new SnapShotEvent(actionInfo, uuid, sizeGB, owner.getUserId(),
		owner.getUserName(), owner.getAccountNumber(),
		owner.getAccountName(), displayName);
    }

    private SnapShotEvent(ActionInfo actionInfo, String uuid, String sizeGB,
	    String ownerId, String ownerName, String accountId,
	    String accountName, String displayName) {

	assertThat(actionInfo, notNullValue());
	assertThat(uuid, notNullValue());
	assertThat(sizeGB, notNullValue());
	assertThat(ownerId, notNullValue());
	assertThat(accountId, notNullValue());
	assertThat(accountName, notNullValue());
	assertThat(displayName, notNullValue());

	this.actionInfo = actionInfo;
	this.uuid = uuid;
	this.sizeGB = sizeGB;
	this.ownerId = ownerId;
	this.ownerName = ownerName;
	this.accountId = accountId;
	this.accountName = accountName;
	this.displayName = displayName;
    }

    public String getDisplayName() {

	return displayName;
    }

    public String getSizeMegs() {
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
    
    public Long getTimeInMs() {
	return System.currentTimeMillis();
    }
    
    @Override
    public boolean requiresReliableTransmission() {
	return false;
    }

    @Override
    public String toString() {
	return "SnapShotEvent [actionInfo=" + actionInfo + ", uuid=" + uuid
		+ ", sizeGB=" + sizeGB + ", ownerId=" + ownerId
		+ ", ownerName=" + ownerName + ", accountId=" + accountId
		+ ", accountName=" + accountName + ", displayName="
		+ displayName + "]";
    }

}
