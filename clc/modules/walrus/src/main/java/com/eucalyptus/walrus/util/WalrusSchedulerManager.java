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

package com.eucalyptus.walrus.util;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.walrus.entities.ScheduledJobInfo;
import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/*
 *
 */
public class WalrusSchedulerManager {

    private static Logger LOG = Logger.getLogger(WalrusSchedulerManager.class);

    private static final String WALRUS_JOB_GROUP = "WalrusJobs";

    private static Scheduler scheduler = null;
    private static Lock lock = new ReentrantLock(true);
    private static boolean initted = false;

    static {
        lock.lock();
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            if (! scheduler.isStarted()) {
                scheduler.start();
            }
            initted = true;
        }
        catch (SchedulerException se) {
            LOG.error("quartz scheduler for WalrusBackend jobs did not initialize properly, " +
                    "exception caught with message - " + se.getMessage());
        }
        finally {
            lock.unlock();
        }
    }

    public static void start() {
        lock.lock();
        try {
            if (! initted) {
                LOG.error("cannot start quartz scheduler for WalrusBackend because it is not initialized");
            }
            else {
                populateJobs();
            }
        }
        catch (Exception e) {
            LOG.error("caught an exception while attempting to start the quartz scheduler for WalrusBackend jobs, " +
                    "the exception had the message - " + e.getMessage());
        }
        finally {
            lock.unlock();
        }
    }

    public static void stop() {
        lock.lock();
        try {
            if (! initted) {
                LOG.error("cannot shutdown quartz scheduler for WalrusBackend because it is not initialized");
            }
            else {
                Set<JobKey> walrusJobKeys = scheduler.getJobKeys( GroupMatcher.<JobKey>groupEquals(WALRUS_JOB_GROUP) );
                if (walrusJobKeys != null && walrusJobKeys.size() > 0) {
                    for (JobKey jobKey : walrusJobKeys) {
                        scheduler.deleteJob(jobKey);
                    }
                }
            }
        }
        catch (Exception e) {
            LOG.error("caught an exception while attempting to shutdown the quartz scheduler for WalrusBackend jobs, " +
                    "the exception had the message - " + e.getMessage());
        }
        finally {
            lock.unlock();
        }
    }

    public static String dumpInfo() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("scheduler is named " + scheduler.getSchedulerName() + "\n");
            if (scheduler.isStarted()) {
                sb.append("scheduler is started\n");

                if (scheduler.getJobGroupNames() != null && scheduler.getJobGroupNames().size() > 0) {
                    sb.append("scheduler has " + scheduler.getJobGroupNames().size() + " job groups\n");

                    for (String groupName : scheduler.getJobGroupNames()) {
                        Set<JobKey> jobKeys = scheduler.getJobKeys( GroupMatcher.<JobKey>groupEquals(groupName) );
                        if (jobKeys != null && jobKeys.size() > 0) {
                            sb.append(" in job group " + groupName + " there are " + jobKeys.size() + " jobs\n");
                            for (JobKey jobKey : jobKeys) {
                                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                                if (jobDetail != null) {
                                    sb.append("  job named " + jobKey.getName() +
                                            " using class " + jobDetail.getJobClass().getName() + "\n");
                                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                                    if ( triggers != null && triggers.size() > 0) {
                                        for (Trigger trigger : triggers) {
                                            sb.append("   " + jobKey.getName() + " is triggered by " +
                                                    trigger.getKey().getName() + "\n");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    sb.append("scheduler does not have any job groups\n");
                }

                if (scheduler.getTriggerGroupNames() != null && scheduler.getTriggerGroupNames().size() > 0) {
                    sb.append("scheduler has " + scheduler.getTriggerGroupNames().size() + " trigger groups\n");

                    for (String triggerGroupName : scheduler.getTriggerGroupNames()) {
                        Set<TriggerKey> triggerKeys =
                                scheduler.getTriggerKeys( GroupMatcher.<TriggerKey>groupEquals(triggerGroupName));
                        if (triggerKeys != null && triggerKeys.size() > 0) {
                            sb.append(" in trigger group " + triggerGroupName + " there are "
                                    + triggerKeys.size() + " triggers\n");
                            for (TriggerKey triggerKey : triggerKeys) {
                                sb.append("   trigger named " + triggerKey.getName() + " is currently in state " +
                                        scheduler.getTriggerState(triggerKey).name() + " and will fire again at "
                                        + scheduler.getTrigger(triggerKey).getNextFireTime().toString() + "\n");

                            }
                        }
                    }
                }
                else {
                    sb.append("scheduler does not have any trigger groups\n");
                }

            }
            else {
                sb.append("scheduler is shutdown\n");
            }
        }
        catch (SchedulerException se) {
            sb.append("exception caught while dumping scheduler info - " + se.getMessage() + "\n");
        }
        return sb.toString();
    }

    private static void populateJobs() {
        List<ScheduledJobInfo> jobs = null;
        EntityTransaction tran = Entities.get(ScheduledJobInfo.class);
        try {
            jobs = Entities.query(new ScheduledJobInfo());
            tran.commit();
        }
        catch (Exception ex) {
            LOG.error("exception encountered while populating walrus scheduled jobs from database - "
                    + ex.getMessage());
        }
        finally {
            if (tran.isActive()) {
                tran.rollback();
            }
        }
        if (jobs != null && jobs.size() > 0) {
            int jobIdx = 1;
            for (ScheduledJobInfo job : jobs) {
                Class jobClass = null;
                try {
                    jobClass = Class.forName(job.getJobClassName());
                }
                catch (ClassNotFoundException e) {
                    LOG.error("attempting to add job of type " + job.getJobClassName() +
                            ", but the class was not found, job will not be added");
                }
                if (jobClass != null) {
                    // make job
                    try {
                        JobDetail jobDetail = newJob(jobClass).withIdentity("job" + jobIdx, WALRUS_JOB_GROUP).build();
                        CronTrigger trigger = newTrigger().withIdentity("trigger" + jobIdx, WALRUS_JOB_GROUP)
                                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronSchedule())).build();
                        scheduler.scheduleJob(jobDetail, trigger);
                        LOG.info("job named job" + jobIdx + " added to group WalrusJobs using class " +
                                job.getJobClassName() + " with cron schedule '" + job.getCronSchedule() + "'" );
                    }
                    catch (SchedulerException se) {
                        LOG.error("while attempting to schedule job using class " + job.getJobClassName() +
                                " an exception occurred with message - " + se.getMessage());
                    }
                }
                else {
                    LOG.error("job was not scheduled because class " + job.getJobClassName() +
                            " was either not found or invalid");
                }
                jobIdx++;
            }
        }
        else {
            LOG.debug("jobs were either not found in the database, or an exception occurred while querying " +
                    "for scheduled jobs");
        }
    }

}
