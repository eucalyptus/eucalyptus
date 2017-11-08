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
 * <p>ReportingAccountCrud is an object for Creating, Updating, and Deleting accounts. This class should
 * be used instead of creating Account hibernate objects and storing them directly.
 */
public class ReportingAccountCrud
{
	private static Logger LOG = Logger.getLogger( ReportingAccountCrud.class );

	private static ReportingAccountCrud instance = null;
	
	public static synchronized ReportingAccountCrud getInstance()
	{
		if (instance == null) {
			instance = new ReportingAccountCrud();
		}
		return instance;
	}
		
	protected ReportingAccountCrud()
	{
		
	}

	/**
 	 * Create or update an account. This method can be called repeatedly every time an event is
 	 * received that has account data. This method will update the account with a new name if
 	 * the name has changed, or will do nothing if the account already exists with the supplied
 	 * name.
 	 */
	public void createOrUpdateAccount(String id, String name)
	{
		if (id==null || name==null) throw new IllegalArgumentException("args cant be null");

		LOG.debug("createOrUpdateAccount id:" + id + " name:" + name);
		if (id.matches("\\D+") || name.matches("\\d+")) {
			LOG.error("Funnny account, id:" + id + " name:" + name, new IllegalArgumentException());
		}
		
		ReportingAccount account = new ReportingAccount(id, name);
		ReportingAccount oldAccount = ReportingAccountDao.getInstance().getReportingAccount(account.getId());
		if (oldAccount!=null && oldAccount.getName().equals(account.getName())) {
			return;
		} else if (oldAccount!=null) {
			updateInDb(account);
		} else {
			try {
				addToDb(account);
			} catch (RuntimeException e) {
				LOG.error(e);
			}
		}

	}
	
	private void updateInDb( final ReportingAccount account )
	{
		LOG.debug("Update reporting account in db, account:" + account);
		try ( final TransactionResource db = Entities.transactionFor( ReportingAccount.class ) ) {
			final ReportingAccount searchAccount = new ReportingAccount( );
			searchAccount.setId( account.getId() );
			Entities.uniqueResult( searchAccount ).setName( account.getName() );
			db.commit();
		} catch (Exception ex) {
			LOG.error(ex, ex);
			throw new RuntimeException(ex);
		}			
	}
	
	private void addToDb(ReportingAccount account)
	{
		LOG.debug("Add reporting account to db, account:" + account);

		try ( final TransactionResource db = Entities.transactionFor( ReportingAccount.class ) ) {
			Entities.persist( account );
			db.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			throw new RuntimeException(ex);
		}					
	}

}
