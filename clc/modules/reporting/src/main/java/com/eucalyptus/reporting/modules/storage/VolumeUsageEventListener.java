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

package com.eucalyptus.reporting.modules.storage;

import javax.annotation.Nonnull;

import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.VolumeEvent;
import com.eucalyptus.reporting.event.VolumeEvent.InstanceActionInfo;
import com.eucalyptus.reporting.event_store.ReportingVolumeEventStore;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.google.common.base.Preconditions;

/**
 * Volume event listener for user actions.
 */
public class VolumeUsageEventListener implements EventListener<VolumeEvent> {

    public static void register() {
	Listeners.register(VolumeEvent.class, new VolumeUsageEventListener());
    }

    @Override
    public void fireEvent(@Nonnull final VolumeEvent event) {
	Preconditions.checkNotNull(event, "Event is required");

	final long timeInMs = getCurrentTimeMillis();

	final ReportingAccountCrud reportingAccountCrud = getReportingAccountCrud();

	reportingAccountCrud.createOrUpdateAccount(event.getAccountId(), event.getAccountName());
	
	final ReportingUserCrud reportingUserCrud = getReportingUserCrud();

	reportingUserCrud.createOrUpdateUser(event.getOwnerId(), event.getAccountId(), event.getAccountName());

	final ReportingVolumeEventStore eventStore = getReportingVolumeEventStore();

	switch (event.getActionInfo().getAction()) {
	case VOLUMECREATE:
	    eventStore.insertCreateEvent(event.getUuid(),
		    event.getDisplayName(), timeInMs, event.getOwnerId(),
		    event.getAvailabilityZone(), event.getSizeGB());
	    break;
	case VOLUMEDELETE:
	    eventStore.insertDeleteEvent(event.getUuid(), timeInMs);
	    break;
	case VOLUMEATTACH:
	    eventStore.insertAttachEvent(event.getUuid(),
		    ((InstanceActionInfo) event.getActionInfo())
			    .getInstanceUuid(), event.getSizeGB(), timeInMs);
	    break;
	case VOLUMEDETACH:
	    eventStore.insertDetachEvent(event.getUuid(),
		    ((InstanceActionInfo) event.getActionInfo())
			    .getInstanceUuid(), event.getSizeGB(), timeInMs);
	    break;
	}

    }

    protected ReportingAccountCrud getReportingAccountCrud() {
	return ReportingAccountCrud.getInstance();
    }

    protected ReportingUserCrud getReportingUserCrud() {
	return ReportingUserCrud.getInstance();
    }

    protected ReportingVolumeEventStore getReportingVolumeEventStore() {
	return ReportingVolumeEventStore.getInstance();
    }

    protected long getCurrentTimeMillis() {
	return System.currentTimeMillis();
    }

}
