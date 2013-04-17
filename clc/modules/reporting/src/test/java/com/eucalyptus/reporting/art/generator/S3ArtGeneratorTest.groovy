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
 ************************************************************************/
package com.eucalyptus.reporting.art.generator

import static org.junit.Assert.*
import org.junit.Test
import com.eucalyptus.reporting.art.entity.ReportArtEntity
import com.google.common.base.Predicate
import com.eucalyptus.reporting.domain.ReportingUser
import com.eucalyptus.reporting.domain.ReportingAccount
import java.text.SimpleDateFormat
import com.eucalyptus.reporting.event_store.ReportingS3ObjectCreateEvent
import com.eucalyptus.reporting.event_store.ReportingS3ObjectDeleteEvent
import com.eucalyptus.reporting.units.SizeUnit
import com.google.common.collect.Sets
import com.eucalyptus.reporting.art.entity.AccountArtEntity
import com.eucalyptus.reporting.art.entity.UserArtEntity
import com.eucalyptus.reporting.art.entity.BucketUsageArtEntity
import java.util.concurrent.TimeUnit

/**
 * 
 */
class S3ArtGeneratorTest {
  private static String ACCOUNT1 = "account1"
  private static String ACCOUNT2 = "account2"
  private static String USER1 = "user1"
  private static String USER2 = "user2"
  private static String USER3 = "user3"
  private static String BUCKET1 = "bucket1"
  private static String BUCKET2 = "bucket2"
  private static String VERSION1 = "version1"
  private static String VERSION2 = "version2"
  private static String VERSION3 = "version3"
  private static String KEY1 = "key1"
  private static String KEY2 = "key2"
  private static Map<String,String> userToAccount = [
      (USER1): ACCOUNT1,
      (USER2): ACCOUNT1,
      (USER3): ACCOUNT2,
  ]

  @Test
  void testGenerationNoDataInPeriod(){
    S3ArtGenerator generator = testGeneratorWith( basicCreateAndDelete() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T06:00:00"), millis("2012-09-01T12:00:00") ) )
    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )
    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
    //dumpArt( art )
  }

  @Test
  void testGenerationWithVersions(){
    S3ArtGenerator generator = testGeneratorWith( basicVersioned() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertEquals( "Accounts", Sets.newHashSet(name(ACCOUNT1)), art.getAccounts().keySet() )
    AccountArtEntity account1 = art.getAccounts().get(name(ACCOUNT1))
    assertEquals( "Account1 users", Sets.newHashSet(name(USER1)), account1.getUsers().keySet() )
    BucketUsageArtEntity account1BucketUsage = account1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 gbsecs", 2 * TimeUnit.HOURS.toSeconds(12), account1BucketUsage.getGBSecs() )
    assertEquals( "Account1 size", gb(3), account1BucketUsage.getSize() )
    assertEquals( "Account1 object versions", 3, account1BucketUsage.getObjectsNum() )

    UserArtEntity user1 = account1.getUsers().get(name(USER1))
    assertEquals( "Account1 user1 buckets", Sets.newHashSet(BUCKET1), user1.getBucketUsage().keySet() )
    BucketUsageArtEntity user1BucketUsage = user1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 user1 gbsecs", 2 * TimeUnit.HOURS.toSeconds(12), user1BucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 size", gb(3), user1BucketUsage.getSize() )
    assertEquals( "Account1 user1 object versions", 3, user1BucketUsage.getObjectsNum() )

    BucketUsageArtEntity bucketUsage = user1.getBucketUsage().get(BUCKET1)
    assertEquals( "Account1 user1 bucket1 gbsecs", 2 * TimeUnit.HOURS.toSeconds(12), bucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 bucket1 size", gb(3), bucketUsage.getSize() )
    assertEquals( "Account1 user1 bucket1 object versions", 3, bucketUsage.getObjectsNum() )

    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
    //dumpArt( art )
  }

  @Test
  void testGenerationOverwrittenObjects(){
    S3ArtGenerator generator = testGeneratorWith( basicUnversioned() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertEquals( "Accounts", Sets.newHashSet(name(ACCOUNT1)), art.getAccounts().keySet() )
    AccountArtEntity account1 = art.getAccounts().get(name(ACCOUNT1))
    assertEquals( "Account1 users", Sets.newHashSet(name(USER1)), account1.getUsers().keySet() )
    BucketUsageArtEntity account1BucketUsage = account1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 gbsecs", TimeUnit.HOURS.toSeconds(12), account1BucketUsage.getGBSecs() )
    assertEquals( "Account1 size", gb(2), account1BucketUsage.getSize() )
    assertEquals( "Account1 object versions", 2, account1BucketUsage.getObjectsNum() )

    UserArtEntity user1 = account1.getUsers().get(name(USER1))
    assertEquals( "Account1 user1 buckets", Sets.newHashSet(BUCKET1), user1.getBucketUsage().keySet() )
    BucketUsageArtEntity user1BucketUsage = user1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 user1 gbsecs",  TimeUnit.HOURS.toSeconds(12), user1BucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 size", gb(2), user1BucketUsage.getSize() )
    assertEquals( "Account1 user1 object versions", 2, user1BucketUsage.getObjectsNum() )

    BucketUsageArtEntity bucketUsage = user1.getBucketUsage().get(BUCKET1)
    assertEquals( "Account1 user1 bucket1 gbsecs", TimeUnit.HOURS.toSeconds(12), bucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 bucket1 size", gb(2), bucketUsage.getSize() )
    assertEquals( "Account1 user1 bucket1 object versions", 2, bucketUsage.getObjectsNum() )

    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
    //dumpArt( art )
  }

  @Test
  void testGenerationCreatedInReportPeriod(){
    S3ArtGenerator generator = testGeneratorWith( data( [
        objectCreate( BUCKET1, KEY1, null, gb(1), "2012-09-01T00:00:00", USER1 )
    ], [] ) )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-08-01T00:00:00"), millis("2012-09-01T01:00:00") ) )
    assertEquals( "Accounts", Sets.newHashSet(name(ACCOUNT1)), art.getAccounts().keySet() )
    AccountArtEntity account1 = art.getAccounts().get(name(ACCOUNT1))
    assertEquals( "Account1 users", Sets.newHashSet(name(USER1)), account1.getUsers().keySet() )
    BucketUsageArtEntity account1BucketUsage = account1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 gbsecs", TimeUnit.HOURS.toSeconds(1), account1BucketUsage.getGBSecs() )
    assertEquals( "Account1 size", gb(1), account1BucketUsage.getSize() )
    assertEquals( "Account1 object versions", 1, account1BucketUsage.getObjectsNum() )

    UserArtEntity user1 = account1.getUsers().get(name(USER1))
    assertEquals( "Account1 user1 buckets", Sets.newHashSet(BUCKET1), user1.getBucketUsage().keySet() )
    BucketUsageArtEntity user1BucketUsage = user1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 user1 gbsecs", TimeUnit.HOURS.toSeconds(1), user1BucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 size", gb(1), user1BucketUsage.getSize() )
    assertEquals( "Account1 user1 object versions", 1, user1BucketUsage.getObjectsNum() )

    BucketUsageArtEntity bucketUsage = user1.getBucketUsage().get(BUCKET1)
    assertEquals( "Account1 user1 bucket1 gbsecs", TimeUnit.HOURS.toSeconds(1), bucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 bucket1 size", gb(1), bucketUsage.getSize() )
    assertEquals( "Account1 user1 bucket1 object versions", 1, bucketUsage.getObjectsNum() )

    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
    //dumpArt( art )
  }

  @Test
  void testGenerationDeletedInReportPeriod(){
    S3ArtGenerator generator = testGeneratorWith( data( [
        objectCreate( BUCKET1, KEY1, null, gb(1), "2012-09-01T00:00:00", USER1 )
    ], [
        objectDelete( BUCKET1, KEY1, null, "2012-09-01T07:00:00" )
    ] ) )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T06:00:00"), millis("2012-09-01T12:00:00") ) )
    assertEquals( "Accounts", Sets.newHashSet(name(ACCOUNT1)), art.getAccounts().keySet() )
    AccountArtEntity account1 = art.getAccounts().get(name(ACCOUNT1))
    assertEquals( "Account1 users", Sets.newHashSet(name(USER1)), account1.getUsers().keySet() )
    BucketUsageArtEntity account1BucketUsage = account1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 gbsecs", TimeUnit.HOURS.toSeconds(1), account1BucketUsage.getGBSecs() )
    assertEquals( "Account1 size", gb(1), account1BucketUsage.getSize() )
    assertEquals( "Account1 object versions", 1, account1BucketUsage.getObjectsNum() )

    UserArtEntity user1 = account1.getUsers().get(name(USER1))
    assertEquals( "Account1 user1 buckets", Sets.newHashSet(BUCKET1), user1.getBucketUsage().keySet() )
    BucketUsageArtEntity user1BucketUsage = user1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 user1 gbsecs", TimeUnit.HOURS.toSeconds(1), user1BucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 size", gb(1), user1BucketUsage.getSize() )
    assertEquals( "Account1 user1 object versions", 1, user1BucketUsage.getObjectsNum() )

    BucketUsageArtEntity bucketUsage = user1.getBucketUsage().get(BUCKET1)
    assertEquals( "Account1 user1 bucket1 gbsecs", TimeUnit.HOURS.toSeconds(1), bucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 bucket1 size", gb(1), bucketUsage.getSize() )
    assertEquals( "Account1 user1 bucket1 object versions", 1, bucketUsage.getObjectsNum() )

    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
    //dumpArt( art )
  }

  @Test
  void testGenerationMultipleUsers(){
    S3ArtGenerator generator = testGeneratorWith( data( [
        objectCreate( BUCKET1, KEY1, null, gb(1), "2012-09-01T00:00:00", USER1 ),
        objectCreate( BUCKET1, KEY2, null, gb(1), "2012-09-01T00:00:00", USER2 )
    ] , [] ) )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertEquals( "Accounts", Sets.newHashSet(name(ACCOUNT1)), art.getAccounts().keySet() )
    AccountArtEntity account1 = art.getAccounts().get(name(ACCOUNT1))
    assertEquals( "Account1 users", Sets.newHashSet(name(USER1),name(USER2)), account1.getUsers().keySet() )
    BucketUsageArtEntity account1BucketUsage = account1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 gbsecs", 2 * TimeUnit.HOURS.toSeconds(12), account1BucketUsage.getGBSecs() )
    assertEquals( "Account1 size", gb(2), account1BucketUsage.getSize() )
    assertEquals( "Account1 object versions", 2, account1BucketUsage.getObjectsNum() )

    UserArtEntity user1 = account1.getUsers().get(name(USER1))
    assertEquals( "Account1 user1 buckets", Sets.newHashSet(BUCKET1), user1.getBucketUsage().keySet() )
    BucketUsageArtEntity user1BucketUsage = user1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 user1 gbsecs", TimeUnit.HOURS.toSeconds(12), user1BucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 size", gb(1), user1BucketUsage.getSize() )
    assertEquals( "Account1 user1 object versions", 1, user1BucketUsage.getObjectsNum() )

    BucketUsageArtEntity bucket1Usage = user1.getBucketUsage().get(BUCKET1)
    assertEquals( "Account1 user1 bucket1 gbsecs", TimeUnit.HOURS.toSeconds(12), bucket1Usage.getGBSecs() )
    assertEquals( "Account1 user1 bucket1 size", gb(1), bucket1Usage.getSize() )
    assertEquals( "Account1 user1 bucket1 object versions", 1, bucket1Usage.getObjectsNum() )

    UserArtEntity user2 = account1.getUsers().get(name(USER2))
    assertEquals( "Account1 user2 buckets", Sets.newHashSet(BUCKET1), user2.getBucketUsage().keySet() )
    BucketUsageArtEntity user2BucketUsage = user2.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 user2 gbsecs", TimeUnit.HOURS.toSeconds(12), user1BucketUsage.getGBSecs() )
    assertEquals( "Account1 user2 size", gb(1), user2BucketUsage.getSize() )
    assertEquals( "Account1 user2 object versions", 1, user2BucketUsage.getObjectsNum() )

    BucketUsageArtEntity bucket2Usage = user2.getBucketUsage().get(BUCKET1)
    assertEquals( "Account1 user2 bucket1 gbsecs", TimeUnit.HOURS.toSeconds(12), bucket2Usage.getGBSecs() )
    assertEquals( "Account1 user2 bucket1 size", gb(1), bucket2Usage.getSize() )
    assertEquals( "Account1 user2 bucket1 object versions", 1, bucket2Usage.getObjectsNum() )

    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
  }

  @Test
  void testGenerationMultipleAccounts(){
    S3ArtGenerator generator = testGeneratorWith( data( [
        objectCreate( BUCKET1, KEY1, null, gb(1), "2012-09-01T00:00:00", USER1 ),
        objectCreate( BUCKET1, KEY2, null, gb(1), "2012-09-01T00:00:00", USER3 )
    ] , [] ) )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertEquals( "Accounts", Sets.newHashSet(name(ACCOUNT1),name(ACCOUNT2)), art.getAccounts().keySet() )

    for ( String accountId : [ ACCOUNT1, ACCOUNT2 ] ) {
      String userId = ACCOUNT1.equals(accountId) ? USER1 : USER3

      AccountArtEntity account = art.getAccounts().get(name(accountId))
      assertEquals( accountId + " users", Sets.newHashSet(name(userId)), account.getUsers().keySet() )

      BucketUsageArtEntity accountBucketUsage = account.getUsageTotals().getBucketTotals()
      assertEquals( accountId + " gbsecs", TimeUnit.HOURS.toSeconds(12), accountBucketUsage.getGBSecs() )
      assertEquals( accountId + " size", gb(1), accountBucketUsage.getSize() )
      assertEquals( accountId + " object versions", 1, accountBucketUsage.getObjectsNum() )

      UserArtEntity user1 = account.getUsers().get(name(userId))
      assertEquals( accountId + " " + userId + " buckets", Sets.newHashSet(BUCKET1), user1.getBucketUsage().keySet() )
      BucketUsageArtEntity user1BucketUsage = user1.getUsageTotals().getBucketTotals()
      assertEquals( accountId + " " + userId + " gbsecs", TimeUnit.HOURS.toSeconds(12), user1BucketUsage.getGBSecs() )
      assertEquals( accountId + " " + userId + " size", gb(1), user1BucketUsage.getSize() )
      assertEquals( accountId + " " + userId + " object versions", 1, user1BucketUsage.getObjectsNum() )

      BucketUsageArtEntity bucketUsage = user1.getBucketUsage().get(BUCKET1)
      assertEquals( accountId + " " + userId + " bucket gbsecs", TimeUnit.HOURS.toSeconds(12), bucketUsage.getGBSecs() )
      assertEquals( accountId + " " + userId + " bucket size", gb(1), bucketUsage.getSize() )
      assertEquals( accountId + " " + userId + " bucket object versions", 1, bucketUsage.getObjectsNum() )
    }

    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )  }

  @Test
  void testGenerationMultipleBuckets(){
    S3ArtGenerator generator = testGeneratorWith( data( [
        objectCreate( BUCKET1, KEY1, null, gb(1), "2012-09-01T00:00:00", USER1 ),
        objectCreate( BUCKET2, KEY1, null, gb(1), "2012-09-01T00:00:00", USER1 )
    ], [] ) )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T01:00:00") ) )
    assertEquals( "Accounts", Sets.newHashSet(name(ACCOUNT1)), art.getAccounts().keySet() )
    AccountArtEntity account1 = art.getAccounts().get(name(ACCOUNT1))
    assertEquals( "Account1 users", Sets.newHashSet(name(USER1)), account1.getUsers().keySet() )
    BucketUsageArtEntity account1BucketUsage = account1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 gbsecs", 2 * TimeUnit.HOURS.toSeconds(1), account1BucketUsage.getGBSecs() )
    assertEquals( "Account1 size", gb(2), account1BucketUsage.getSize() )
    assertEquals( "Account1 object versions", 2, account1BucketUsage.getObjectsNum() )

    UserArtEntity user1 = account1.getUsers().get(name(USER1))
    BucketUsageArtEntity user1BucketUsage = user1.getUsageTotals().getBucketTotals()
    assertEquals( "Account1 user1 gbsecs", 2 * TimeUnit.HOURS.toSeconds(1), user1BucketUsage.getGBSecs() )
    assertEquals( "Account1 user1 size", gb(2), user1BucketUsage.getSize() )
    assertEquals( "Account1 user1 object versions", 2, user1BucketUsage.getObjectsNum() )
    assertEquals( "Account1 user1 buckets", Sets.newHashSet(BUCKET1, BUCKET2), user1.getBucketUsage().keySet() )

    BucketUsageArtEntity bucket1Usage = user1.getBucketUsage().get(BUCKET1)
    assertEquals( "Account1 user1 bucket1 gbsecs", TimeUnit.HOURS.toSeconds(1), bucket1Usage.getGBSecs() )
    assertEquals( "Account1 user1 bucket1 size", gb(1), bucket1Usage.getSize() )
    assertEquals( "Account1 user1 bucket1 object versions", 1, bucket1Usage.getObjectsNum() )

    BucketUsageArtEntity bucket2Usage = user1.getBucketUsage().get(BUCKET2)
    assertEquals( "Account1 user1 bucket2 gbsecs", TimeUnit.HOURS.toSeconds(1), bucket2Usage.getGBSecs() )
    assertEquals( "Account1 user1 bucket2 size", gb(1), bucket2Usage.getSize() )
    assertEquals( "Account1 user1 bucket2 object versions", 1, bucket2Usage.getObjectsNum() )

    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
    //dumpArt( art )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingS3ObjectCreateEvent objectCreate(
      String bucketName,
      String objectKey,
      String objectVersion,
      Long size,
      String timestamp,
      String userId
  ) {
    new ReportingS3ObjectCreateEvent( bucketName, objectKey, objectVersion, size, millis(timestamp), userId )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingS3ObjectDeleteEvent objectDelete(
      String bucketName,
      String objectKey,
      String objectVersion,
      String timestamp
  ) {
    new ReportingS3ObjectDeleteEvent( bucketName, objectKey, objectVersion, millis(timestamp) )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingUser user( String id, String accountId ) {
    new ReportingUser( id, accountId, name(id) )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingAccount account( String id ) {
    new ReportingAccount( id, name(id) )
  }

  private long millis( String timestamp ) {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    sdf.parse( timestamp ).getTime()
  }

  private String name( String id ) {
    id + "-name"
  }

  private Long gb( int value ) {
    SizeUnit.GB.factor * value
  }

  private TestData basicCreateAndDelete() {
    List<ReportingS3ObjectCreateEvent> objectCreateList = [
        objectCreate( BUCKET1, KEY1, null, gb(1), "2012-09-01T00:00:00", USER1 ),
    ]
    List<ReportingS3ObjectDeleteEvent> objectDeleteList = [
        objectDelete( BUCKET1, KEY1, null, "2012-09-01T01:00:00" ),
    ]
    data( objectCreateList, objectDeleteList )
  }

  private TestData basicUnversioned() {
    List<ReportingS3ObjectCreateEvent> objectCreateList = [
        objectCreate( BUCKET1, KEY1, null, gb(1), "2012-09-01T00:00:00", USER1 ),
        objectCreate( BUCKET1, KEY1, null, gb(1), "2012-09-01T06:00:00", USER1 ),
    ]
    List<ReportingS3ObjectDeleteEvent> objectDeleteList = [
        objectDelete( BUCKET1, KEY1, null, "2012-09-01T06:00:00" ),
    ]
    data( objectCreateList, objectDeleteList )
  }

  private TestData basicVersioned() {
    List<ReportingS3ObjectCreateEvent> objectCreateList = [
        objectCreate( BUCKET1, KEY1, VERSION1, gb(1), "2012-09-01T00:00:00", USER1 ),
        objectCreate( BUCKET1, KEY1, VERSION2, gb(1), "2012-09-01T00:00:00", USER1 ),
        objectCreate( BUCKET1, KEY1, VERSION3, gb(1), "2012-09-01T06:00:00", USER1 ),
    ]
    List<ReportingS3ObjectDeleteEvent> objectDeleteList = [
        objectDelete( BUCKET1, KEY1, VERSION2, "2012-09-01T06:00:00" ),
    ]
    data( objectCreateList, objectDeleteList )
  }

  private TestData data( List<ReportingS3ObjectCreateEvent> objectCreateList,
                         List<ReportingS3ObjectDeleteEvent> objectDeleteList ) {
    new TestData( objectCreateList, objectDeleteList )
  }

  private static class TestData {
    private List<ReportingS3ObjectCreateEvent> objectCreateList;
    private List<ReportingS3ObjectDeleteEvent> objectDeleteList;

    TestData( List<ReportingS3ObjectCreateEvent> objectCreateList,
              List<ReportingS3ObjectDeleteEvent> objectDeleteList ) {
      this.objectCreateList = objectCreateList
      this.objectDeleteList = objectDeleteList
    }
  }

  private S3ArtGenerator testGeneratorWith( TestData data ) {
    new S3ArtGenerator() {

      @Override
      protected void foreachReportingS3ObjectCreateEvent( final long endExclusive,
                                                          final Predicate<ReportingS3ObjectCreateEvent> callback ) {
        data.objectCreateList.findAll{ event -> event.getTimestampMs() < endExclusive }
            .every{ event -> callback.apply( event ) }
      }

      @Override
      protected void foreachReportingS3ObjectDeleteEvent( final long endExclusive,
                                                          final Predicate<ReportingS3ObjectDeleteEvent> callback ) {
        data.objectDeleteList.findAll{ event -> event.getTimestampMs() < endExclusive }
            .every{ event -> callback.apply( event ) }
      }

      @Override
      protected ReportingUser getUserById(String userId) {
        return user( userId, userToAccount[userId] )
      }

      @Override
      protected ReportingAccount getAccountById(String accountId) {
        return account( accountId )
      }
    }
  }

  private void dumpArt( ReportArtEntity art ) {
    dumpObject( "art", art )
  }

  private void dumpObject( String prefix, Object object ) {
    if ( object instanceof Map ) {
      ((Map)object).each { name, value ->
        dumpNVP( prefix, name, value )
      }
    } else {
      object.properties.each { name, value ->
        if ( "class".equals( name ) ) return
        dumpNVP( prefix, name, value )
      }
    }
  }

  private void dumpNVP( String prefix, Object name, Object value ) {
    if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
      println prefix + "." + name + " = " + value
    } else {
      dumpObject(prefix + "." + name, value)
    }
  }
}
