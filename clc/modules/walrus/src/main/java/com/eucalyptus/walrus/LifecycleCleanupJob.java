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

package com.eucalyptus.walrus;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.walrus.entities.BucketInfo;
import com.eucalyptus.walrus.entities.LifecycleRuleInfo;
import com.eucalyptus.walrus.entities.LifecycleStatus;
import com.eucalyptus.reporting.event.S3ObjectEvent;
import com.eucalyptus.walrus.entities.ObjectInfo;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.persistence.EntityTransaction;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 *
 */
public class LifecycleCleanupJob implements Job {

    private static Logger LOG = Logger.getLogger(LifecycleCleanupJob.class);

    private static final Lock lock = new ReentrantLock();

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        EntityTransaction tran = Entities.get(LifecycleRuleInfo.class);
        List<LifecycleRuleInfo> rules = null;
        try {
            rules = Entities.query(new LifecycleRuleInfo());
            tran.commit();
        }
        catch (Exception ex) {
            LOG.error("exception occurred while retrieving lifecycle rules - " + ex.getMessage());
        }
        finally {
            if (tran.isActive()) {
                tran.rollback();
            }
        }

        // phase one - set reaped on objectinfo
        if (rules != null && rules.size() > 0) {
            for (LifecycleRuleInfo rule : rules) {
                if ( rule.getEnabled() != null && rule.getEnabled().booleanValue()) {
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

        // phase two - do the real work
        if (lock.tryLock()) {
            // got the lock
            try {
                processExpirations();
            }
            finally {
                lock.unlock();
            }
        }
        else {
            // must still be running, this is probably a problem
            LOG.error("unable to remove newly expired objects because prior delete tasks are not complete");
        }
    }

    private List<String> findMatchingObjects(String bucketName, String objPrefix, Date age) {

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

        List<ObjectInfo> results = null;
        EntityTransaction tran = Entities.get(ObjectInfo.class);
        try {
            // setup example and criteria
            ObjectInfo example = new ObjectInfo();
            example.setBucketName(bucketName);
            Criterion criterion = Restrictions.and(
                    Restrictions.like("objectKey", objPrefix, MatchMode.START),
                    Restrictions.lt("creationTimestamp", queryCal.getTime() ) );

            results = Entities.query(example, true, criterion, Collections.EMPTY_MAP);
        }
        catch (Exception ex) {
            LOG.error("exception caught while retrieving objects prefix with " + objPrefix +
                    " from bucket " + bucketName + ", error message - " + ex.getMessage());
            return Collections.EMPTY_LIST;
        }
        finally {
            tran.commit();
        }

        if (results == null || results.size() == 0) {
            // no matches
            return Collections.EMPTY_LIST;
        }

        // gather up keys
        List<String> objectKeys = Lists.newArrayList();
        for (ObjectInfo objectInfo : results) {
            objectKeys.add(objectInfo.getObjectKey());
        }

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
                    EntityTransaction tran = Entities.get(ObjectInfo.class);
                    try {
                        ObjectInfo objectInfo = new ObjectInfo(bucketName, objectKey);
                        List<ObjectInfo> results = Entities.query(objectInfo);
                        if (results != null && results.size() > 0) {
                            ObjectInfo retrieved = results.get(0);
                            handle(retrieved);
                        }
                        else {
                            LOG.debug("failed to process object " + objectKey + " in bucket " +
                                    bucketName + " because it was not found in the database");
                        }
                        tran.commit();
                    }
                    catch (Exception ex) {
                        LOG.error("failed to process object " + objectKey + " in bucket " + bucketName +
                                " because an exception occurred with message " + ex.getMessage());
                    }
                    finally {
                        if (tran.isActive()) {
                            tran.rollback();
                        }
                    }
                }
            }
        }

        public abstract void handle(ObjectInfo retrieved) ;
    }

    public void processExpirationByDate(String ruleId, String bucketName, String prefix, Date expirationDate) {
        LOG.info("processing phase one for ruleId '" + ruleId + "' for bucket " + bucketName +
                " against objects prefixed '" + prefix +
                "', marking matches for expiration if it is now past " + expirationDate.toString());

        List<String> expiredObjectKeys = findMatchingObjects(bucketName, prefix, expirationDate);
        ObjectInfoProcessor processor = new ObjectInfoProcessor(expiredObjectKeys, bucketName) {
            @Override
            public void handle(ObjectInfo retrieved) {
                retrieved.setLifecycleStatus(LifecycleStatus.REAPED_FOR_EXPIRE);
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
        List<String> expiredObjectKeys = findMatchingObjects(bucketName, prefix, expireDay.getTime());
        ObjectInfoProcessor processor = new ObjectInfoProcessor(expiredObjectKeys, bucketName) {
            @Override
            public void handle(ObjectInfo retrieved) {
                retrieved.setLifecycleStatus(LifecycleStatus.REAPED_FOR_EXPIRE);
                Entities.merge(retrieved);
            }
        };
        processor.process();
    }

    public void processTransitionByDate(String ruleId, String bucketName, String prefix, Date transitionDate) {
        LOG.info("processing phase one for ruleId '" + ruleId + "' for bucket " + bucketName +
                " against objects prefixed '" + prefix +
                "', marking matches for transition if it is now past " + transitionDate.toString());
        List<String> expiredObjectKeys = findMatchingObjects(bucketName, prefix, transitionDate);
        ObjectInfoProcessor processor = new ObjectInfoProcessor(expiredObjectKeys, bucketName) {
            @Override
            public void handle(ObjectInfo retrieved) {
                retrieved.setLifecycleStatus(LifecycleStatus.REAPED_FOR_TRANSITION);
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
        List<String> transitionObjectKeys = findMatchingObjects(bucketName, prefix, transitionDay.getTime());
        ObjectInfoProcessor processor = new ObjectInfoProcessor(transitionObjectKeys, bucketName) {
            @Override
            public void handle(ObjectInfo retrieved) {
                retrieved.setLifecycleStatus(LifecycleStatus.REAPED_FOR_TRANSITION);
                Entities.merge(retrieved);
            }
        };
        processor.process();
    }

    public void processExpirations( ) {
        LOG.info("processing phase two, deleting objects that have been marked as expired");
        StorageManager storageManager = null;
        try {
            storageManager = BackendStorageManagerFactory.getStorageManager();
        } catch (Exception e) {
            LOG.error("failed to retrieve storage manager, unable to delete expired objects - " + e.getMessage());
            return;
        }

        if (storageManager != null) {
            List<ObjectInfo> needsReaped = null;
            ObjectInfo example = new ObjectInfo();
            example.setLifecycleStatus(LifecycleStatus.REAPED_FOR_EXPIRE);
            EntityTransaction tran = Entities.get(ObjectInfo.class);
            try {
                needsReaped = Entities.query(example);
                tran.commit();
            }
            catch (Exception ex) {
                LOG.error("an exception occurred while attempting to retrieve objects marked for expiration, " +
                        "the exception message is - " + ex.getMessage());
            }
            finally {
                if (tran.isActive()) {
                    tran.rollback();
                }
            }

            if (needsReaped != null && needsReaped.size() > 0) {
                for (ObjectInfo deleteMe : needsReaped) {
                    // file system work
                    try {
                        storageManager.deleteObject(deleteMe.getBucketName(), deleteMe.getObjectName());
                    }
                    catch (Exception ex) {
                        LOG.error("an exception occurred while attempting to have the storage manager delete " +
                                "the object with key - " + deleteMe.getObjectKey() + " in bucket - " +
                                deleteMe.getBucketName() + ", the exception message - " + ex.getMessage());
                    }

                    // db work
                    tran = Entities.get(ObjectInfo.class);
                    try {
                        ObjectInfo needsMarkedExample = new ObjectInfo(deleteMe.getBucketName(), deleteMe.getObjectKey());
                        needsMarkedExample.setLifecycleStatus(LifecycleStatus.REAPED_FOR_EXPIRE);
                        ObjectInfo needsMarked = Entities.uniqueResult(needsMarkedExample);
                        needsMarked.setLifecycleStatus(LifecycleStatus.EXPIRED);
                        Entities.merge(needsMarked);
                        tran.commit();
                    }
                    catch (Exception ex) {
                        LOG.error("an exception occurred while attempting to set object with key " +
                                deleteMe.getObjectKey() + " and bucket name " + deleteMe.getBucketName() +
                                " as expired, the exception message is - " + ex.getMessage());
                    }
                    finally {
                        if (tran.isActive()) {
                            tran.rollback();
                        }
                    }

                    // event work
                    if (deleteMe.getSize().longValue() > 0) {
                        EntityTransaction buckInfoTran = Entities.get(BucketInfo.class);
                        try {
                            BucketInfo deleteMeBucket = new BucketInfo();
                            deleteMeBucket.setBucketName(deleteMe.getBucketName());
                            List<BucketInfo> deleteMeBuckets = Entities.query(deleteMeBucket);
                            if (deleteMeBuckets != null && deleteMeBuckets.size() > 0) {
                                BucketInfo bucket = deleteMeBuckets.get(0);
                                if (bucket != null && bucket.getOwnerId() != null) {
                                    ListenerRegistry.getInstance().fireEvent(S3ObjectEvent.with(
                                            S3ObjectEvent.S3ObjectAction.OBJECTDELETE,
                                            deleteMe.getBucketName(),
                                            deleteMe.getObjectKey(),
                                            deleteMe.getVersionId(),
                                            bucket.getUserId(),
                                            deleteMe.getSize()));
                                }
                                else {
                                    LOG.error("failed to fire usage event when deleting object with key - " +
                                            deleteMe.getObjectKey() + " in bucket - " + deleteMe.getBucketName() +
                                            " because the bucket's owner was not able to be found");
                                }
                            }
                            else {
                                LOG.error("failed to fire usage event when deleting object with key - " +
                                        deleteMe.getObjectKey() + " in bucket - " + deleteMe.getBucketName() +
                                        " because the bucket could not be found in the database");
                            }
                            buckInfoTran.commit();
                        }
                        catch (Exception ex) {
                            LOG.error("failed to fire usage event when deleting object with key - " +
                                    deleteMe.getObjectKey() + " in bucket - " + deleteMe.getBucketName() +
                                    " because exception was encountered with message " + ex.getMessage());
                            if (buckInfoTran.isActive() ) {
                                buckInfoTran.rollback();
                            }
                        }
                    }
                }
            }
            // clean up the database now
            // starting with the grants
            tran = Entities.get(ObjectInfo.class);
            try {
                ObjectInfo needsMarkedExample = new ObjectInfo( );
                needsMarkedExample.setLifecycleStatus(LifecycleStatus.EXPIRED);
                List<ObjectInfo> needsMarked = Entities.query(needsMarkedExample);
                if (needsMarked != null && needsMarked.size() > 0) {
                    for (ObjectInfo objectInfo : needsMarked) {
                        if (objectInfo.getGrants() != null) {
                            objectInfo.getGrants().clear();
                            try {
                                Entities.merge(objectInfo);
                            }
                            catch (Exception ex) {
                                LOG.error("attempting to clear the grants from object with key - " +
                                        objectInfo.getObjectKey() + " in bucket - " + objectInfo.getBucketName() +
                                        " the message on the exception is - " + ex.getMessage());
                            }
                        }
                    }
                }
                tran.commit();
            }
            catch (Exception ex) {
                LOG.error("an exception occurred while attempting to clear grants on objects, message - "
                        + ex.getMessage());
            }
            finally {
                if (tran.isActive()) {
                    tran.rollback();
                }
            }


            tran = Entities.get(ObjectInfo.class);
            try {
                Map<String,LifecycleStatus> criteria = new HashMap<>();
                criteria.put("lifecycleStatus", LifecycleStatus.EXPIRED);
                Entities.deleteAllMatching(ObjectInfo.class, "WHERE lifecycleStatus = :lifecycleStatus", criteria);
                tran.commit();
            }
            catch (Exception ex) {
                LOG.error("exception encountered while attempting to delete objects from database that have been reaped");
                if (tran.isActive()) {
                    tran.rollback();
                }
            }
        }
        else {
            // no storage manager
            LOG.error("unable to proceed with second phase of object lifecycle reaping because no storage manager was found");
        }
    }

}
