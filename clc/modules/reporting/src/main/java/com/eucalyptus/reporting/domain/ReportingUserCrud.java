/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.reporting.domain;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 * <p>ReportingUserCrud is an object for CReating, Updating, and Deleting users. This class should
 * be used instead of creating User hibernate objects and storing them directly.
 */
public class ReportingUserCrud
{
	private static Logger LOG = Logger.getLogger( ReportingUserCrud.class );

	private static ReportingUserCrud instance = null;

	public static synchronized ReportingUserCrud getInstance()
	{
		if (instance == null) {
			instance = new ReportingUserCrud();
		}
		return instance;
	}

	protected ReportingUserCrud()
	{
	}

	/**
 	 * Create or update a user. This method can be called repeatedly every time an event is
 	 * received that has user data. This method will update the user with a new name if
 	 * the name has changed, or will do nothing if the user already exists with the supplied
 	 * name.
 	 */
	public void createOrUpdateUser(String id, String accountId, String name)
	{
		if (id==null || accountId==null || name==null) throw new IllegalArgumentException("args cant be null");

		ReportingUser oldUser = ReportingUserDao.getInstance().getReportingUser(id);
		if (oldUser!=null && oldUser.getName().equals(name)) {
			return;
		} else if (oldUser!=null) {
			updateInDb(id, name);
		} else {
			try {
				addToDb(id, accountId, name);
			} catch (RuntimeException e) {
				LOG.error(e);
			}
		}

	}

	private void updateInDb(String id, String name)
	{
		LOG.debug("Update reporting user in db, id:" + id + " name:" + name);

		try ( final TransactionResource db = Entities.transactionFor( ReportingUser.class ) ) {
			final ReportingUser searchUser = new ReportingUser( );
			searchUser.setId( id );
			Entities.uniqueResult( searchUser ).setName( name );
			db.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			throw new RuntimeException(ex);
		}			
	}

	private void addToDb(String id, String accountId, String name)
	{
		LOG.debug("Add reporting user to db, id:" + id + " accountId:" + accountId + " name:" + name);

		try ( final TransactionResource db = Entities.transactionFor( ReportingUser.class ) ) {
			Entities.persist( new ReportingUser( id, accountId, name ) );
			db.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			throw new RuntimeException(ex);
		}					
	}

}
