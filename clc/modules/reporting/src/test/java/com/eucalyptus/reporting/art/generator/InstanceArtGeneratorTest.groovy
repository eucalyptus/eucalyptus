package com.eucalyptus.reporting.art.generator

import static org.junit.Assert.*
import org.junit.Test
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent
import com.eucalyptus.reporting.domain.ReportingUser
import com.eucalyptus.reporting.domain.ReportingAccount
import java.text.SimpleDateFormat
import com.google.common.base.Charsets
import com.google.common.collect.Sets
import com.eucalyptus.reporting.art.entity.AccountArtEntity
import com.eucalyptus.reporting.art.entity.ReportArtEntity
import com.eucalyptus.reporting.art.entity.UserArtEntity
import java.util.concurrent.TimeUnit
import com.eucalyptus.reporting.units.SizeUnit
import com.eucalyptus.reporting.art.entity.InstanceArtEntity
import com.eucalyptus.reporting.art.entity.InstanceUsageArtEntity
import com.eucalyptus.reporting.art.entity.UsageTotalsArtEntity
import com.google.common.base.Predicate

/**
 *
 */
class InstanceArtGeneratorTest {
  private static String ACCOUNT1 = "account1"
  private static String USER1 = "user1"
  private static String USER2 = "user2"
  private static String INSTANCE1 = "i-00000001"
  private static String INSTANCE2 = "i-00000002"
  private static String ZONE1 = "zone1"
  private static String ZONE2 = "zone2"
  private static String VMTYPE1 = "vmtype1"
  private static String VMTYPE2 = "vmtype2"
  private static Map<String,String> metricToDimension = [
      "NetworkIn": "total",
      "NetworkOut": "total",
      "CPUUtilization": "default",
  ]
  private static Map<String,String> userToAccount = [
      (USER1): ACCOUNT1,
      (USER2): ACCOUNT1,
  ]

  @Test
  void testGenerationNoDataInPeriod(){
    InstanceArtGenerator generator = testGeneratorWith( usageBeforeReportPeriod() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-08-25T00:00:00"), millis("2012-08-30T00:00:00") ) )
    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )
    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
  }

  @Test
  void testBasicGeneration(){
    InstanceArtGenerator generator = testGeneratorWith( basicUsageInReportPeriod() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art )
  }

  @Test
  void testBasicGenerationMultipleZones(){
    InstanceArtGenerator generator = testGeneratorWith( basicUsageInReportPeriodMultipleZones() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art, 1, 1, [ ZONE1, ZONE2 ] )
  }

  @Test
  void testBasicGenerationMultipleDisks(){
    InstanceArtGenerator generator = testGeneratorWith( basicUsageInReportPeriodWithMultipleDisks() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art, 3 )
  }

  @Test
  void testBasicMultipleUsageEvents() {
    InstanceArtGenerator generator = testGeneratorWith( basicUsageMultipleEvents() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art )
  }

  @Test
  void testInterpolatedUsageExitingReportPeriod(){
    InstanceArtGenerator generator = testGeneratorWith( interpolatedUsageWithDates( "2012-09-01T00:00:00", "2012-09-02T00:00:00", 2 ) )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art )
  }

  @Test
  void testInterpolatedUsageEnteringReportPeriod(){
    InstanceArtGenerator generator = testGeneratorWith( interpolatedUsageWithDates( "2012-08-31T12:00:00", "2012-09-01T12:00:00", 2 ) )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art )
  }

  @Test
  void testInterpolatedUsageReportPeriod(){
    InstanceArtGenerator generator = testGeneratorWith( interpolatedUsageWithDates( "2012-08-31T12:00:00", "2012-09-02T00:00:00", 3 ) )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art )
  }

  @Test
  void testSequenceResetEntering() {
    InstanceArtGenerator generator = testGeneratorWith( sequenceResetUsageEntering() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art  )
  }

  @Test
  void testSequenceResetExiting() {
    InstanceArtGenerator generator = testGeneratorWith( sequenceResetUsageExiting() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art  )
  }

  @Test
  void testSequenceResetsInReport() {
    InstanceArtGenerator generator = testGeneratorWith( sequenceResetUsage() )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art  )
  }

  @Test
  void testCreatedInReport() {
    InstanceArtGenerator generator = testGeneratorWith( createdInReportUsage(), "2012-09-01T06:00:00" )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art, 1, 0.5  )
  }

  @Test
  void testCreatedEnteringReport() {
    InstanceArtGenerator generator = testGeneratorWith( createdEnteringReportUsage(), "2012-08-31T12:00:00" )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art  )
  }

  @Test
  void testCreatedAfterUsage() {
    InstanceArtGenerator generator = testGeneratorWith( createdBeforeUsage(), "2012-09-01T06:30:00" )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )
    assertArt( art, 1, 0.5  )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingInstanceCreateEvent instanceCreate(
      String instanceId,
      String userId,
      String timestamp,
      String vmType,
      String zone
  ) {
    new ReportingInstanceCreateEvent( uuid(instanceId), instanceId, millis(timestamp), vmType, userId, zone )
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingInstanceUsageEvent instanceUsage(
      String instanceId,
      String metric,
      Integer sequenceNum,
      String dimension,
      Double value,
      String timestamp ) {
    new ReportingInstanceUsageEvent( uuid(instanceId), metric, sequenceNum, dimension, value, millis(timestamp) )
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

  private Double mbd( int mibibytes ) {
    mb(mibibytes)
  }

  private long mb( int mibibytes ) {
    mibibytes * SizeUnit.MB.factor
  }

  private Double msd( int hours ) {
    ms(hours)
  }

  private long ms( int hours ) {
    TimeUnit.HOURS.toMillis( hours )
  }

  private void assertArt( ReportArtEntity art, int diskUsageMultiplier=1, double durationMultiplier=1, List<String> zones=[ZONE1] ) {
    assertEquals("Accounts", Collections.emptySet(), art.getAccounts().keySet())
    assertEquals("Zones", Sets.newHashSet(zones), art.getZones().keySet())

    zones.each{ zone ->
      UsageTotalsArtEntity zoneUsageTotals = art.getZones().get(zone).getUsageTotals()
      InstanceUsageArtEntity zoneUsage = zoneUsageTotals.getInstanceTotals()
      assertEquals( zone + " total usage instances", 1, zoneUsage.getInstanceCnt() )
      assertUsage( zone + " total", zoneUsage, diskUsageMultiplier, durationMultiplier )

      Map<String,AccountArtEntity> accounts = art.getZones().get(zone).getAccounts()
      assertEquals( zone + " accounts", Sets.newHashSet(name(ACCOUNT1)), accounts.keySet() )
      InstanceUsageArtEntity account1Usage = accounts.get(name(ACCOUNT1)).getUsageTotals().getInstanceTotals()
      assertEquals( "Account1 total usage instances", 1, account1Usage.getInstanceCnt() )
      assertUsage( "Account1 total", account1Usage, diskUsageMultiplier, durationMultiplier )

      if ( ZONE1.equals( zone ) ) {
        assertUser( accounts, diskUsageMultiplier, durationMultiplier, zone, USER1, INSTANCE1, VMTYPE1 )
        assertVmTypeTotals( zone, diskUsageMultiplier, durationMultiplier, zoneUsageTotals.getTypeTotals(), VMTYPE1 )
        assertVmTypeTotals( zone + " " + ACCOUNT1, diskUsageMultiplier, durationMultiplier, accounts.get(name(ACCOUNT1)).getUsageTotals().getTypeTotals(), VMTYPE1 )
      } else if ( ZONE2.equals( zone ) ) {
        assertUser( accounts, diskUsageMultiplier, durationMultiplier, zone, USER2, INSTANCE2, VMTYPE2 )
        assertVmTypeTotals( zone, diskUsageMultiplier, durationMultiplier, zoneUsageTotals.getTypeTotals(), VMTYPE2 )
        assertVmTypeTotals( zone + " " + ACCOUNT1, diskUsageMultiplier, durationMultiplier, accounts.get(name(ACCOUNT1)).getUsageTotals().getTypeTotals(), VMTYPE2 )
      }
    }
  }

  void assertVmTypeTotals( String description,
                           int diskUsageMultiplier,
                           double durationMultiplier,
                           Map<String, InstanceUsageArtEntity> typeToUsage,
                           String vmType ) {
    assertNotNull( description + " " + vmType, typeToUsage.get(vmType) )
    assertUsage( description + " " + vmType + " total", typeToUsage.get(vmType), diskUsageMultiplier, durationMultiplier )
  }

  private void assertUser( Map<String, AccountArtEntity> accounts, int diskUsageMultiplier, double durationMultiplier, String zone, String user, String instance, String vmType ) {
    UserArtEntity userArtEntity = accounts.get(name(ACCOUNT1)).users.get(name(user))
    assertEquals(zone + "account1 " +user+ " instances", Sets.newHashSet(uuid(instance)), userArtEntity.getInstances().keySet())
    InstanceUsageArtEntity userUsage = userArtEntity.getUsageTotals().getInstanceTotals();
    assertEquals( user + " total usage instances", 1, userUsage.getInstanceCnt())
    assertUsage( user + " total", userUsage, diskUsageMultiplier, durationMultiplier )
    assertVmTypeTotals( zone + " " + ACCOUNT1 + " " + user, diskUsageMultiplier, durationMultiplier, userArtEntity.getUsageTotals().getTypeTotals(), vmType )

    InstanceArtEntity instance1 = userArtEntity.getInstances().get(uuid(instance))
    assertEquals(instance + " id", instance, instance1.getInstanceId())
    assertEquals(instance + " type", vmType, instance1.getInstanceType())
    assertUsage(instance, instance1.getUsage(), diskUsageMultiplier, durationMultiplier)
  }

  private void assertUsage( String description, InstanceUsageArtEntity usage, int diskUsageMultiplier, double durationMultiplier ) {
    assertEquals( description + " duration", (long)(durationMultiplier * ms(12)), usage.getDurationMs() );
    assertEquals( description + " usage net in", 100, usage.getNetTotalInMegs() )
    assertEquals( description + " usage net out", 200, usage.getNetTotalOutMegs() )
    assertEquals( description + " usage cpu ms", ms(6), usage.getCpuUtilizationMs() )
    assertEquals( description + " usage disk read ops", diskUsageMultiplier * 50000, usage.getDiskReadOps() )
    assertEquals( description + " usage disk write ops", diskUsageMultiplier * 20000, usage.getDiskWriteOps() )
    assertEquals( description + " usage disk read size", diskUsageMultiplier * 2000, usage.getDiskReadMegs() )
    assertEquals( description + " usage disk write size", diskUsageMultiplier * 1000, usage.getDiskWriteMegs() )
    assertEquals( description + " usage disk read time", diskUsageMultiplier * 8000, usage.getDiskReadTime() )
    assertEquals( description + " usage disk write time", diskUsageMultiplier * 4000, usage.getDiskWriteTime() )
  }

  private List<ReportingInstanceUsageEvent> usageBeforeReportPeriod() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-08-01T01:00:01", 0, [
        "NetworkIn": 0,
        "NetworkOut": 0,
        "CPUUtilization": 0,
    ], [ "vda": [
        "DiskReadOps": 0,
        "DiskWriteOps": 0,
        "DiskReadBytes": 0,
        "DiskWriteBytes": 0,
        "VolumeTotalReadTime": 0,
        "VolumeTotalWriteTime": 0,
    ] ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> basicUsageInReportPeriod() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T00:00:00", 1, [
        "NetworkIn": 0,
        "NetworkOut": 0,
        "CPUUtilization": 0,
    ], [ "vda": [
        "DiskReadOps": 0,
        "DiskWriteOps": 0,
        "DiskReadBytes": 0,
        "DiskWriteBytes": 0,
        "VolumeTotalReadTime": 0,
        "VolumeTotalWriteTime": 0,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T12:00:00", 2, [
        "NetworkIn": mbd(100),
        "NetworkOut": mbd(200),
        "CPUUtilization": msd(6),
    ], [ "vda": [
        "DiskReadOps": 50000,
        "DiskWriteOps": 20000,
        "DiskReadBytes": mbd(2000),
        "DiskWriteBytes": mbd(1000),
        "VolumeTotalReadTime": 8000,
        "VolumeTotalWriteTime": 4000,
    ] ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> basicUsageInReportPeriodMultipleZones() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    [ INSTANCE1, INSTANCE2 ].each{ instance ->
      addUsage( instanceUsageList, instance, "2012-09-01T00:00:00", 1, [
          "NetworkIn": 0,
          "NetworkOut": 0,
          "CPUUtilization": 0,
      ], [ "vda": [
          "DiskReadOps": 0,
          "DiskWriteOps": 0,
          "DiskReadBytes": 0,
          "DiskWriteBytes": 0,
          "VolumeTotalReadTime": 0,
          "VolumeTotalWriteTime": 0,
      ] ] )
      addUsage( instanceUsageList, instance, "2012-09-01T12:00:00", 2, [
          "NetworkIn": mbd(100),
          "NetworkOut": mbd(200),
          "CPUUtilization": msd(6),
      ], [ "vda": [
          "DiskReadOps": 50000,
          "DiskWriteOps": 20000,
          "DiskReadBytes": mbd(2000),
          "DiskWriteBytes": mbd(1000),
          "VolumeTotalReadTime": 8000,
          "VolumeTotalWriteTime": 4000,
      ] ] )
    }
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> basicUsageInReportPeriodWithMultipleDisks() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T00:00:00", 1, [
        "NetworkIn": 0,
        "NetworkOut": 0,
        "CPUUtilization": 0,
    ], [ "vda": [
        "DiskReadOps": 0,
        "DiskWriteOps": 0,
        "DiskReadBytes": 0,
        "DiskWriteBytes": 0,
        "VolumeTotalReadTime": 0,
        "VolumeTotalWriteTime": 0,
    ], "vdb": [
        "DiskReadOps": 0,
        "DiskWriteOps": 0,
        "DiskReadBytes": 0,
        "DiskWriteBytes": 0,
        "VolumeTotalReadTime": 0,
        "VolumeTotalWriteTime": 0,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T12:00:00", 2, [
        "NetworkIn": mbd(100),
        "NetworkOut": mbd(200),
        "CPUUtilization": msd(6),
    ], [ "vda": [
        "DiskReadOps": 50000,
        "DiskWriteOps": 20000,
        "DiskReadBytes": mbd(2000),
        "DiskWriteBytes": mbd(1000),
        "VolumeTotalReadTime": 8000,
        "VolumeTotalWriteTime": 4000,
    ], "vdb": [
        "DiskReadOps": 100000,
        "DiskWriteOps": 40000,
        "DiskReadBytes": mbd(4000),
        "DiskWriteBytes": mbd(2000),
        "VolumeTotalReadTime": 16000,
        "VolumeTotalWriteTime": 8000,
    ]  ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> basicUsageMultipleEvents() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T00:00:00", 3, [
        "NetworkIn": 0,
        "NetworkOut": 0,
        "CPUUtilization": 0,
    ], [ "vda": [
        "DiskReadOps": 0,
        "DiskWriteOps": 0,
        "DiskReadBytes": 0,
        "DiskWriteBytes": 0,
        "VolumeTotalReadTime": 0,
        "VolumeTotalWriteTime": 0,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T00:00:00", 4, [
        "NetworkIn": mbd(12),
        "NetworkOut": mbd(23),
        "CPUUtilization": msd(1),
    ], [ "vda": [
        "DiskReadOps": 5000,
        "DiskWriteOps": 0000,
        "DiskReadBytes": mbd(100),
        "DiskWriteBytes": mbd(50),
        "VolumeTotalReadTime": 400,
        "VolumeTotalWriteTime": 200,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T06:00:00", 5, [
        "NetworkIn": mbd(50),
        "NetworkOut": mbd(100),
        "CPUUtilization": msd(3),
    ], [ "vda": [
        "DiskReadOps": 25000,
        "DiskWriteOps": 10000,
        "DiskReadBytes": mbd(1000),
        "DiskWriteBytes": mbd(500),
        "VolumeTotalReadTime": 4000,
        "VolumeTotalWriteTime": 2000,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T09:00:00", 6, [
        "NetworkIn": mbd(80),
        "NetworkOut": mbd(140),
        "CPUUtilization": msd(4),
    ], [ "vda": [
        "DiskReadOps": 45000,
        "DiskWriteOps": 17000,
        "DiskReadBytes": mbd(1700),
        "DiskWriteBytes": mbd(777),
        "VolumeTotalReadTime": 6000,
        "VolumeTotalWriteTime": 3000,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T12:00:00", 7, [
        "NetworkIn": mbd(100),
        "NetworkOut": mbd(200),
        "CPUUtilization": msd(6),
    ], [ "vda": [
        "DiskReadOps": 50000,
        "DiskWriteOps": 20000,
        "DiskReadBytes": mbd(2000),
        "DiskWriteBytes": mbd(1000),
        "VolumeTotalReadTime": 8000,
        "VolumeTotalWriteTime": 4000,
    ] ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> interpolatedUsageWithDates( String usage1Timestamp,
                                                                        String usage2Timestamp,
                                                                        int multiplier ) {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, usage1Timestamp, 1, [
        "NetworkIn": 0,
        "NetworkOut": 0,
        "CPUUtilization": 0,
    ], [ "vda": [
        "DiskReadOps": 0,
        "DiskWriteOps": 0,
        "DiskReadBytes": 0,
        "DiskWriteBytes": 0,
        "VolumeTotalReadTime": 0,
        "VolumeTotalWriteTime": 0,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, usage2Timestamp, 2, [
        "NetworkIn": mbd(multiplier * 100),
        "NetworkOut": mbd(multiplier * 200),
        "CPUUtilization": msd(multiplier * 6),
    ], [ "vda": [
        "DiskReadOps": multiplier * 50000,
        "DiskWriteOps": multiplier * 20000,
        "DiskReadBytes": mbd(multiplier * 2000),
        "DiskWriteBytes": mbd(multiplier * 1000),
        "VolumeTotalReadTime": multiplier * 8000,
        "VolumeTotalWriteTime": multiplier * 4000,
    ] ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> sequenceResetUsageExiting() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T00:00:00", 100, [
        "NetworkIn": 0,
        "NetworkOut": 0,
        "CPUUtilization": 0,
    ], [ "vda": [
        "DiskReadOps": 0,
        "DiskWriteOps": 0,
        "DiskReadBytes": 0,
        "DiskWriteBytes": 0,
        "VolumeTotalReadTime": 0,
        "VolumeTotalWriteTime": 0,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T06:00:00", 101, [
        "NetworkIn": mbd(50),
        "NetworkOut": mbd(100),
        "CPUUtilization": msd(3),
    ], [ "vda": [
        "DiskReadOps": 25000,
        "DiskWriteOps": 10000,
        "DiskReadBytes": mbd(1000),
        "DiskWriteBytes": mbd(500),
        "VolumeTotalReadTime": 4000,
        "VolumeTotalWriteTime": 2000,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T12:00:00", 1, [
        "NetworkIn": mbd(50),
        "NetworkOut": mbd(100),
        "CPUUtilization": msd(3),
    ], [ "vda": [
        "DiskReadOps": 25000,
        "DiskWriteOps": 10000,
        "DiskReadBytes": mbd(1000),
        "DiskWriteBytes": mbd(500),
        "VolumeTotalReadTime": 4000,
        "VolumeTotalWriteTime": 2000,
    ] ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> sequenceResetUsage() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T00:00:00", 1, [
        "NetworkIn": mbd(50),
        "NetworkOut": mbd(50),
        "CPUUtilization": msd(3),
    ], [ "vda": [
        "DiskReadOps": 25000,
        "DiskWriteOps": 10000,
        "DiskReadBytes": mbd(1000),
        "DiskWriteBytes": mbd(500),
        "VolumeTotalReadTime": 4000,
        "VolumeTotalWriteTime": 2000,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T06:00:00", 0, [
        "NetworkIn": mbd(50),
        "NetworkOut": mbd(100),
        "CPUUtilization": msd(3),
    ], [ "vda": [
        "DiskReadOps": 25000,
        "DiskWriteOps": 10000,
        "DiskReadBytes": mbd(1000),
        "DiskWriteBytes": mbd(500),
        "VolumeTotalReadTime": 4000,
        "VolumeTotalWriteTime": 2000,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T09:00:00", 0, [
        "NetworkIn": mbd(50),
        "NetworkOut": mbd(100),
        "CPUUtilization": msd(3),
    ], [ "vda": [
        "DiskReadOps": 25000,
        "DiskWriteOps": 10000,
        "DiskReadBytes": mbd(1000),
        "DiskWriteBytes": mbd(500),
        "VolumeTotalReadTime": 4000,
        "VolumeTotalWriteTime": 2000,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T12:00:00", 1, [
        "NetworkIn": mbd(50),
        "NetworkOut": mbd(100),
        "CPUUtilization": msd(3),
    ], [ "vda": [
        "DiskReadOps": 25000,
        "DiskWriteOps": 10000,
        "DiskReadBytes": mbd(1000),
        "DiskWriteBytes": mbd(500),
        "VolumeTotalReadTime": 4000,
        "VolumeTotalWriteTime": 2000,
    ] ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> sequenceResetUsageEntering() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-08-31T23:00:00", 100, [
        "NetworkIn": mbd(500000),
        "NetworkOut": mbd(500000),
        "CPUUtilization": msd(3000),
    ], [ "vda": [
        "DiskReadOps": 1000000,
        "DiskWriteOps": 1000000,
        "DiskReadBytes": mbd(1000000),
        "DiskWriteBytes": mbd(1000000),
        "VolumeTotalReadTime": 1000000,
        "VolumeTotalWriteTime": 1000000,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T01:00:00", 0, [
        "NetworkIn": mbd(100),
        "NetworkOut": mbd(200),
        "CPUUtilization": msd(6),
    ], [ "vda": [
        "DiskReadOps": 50000,
        "DiskWriteOps": 20000,
        "DiskReadBytes": mbd(2000),
        "DiskWriteBytes": mbd(1000),
        "VolumeTotalReadTime": 8000,
        "VolumeTotalWriteTime": 4000,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T12:00:00", 1, [
        "NetworkIn": mbd(150),
        "NetworkOut": mbd(300),
        "CPUUtilization": msd(9),
    ], [ "vda": [
        "DiskReadOps": 75000,
        "DiskWriteOps": 30000,
        "DiskReadBytes": mbd(3000),
        "DiskWriteBytes": mbd(1500),
        "VolumeTotalReadTime": 12000,
        "VolumeTotalWriteTime": 6000,
    ] ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> createdInReportUsage() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T12:00:00", 0, [
        "NetworkIn": mbd(100),
        "NetworkOut": mbd(200),
        "CPUUtilization": msd(6),
    ], [ "vda": [
        "DiskReadOps": 50000,
        "DiskWriteOps": 20000,
        "DiskReadBytes": mbd(2000),
        "DiskWriteBytes": mbd(1000),
        "VolumeTotalReadTime": 8000,
        "VolumeTotalWriteTime": 4000,
    ] ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> createdEnteringReportUsage() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T12:00:00", 0, [
        "NetworkIn": mbd(200),
        "NetworkOut": mbd(400),
        "CPUUtilization": msd(12),
    ], [ "vda": [
        "DiskReadOps": 100000,
        "DiskWriteOps": 40000,
        "DiskReadBytes": mbd(4000),
        "DiskWriteBytes": mbd(2000),
        "VolumeTotalReadTime": 16000,
        "VolumeTotalWriteTime": 8000,
    ] ] )
    instanceUsageList
  }

  private List<ReportingInstanceUsageEvent> createdBeforeUsage() {
    List<ReportingInstanceUsageEvent> instanceUsageList = []
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T06:00:00", 0, [
        "NetworkIn": mbd(100),
        "NetworkOut": mbd(200),
        "CPUUtilization": msd(6),
    ], [ "vda": [
        "DiskReadOps": 50000,
        "DiskWriteOps": 20000,
        "DiskReadBytes": mbd(2000),
        "DiskWriteBytes": mbd(1000),
        "VolumeTotalReadTime": 8000,
        "VolumeTotalWriteTime": 4000,
    ] ] )
    addUsage( instanceUsageList, INSTANCE1, "2012-09-01T12:00:00", 1, [
        "NetworkIn": mbd(100),
        "NetworkOut": mbd(200),
        "CPUUtilization": msd(6),
    ], [ "vda": [
        "DiskReadOps": 50000,
        "DiskWriteOps": 20000,
        "DiskReadBytes": mbd(2000),
        "DiskWriteBytes": mbd(1000),
        "VolumeTotalReadTime": 8000,
        "VolumeTotalWriteTime": 4000,
    ] ] )
    instanceUsageList
  }

  private InstanceArtGenerator testGeneratorWith( List<ReportingInstanceUsageEvent> usage, String instance1CreateTime="2012-08-01T01:00:00" ) {
    List<ReportingInstanceCreateEvent> instanceCreateList = [
        instanceCreate( INSTANCE1, USER1, instance1CreateTime, VMTYPE1, ZONE1 ),
        instanceCreate( INSTANCE2, USER2, "2012-09-01T00:00:00", VMTYPE2, ZONE2 ),
    ]
    List<ReportingInstanceUsageEvent> instanceUsageList = usage.sort{ event -> event.getTimestampMs() }
    new InstanceArtGenerator() {

      @Override
      protected void foreachInstanceUsageEvent( final long startInclusive,
                                                final long endExclusive,
                                                final Predicate<? super ReportingInstanceUsageEvent> callback ) {
        instanceUsageList.findAll{ event ->
          startInclusive <= event.getTimestampMs() && event.getTimestampMs() < endExclusive
        }.every { event -> callback.apply( event ) }
      }

      @Override
      protected void foreachInstanceCreateEvent( final long endExclusive,
                                                 final Predicate<? super ReportingInstanceCreateEvent> callback ) {
        instanceCreateList.reverse().findAll{ event ->
          event.getTimestampMs() < endExclusive }.every { event -> callback.apply( event ) }
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

  private void addUsage( List<ReportingInstanceUsageEvent> instanceUsageList,
                         String instanceId,
                         String timestamp,
                         int sequence,
                         Map<String,Double> metrics,
                         Map<String,Map<String,Double>> diskMetrics ) {
    metrics.each { metric, value ->
      instanceUsageList  \
         << instanceUsage( instanceId, metric, sequence, metricToDimension[metric], value, timestamp )
    }

    diskMetrics.each { disk, metricMap ->
      metricMap.each { metric, value ->
        instanceUsageList  \
         << instanceUsage( instanceId, metric, sequence, disk, value, timestamp )
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
