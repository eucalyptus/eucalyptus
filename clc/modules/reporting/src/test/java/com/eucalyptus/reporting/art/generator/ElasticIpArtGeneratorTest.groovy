package com.eucalyptus.reporting.art.generator

import static org.junit.Assert.*
import org.junit.Test
import com.eucalyptus.reporting.event_store.ReportingElasticIpCreateEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpDeleteEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpAttachEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpDetachEvent
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent
import com.eucalyptus.reporting.domain.ReportingUser
import com.eucalyptus.reporting.domain.ReportingAccount
import com.eucalyptus.reporting.art.entity.ReportArtEntity
import com.eucalyptus.reporting.art.entity.UsageTotalsArtEntity
import com.eucalyptus.reporting.art.entity.AccountArtEntity
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity
import com.google.common.base.Charsets
import com.google.common.collect.Sets
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import com.eucalyptus.reporting.art.entity.UserArtEntity

/**
 * Unit test for Elastic IP ART generator
 */
class ElasticIpArtGeneratorTest {

  private static String IP1 = "10.10.10.1"
  private static String IP2 = "10.10.10.2"
  private static String ACCOUNT1 = "account1"
  private static String USER1 = "user1"
  private static String USER2 = "user2"
  private static String INSTANCE1 = "i-00000001"
  private static String INSTANCE2 = "i-00000002"

  @Test
  void testGenerationNoDataInPeriod(){
    ElasticIpArtGenerator generator = testGenerator( false, false );
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-08-01T00:00:00"), millis("2012-08-01T12:00:00") ) )
    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )
    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
    assertEquals( "Total usage count", 0L, art.getUsageTotals().getElasticIpTotals().getIpNum() )
  }

  @Test
  void testBasicGeneration() {
    ElasticIpArtGenerator generator = testGenerator( false, false );
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )

    Map<String,AccountArtEntity> accounts = art.getAccounts()
    assertEquals( "Accounts", Sets.newHashSet( name(ACCOUNT1) ), accounts.keySet() )
    assertEquals( "Account1 users", Sets.newHashSet( name(USER1) ), accounts.get( name(ACCOUNT1) ).users.keySet() )
    UserArtEntity user1ArtEntity = accounts.get(name(ACCOUNT1)).users.get(name(USER1))
    assertEquals( "Account1 user1 ips", 2, user1ArtEntity.elasticIps.size() )
    assertEquals( "User1 usage count", 2L, user1ArtEntity.getUsageTotals().getElasticIpTotals().getIpNum() )
    assertEquals( "User1 usage duration", TimeUnit.MINUTES.toMillis(10L), user1ArtEntity.getUsageTotals().getElasticIpTotals().getDurationMs() )
    assertEquals( "User1 ip1 usage count", 1, user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getIpNum() )
    assertEquals( "User1 ip1 usage duration", TimeUnit.MINUTES.toMillis(5L), user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getDurationMs() )
    assertEquals( "User1 ip2 usage count", 1, user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getIpNum() )
    assertEquals( "User1 ip2 usage duration", TimeUnit.MINUTES.toMillis(5L), user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getDurationMs() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Collections.emptySet(), zones.keySet() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 2L, totals.getElasticIpTotals().getIpNum() )
    assertEquals( "Total usage duration", TimeUnit.MINUTES.toMillis(10L), totals.getElasticIpTotals().getDurationMs() )
  }

  @Test
  void testEndWithActiveIPsGeneration() {
    ElasticIpArtGenerator generator = testGenerator( false, false );
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T11:50:00"), millis("2012-09-01T11:57:00") ) )

    Map<String,AccountArtEntity> accounts = art.getAccounts()
    assertEquals( "Accounts", Sets.newHashSet( name(ACCOUNT1) ), accounts.keySet() )
    assertEquals( "Account1 users", Sets.newHashSet( name(USER1) ), accounts.get( name(ACCOUNT1) ).users.keySet() )
    UserArtEntity user1ArtEntity = accounts.get(name(ACCOUNT1)).users.get(name(USER1))
    assertEquals( "Account1 user1 ips", 2, user1ArtEntity.elasticIps.size() )
    assertEquals( "User1 usage count", 2L, user1ArtEntity.getUsageTotals().getElasticIpTotals().getIpNum() )
    assertEquals( "User1 usage duration", TimeUnit.MINUTES.toMillis(4L), user1ArtEntity.getUsageTotals().getElasticIpTotals().getDurationMs() )
    assertEquals( "User1 ip1 usage count", 1, user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getIpNum() )
    assertEquals( "User1 ip1 usage duration", TimeUnit.MINUTES.toMillis(2L), user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getDurationMs() )
    assertEquals( "User1 ip2 usage count", 1, user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getIpNum() )
    assertEquals( "User1 ip2 usage duration", TimeUnit.MINUTES.toMillis(2L), user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getDurationMs() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Collections.emptySet(), zones.keySet() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 2L, totals.getElasticIpTotals().getIpNum() )
    assertEquals( "Total usage duration", TimeUnit.MINUTES.toMillis(4L), totals.getElasticIpTotals().getDurationMs() )
  }

  @Test
  void testStartWithActiveIPsGeneration() {
    ElasticIpArtGenerator generator = testGenerator( false, false );
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T11:56:00"), millis("2012-09-01T11:58:00") ) )

    Map<String,AccountArtEntity> accounts = art.getAccounts()
    assertEquals( "Accounts", Sets.newHashSet( name(ACCOUNT1) ), accounts.keySet() )
    assertEquals( "Account1 users", Sets.newHashSet( name(USER1) ), accounts.get( name(ACCOUNT1) ).users.keySet() )
    UserArtEntity user1ArtEntity = accounts.get(name(ACCOUNT1)).users.get(name(USER1))
    assertEquals( "Account1 user1 ips", 2, user1ArtEntity.elasticIps.size() )
    assertEquals( "User1 usage count", 2L, user1ArtEntity.getUsageTotals().getElasticIpTotals().getIpNum() )
    assertEquals( "User1 usage duration", TimeUnit.MINUTES.toMillis(4L), user1ArtEntity.getUsageTotals().getElasticIpTotals().getDurationMs() )
    assertEquals( "User1 ip1 usage count", 1, user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getIpNum() )
    assertEquals( "User1 ip1 usage duration", TimeUnit.MINUTES.toMillis(2L), user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getDurationMs() )
    assertEquals( "User1 ip2 usage count", 1, user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getIpNum() )
    assertEquals( "User1 ip2 usage duration", TimeUnit.MINUTES.toMillis(2L), user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getDurationMs() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Collections.emptySet(), zones.keySet() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 2L, totals.getElasticIpTotals().getIpNum() )
    assertEquals( "Total usage duration", TimeUnit.MINUTES.toMillis(4L), totals.getElasticIpTotals().getDurationMs() )
  }

  @Test
  void testCreateDeleteGeneration() {
    ElasticIpArtGenerator generator = testGenerator( true, false );
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:10:00") ) )

    Map<String,AccountArtEntity> accounts = art.getAccounts()
    assertEquals( "Accounts", Sets.newHashSet( name(ACCOUNT1) ), accounts.keySet() )
    assertEquals( "Account1 users", Sets.newHashSet( name(USER1), name(USER2) ), accounts.get( name(ACCOUNT1) ).users.keySet() )

    UserArtEntity user1ArtEntity = accounts.get(name(ACCOUNT1)).users.get(name(USER1))
    assertEquals( "Account1 user1 ips", 2, user1ArtEntity.elasticIps.size() )
    assertEquals( "User1 usage count", 2L, user1ArtEntity.getUsageTotals().getElasticIpTotals().getIpNum() )
    assertEquals( "User1 usage duration", TimeUnit.MINUTES.toMillis(12L), user1ArtEntity.getUsageTotals().getElasticIpTotals().getDurationMs() )
    assertEquals( "User1 ip1 usage count", 1, user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getIpNum() )
    assertEquals( "User1 ip1 usage duration", TimeUnit.MINUTES.toMillis(5L), user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getDurationMs() )
    assertEquals( "User1 ip2 usage count", 1, user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getIpNum() )
    assertEquals( "User1 ip2 usage duration", TimeUnit.MINUTES.toMillis(7L), user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getDurationMs() )

    UserArtEntity user2ArtEntity = accounts.get(name(ACCOUNT1)).users.get(name(USER2))
    assertEquals( "Account1 user2 ips", 2, user2ArtEntity.elasticIps.size() )
    assertEquals( "User2 usage count", 2L, user2ArtEntity.getUsageTotals().getElasticIpTotals().getIpNum() )
    assertEquals( "User2 usage duration", TimeUnit.MINUTES.toMillis(7L), user2ArtEntity.getUsageTotals().getElasticIpTotals().getDurationMs() )
    assertEquals( "User2 ip1 usage count", 1, user2ArtEntity.getElasticIps().get( IP1 ).getUsage().getIpNum() )
    assertEquals( "User2 ip1 usage duration", TimeUnit.MINUTES.toMillis(2L), user2ArtEntity.getElasticIps().get( IP1 ).getUsage().getDurationMs() )
    assertEquals( "User2 ip2 usage count", 1, user2ArtEntity.getElasticIps().get( IP2 ).getUsage().getIpNum() )
    assertEquals( "User2 ip2 usage duration", TimeUnit.MINUTES.toMillis(5L), user2ArtEntity.getElasticIps().get( IP2 ).getUsage().getDurationMs() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Collections.emptySet(), zones.keySet() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 4L, totals.getElasticIpTotals().getIpNum() )
    assertEquals( "Total usage duration", TimeUnit.MINUTES.toMillis(19L), totals.getElasticIpTotals().getDurationMs() )

    //new ElasticIpRenderer( new CsvDocument() ).render( art, System.out, new Units( com.eucalyptus.reporting.units.TimeUnit.MINS,  com.eucalyptus.reporting.units.SizeUnit.GB, com.eucalyptus.reporting.units.TimeUnit.MINS,  com.eucalyptus.reporting.units.SizeUnit.GB ) )
  }

  @Test
  void testAttachDetachGeneration() {
    ElasticIpArtGenerator generator = testGenerator( true, true );
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:10:00") ) )

    Map<String,AccountArtEntity> accounts = art.getAccounts()
    assertEquals( "Accounts", Sets.newHashSet( name(ACCOUNT1) ), accounts.keySet() )
    assertEquals( "Account1 users", Sets.newHashSet( name(USER1), name(USER2) ), accounts.get( name(ACCOUNT1) ).users.keySet() )

    UserArtEntity user1ArtEntity = accounts.get(name(ACCOUNT1)).users.get(name(USER1))
    assertEquals( "Account1 user1 ips", 2, user1ArtEntity.elasticIps.size() )
    assertEquals( "User1 usage count", 2L, user1ArtEntity.getUsageTotals().getElasticIpTotals().getIpNum() )
    assertEquals( "User1 usage duration", TimeUnit.MINUTES.toMillis(12L), user1ArtEntity.getUsageTotals().getElasticIpTotals().getDurationMs() )
    assertEquals( "User1 ip1 usage count", 1, user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getIpNum() )
    assertEquals( "User1 ip1 usage duration", TimeUnit.MINUTES.toMillis(5L), user1ArtEntity.getElasticIps().get( IP1 ).getUsage().getDurationMs() )
    assertEquals( "User1 ip2 usage count", 1, user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getIpNum() )
    assertEquals( "User1 ip2 usage duration", TimeUnit.MINUTES.toMillis(7L), user1ArtEntity.getElasticIps().get( IP2 ).getUsage().getDurationMs() )
    assertEquals( "User1 instance attachments", Sets.newHashSet(INSTANCE1), user1ArtEntity.getElasticIps().get( IP1 ).getInstanceAttachments().keySet() )
    assertEquals( "User1 instance1 attachment count", 1, user1ArtEntity.getElasticIps().get( IP1 ).getInstanceAttachments().get( INSTANCE1 ).getIpNum() )
    assertEquals( "User1 instance1 attachment duration", TimeUnit.MINUTES.toMillis(3L), user1ArtEntity.getElasticIps().get( IP1 ).getInstanceAttachments().get( INSTANCE1 ).getDurationMs() )

    UserArtEntity user2ArtEntity = accounts.get(name(ACCOUNT1)).users.get(name(USER2))
    assertEquals( "Account1 user2 ips", 2, user2ArtEntity.elasticIps.size() )
    assertEquals( "User2 usage count", 2L, user2ArtEntity.getUsageTotals().getElasticIpTotals().getIpNum() )
    assertEquals( "User2 usage duration", TimeUnit.MINUTES.toMillis(7L), user2ArtEntity.getUsageTotals().getElasticIpTotals().getDurationMs() )
    assertEquals( "User2 ip1 usage count", 1, user2ArtEntity.getElasticIps().get( IP1 ).getUsage().getIpNum() )
    assertEquals( "User2 ip1 usage duration", TimeUnit.MINUTES.toMillis(2L), user2ArtEntity.getElasticIps().get( IP1 ).getUsage().getDurationMs() )
    assertEquals( "User2 ip2 usage count", 1, user2ArtEntity.getElasticIps().get( IP2 ).getUsage().getIpNum() )
    assertEquals( "User2 ip2 usage duration", TimeUnit.MINUTES.toMillis(5L), user2ArtEntity.getElasticIps().get( IP2 ).getUsage().getDurationMs() )
    assertEquals( "User2 instance attachments", Sets.newHashSet(INSTANCE2), user2ArtEntity.getElasticIps().get( IP2 ).getInstanceAttachments().keySet() )
    assertEquals( "User2 instance2 attachment count", 1, user2ArtEntity.getElasticIps().get( IP2 ).getInstanceAttachments().get( INSTANCE2 ).getIpNum() )
    assertEquals( "User2 instance2 attachment duration", TimeUnit.MINUTES.toMillis(4L), user2ArtEntity.getElasticIps().get( IP2 ).getInstanceAttachments().get( INSTANCE2 ).getDurationMs() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Collections.emptySet(), zones.keySet() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 4L, totals.getElasticIpTotals().getIpNum() )
    assertEquals( "Total usage duration", TimeUnit.MINUTES.toMillis(19L), totals.getElasticIpTotals().getDurationMs() )

    //new ElasticIpRenderer( new CsvDocument() ).render( art, System.out, new Units( com.eucalyptus.reporting.units.TimeUnit.MINS,  com.eucalyptus.reporting.units.SizeUnit.GB, com.eucalyptus.reporting.units.TimeUnit.MINS,  com.eucalyptus.reporting.units.SizeUnit.GB ) )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingElasticIpCreateEvent ipCreate( String ip, String userId, String timestamp ) {
    new ReportingElasticIpCreateEvent( uuid(ip), millis(timestamp), ip, userId )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingElasticIpDeleteEvent ipDelete( String ip, String timestamp ) {
    new ReportingElasticIpDeleteEvent( uuid(ip), millis(timestamp) )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingElasticIpAttachEvent ipAttach( String ip, String instanceId, String timestamp ) {
    new ReportingElasticIpAttachEvent( uuid(ip), uuid(instanceId), millis(timestamp) )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingElasticIpDetachEvent ipDetach( String ip, String instanceId, String timestamp ) {
    new ReportingElasticIpDetachEvent( uuid(ip), uuid(instanceId), millis(timestamp) )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingInstanceCreateEvent instance( String instanceId, String userId ) {
    new ReportingInstanceCreateEvent( uuid(instanceId), instanceId, millis("2012-09-01T00:00:00"), "m1.small", userId, "PARTI00" )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingUser user( String id, String accountId ) {
    new ReportingUser( id, accountId, name(id) )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingAccount account( String id ) {
    new ReportingAccount( id, name(id) )
  }

  private String name( String id ) {
    id + "-name"
  }

  private long millis( String timestamp ) {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    sdf.parse( timestamp ).getTime()
  }

  private String uuid( String seed ) {
    UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString();
  }

  private ElasticIpArtGenerator testGenerator( boolean withDeletes, boolean withAttachments ) {
    List<ReportingElasticIpCreateEvent> createList = [
        ipCreate( IP1, USER1, "2012-09-01T11:55:00" ),
        ipCreate( IP2, USER1, "2012-09-01T11:55:00" )
    ]
    List<ReportingElasticIpDeleteEvent> deleteList = []
    List<ReportingElasticIpAttachEvent> attachList = []
    List<ReportingElasticIpDetachEvent> detachList = []
    List<ReportingInstanceCreateEvent> instanceCreateList = []

    if ( withDeletes ) {
      createList \
          << ipCreate( IP1, USER2, "2012-09-01T12:05:00" ) \
          << ipCreate( IP2, USER2, "2012-09-01T12:05:00" )

      deleteList \
          << ipDelete( IP1, "2012-09-01T12:00:00"  ) \
          << ipDelete( IP2, "2012-09-01T12:02:00"  ) \
          << ipDelete( IP1, "2012-09-01T12:07:00"  )
    }

    if ( withAttachments ) {
      instanceCreateList \
          << instance( INSTANCE1, USER1 ) \
          << instance( INSTANCE2, USER2 )

      attachList \
          << ipAttach( IP1, INSTANCE1, "2012-09-01T11:56:00" ) \
          << ipAttach( IP1, INSTANCE1, "2012-09-01T11:58:00" ) \
          << ipAttach( IP2, INSTANCE2, "2012-09-01T12:06:00" )

      detachList \
          << ipDetach( IP1, INSTANCE1, "2012-09-01T11:57:00" )
    }

    new ElasticIpArtGenerator() {
      @Override
      protected Iterator<ReportingElasticIpCreateEvent> getElasticIpCreateEventIterator() {
        return createList.iterator()
      }

      @Override
      protected Iterator<ReportingElasticIpDeleteEvent> getElasticIpDeleteEventIterator() {
        return deleteList.iterator()
      }

      @Override
      protected Iterator<ReportingElasticIpAttachEvent> getElasticIpAttachEventIterator() {
        return attachList.iterator()
      }

      @Override
      protected Iterator<ReportingElasticIpDetachEvent> getElasticIpDetachEventIterator() {
        return detachList.iterator()
      }

      @Override
      protected Iterator<ReportingInstanceCreateEvent> getInstanceCreateEventIterator() {
        return instanceCreateList.iterator()
      }

      @Override
      protected ReportingUser getUserById(String userId) {
        return user( userId, ACCOUNT1 )
      }

      @Override
      protected ReportingAccount getAccountById(String accountId) {
        return account( accountId )
      }
    }
  }
}
