/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityTransaction;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.TestProvider;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.PersistenceContextConfiguration;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.BucketTags;
import com.eucalyptus.objectstorage.entities.LifecycleRule;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
import com.eucalyptus.objectstorage.entities.S3ProviderConfiguration;
import com.eucalyptus.objectstorage.entities.ScheduledJob;
import com.eucalyptus.objectstorage.entities.TorrentInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class UnitTestSupport {
  private static Map<String, List<String>> userMap = new HashMap<String, List<String>>();
  private static TestProvider testProvider;

  public static void setupOsgPersistenceContext() {
    Map<String,String> props = Maps.newHashMap( );
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
    props.put("hibernate.connection.url", "jdbc:derby:memory:test;create=true");

    PersistenceContextConfiguration config = new PersistenceContextConfiguration(
        "eucalyptus_osg",
        ImmutableList.<Class<?>>builder( ).add(Bucket.class).add(ObjectEntity.class)
            .add(PartEntity.class).add(TorrentInfo.class).add(BucketTags.class)
            .add(LifecycleRule.class).add(ScheduledJob.class).add(ObjectStorageGlobalConfiguration.class)
            .add( S3AccessControlledEntity.class ).add( S3ProviderConfiguration.class ).build( ),
        props
    );

    PersistenceContexts.registerPersistenceContext( config );
  }

  public static void tearDownOsgPersistenceContext() {
    PersistenceContexts.shutdown();
  }

  public static void setupAuthPersistenceContext() {
    TestProvider testProvider = new TestProvider( );
    Accounts.setIdentityProvider( testProvider );
    UnitTestSupport.testProvider = testProvider;
  }

  public static void tearDownAuthPersistenceContext() {
  }

  /**
   * Create a set of accounts and users for use in test units
   * 
   * @param numAccounts
   * @param usersPerAccount
   * @throws Exception
   */
  public static void initializeAuth( int numAccounts, int usersPerAccount) throws Exception {
    String accountName;
    String userName;
    TestProvider.AccountInfo accnt;
    HashMap<String, String> props = null;
    for (int i = 0; i < numAccounts; i++) {
      accountName = "unittestaccount" + i;
      userMap.put(accountName, new ArrayList<String>());
      accnt = UnitTestSupport.testProvider.addTestAccount( accountName );
      for (int j = 0; j < usersPerAccount; j++) {
        props = new HashMap<>();
        userName = "unittestuser" + j;
        props.put("email", userName + "@unit-test.com");
        User usr = accnt.addTestUser( userName, "/", props );
        userMap.get(accountName).add(usr.getUserId());
      }
      accnt.addTestUser( "admin", "/", null );
    }
  }

  public static Set<String> getTestAccounts() {
    return userMap.keySet();
  }

  public static List<UserPrincipal> getTestUsers( int index ) {
    return testProvider.getAccounts( ).get( index ).getUsers( );
  }

  public static List<String> getUsersByAccountName(String accountName) {
    return userMap.get(accountName);
  }

  public static void flushBuckets() throws Exception {
    EntityTransaction trans = Entities.get(Bucket.class);
    try {
      Entities.deleteAll(Bucket.class);
      trans.commit();
    } catch (Throwable f) {
      throw new Exception("Error flushing bucket records " + f.getMessage());
    } finally {
      if (trans != null && trans.isActive()) {
        trans.rollback();
      }
    }
  }

  public static void flushObjects() throws Exception {
    EntityTransaction trans = Entities.get(ObjectEntity.class);
    try {
      Entities.deleteAll(ObjectEntity.class);
      trans.commit();
    } catch (Throwable f) {
      throw new Exception("Error flushing bucket records " + f.getMessage());
    } finally {
      if (trans != null && trans.isActive()) {
        trans.rollback();
      }
    }
  }
}
