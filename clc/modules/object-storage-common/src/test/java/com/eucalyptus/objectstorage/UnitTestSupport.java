/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class UnitTestSupport {
  private static Map<String, List<String>> userMap = new HashMap<String, List<String>>();
  private static TestProvider testProvider;

  public static void setupOsgPersistenceContext() {
    Map<String,String> props = Maps.newHashMap( );
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
            .add(PartEntity.class).add(BucketTags.class)
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
