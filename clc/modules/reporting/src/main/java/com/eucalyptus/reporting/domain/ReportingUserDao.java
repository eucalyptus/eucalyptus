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
 * ReportingUserDao is an object for reading ReportingUser objects from the
 * database.
 */
public class ReportingUserDao {
    private static Logger LOG = Logger.getLogger(ReportingUserDao.class);

    private static ReportingUserDao instance = null;

    public static ReportingUserDao getInstance() {
	if (instance == null) {
	    instance = new ReportingUserDao();
	}
	return instance;
    }

    private ReportingUserDao() {
    }

    public ReportingUser getReportingUser(String userId) {
	ReportingUser searchUser = new ReportingUser();
	searchUser.setId(userId);
	try {
	    return searchReportingUser(searchUser).get(0);
	} catch (Exception ex) {
	    LOG.debug(ex, ex);
	    return null;
	}
    }

    public List<ReportingUser> getReportingUsersByAccount(String accountId) {
	ReportingUser searchUser = new ReportingUser();
	searchUser.setAccountId(accountId);
	return searchReportingUser(searchUser);
    }

    private List<ReportingUser> searchReportingUser(
	    final ReportingUser searchUser) {

	List<ReportingUser> reportingUserList = Lists.newArrayList();
	EntityTransaction db = Entities.get(ReportingUser.class);

	try {
	    reportingUserList = (List<ReportingUser>) Entities.query(
		    searchUser, true);
	    db.commit();
	} catch (Exception ex) {
	    LOG.error(ex, ex);
	    reportingUserList.clear();
	} finally {
	    if (db.isActive())
		db.rollback();
	}

	Iterables.removeIf(reportingUserList, Predicates.isNull());
	if (!reportingUserList.isEmpty()) {
	    return reportingUserList;
	} else {
	    return null;
	}
    }
}
