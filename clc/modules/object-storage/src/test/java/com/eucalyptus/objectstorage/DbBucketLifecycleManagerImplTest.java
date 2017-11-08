/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.eucalyptus.objectstorage.metadata.BucketLifecycleManager;
import com.eucalyptus.storage.msgs.s3.Expiration;
import com.eucalyptus.storage.msgs.s3.LifecycleRule;
import com.eucalyptus.storage.msgs.s3.Transition;

@RunWith(JUnit4.class)
public class DbBucketLifecycleManagerImplTest {

  private BucketLifecycleManager mgr = BucketLifecycleManagers.getInstance();

  private void cleanRules(String bucketName) throws Exception {
    mgr.deleteLifecycleRules(bucketName);
    List<LifecycleRule> retrievedRules = mgr.getLifecycleRules(bucketName);

    assertTrue("expected rules to be gone after delete call", retrievedRules == null || retrievedRules.size() == 0);
  }

  @BeforeClass
  public static void testSetup() {
    UnitTestSupport.setupOsgPersistenceContext();
  }

  @AfterClass
  public static void testTeardown() {
    UnitTestSupport.tearDownOsgPersistenceContext();
  }

  @Test
  public void addLifecycleRulesTest() throws Exception {
    // make up some dummy data
    List<LifecycleRule> testRules = new ArrayList<>();
    for (int idx = 0; idx < 10; idx++) {
      LifecycleRule testRule = new LifecycleRule();
      testRule.setId("test-rule-" + idx);
      testRule.setStatus(BucketLifecycleManager.RULE_STATUS_ENABLED);
      testRule.setPrefix("/some/test/data/" + idx);
      // odd indices get expiration by days and transition by date, even days vice versa
      boolean oddIdx = idx % 2 == 0;
      Expiration expiration = new Expiration();
      Transition transition = new Transition();
      transition.setDestinationClass("GLACIER");
      if (oddIdx) {
        expiration.setCreationDelayDays(idx);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, idx);
        Date dtNow = now.getTime();
        transition.setEffectiveDate(dtNow);
        testRule.setExpiration(expiration);
        testRule.setTransition(transition);
      } else {
        transition.setCreationDelayDays(idx);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, idx);
        Date dtNow = now.getTime();
        expiration.setEffectiveDate(dtNow);
        testRule.setExpiration(expiration);
        testRule.setTransition(transition);
      }
      testRules.add(testRule);
    }

    // now the magic happens
    mgr.addLifecycleRules(testRules, "my-unit-test-bucket");
    mgr.addLifecycleRules(testRules, "my-other-unit-test-bucket");

    List<LifecycleRule> retrievedRules = mgr.getLifecycleRules("my-unit-test-bucket");

    assertTrue("expected to be able to retrieve rules after adding", retrievedRules != null);
    assertTrue("expected 10 rules to be retrieved after adding 10 rules", retrievedRules.size() == 10);

    cleanRules("my-unit-test-bucket");
    cleanRules("my-other-unit-test-bucket");
  }

  @Test
  public void moreAddLifecycleRulesTest() throws Exception {
    LifecycleRule testRule = new LifecycleRule();
    testRule.setId("test-rule");
    testRule.setStatus(BucketLifecycleManager.RULE_STATUS_ENABLED);
    testRule.setPrefix("/some/test/data");
    Expiration expiration = new Expiration();
    Transition transition = new Transition();
    transition.setDestinationClass("GLACIER");
    expiration.setCreationDelayDays(3);
    Calendar now = Calendar.getInstance();
    now.add(Calendar.DATE, 2);
    Date dtNow = now.getTime();
    transition.setEffectiveDate(dtNow);
    testRule.setExpiration(expiration);
    testRule.setTransition(transition);
    List<LifecycleRule> rules = new ArrayList<>();
    rules.add(testRule);
    mgr.addLifecycleRules(rules, "my-unit-test-bucket");

    List<LifecycleRule> retrievedRules = mgr.getLifecycleRules("my-unit-test-bucket");

    assertTrue("expected to be able to retrieve rules after adding", retrievedRules != null);
    assertTrue("expected 1 rule to be retrieved after adding 1 rule", retrievedRules.size() == 1);

    LifecycleRule retrieved = retrievedRules.get(0);
    assertTrue("expected the retrieved rule id to match", retrieved.getId().equals("test-rule"));
    assertTrue("expected the retrieved rule status to match", retrieved.getStatus().equals(BucketLifecycleManager.RULE_STATUS_ENABLED));
    assertTrue("expected the retrieved rule status to match", retrieved.getPrefix().equals("/some/test/data"));
    Expiration retrievedExpiration = retrieved.getExpiration();
    assertTrue("expected the retrieved rule expiration creation delay days to match", retrievedExpiration.getCreationDelayDays() == 3);
    Transition retrievedTransition = retrieved.getTransition();
    assertTrue("expected the retrieved rule transition destination class to match", retrievedTransition.getDestinationClass().equals("GLACIER"));

    Calendar nowAgain = Calendar.getInstance();
    nowAgain.add(Calendar.DATE, 2);
    Date dtNowAgain = now.getTime();
    long difference = dtNowAgain.getTime() - retrievedTransition.getEffectiveDate().getTime();
    assertTrue("expected the retrieved rule transition effective date to be relatively close", difference <= 30l);

    cleanRules("my-unit-test-bucket");

  }

  @Test
  public void deleteLifecycleRulesTest() throws Exception {
    // make up some dummy data
    List<LifecycleRule> testRules = new ArrayList<>();
    for (int idx = 0; idx < 10; idx++) {
      LifecycleRule testRule = new LifecycleRule();
      testRule.setId("test-rule-" + idx);
      testRule.setStatus(BucketLifecycleManager.RULE_STATUS_ENABLED);
      testRule.setPrefix("/some/test/data/" + idx);
      // odd indices get expiration by days and transition by date, even days vice versa
      boolean oddIdx = idx % 2 == 0;
      Expiration expiration = new Expiration();
      Transition transition = new Transition();
      transition.setDestinationClass("GLACIER");
      if (oddIdx) {
        expiration.setCreationDelayDays(idx);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, idx);
        Date dtNow = now.getTime();
        transition.setEffectiveDate(dtNow);
        testRule.setExpiration(expiration);
        testRule.setTransition(transition);
      } else {
        transition.setCreationDelayDays(idx);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, idx);
        Date dtNow = now.getTime();
        expiration.setEffectiveDate(dtNow);
        testRule.setExpiration(expiration);
        testRule.setTransition(transition);
      }
      testRules.add(testRule);
    }

    // now the magic happens
    mgr.addLifecycleRules(testRules, "my-unit-test-bucket");
    mgr.addLifecycleRules(testRules, "my-other-unit-test-bucket");

    List<LifecycleRule> retrievedRules = mgr.getLifecycleRules("my-unit-test-bucket");
    assertTrue("expected to be able to retrieve rules after adding", retrievedRules != null);
    assertTrue("expected 10 rules to be retrieved after adding 10 rules", retrievedRules.size() == 10);

    List<LifecycleRule> moreRetrievedRules = mgr.getLifecycleRules("my-other-unit-test-bucket");
    assertTrue("expected to be able to retrieve other rules after adding", moreRetrievedRules != null);
    assertTrue("expected 10 rules to be retrieved from other bucket after adding 10 rules", moreRetrievedRules.size() == 10);

    mgr.deleteLifecycleRules("my-unit-test-bucket");
    retrievedRules = mgr.getLifecycleRules("my-unit-test-bucket");
    assertTrue("expected rules to be gone after deleting them", retrievedRules == null || retrievedRules.size() == 0);

    moreRetrievedRules = mgr.getLifecycleRules("my-other-unit-test-bucket");
    assertTrue("expected to be able to retrieve other rules after deleting the first set of rules", moreRetrievedRules != null);
    assertTrue("expected 10 rules to be retrieved after deleting the first set of rules", moreRetrievedRules.size() == 10);

    mgr.deleteLifecycleRules("my-other-unit-test-bucket");
    moreRetrievedRules = mgr.getLifecycleRules("my-other-unit-test-bucket");
    assertTrue("expected other rules to be gone after deleting them", moreRetrievedRules == null || moreRetrievedRules.size() == 0);
  }

  @Test
  public void getLifecycleRulesTest() throws Exception {
    // make up some dummy data
    String bucketOneName = "my-unit-test-bucket";
    List<LifecycleRule> testOneRules = new ArrayList<>();
    for (int idx = 0; idx < 10; idx++) {
      LifecycleRule testRule = new LifecycleRule();
      testRule.setId(bucketOneName + "-test-rule-" + idx);
      testRule.setStatus(BucketLifecycleManager.RULE_STATUS_ENABLED);
      testRule.setPrefix("/" + bucketOneName + "/" + idx);
      // odd indices get expiration by days and transition by date, even days vice versa
      boolean oddIdx = idx % 2 == 0;
      Expiration expiration = new Expiration();
      Transition transition = new Transition();
      transition.setDestinationClass("GLACIER");
      if (oddIdx) {
        expiration.setCreationDelayDays(idx);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, idx);
        Date dtNow = now.getTime();
        transition.setEffectiveDate(dtNow);
        testRule.setExpiration(expiration);
        testRule.setTransition(transition);
      } else {
        transition.setCreationDelayDays(idx);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, idx);
        Date dtNow = now.getTime();
        expiration.setEffectiveDate(dtNow);
        testRule.setExpiration(expiration);
        testRule.setTransition(transition);
      }
      testOneRules.add(testRule);
    }

    String bucketTwoName = "my-other-unit-test-bucket";
    List<LifecycleRule> testTwoRules = new ArrayList<>();
    for (int idx = 0; idx < 10; idx++) {
      LifecycleRule testRule = new LifecycleRule();
      testRule.setId(bucketTwoName + "-test-rule-" + idx);
      testRule.setStatus(BucketLifecycleManager.RULE_STATUS_ENABLED);
      testRule.setPrefix("/" + bucketTwoName + "/" + idx);
      // odd indices get expiration by days and transition by date, even days vice versa
      boolean oddIdx = idx % 2 == 0;
      Expiration expiration = new Expiration();
      Transition transition = new Transition();
      transition.setDestinationClass("GLACIER");
      if (oddIdx) {
        expiration.setCreationDelayDays(idx);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, idx);
        Date dtNow = now.getTime();
        transition.setEffectiveDate(dtNow);
        testRule.setExpiration(expiration);
        testRule.setTransition(transition);
      } else {
        transition.setCreationDelayDays(idx);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, idx);
        Date dtNow = now.getTime();
        expiration.setEffectiveDate(dtNow);
        testRule.setExpiration(expiration);
        testRule.setTransition(transition);
      }
      testTwoRules.add(testRule);
    }

    // now the magic happens
    mgr.addLifecycleRules(testOneRules, bucketOneName);
    mgr.addLifecycleRules(testTwoRules, bucketTwoName);

    List<LifecycleRule> retrievedRulesOne = mgr.getLifecycleRules(bucketOneName);
    assertTrue("expected to be able to retrieve rules after adding", retrievedRulesOne != null);
    assertTrue("expected 10 rules to be retrieved after adding 10 rules", retrievedRulesOne.size() == 10);
    for (LifecycleRule rule : retrievedRulesOne) {
      assertTrue("expected rule id to be correct", rule.getId().startsWith(bucketOneName + "-test-rule-"));
      assertTrue("expected prefix to match", rule.getPrefix().startsWith("/" + bucketOneName + "/"));
    }

    List<LifecycleRule> retrievedRulesTwo = mgr.getLifecycleRules(bucketTwoName);
    assertTrue("expected to be able to retrieve rules after adding", retrievedRulesTwo != null);
    assertTrue("expected 10 rules to be retrieved after adding 10 rules", retrievedRulesTwo.size() == 10);
    for (LifecycleRule rule : retrievedRulesTwo) {
      assertTrue("expected rule id to be correct", rule.getId().startsWith(bucketTwoName + "-test-rule-"));
      assertTrue("expected prefix to match", rule.getPrefix().startsWith("/" + bucketTwoName + "/"));
    }

    // now let's overwrite the rules and check
    mgr.addLifecycleRules(testOneRules, bucketTwoName);
    retrievedRulesTwo = mgr.getLifecycleRules(bucketTwoName);
    assertTrue("expected to be able to retrieve rules after adding", retrievedRulesTwo != null);
    assertTrue("expected 10 rules to be retrieved after adding 10 rules", retrievedRulesTwo.size() == 10);
    for (LifecycleRule rule : retrievedRulesTwo) {
      assertTrue("expected rule id to be correct", rule.getId().startsWith(bucketOneName + "-test-rule-"));
      assertTrue("expected prefix to match", rule.getPrefix().startsWith("/" + bucketOneName + "/"));
    }
    // make sure the original rules are still intact
    retrievedRulesOne = mgr.getLifecycleRules(bucketOneName);
    assertTrue("expected to be able to retrieve rules after adding", retrievedRulesOne != null);
    assertTrue("expected 10 rules to be retrieved after adding 10 rules", retrievedRulesOne.size() == 10);
    for (LifecycleRule rule : retrievedRulesOne) {
      assertTrue("expected rule id to be correct", rule.getId().startsWith(bucketOneName + "-test-rule-"));
      assertTrue("expected prefix to match", rule.getPrefix().startsWith("/" + bucketOneName + "/"));
    }

    cleanRules(bucketOneName);
    cleanRules(bucketTwoName);
  }

  @Ignore
  @Test
  public void getLifecycleForReapingTest() throws Exception {
    LifecycleRule testRule = new LifecycleRule();
    testRule.setId("test-rule");
    testRule.setStatus(BucketLifecycleManager.RULE_STATUS_ENABLED);
    testRule.setPrefix("/some/test/data");
    Expiration expiration = new Expiration();
    Transition transition = new Transition();
    transition.setDestinationClass("GLACIER");
    expiration.setCreationDelayDays(3);
    Calendar now = Calendar.getInstance();
    now.add(Calendar.DATE, 2);
    Date dtNow = now.getTime();
    transition.setEffectiveDate(dtNow);
    testRule.setExpiration(expiration);
    testRule.setTransition(transition);
    List<LifecycleRule> rules = new ArrayList<>();
    rules.add(testRule);
    mgr.addLifecycleRules(rules, "my-unit-test-bucket");

    // this first grab should work
    com.eucalyptus.objectstorage.entities.LifecycleRule retrievedFirst = mgr.getLifecycleRuleForReaping("test-rule", "my-unit-test-bucket");

    // a second grab should not
    com.eucalyptus.objectstorage.entities.LifecycleRule retrievedSecond = mgr.getLifecycleRuleForReaping("test-rule", "my-unit-test-bucket");

    assertTrue("expected the lifecycle rule to be retrievable on the first call", retrievedFirst != null);
    assertTrue("expected the lifecycle rule to be unretrievable on the second call", retrievedSecond == null);

    Thread.sleep(BucketLifecycleManager.MAX_WAIT_TIME_FOR_PROCESSING + 1000l);

    // this third grab should work because we waited long enough
    com.eucalyptus.objectstorage.entities.LifecycleRule retrievedThird = mgr.getLifecycleRuleForReaping("test-rule", "my-unit-test-bucket");

    assertTrue("expected the lifecycle rule to be retrievable now on the third call", retrievedThird != null);

  }

}
