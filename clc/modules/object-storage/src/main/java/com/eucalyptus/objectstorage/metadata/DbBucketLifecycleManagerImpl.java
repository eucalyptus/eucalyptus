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

package com.eucalyptus.objectstorage.metadata;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.objectstorage.BucketLifecycleManagers;
import com.eucalyptus.objectstorage.entities.LifecycleRule;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.storage.msgs.s3.Expiration;
import com.eucalyptus.storage.msgs.s3.Transition;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/*
 *
 */
public class DbBucketLifecycleManagerImpl implements BucketLifecycleManager {

    private static Logger LOG = Logger.getLogger( DbBucketLifecycleManagerImpl.class );

    @Override
    public void start() throws Exception {
        // no-op
    }

    @Override
    public void stop() throws Exception {
        // no-op
    }

    @Override
    public void deleteLifecycleRules(
            @Nonnull String bucketUuid,
            TransactionResource tran) {

        if (tran == null || ! tran.isActive()) {
            throw new RuntimeException(
                    new ObjectStorageException("in DbBucketLifecycleManagerImpl.deleteLifecycleRules, " +
                        "but was not given an active transaction")
            );
        }
        LifecycleRule example = new LifecycleRule();
        example.setBucketUuid(bucketUuid);
        List<LifecycleRule> existing = Entities.query(example);
        if (existing != null && existing.size() > 0) {
            // delete them
            Map<String,String> criteria = new HashMap<>();
            criteria.put("bucketUuid", bucketUuid);
            Entities.deleteAllMatching(LifecycleRule.class, "WHERE bucketUuid = :bucketUuid", criteria);
        }
    }

    @Override
    public void deleteLifecycleRules(@Nonnull String bucketUuid) throws ObjectStorageException {
        try (final TransactionResource tran = Entities.transactionFor(LifecycleRule.class)) {
            BucketLifecycleManagers.getInstance().deleteLifecycleRules(bucketUuid, tran);
            tran.commit();
        }
        catch (Exception ex) {
            LOG.error("caught exception while deleting object lifecycle rules for bucket - " +
                    bucketUuid + ", with error - " + ex.getMessage());
            throw new ObjectStorageException("InternalServerError",
                    "An exception was caught while deleting the object lifecycle rules for bucket - " + bucketUuid,
                    "Bucket", bucketUuid, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void addLifecycleRules(
            @Nonnull List<com.eucalyptus.storage.msgs.s3.LifecycleRule> rules,
            @Nonnull String bucketUuid)
            throws ObjectStorageException {

        try(TransactionResource tran = Entities.transactionFor(LifecycleRule.class))  {
            // first get rid of existing rules
            BucketLifecycleManagers.getInstance().deleteLifecycleRules(bucketUuid, tran);
            // now add the rules from the messages
            if (rules != null && rules.size() > 0) {
                for (com.eucalyptus.storage.msgs.s3.LifecycleRule ruleInfo : rules) {
                    LifecycleRule converted = convertLifecycleRule(ruleInfo, bucketUuid);
                    Entities.merge(converted);
                }
            }
            tran.commit();
        }
        catch ( Exception ex) {
            LOG.error("caught exception while managing object lifecycle for bucket - " +
                    bucketUuid + ", with error - " + ex.getMessage());
            throw new ObjectStorageException("InternalServerError",
                    "An exception was caught while managing the object lifecycle for bucket - " + bucketUuid,
                    "Bucket", bucketUuid, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<com.eucalyptus.storage.msgs.s3.LifecycleRule> getLifecycleRules(
            @Nonnull String bucketUuid) throws Exception {

        List<com.eucalyptus.storage.msgs.s3.LifecycleRule> responseRules = Lists.newArrayList();

        List<LifecycleRule> rulesFromDb = null;

        LifecycleRule exampleRule = new LifecycleRule();
        exampleRule.setBucketUuid(bucketUuid);
        try (final TransactionResource tran = Entities.transactionFor(LifecycleRule.class)) {
            rulesFromDb = Entities.query(exampleRule);
            tran.commit();
        }
        catch(NoSuchElementException e) {
            // this is fine, just means no lifecycle exists
        }
        catch (Exception ex) {
            LOG.error("exception caught while retrieving lifecycle rules for bucket " + bucketUuid, ex);
        }

        if (rulesFromDb != null) {
            for (LifecycleRule fromDb : rulesFromDb) {
                responseRules.add(convertLifecycleRule(fromDb));
            }
        }

        return responseRules;
    }

    private LifecycleRule convertLifecycleRule( com.eucalyptus.storage.msgs.s3.LifecycleRule rule, String bucketUuid) {

        LifecycleRule entity = new LifecycleRule();
        entity.setBucketUuid(bucketUuid);
        entity.setRuleId(rule.getId());
        entity.setPrefix(rule.getPrefix());

        Boolean enabled = new Boolean(false);
        if (rule.getStatus() != null && RULE_STATUS_ENABLED.equals(rule.getStatus())) {
            enabled = new Boolean(true);
        }
        entity.setEnabled(enabled);

        if (rule.getTransition() != null ) {
            Transition transition = rule.getTransition();
            if (transition.getEffectiveDate() != null) {
                entity.setTransitionDate(transition.getEffectiveDate());
            }
            if (transition.getCreationDelayDays() > 0) {
                entity.setTransitionDays( new Integer(transition.getCreationDelayDays()) );
            }
            if (transition.getDestinationClass() != null) {
                entity.setTransitionStorageClass( transition.getDestinationClass() );
            }
        }
        if (rule.getExpiration() != null) { // must be only Expiration
            Expiration expiration = rule.getExpiration();
            if (expiration.getEffectiveDate() != null) {
                entity.setExpirationDate( expiration.getEffectiveDate() );
            }
            if (expiration.getCreationDelayDays() > 0) {
                entity.setExpirationDays( new Integer(expiration.getCreationDelayDays()) );
            }
        }

        return entity;
    }

    private com.eucalyptus.storage.msgs.s3.LifecycleRule convertLifecycleRule(LifecycleRule entity) {

        com.eucalyptus.storage.msgs.s3.LifecycleRule ruleResponse =
                new com.eucalyptus.storage.msgs.s3.LifecycleRule();
        ruleResponse.setId(entity.getRuleId());
        ruleResponse.setStatus(
                entity.getEnabled() != null && entity.getEnabled().booleanValue() ?
                        "Enabled" : "Disabled" );
        ruleResponse.setPrefix(entity.getPrefix());
        if (entity.getExpirationDate() != null) {
            Expiration expiration = new Expiration();
            expiration.setEffectiveDate(entity.getExpirationDate());
            ruleResponse.setExpiration(expiration);
        }
        if (entity.getExpirationDays() != null) {
            Expiration expiration = new Expiration();
            expiration.setCreationDelayDays(entity.getExpirationDays().intValue());
            ruleResponse.setExpiration(expiration);
        }
        if (entity.getTransitionDate() != null) {
            Transition transition = new Transition();
            transition.setDestinationClass(entity.getTransitionStorageClass());
            transition.setEffectiveDate(entity.getTransitionDate());
            ruleResponse.setTransition(transition);
        }
        if (entity.getTransitionDays() != null) {
            Transition transition = new Transition();
            transition.setDestinationClass(entity.getTransitionStorageClass());
            transition.setCreationDelayDays(entity.getTransitionDays());
            ruleResponse.setTransition(transition);
        }
        return ruleResponse;
    }

    @Override
    public List<LifecycleRule> getLifecycleRules() throws Exception {
        List<LifecycleRule> rules = null;
        try (TransactionResource tran = Entities.transactionFor(LifecycleRule.class)) {
            rules = Entities.query(new LifecycleRule());
            tran.commit();
        }
        catch (Exception ex) {
            LOG.error("exception occurred while retrieving lifecycle rules - " + ex.getMessage());
        }
        return rules;
    }

    @Override
    public LifecycleRule getLifecycleRuleForReaping(String ruleId, String bucketUuid) throws ObjectStorageException {
        LifecycleRule result = null;
        LifecycleRule example = new LifecycleRule();
        example.setBucketUuid(bucketUuid);
        example.setRuleId(ruleId);
        try (TransactionResource tran = Entities.transactionFor(LifecycleRule.class)) {
            List<LifecycleRule> results = Entities.query(example);
            if (results != null && results.size() > 1) {
                tran.commit();
                throw new ObjectStorageException("duplicate rule ids in bucket");
            }
            else if (results != null && results.size() == 1) {
                result = results.get(0);
                if (result.getLastProcessingStart() != null) {
                    Date now = new Date();
                    long difference = now.getTime() - result.getLastProcessingStart().getTime();
                    if (difference > MAX_WAIT_TIME_FOR_PROCESSING) {
                        result.setLastProcessingStart(now);
                        //result = Entities.merge(result);
                        tran.commit();
                    }
                    else {
                        // must be getting processed by another host
                        result = null;
                    }
                }
                else {
                    // has not been processed before now
                    result.setLastProcessingStart(new Date());
                    //result = Entities.mergeDirect(result);
                    tran.commit();
                }
            }
            else {
                tran.commit();
                throw new ObjectStorageException("unexpected results querying for lifecycle rules, potential " +
                        "data corruption in lifecycle_rules");
            }
        }
        catch(NoSuchElementException nex) {
            // this is okay, just let it be
        }
        catch(Exception ex) {
            LOG.error("exception occurred while retrieving lifecycle rule with id - " + ruleId + " in bucket - " +
                    bucketUuid + " with message - " + ex.getMessage());
            throw new ObjectStorageException("exception occurred while retrieving lifecycle rule with id - " +
                    ruleId + " in bucket - " + bucketUuid + " with message - " + ex.getMessage(),
                    ex);
        }
        return result;
    }
}
