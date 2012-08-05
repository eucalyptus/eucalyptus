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
import com.eucalyptus.reporting.event.SnapShotEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotEventStore;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

import com.google.common.base.Preconditions;

public class SnapShotUsageEventListener implements EventListener<SnapShotEvent> {

    public static void register() {
	Listeners.register(SnapShotEvent.class, new SnapShotUsageEventListener());
    }

    @Override
	  public void fireEvent( @Nonnull final SnapShotEvent event ) {
	    Preconditions.checkNotNull( event, "Event is required" );
 
	    // Ensure account / user info is present and up to date
	    ReportingAccountDao.getInstance().addUpdateAccount( event.getAccountId(), event.getAccountName() );
	    ReportingUserDao.getInstance().addUpdateUser( event.getOwnerId(), event.getOwnerName() );

	    final ReportingVolumeSnapshotEventStore eventStore = ReportingVolumeSnapshotEventStore.getInstance();
	    
	    switch (event.getActionInfo().getAction()) {
	    case SNAPSHOTCREATE :
		eventStore.insertCreateEvent(event.getUuid(), event.getDisplayName(), event.getTimeInMs(), 
			event.getOwnerName(), event.getSizeGB());
	      break;
	    case SNAPSHOTDELETE :
		eventStore.insertDeleteEvent(event.getUuid(), event.getTimeInMs());
	      break;
	    }
	    
	  }    protected ReportingAccountDao getReportingAccountDao() {
	return ReportingAccountDao.getInstance();
    }

    protected ReportingUserDao getReportingUserDao() {
	return ReportingUserDao.getInstance();
    }

}
