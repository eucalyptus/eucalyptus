package com.eucalyptus.reporting.art.generator

import static org.junit.Assert.*
import org.junit.Test
import com.eucalyptus.reporting.art.entity.ReportArtEntity
import com.eucalyptus.reporting.art.entity.AccountArtEntity
import com.google.common.collect.Sets
import com.eucalyptus.reporting.art.entity.UserArtEntity
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity
import com.eucalyptus.reporting.art.entity.UsageTotalsArtEntity
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent
import com.eucalyptus.reporting.domain.ReportingUser
import com.eucalyptus.reporting.domain.ReportingAccount
import java.text.SimpleDateFormat
import com.google.common.base.Charsets
import com.google.common.base.Predicate
import com.eucalyptus.reporting.event_store.ReportingVolumeCreateEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeDetachEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeAttachEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeDeleteEvent
import com.eucalyptus.reporting.art.entity.VolumeUsageArtEntity

/**
 * 
 */
class VolumeArtGeneratorTest {
  private static String ACCOUNT1 = "account1"
  private static String USER1 = "user1"
  private static String USER2 = "user2"
  private static String INSTANCE1 = "i-00000001"
  private static String INSTANCE2 = "i-00000002"
  private static String VOLUME1 = "vol-00000001"
  private static String VOLUME2 = "vol-00000002"
  private static String ZONE1 = "zone1"
  private static String ZONE2 = "zone2"

  @Test
  void testGenerationNoDataInPeriod(){
    VolumeArtGenerator generator = testGenerator( false, false )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-08-01T00:00:00"), millis("2012-08-01T12:00:00") ) )
    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )
    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
    assertEquals( "Total usage count", 0L, art.getUsageTotals().getVolumeTotals().getVolumeCnt() )
  }

  @Test
  void testBasicGeneration() {
    VolumeArtGenerator generator = testGenerator( false, false )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )

    //dumpArt( art )

    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Sets.newHashSet(ZONE1, ZONE2), zones.keySet() )

    VolumeUsageArtEntity zone1Usage = zones.get(ZONE1).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone1 usage count", 1L, zone1Usage.getVolumeCnt() )
    assertEquals( "Zone1 usage size", 1L, zone1Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 300L, zone1Usage.getGBSecs() )

    assertEquals( "Zone1 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE1).getAccounts().keySet() )
    AccountArtEntity zone1Account1 = zones.get(ZONE1).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone1 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone1User1ArtEntity = zone1Account1.users.get(name(USER1))
    assertEquals( "Zone1 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME1) ), zone1User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone1 User1 usage count", 1L, zone1User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone1 User1 volume1 usage count", 1, zone1User1ArtEntity.getVolumes().get( uuid(VOLUME1) ).getUsage().getVolumeCnt() )

    VolumeUsageArtEntity zone2Usage = zones.get(ZONE2).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone2 usage count", 1L, zone2Usage.getVolumeCnt() )
    assertEquals( "Zone2 usage size", 2L, zone2Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 600L, zone2Usage.getGBSecs() )

    assertEquals( "Zone2 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE2).getAccounts().keySet() )
    AccountArtEntity zone2Account1 = zones.get(ZONE2).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone2 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone2User1ArtEntity = zone2Account1.users.get(name(USER1))
    assertEquals( "Zone2 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME2) ), zone2User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone2 User1 usage count", 1L, zone2User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone2 User1 volume2 usage count", 1, zone2User1ArtEntity.getVolumes().get( uuid(VOLUME2) ).getUsage().getVolumeCnt() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 0L, totals.getVolumeTotals().getVolumeCnt() )
  }

  @Test
  void testEndWithActiveVolumesGeneration() {
    VolumeArtGenerator generator = testGenerator( false, false )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T11:50:00"), millis("2012-09-01T11:57:00") ) )

    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Sets.newHashSet(ZONE1, ZONE2), zones.keySet() )

    VolumeUsageArtEntity zone1Usage = zones.get(ZONE1).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone1 usage count", 1L, zone1Usage.getVolumeCnt() )
    assertEquals( "Zone1 usage size", 1L, zone1Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 120L, zone1Usage.getGBSecs() )

    assertEquals( "Zone1 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE1).getAccounts().keySet() )
    AccountArtEntity zone1Account1 = zones.get(ZONE1).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone1 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone1User1ArtEntity = zone1Account1.users.get(name(USER1))
    assertEquals( "Zone1 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME1) ), zone1User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone1 User1 usage count", 1L, zone1User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone1 User1 volume1 usage count", 1, zone1User1ArtEntity.getVolumes().get( uuid(VOLUME1) ).getUsage().getVolumeCnt() )

    VolumeUsageArtEntity zone2Usage = zones.get(ZONE2).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone2 usage count", 1L, zone2Usage.getVolumeCnt() )
    assertEquals( "Zone2 usage size", 2L, zone2Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 240L, zone2Usage.getGBSecs() )

    assertEquals( "Zone2 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE2).getAccounts().keySet() )
    AccountArtEntity zone2Account1 = zones.get(ZONE2).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone2 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone2User1ArtEntity = zone2Account1.users.get(name(USER1))
    assertEquals( "Zone2 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME2) ), zone2User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone2 User1 usage count", 1L, zone2User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone2 User1 volume2 usage count", 1, zone2User1ArtEntity.getVolumes().get( uuid(VOLUME2) ).getUsage().getVolumeCnt() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 0L, totals.getVolumeTotals().getVolumeCnt() )
  }

  @Test
  void testStartWithActiveVolumesGeneration() {
    VolumeArtGenerator generator = testGenerator( false, false )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T11:56:00"), millis("2012-09-01T11:58:00") ) )

    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Sets.newHashSet(ZONE1, ZONE2), zones.keySet() )

    VolumeUsageArtEntity zone1Usage = zones.get(ZONE1).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone1 usage count", 1L, zone1Usage.getVolumeCnt() )
    assertEquals( "Zone1 usage size", 1L, zone1Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 120L, zone1Usage.getGBSecs() )

    assertEquals( "Zone1 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE1).getAccounts().keySet() )
    AccountArtEntity zone1Account1 = zones.get(ZONE1).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone1 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone1User1ArtEntity = zone1Account1.users.get(name(USER1))
    assertEquals( "Zone1 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME1) ), zone1User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone1 User1 usage count", 1L, zone1User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone1 User1 volume1 usage count", 1, zone1User1ArtEntity.getVolumes().get( uuid(VOLUME1) ).getUsage().getVolumeCnt() )

    VolumeUsageArtEntity zone2Usage = zones.get(ZONE2).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone2 usage count", 1L, zone2Usage.getVolumeCnt() )
    assertEquals( "Zone2 usage size", 2L, zone2Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 240L, zone2Usage.getGBSecs() )

    assertEquals( "Zone2 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE2).getAccounts().keySet() )
    AccountArtEntity zone2Account1 = zones.get(ZONE2).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone2 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone2User1ArtEntity = zone2Account1.users.get(name(USER1))
    assertEquals( "Zone2 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME2) ), zone2User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone2 User1 usage count", 1L, zone2User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone2 User1 volume2 usage count", 1, zone2User1ArtEntity.getVolumes().get( uuid(VOLUME2) ).getUsage().getVolumeCnt() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 0L, totals.getVolumeTotals().getVolumeCnt() )
  }

  @Test
  void testCreateDeleteGeneration() {
    VolumeArtGenerator generator = testGenerator( true, false )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:10:00") ) )

    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Sets.newHashSet(ZONE1, ZONE2), zones.keySet() )

    VolumeUsageArtEntity zone1Usage = zones.get(ZONE1).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone1 usage count", 1L, zone1Usage.getVolumeCnt() )
    assertEquals( "Zone1 usage size", 1L, zone1Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 300L, zone1Usage.getGBSecs() )

    assertEquals( "Zone1 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE1).getAccounts().keySet() )
    AccountArtEntity zone1Account1 = zones.get(ZONE1).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone1 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone1User1ArtEntity = zone1Account1.users.get(name(USER1))
    assertEquals( "Zone1 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME1) ), zone1User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone1 User1 usage count", 1L, zone1User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone1 User1 volume1 usage count", 1, zone1User1ArtEntity.getVolumes().get( uuid(VOLUME1) ).getUsage().getVolumeCnt() )

    VolumeUsageArtEntity zone2Usage = zones.get(ZONE2).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone2 usage count", 1L, zone2Usage.getVolumeCnt() )
    assertEquals( "Zone2 usage size", 2L, zone2Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 840L, zone2Usage.getGBSecs() )

    assertEquals( "Zone2 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE2).getAccounts().keySet() )
    AccountArtEntity zone2Account1 = zones.get(ZONE2).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone2 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone2User1ArtEntity = zone2Account1.users.get(name(USER1))
    assertEquals( "Zone2 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME2) ), zone2User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone2 User1 usage count", 1L, zone2User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone2 User1 volume2 usage count", 1, zone2User1ArtEntity.getVolumes().get( uuid(VOLUME2) ).getUsage().getVolumeCnt() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 0L, totals.getVolumeTotals().getVolumeCnt() )
  }

  @Test
  void testAttachDetachGeneration() {
    VolumeArtGenerator generator = testGenerator( false, true )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:10:00") ) )

    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )

    Map<String,AvailabilityZoneArtEntity> zones = art.getZones()
    assertEquals( "Zones", Sets.newHashSet(ZONE1, ZONE2), zones.keySet() )

    VolumeUsageArtEntity zone1Usage = zones.get(ZONE1).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone1 usage count", 1L, zone1Usage.getVolumeCnt() )
    assertEquals( "Zone1 usage size", 1L, zone1Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 900L, zone1Usage.getGBSecs() )

    assertEquals( "Zone1 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE1).getAccounts().keySet() )
    AccountArtEntity zone1Account1 = zones.get(ZONE1).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone1 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone1User1ArtEntity = zone1Account1.users.get(name(USER1))
    assertEquals( "Zone1 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME1) ), zone1User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone1 User1 usage count", 1L, zone1User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone1 User1 volume1 id", VOLUME1, zone1User1ArtEntity.getVolumes().get( uuid(VOLUME1) ).getVolumeId() )
    assertEquals( "Zone1 User1 volume1 usage count", 1, zone1User1ArtEntity.getVolumes().get( uuid(VOLUME1) ).getUsage().getVolumeCnt() )
    assertEquals( "Zone1 User1 volume1 instances", Sets.newHashSet(INSTANCE1), zone1User1ArtEntity.getVolumes().get( uuid(VOLUME1) ).getInstanceAttachments().keySet() )

    VolumeUsageArtEntity zone2Usage = zones.get(ZONE2).getUsageTotals().getVolumeTotals()
    assertEquals( "Zone2 usage count", 1L, zone2Usage.getVolumeCnt() )
    assertEquals( "Zone2 usage size", 2L, zone2Usage.getSizeGB() )
    assertEquals( "Zone1 usage GBs", 1800L, zone2Usage.getGBSecs() )

    assertEquals( "Zone2 accounts", Sets.newHashSet(name(ACCOUNT1)), zones.get(ZONE2).getAccounts().keySet() )
    AccountArtEntity zone2Account1 = zones.get(ZONE2).getAccounts().get( name(ACCOUNT1) )
    assertEquals( "Zone2 account1 users", Sets.newHashSet( name(USER1) ), zone1Account1.users.keySet() )
    UserArtEntity zone2User1ArtEntity = zone2Account1.users.get(name(USER1))
    assertEquals( "Zone2 Account1 user1 volumes", Sets.newHashSet( uuid(VOLUME2) ), zone2User1ArtEntity.getVolumes().keySet() )
    assertEquals( "Zone2 User1 usage count", 1L, zone2User1ArtEntity.getUsageTotals().getVolumeTotals().getVolumeCnt() )
    assertEquals( "Zone1 User1 volume2 id", VOLUME2, zone2User1ArtEntity.getVolumes().get( uuid(VOLUME2) ).getVolumeId() )
    assertEquals( "Zone2 User1 volume2 usage count", 1, zone2User1ArtEntity.getVolumes().get( uuid(VOLUME2) ).getUsage().getVolumeCnt() )
    assertEquals( "Zone1 User1 volume2 instances", Sets.newHashSet(INSTANCE2), zone2User1ArtEntity.getVolumes().get( uuid(VOLUME2) ).getInstanceAttachments().keySet() )

    UsageTotalsArtEntity totals = art.getUsageTotals()
    assertEquals( "Total usage count", 0L, totals.getVolumeTotals().getVolumeCnt() )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingVolumeCreateEvent volumeCreate( String volumeId, long sizeGb, String zone, String userId, String timestamp ) {
    new ReportingVolumeCreateEvent( uuid(volumeId), volumeId, millis(timestamp), userId, zone, sizeGb )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingVolumeDeleteEvent volumeDelete( String volumeId, String timestamp ) {
    new ReportingVolumeDeleteEvent( uuid(volumeId), millis(timestamp) )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingVolumeAttachEvent volumeAttach( String volumeId, String instanceId, long sizeGb, String timestamp ) {
    new ReportingVolumeAttachEvent( uuid(volumeId), uuid(instanceId), sizeGb, millis(timestamp) )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingVolumeDetachEvent volumeDetach( String volumeId, String instanceId, String timestamp ) {
    new ReportingVolumeDetachEvent( uuid(volumeId), uuid(instanceId), millis(timestamp) )
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
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    sdf.parse( timestamp ).getTime()
  }

  private String uuid( String seed ) {
    UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString()
  }

  private VolumeArtGenerator testGenerator( boolean withDeletes, boolean withAttachments ) {
    List<ReportingVolumeCreateEvent> createList = [
        volumeCreate( VOLUME1, 1, ZONE1, USER1, "2012-09-01T11:55:00" ),
        volumeCreate( VOLUME2, 2, ZONE2, USER1, "2012-09-01T11:55:00" )
    ]
    List<ReportingVolumeDeleteEvent> deleteList = []
    List<ReportingVolumeAttachEvent> attachList = []
    List<ReportingVolumeDetachEvent> detachList = []
    List<ReportingInstanceCreateEvent> instanceCreateList = []

    if ( withDeletes ) {
      deleteList \
          << volumeDelete( VOLUME1, "2012-09-01T12:00:00"  ) \
          << volumeDelete( VOLUME2, "2012-09-01T12:02:00"  )
    }

    if ( withAttachments ) {
      instanceCreateList \
          << instance( INSTANCE1, USER1 ) \
          << instance( INSTANCE2, USER2 )

      attachList \
          << volumeAttach( VOLUME1, INSTANCE1, 1, "2012-09-01T11:56:00" ) \
          << volumeAttach( VOLUME1, INSTANCE1, 1, "2012-09-01T11:58:00" ) \
          << volumeAttach( VOLUME2, INSTANCE2, 2, "2012-09-01T12:06:00" )

      detachList \
          << volumeDetach( VOLUME1, INSTANCE1, "2012-09-01T11:57:00" )
    }

    new VolumeArtGenerator() {
      @Override
      protected void foreachReportingVolumeCreateEvent( long endExclusive,
                                                        Predicate<ReportingVolumeCreateEvent> callback ) {
        createList.findAll{ event ->
          event.getTimestampMs() < endExclusive }.every { event -> callback.apply( event ) }
      }

      @Override
      protected void foreachReportingVolumeDeleteEvent( long endExclusive,
                                                        Predicate<ReportingVolumeDeleteEvent> callback ) {
        deleteList.findAll{ event ->
          event.getTimestampMs() < endExclusive }.every { event -> callback.apply( event ) }
      }

      @Override
      protected void foreachReportingVolumeAttachEvent( long endExclusive,
                                                        Predicate<ReportingVolumeAttachEvent> callback ) {
        attachList.findAll{ event ->
          event.getTimestampMs() < endExclusive }.every { event -> callback.apply( event ) }
      }

      @Override
      protected void foreachReportingVolumeDetachEvent( long endExclusive,
                                                        Predicate<ReportingVolumeDetachEvent> callback ) {
        detachList.findAll{ event ->
          event.getTimestampMs() < endExclusive }.every { event -> callback.apply( event ) }
      }

      @Override
      protected void foreachInstanceCreateEvent( long endExclusive,
                                                 Predicate<? super ReportingInstanceCreateEvent> callback ) {
        instanceCreateList.findAll{ event ->
          event.getTimestampMs() < endExclusive }.every { event -> callback.apply( event ) }
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
