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
package com.eucalyptus.reporting.domain;

import java.util.List;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * <p>
 * ReportingAccountDao is an object for reading ReportingAccount objects from
 * the database.
 */
public class ReportingAccountDao {
    private static Logger LOG = Logger.getLogger(ReportingAccountDao.class);

    private static ReportingAccountDao instance = null;

    public static ReportingAccountDao getInstance() {
	if (instance == null) {
	    instance = new ReportingAccountDao();
	}
	return instance;
    }

    private ReportingAccountDao() {}

    public ReportingAccount getReportingAccount(String accountId) {

	ReportingAccount searchAccount = new ReportingAccount();
	searchAccount.setId(accountId);
	List<ReportingAccount> foundAccountList = Lists.newArrayList();

	EntityTransaction db = Entities.get(ReportingAccount.class);
	try {

	    foundAccountList = (List<ReportingAccount>) Entities.query(
		    searchAccount, true);

	    db.commit();
	} catch (Exception ex) {
	    LOG.error(ex, ex);
	    foundAccountList.clear();
	} finally {
	    if (db.isActive())
		db.rollback();
	}

	if (foundAccountList.isEmpty()) {
	    return null;
	}
	
	Iterables.removeIf(foundAccountList, Predicates.isNull());
	return foundAccountList.get(0);

    }
}
