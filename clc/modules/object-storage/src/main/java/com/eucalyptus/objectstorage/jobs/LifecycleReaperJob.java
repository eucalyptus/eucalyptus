/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.jobs;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.objectstorage.BucketLifecycleManagers;
import com.eucalyptus.objectstorage.entities.LifecycleRule;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/*
 *
 */
public class LifecycleReaperJob implements Job {

    private static Logger LOG = Logger.getLogger(LifecycleReaperJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.info("beginning Object Lifecycle processing");
        LOG.debug("retrieving Object Lifecycle rules from the database");
        List<LifecycleRule> rules = null;
        try {
            rules = BucketLifecycleManagers.getInstance().getLifecycleRules();
        }
        catch (Exception ex) {
            LOG.error("exception occurred while retrieving lifecycle rules - " + ex.getMessage());
            throw new JobExecutionException("exception occurred while retrieving lifecycle rules", ex);
        }

        // phase one - set reaped on objectinfo
        if (rules != null && rules.size() > 0) {
            LOG.debug("found " + rules.size() + " Object Lifecycle rules");
            for (LifecycleRule rule : rules) {
                if ( rule.getEnabled() != null && rule.getEnabled().booleanValue()) {
                    LOG.debug("rule id - " + rule.getRuleId() + " on bucket " + rule.getBucketName() + " processing");
                    String ruleId = rule.getRuleId();
                    String bucketName = rule.getBucketName();
                    String prefix = rule.getPrefix();
                    if (rule.getExpirationDate() != null) {
                        processExpirationByDate(ruleId, bucketName, prefix, rule.getExpirationDate());
                    }
                    else if (rule.getExpirationDays() != null ) {
                        processExpirationByDays(ruleId, bucketName, prefix, rule.getExpirationDays());
                    }
                    if (rule.getTransitionDate() != null) {
                        processTransitionByDate(ruleId, bucketName, prefix, rule.getTransitionDate());
                    }
                    else if (rule.getTransitionDays() != null) {
                        processTransitionByDays(ruleId, bucketName, prefix, rule.getExpirationDays());
                    }
                }
                else {
                    LOG.debug("rule id - " + rule.getRuleId() + " on bucket " + rule.getBucketName() + " is not enabled");
                }
            }
        }
        else {
            LOG.info("there are no rules to process");
        }

        // phase two - (profit?) there is no phase two

    }

    private List<String> findMatchingObjects(String ruleId, String bucketName, String objPrefix, Date age) {

        try {
            // this check has the additional responsibility of keeping other OSGs from processing the same rule
            LifecycleRule retrievedRule = BucketLifecycleManagers.getInstance().getLifecycleRuleForReaping(ruleId, bucketName);
            if (retrievedRule == null) {
                return Collections.EMPTY_LIST;
            }
        }
        catch (ObjectStorageException e) {
            LOG.error("exception caught while attempting to retrieve lifecycle rule with id - " + ruleId +
                    " in bucket - " + bucketName + " with message " + e.getMessage());
        }

        // normalize the date to query by
        Calendar ageCal = Calendar.getInstance();
        ageCal.setTime(age);

        Calendar queryCal = Calendar.getInstance();
        queryCal.set(Calendar.DAY_OF_MONTH, ageCal.get(Calendar.DAY_OF_MONTH) );
        queryCal.set(Calendar.MONTH, ageCal.get(Calendar.MONTH));
        queryCal.set(Calendar.YEAR, ageCal.get(Calendar.YEAR));
        queryCal.set(Calendar.HOUR_OF_DAY, 0);
        queryCal.set(Calendar.MINUTE, 0);
        queryCal.set(Calendar.SECOND, 0);
        queryCal.set(Calendar.MILLISECOND, 0);

        List<ObjectEntity> results = null;
        try (TransactionResource tran = Entities.transactionFor(ObjectEntity.class)) {
            // setup example and criteria
            ObjectEntity example = new ObjectEntity();
            example.setBucketName(bucketName);
            Criterion criterion = Restrictions.and(
                    Restrictions.like("objectKey", objPrefix, MatchMode.START),
                    Restrictions.lt("creationTimestamp", queryCal.getTime()));

            results = Entities.query(example, true, criterion, Collections.EMPTY_MAP);
            tran.commit();
        }
        catch (Exception ex) {
            LOG.error("exception caught while retrieving objects prefix with " + objPrefix +
                    " from bucket " + bucketName + ", error message - " + ex.getMessage());
            return Collections.EMPTY_LIST;
        }

        if (results == null || results.size() == 0) {
            LOG.debug("there were no objects in bucket " + bucketName + " with prefix " + objPrefix);
            // no matches
            return Collections.EMPTY_LIST;
        }

        // gather up keys
        List<String> objectKeys = Lists.newArrayList();
        for (ObjectEntity objectInfo : results) {
            objectKeys.add(objectInfo.getObjectKey());
        }
        LOG.debug("found " + objectKeys.size() + " matching objects in bucket " + bucketName);
        return objectKeys;
    }

    private static abstract class ObjectInfoProcessor {
        private List<String> objectKeys;
        private String bucketName;

        public ObjectInfoProcessor(List<String> objectKeys, String bucketName) {
            this.objectKeys = objectKeys;
            this.bucketName = bucketName;
        }

        public void process() {
            if (objectKeys != null && objectKeys.size() > 0) {
                for (String objectKey : objectKeys) {

                    // versioning and lifecycle cannot coexist, so there should only be one record per bucket/key
                    try (TransactionResource tran = Entities.transactionFor(ObjectEntity.class)) {
                        ObjectEntity objectInfo = new ObjectEntity();
                        objectInfo.setBucketName(bucketName);
                        objectInfo.setObjectKey(objectKey);
                        List<ObjectEntity> results = Entities.query(objectInfo);
                        if (results == null || results.size() < 1) {
                            LOG.debug("failed to process object " + objectKey + " in bucket " +
                                    bucketName + " because it was not found in the database");
                        }
                        else {
                            for (ObjectEntity obj : results) {
                                handle(obj);
                            }
                        }
                        tran.commit();
                    }
                    catch (Exception ex) {
                        LOG.error("failed to process object " + objectKey + " in bucket " + bucketName +
                                " because an exception occurred with message " + ex.getMessage());
                    }
                }
            }
        }

        public abstract void handle(ObjectEntity retrieved) ;
    }

    public void processExpirationByDate(String ruleId, String bucketName, String prefix, Date expirationDate) {
        LOG.info("processing phase one for ruleId '" + ruleId + "' for bucket " + bucketName +
                " against objects prefixed '" + prefix +
                "', marking matches for expiration if it is now past " + expirationDate.toString());

        List<String> expiredObjectKeys = findMatchingObjects(ruleId, bucketName, prefix, expirationDate);
        ObjectInfoProcessor processor = new ObjectInfoProcessor(expiredObjectKeys, bucketName) {
            @Override
            public void handle(ObjectEntity retrieved) {
                retrieved.setDeletedTimestamp(new Date());
                Entities.merge(retrieved);
            }
        };
        processor.process();
    }

    public void processExpirationByDays(String ruleId, String bucketName, String prefix, Integer expirationDays) {
        LOG.info("processing phase one for ruleId '" + ruleId + "' for bucket " + bucketName +
                " against objects prefixed '" + prefix +
                "', marking matches for expiration if they are older than "
                + expirationDays.toString() + " days old");

        Calendar expireDay = Calendar.getInstance();
        expireDay.add( Calendar.DATE, (-1 * expirationDays.intValue()) );
        List<String> expiredObjectKeys = findMatchingObjects(ruleId, bucketName, prefix, expireDay.getTime());
        ObjectInfoProcessor processor = new ObjectInfoProcessor(expiredObjectKeys, bucketName) {
            @Override
            public void handle(ObjectEntity retrieved) {
                retrieved.setDeletedTimestamp(new Date());
                Entities.merge(retrieved);
            }
        };
        processor.process();
    }

    public void processTransitionByDate(String ruleId, String bucketName, String prefix, Date transitionDate) {
        LOG.info("processing phase one for ruleId '" + ruleId + "' for bucket " + bucketName +
                " against objects prefixed '" + prefix +
                "', marking matches for transition if it is now past " + transitionDate.toString());
        List<String> expiredObjectKeys = findMatchingObjects(ruleId, bucketName, prefix, transitionDate);
        ObjectInfoProcessor processor = new ObjectInfoProcessor(expiredObjectKeys, bucketName) {
            @Override
            public void handle(ObjectEntity retrieved) {
                // TODO what to do?
                Entities.merge(retrieved);
            }
        };
        processor.process();
    }

    public void processTransitionByDays(String ruleId, String bucketName, String prefix, Integer transitionDays) {
        LOG.info("processing phase one for ruleId '" + ruleId + "' for bucket " + bucketName +
                " against objects prefixed '" + prefix +
                "', marking matches for transition if they are older than "
                + transitionDays.toString() + " days old");
        Calendar transitionDay = Calendar.getInstance();
        transitionDay.add(Calendar.DATE, (-1 * transitionDays.intValue()));
        List<String> transitionObjectKeys = findMatchingObjects(ruleId, bucketName, prefix, transitionDay.getTime());
        ObjectInfoProcessor processor = new ObjectInfoProcessor(transitionObjectKeys, bucketName) {
            @Override
            public void handle(ObjectEntity retrieved) {
                // TODO what to do?
                Entities.merge(retrieved);
            }
        };
        processor.process();
    }

}
