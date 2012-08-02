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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.reporting.user;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;

import com.eucalyptus.entities.EntityWrapper;

public class ReportingAccountDao
{
	private static Logger LOG = Logger.getLogger( ReportingUserDao.class );

	private static ReportingAccountDao instance = null;
	
	public static synchronized ReportingAccountDao getInstance()
	{
		if (instance == null) {
			instance = new ReportingAccountDao();
			instance.loadFromDb();
		}
		return instance;
	}
	
	private final Map<String,String> accounts = new ConcurrentHashMap<String,String>();
	
	protected ReportingAccountDao()
	{
		
	}

	public void addUpdateAccount(String id, String name)
	{
		if (id==null || name==null) throw new IllegalArgumentException("args cant be null");

		if (accounts.containsKey(id) && accounts.get(id).equals(name)) {
			return;
		} else if (accounts.containsKey(id)) {
			updateInDb(id, name);
			accounts.put(id, name);
		} else {
			try {
				addToDb(id, name);
				accounts.put(id, name);
			} catch (RuntimeException e) {
				LOG.error(e);
			}
		}
		
	}

	public String getAccountName(String id)
	{
		return accounts.get(id);
	}
	

	
	private void loadFromDb()
	{
		LOG.debug("Load accounts from db");
		
		EntityWrapper<ReportingAccount> entityWrapper =
			EntityWrapper.get(ReportingAccount.class);

		try {
			@SuppressWarnings("rawtypes")
			List reportingAccounts = (List)
				entityWrapper.createQuery("from ReportingAccount")
				.list();

			for (Object obj: reportingAccounts) {
				ReportingAccount Account = (ReportingAccount) obj;
				accounts.put(Account.getId(), Account.getName());
				LOG.debug("load account from db, id:" + Account.getId() + " name:" + Account.getName());
			}
				
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}			
	}
	
	private void updateInDb(String id, String name)
	{
		LOG.debug("Update reporting account in db, id:" + id + " name:" + name);

		EntityWrapper<ReportingAccount> entityWrapper =
			EntityWrapper.get(ReportingAccount.class);

		try {
			ReportingAccount reportingAccount = (ReportingAccount)
			entityWrapper.createQuery("from ReportingAccount where id = ?")
				.setString(0, id)
				.uniqueResult();
			reportingAccount.setName(name);
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}			
	}
	
	private void addToDb(String id, String name)
	{
		LOG.debug("Add reporting account to db, id:" + id + " name:" + name);
		
		EntityWrapper<ReportingAccount> entityWrapper =
			EntityWrapper.get(ReportingAccount.class);

		try {
			entityWrapper.add(new ReportingAccount(id, name));
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

}
