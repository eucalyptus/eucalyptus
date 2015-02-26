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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityTransaction;

import org.hibernate.ejb.Ejb3Configuration;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.DatabaseAuthProvider;
import com.eucalyptus.auth.entities.AccessKeyEntity;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.CertificateEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.InstanceProfileEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.RoleEntity;
import com.eucalyptus.auth.entities.ServerCertificateEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Entities;
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

public class UnitTestSupport {
  private static Map<String, List<String>> userMap = new HashMap<String, List<String>>();

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
    props.put("hibernate.connection.url", "jdbc:derby:memory:test;create=true");

    Ejb3Configuration config =
        (new Ejb3Configuration()).configure(props).addAnnotatedClass(Bucket.class).addAnnotatedClass(ObjectEntity.class)
            .addAnnotatedClass(PartEntity.class).addAnnotatedClass(TorrentInfo.class).addAnnotatedClass(BucketTags.class)
            .addAnnotatedClass(LifecycleRule.class).addAnnotatedClass(ScheduledJob.class).addAnnotatedClass(ObjectStorageGlobalConfiguration.class)
            .addAnnotatedClass(S3AccessControlledEntity.class).addAnnotatedClass(S3ProviderConfiguration.class);

    PersistenceContexts.registerPersistenceContext("eucalyptus_osg", config);
  }

  public static void tearDownOsgPersistenceContext() {
    PersistenceContexts.shutdown();
  }

  public static void setupAuthPersistenceContext() {
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
    props.put("hibernate.connection.url", "jdbc:derby:memory:test;create=true");

    Ejb3Configuration config =
        (new Ejb3Configuration()).configure( props ).addAnnotatedClass( AccessKeyEntity.class ).addAnnotatedClass( AccountEntity.class )
            .addAnnotatedClass( InstanceProfileEntity.class ).addAnnotatedClass( GroupEntity.class )
            .addAnnotatedClass( PolicyEntity.class ).addAnnotatedClass( UserEntity.class )
            .addAnnotatedClass( RoleEntity.class ).addAnnotatedClass( CertificateEntity.class )
            .addAnnotatedClass( ServerCertificateEntity.class );

    PersistenceContexts.registerPersistenceContext("eucalyptus_auth", config);
    Accounts.setAccountProvider(new DatabaseAuthProvider());
  }

  public static void tearDownAuthPersistenceContext() {
    PersistenceContexts.shutdown();
  }

  /**
   * Create a set of accounts and users for use in test units
   * 
   * @param numAccounts
   * @param usersPerAccount
   * @throws Exception
   */
  public static void initializeAuth(int numAccounts, int usersPerAccount) throws Exception {
    String accountName;
    String userName;
    Account accnt;
    HashMap<String, String> props = null;
    for (int i = 0; i < numAccounts; i++) {
      accountName = "unittestaccount" + i;
      userMap.put(accountName, new ArrayList<String>());
      accnt = Accounts.addAccount(accountName);
      for (int j = 0; j < usersPerAccount; j++) {
        props = new HashMap<>();
        userName = "unittestuser" + j;
        props.put("email", userName + "@unit-test.com");
        User usr = accnt.addUser(userName, "/", true, props);
        userMap.get(accountName).add(usr.getUserId());
      }
    }
  }

  public static Set<String> getTestAccounts() {
    return userMap.keySet();
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
