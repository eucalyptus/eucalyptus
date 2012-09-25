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
package com.eucalyptus.reporting.art.generator;

import java.util.Iterator;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingAccountDao;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.domain.ReportingUserDao;

/**
 *
 */
public abstract class AbstractArtGenerator implements ArtGenerator {

	protected ReportingUser getUserById( final String userId ) {
		return ReportingUserDao.getInstance().getReportingUser( userId );
	}

	protected ReportingAccount getAccountById( final String accountId ) {
		return ReportingAccountDao.getInstance().getReportingAccount( accountId );
	}

	@SuppressWarnings( "unchecked" )
	protected <ET> Iterator<ET> getEventIterator( final Class<ET> eventClass, final String queryName ) {
		final EntityWrapper<ET> wrapper = EntityWrapper.get( eventClass );
		return (Iterator<ET> ) wrapper.scanWithNativeQuery( queryName );
	}

}
