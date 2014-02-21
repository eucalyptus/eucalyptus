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
 ************************************************************************/

package com.eucalyptus.objectstorage;

import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.LifecycleRule;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.ScheduledJob;
import com.eucalyptus.objectstorage.entities.TorrentInfo;
import org.hibernate.ejb.Ejb3Configuration;

import java.util.Properties;

public class UnitTestSupport {

    public static void setupOsgPersistenceContext() {
        Properties props = new Properties();
        props.put("hibernate.archive.autodetection", "jar, class, hbm");
        props.put("hibernate.ejb.interceptor.session_scoped", "com.eucalyptus.entities.DelegatingInterceptor");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.format_sql", "false");
        props.put("hibernate.generate_statistics", "false");
        props.put("hibernate.bytecode.use_reflection_optimizer", "true");
        props.put("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.EmbeddedDriver");
        props.put("javax.persistence.jdbc.user", "root");
        props.put("javax.persistence.jdbc.password", "root");
        props.put("hibernate.hbm2ddl.auto", "create");
        props.put("hibernate.cache.use_second_level_cache", "false");
        props.put("hibernate.dialect", "org.hibernate.dialect.DerbyDialect");
        props.put("hibernate.connection.url", "jdbc:derby:test;create=true");

        Ejb3Configuration config =
                (new Ejb3Configuration()).configure(props)
                        .addAnnotatedClass(Bucket.class)
                        .addAnnotatedClass(LifecycleRule.class)
                        .addAnnotatedClass(ObjectEntity.class)
                        .addAnnotatedClass(ScheduledJob.class)
                        .addAnnotatedClass(TorrentInfo.class);

        PersistenceContexts.registerPersistenceContext("eucalyptus_osg", config);
    }

    public static void tearDownOsgPersistenceContext() {
        PersistenceContexts.shutdown();
    }

}
