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
  private static String VMTYPE1 = "vmType1"
  private static String VMTYPE2 = "vmType2"

  @Test
  void testGenerationNoDataInPeriod(){
    InstanceArtGenerator generator = testGenerator( false )
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-08-25T00:00:00"), millis("2012-08-30T00:00:00") ) )
    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )
    assertEquals( "Zones", Collections.emptySet(), art.getZones().keySet() )
    assertEquals( "Total CPU", 0, art.getUsageTotals().getInstanceTotals().getCpuUtilizationMs() );
  }

  @Test
  void testBasicGeneration (){
    InstanceArtGenerator generator = testGenerator( true )

    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T12:00:00") ) )

    assertEquals( "Accounts", Collections.emptySet(), art.getAccounts().keySet() )
    assertEquals( "Zones", Sets.newHashSet(ZONE1), art.getZones().keySet() )

    Map<String,AccountArtEntity> accounts = art.getZones().get(ZONE1).getAccounts()
    assertEquals( "Zone1 accounts", Sets.newHashSet(name(ACCOUNT1)), accounts.keySet() )

    UserArtEntity user1ArtEntity = accounts.get(name(ACCOUNT1)).users.get(name(USER1))
    assertEquals( "Zone1 account1 user1 instances", Sets.newHashSet(uuid(INSTANCE1)), user1ArtEntity.getInstances().keySet() )
    InstanceArtEntity instance1 = user1ArtEntity.getInstances().get(uuid(INSTANCE1))
    assertEquals( "Instance1 id", INSTANCE1, instance1.getInstanceId() )
    assertEquals( "Instance1 type", VMTYPE1, instance1.getInstanceType() )
    assertEquals( "Instance1 usage net in", 100, instance1.getUsage().getNetTotalInMegs() )
    assertEquals( "Instance1 usage net out", 200, instance1.getUsage().getNetTotalOutMegs() )
    assertEquals( "Instance1 usage cpu ms", ms(6), instance1.getUsage().getCpuUtilizationMs() )
//    assertEquals( "Instance1 usage ", , instance1.getUsage(). disk read ops? )
//    assertEquals( "Instance1 usage ", , instance1.getUsage(). disk write ops?)
    assertEquals( "Instance1 usage disk read", 2000, instance1.getUsage().getDiskInMegs() )
    assertEquals( "Instance1 usage disk write", 1000, instance1.getUsage().getDiskOutMegs() )
//    assertEquals( "Instance1 usage ", , instance1.getUsage(). volume read time ?)
//    assertEquals( "Instance1 usage ", , instance1.getUsage(). volume write time ?)
  }

  @SuppressWarnings("GroovyAccessibility")
  private ReportingInstanceCreateEvent instanceCreate(
      String instanceId,
      String userId,
      String timestamp ) {
    new ReportingInstanceCreateEvent( uuid(instanceId), instanceId, millis(timestamp), VMTYPE1, userId, ZONE1 )
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

  private InstanceArtGenerator testGenerator( boolean usageInReportPeriod ) {
    List<ReportingInstanceCreateEvent> instanceCreateList = [
        instanceCreate( INSTANCE1, USER1, "2012-08-01T01:00:00" ),
        instanceCreate( INSTANCE2, USER2, "2012-09-01T01:00:00" ),
    ]
    List<ReportingInstanceUsageEvent> instanceUsageList = [
        instanceUsage( INSTANCE1, "CPUUtilization", 1, "default", 500, "2012-08-01T01:00:01" )
    ]

    if ( usageInReportPeriod ) {
      instanceUsageList \
        << instanceUsage( INSTANCE1, "NetworkIn", 0, "total", 0, "2012-09-01T00:00:00" ) \
        << instanceUsage( INSTANCE1, "NetworkOut", 0, "total", 0, "2012-09-01T00:00:00" ) \
        << instanceUsage( INSTANCE1, "CPUUtilization", 0, "default", 0, "2012-09-01T00:00:00" ) \
        << instanceUsage( INSTANCE1, "DiskReadOps", 0, "vda", 0, "2012-09-01T00:00:00" ) \
        << instanceUsage( INSTANCE1, "DiskWriteOps", 0, "vda", 0, "2012-09-01T00:00:00" ) \
        << instanceUsage( INSTANCE1, "DiskReadBytes", 0, "vda", 0, "2012-09-01T00:00:00" ) \
        << instanceUsage( INSTANCE1, "DiskWriteBytes", 0, "vda", 0, "2012-09-01T00:00:00" ) \
        << instanceUsage( INSTANCE1, "VolumeTotalReadTime", 0, "vda", 0, "2012-09-01T00:00:00" ) \
        << instanceUsage( INSTANCE1, "VolumeTotalWriteTime", 0, "vda", 0, "2012-09-01T00:00:00" )

      instanceUsageList \
        << instanceUsage( INSTANCE1, "NetworkIn", 1, "total", mbd(100), "2012-09-01T12:00:00" ) \
        << instanceUsage( INSTANCE1, "NetworkOut", 1, "total", mbd(200), "2012-09-01T12:00:00" ) \
        << instanceUsage( INSTANCE1, "CPUUtilization", 1, "default", msd(6), "2012-09-01T12:00:00" ) \
        << instanceUsage( INSTANCE1, "DiskReadOps", 1, "vda", 50000, "2012-09-01T12:00:00" ) \
        << instanceUsage( INSTANCE1, "DiskWriteOps", 1, "vda", 20000, "2012-09-01T12:00:00" ) \
        << instanceUsage( INSTANCE1, "DiskReadBytes", 1, "vda", mbd(2000), "2012-09-01T12:00:00" ) \
        << instanceUsage( INSTANCE1, "DiskWriteBytes", 1, "vda", mbd(1000), "2012-09-01T12:00:00" ) \
        << instanceUsage( INSTANCE1, "VolumeTotalReadTime", 1, "vda", 8000, "2012-09-01T12:00:00" ) \
        << instanceUsage( INSTANCE1, "VolumeTotalWriteTime", 1, "vda", 4000, "2012-09-01T12:00:00" ) \
    }

    new InstanceArtGenerator() {
      @Override
      protected Iterator<ReportingInstanceCreateEvent> getInstanceCreateEventIterator() {
        return instanceCreateList.iterator()
      }

      @Override
      protected Iterator<ReportingInstanceUsageEvent> getInstanceUsageEventIterator() {
        return instanceUsageList.iterator();
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
