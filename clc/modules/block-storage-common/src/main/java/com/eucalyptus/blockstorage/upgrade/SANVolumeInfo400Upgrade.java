/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.blockstorage.upgrade;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_0_0;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;

import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.san.common.entities.SANVolumeInfo;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.upgrade.Upgrades.DatabaseFilters;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.PostUpgrade;
import com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;

public class SANVolumeInfo400Upgrade {

	static final String OLDTABLE = "equallogicvolumeinfo";
	static final String NEWTABLE = "san_volume_info";

	@PreUpgrade(since = v4_0_0, value = Storage.class)
	public static class CopyRows implements Callable<Boolean> {

		private static final Logger LOG = Logger.getLogger(SANVolumeInfo400Upgrade.CopyRows.class);

		@Override
		public Boolean call() throws Exception {
			LOG.info("Copying rows from " + OLDTABLE + " table to " + NEWTABLE);
			Sql sql = null;

			try {
				sql = DatabaseFilters.NEWVERSION.getConnection("eucalyptus_storage");

				try {
					// Check if the new table contains any rows, if it does there is no point updating
					List<GroovyRowResult> result = sql.rows(String.format("select id from %s where id is not null", NEWTABLE));
					if (result != null && result.size() > 0) {
						// Table already contains rows, don't try to copy stuff again
						return true;
					}
				} catch (Exception e) {
					LOG.error("Select statement from " + NEWTABLE + " failed. The table may not exist or was not setup correctly");
					return false;
				}

				try {
					// Check if the old table exists and contains any rows
					List<GroovyRowResult> result = sql.rows(String.format("select id from %s where id is not null", OLDTABLE));
					if (result == null || result.size() <= 0) {
						// No rows exist in this table, nothing to be copied over
						return true;
					}
				} catch (Exception e) {
					LOG.info("Select statement from " + OLDTABLE + " failed. The table may not exist");
					return true;
				}

				try {
					// Copy the rows from equallogicvolumeinfo to san_volume_info
					sql.execute(String
							.format("insert into %s (id, creation_timestamp, last_update_timestamp, metadata_perm_uuid, version, encryptedpassword, iqn, scname, size, snapshot_of, status, storeuser, volumeid) "
									+ "select id, creation_timestamp, last_update_timestamp, metadata_perm_uuid, version, encryptedpassword, iqn, scname, size, snapshot_of, status, storeuser, volumeid from %s",
									NEWTABLE, OLDTABLE));
				} catch (Exception e) {
					LOG.error("Failed to copy rows using select into statement from " + OLDTABLE + " to " + NEWTABLE);
					return false;
				}

				return true;
			} catch (Exception e) {
				LOG.warn("Failed to copy entities from " + OLDTABLE + " table to " + NEWTABLE);
				return false;
			} finally {
				if (sql != null) {
					sql.close();
				}
			}
		}
	}

	@EntityUpgrade(entities = { SANVolumeInfo.class }, since = v4_0_0, value = Storage.class)
	public static enum AddResourceName implements Predicate<Class> {
		INSTANCE;
		private static final Logger LOG = Logger.getLogger(SANVolumeInfo400Upgrade.AddResourceName.class);

		@Override
		public boolean apply(@Nullable Class arg0) {
			LOG.info("Entity upgrade for SANVolumeInfo entities - populating sanVolumeId field");
			SANVolumeInfo example = new SANVolumeInfo();
			example.setScName(null);
			EntityTransaction tran = Entities.get(SANVolumeInfo.class);
			try {
				List<SANVolumeInfo> volumeList = Entities.query(example);
				if (volumeList != null && !volumeList.isEmpty()) {
					for (SANVolumeInfo volumeInfo : volumeList) {
						if (volumeInfo.getSanVolumeId() == null) {
							volumeInfo.setSanVolumeId(volumeInfo.getVolumeId());
						}
					}
				}
				tran.commit();
			} catch (Exception e) {
				LOG.error("Failed to perform entity upgrade for SANVolumeInfo entities", e);
				tran.rollback();
				Exceptions.toUndeclared("Failed to perform entity upgrade for SANVolumeInfo entities", e);
			}
			return true;
		}
	}

	@PostUpgrade(since = v4_0_0, value = Storage.class)
	public static class DropTable implements Callable<Boolean> {
		private static final Logger LOG = Logger.getLogger(SANVolumeInfo400Upgrade.DropTable.class);

		@Override
		public Boolean call() throws Exception {
			LOG.info("Dropping table " + OLDTABLE);
			Sql sql = null;

			try {
				// Drop the old table if it exists
				sql = DatabaseFilters.NEWVERSION.getConnection("eucalyptus_storage");
				sql.execute(String.format("drop table if exists %s", OLDTABLE));
				return true;
			} catch (Exception e) {
				LOG.warn("Failed to drop table " + OLDTABLE);
				return true;
			} finally {
				if (sql != null) {
					sql.close();
				}
			}
		}
	}
}
