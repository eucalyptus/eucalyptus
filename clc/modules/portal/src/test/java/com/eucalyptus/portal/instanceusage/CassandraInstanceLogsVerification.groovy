package com.eucalyptus.portal.instanceusage

import com.datastax.driver.core.SimpleStatement
import com.eucalyptus.portal.awsusage.CassandraSessionManager
import com.eucalyptus.portal.common.model.InstanceUsageFilter
import com.eucalyptus.portal.common.model.InstanceUsageFilters
import com.eucalyptus.portal.workflow.InstanceLog
import com.eucalyptus.portal.workflow.InstanceTag
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by ethomas on 5/9/17.
 */
class CassandraInstanceLogsVerificationImpl {

  static String ACCOUNT_1 = "1" + System.currentTimeMillis();
  static String ACCOUNT_2 = "2" + System.currentTimeMillis();
  static String BAD_ACCOUNT = "3" + System.currentTimeMillis();

  static String INSTANCE_1 = "i-000001";
  static String INSTANCE_2 = "i-000002";
  static String INSTANCE_3 = "i-000003";
  static String INSTANCE_4 = "i-000004";
  static String INSTANCE_5 = "i-000005";
  static String INSTANCE_6 = "i-000006";
  static String INSTANCE_7 = "i-000007";
  static String INSTANCE_8 = "i-000008";
  static String INSTANCE_9 = "i-000009";
  static String INSTANCE_10 = "i-000010";
  static String INSTANCE_11 = "i-000011";
  static String INSTANCE_12 = "i-000012";
  static String INSTANCE_13 = "i-000013";
  static String INSTANCE_14 = "i-000014";
  static String INSTANCE_15 = "i-000015";
  static String INSTANCE_16 = "i-000016";

  static String TYPE_1 = "small";
  static String TYPE_2 = "medium";
  static String TYPE_3 = "large";
  static String BAD_TYPE = "jumbo";

  static String AZ_1 = "one";
  static String AZ_2 = "two";
  static String AZ_3 = "three";
  static String AZ_4 = "four";
  static String BAD_AZ = "five";

  static String PLATFORM_1 = "platform1";
  static String PLATFORM_2 = "platform2";
  static String BAD_PLATFORM = "platform3";

  static String REGION_1 = "region1";
  static String REGION_2 = "region2";

  static Date now = new Date();
  static Date LOG_TIME_1 = new Date(now.getTime() + 1 * 60000L);
  static Date LOG_TIME_2 = new Date(now.getTime() + 2 * 60000L);
  static Date LOG_TIME_3 = new Date(now.getTime() + 3 * 60000L);
  static Date LOG_TIME_4 = new Date(now.getTime() + 4 * 60000L);
  static Date LOG_TIME_5 = new Date(now.getTime() + 5 * 60000L);

  static List<TagForEquals> TAGS_1 = Lists.newArrayList();
  static List<TagForEquals> TAGS_2 = Lists.newArrayList(new TagForEquals(key: "key1", value: "value1"));
  static List<TagForEquals> TAGS_3 = Lists.newArrayList(new TagForEquals(key: "key1", value: "value2"));
  static List<TagForEquals> TAGS_4 = Lists.newArrayList(new TagForEquals(key: "key2", value: "value3"));
  static List<TagForEquals> TAGS_5 = Lists.newArrayList(new TagForEquals(key: "key2", value: "value4"));
  static List<TagForEquals> TAGS_6 = Lists.newArrayList(new TagForEquals(key: "key1", value: "value1"),
    new TagForEquals(key: "key2", value: "value3"));
  static List<TagForEquals> TAGS_7 = Lists.newArrayList(new TagForEquals(key: "key1", value: "value1"),
    new TagForEquals(key: "key2", value: "value4"));
  static List<TagForEquals> TAGS_8 = Lists.newArrayList(new TagForEquals(key: "key1", value: "value2"),
    new TagForEquals(key: "key2", value: "value3"));
  static List<TagForEquals> TAGS_9 = Lists.newArrayList(new TagForEquals(key: "key1", value: "value2"),
    new TagForEquals(key: "key2", value: "value4"));

  static List<TagForEquals> BAD_TAGS_1 = Lists.newArrayList(new TagForEquals(key: "key1", value: "value3"));
  static List<TagForEquals> BAD_TAGS_2 = Lists.newArrayList(new TagForEquals(key: "key1", value: "value2"),
    new TagForEquals(key: "key2", value: "value4"));

  static Collection<InstanceLog> records = [
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_1, instanceType: TYPE_1, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_2, tags: TAGS_1),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_1, instanceType: TYPE_1, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_3, tags: TAGS_2),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_1, instanceType: TYPE_1, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_4, tags: TAGS_3),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_2, instanceType: TYPE_2, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_2, tags: TAGS_4),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_2, instanceType: TYPE_2, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_3, tags: TAGS_5),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_2, instanceType: TYPE_2, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_4, tags: TAGS_6),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_3, instanceType: TYPE_3, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_2, tags: TAGS_7),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_3, instanceType: TYPE_3, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_3, tags: TAGS_8),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_3, instanceType: TYPE_3, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_4, tags: TAGS_9),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_4, instanceType: TYPE_1, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_2, tags: TAGS_1),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_4, instanceType: TYPE_1, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_3, tags: TAGS_2),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_4, instanceType: TYPE_1, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_4, tags: TAGS_3),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_5, instanceType: TYPE_2, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_2, tags: TAGS_4),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_5, instanceType: TYPE_2, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_3, tags: TAGS_5),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_5, instanceType: TYPE_2, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_4, tags: TAGS_6),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_6, instanceType: TYPE_3, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_2, tags: TAGS_7),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_6, instanceType: TYPE_3, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_3, tags: TAGS_8),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_6, instanceType: TYPE_3, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_4, tags: TAGS_9),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_7, instanceType: TYPE_1, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_2, tags: TAGS_1),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_7, instanceType: TYPE_1, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_3, tags: TAGS_2),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_7, instanceType: TYPE_1, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_4, tags: TAGS_3),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_8, instanceType: TYPE_2, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_2, tags: TAGS_4),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_8, instanceType: TYPE_2, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_3, tags: TAGS_5),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_1, instanceId: INSTANCE_8, instanceType: TYPE_2, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_4, tags: TAGS_6),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_9, instanceType: TYPE_3, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_2, tags: TAGS_7),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_9, instanceType: TYPE_3, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_3, tags: TAGS_8),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_9, instanceType: TYPE_3, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_4, tags: TAGS_9),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_10, instanceType: TYPE_1, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_2, tags: TAGS_1),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_10, instanceType: TYPE_1, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_3, tags: TAGS_2),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_10, instanceType: TYPE_1, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_1, logTime: LOG_TIME_4, tags: TAGS_3),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_11, instanceType: TYPE_2, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_2, tags: TAGS_4),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_11, instanceType: TYPE_2, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_3, tags: TAGS_5),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_11, instanceType: TYPE_2, platform: PLATFORM_1,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_4, tags: TAGS_6),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_12, instanceType: TYPE_3, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_2, tags: TAGS_7),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_12, instanceType: TYPE_3, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_3, tags: TAGS_8),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_12, instanceType: TYPE_3, platform: PLATFORM_2,
        region: REGION_1, availabilityZone: AZ_2, logTime: LOG_TIME_4, tags: TAGS_9),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_13, instanceType: TYPE_1, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_2, tags: TAGS_1),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_13, instanceType: TYPE_1, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_3, tags: TAGS_2),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_13, instanceType: TYPE_1, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_4, tags: TAGS_3),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_14, instanceType: TYPE_2, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_2, tags: TAGS_4),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_14, instanceType: TYPE_2, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_3, tags: TAGS_5),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_14, instanceType: TYPE_2, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_3, logTime: LOG_TIME_4, tags: TAGS_6),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_15, instanceType: TYPE_3, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_2, tags: TAGS_7),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_15, instanceType: TYPE_3, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_3, tags: TAGS_8),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_15, instanceType: TYPE_3, platform: PLATFORM_1,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_4, tags: TAGS_9),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_16, instanceType: TYPE_1, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_2, tags: TAGS_1),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_16, instanceType: TYPE_1, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_3, tags: TAGS_2),
    new InstanceLogs.SimpleInstanceLog
      (accountId: ACCOUNT_2, instanceId: INSTANCE_16, instanceType: TYPE_1, platform: PLATFORM_2,
        region: REGION_2, availabilityZone: AZ_4, logTime: LOG_TIME_4, tags: TAGS_3),
  ];

  @CompileStatic
  @EqualsAndHashCode
  @ToString
  static class TagForEquals implements InstanceTag {
    String key;
    String value;
  }

  @CompileStatic
  @EqualsAndHashCode
  @ToString
  static class HourLogForEquals implements InstanceHourLog {
    String instanceId;
    String instanceType;
    String platform;
    String region;
    String availabilityZone;
    Date logTime;
    Long hours;
    List<TagForEquals> tags = Lists.newArrayList();

    @Override
    void setHours(long hours) {
      this.hours = hours;
    }
  }


  public static List<HourLogForEquals> queryHourly(List<InstanceLog> sample,
                                                   final String accountId,
                                                   final Date rangeStart,
                                                   final Date rangeEnd,
                                                   final InstanceUsageFilters filters) {
    if (sample == null) return sample;
    // get filters
    Set<String> instanceTypes = Sets.newHashSet();
    Set<String> platforms = Sets.newHashSet();
    Set<String> availabilityZones = Sets.newHashSet();
    Set<TagForEquals> tags = Sets.newHashSet();
    if (filters != null && filters.member != null) {
      for (InstanceUsageFilter filter : filters.member) {
        if (filter == null || filter.type == null || filter.key == null) continue;
        if ("instancetype".equals(filter.type.toLowerCase())
          || "instance_type".equals(filter.type.toLowerCase())) {
          instanceTypes.add(filter.key);
        } else if ("platform".equals(filter.type.toLowerCase())
          || "platforms".equals(filter.type.toLowerCase())) {
          platforms.add(filter.key)
        } else if ("availabilityzone".equals(filter.type.toLowerCase())
          || "availability_zone".equals(filter.type.toLowerCase())) {
          availabilityZones.add(filter.key)
        } else if (filter.value != null && ("tag".equals(filter.type.toLowerCase())
          || "tags".equals(filter.type.toLowerCase()))) {
          tags.add(new TagForEquals(key: filter.key, value: filter.value));
        }
      }
    }
    return sample.findAll {
      InstanceLog it ->
        it.accountId == accountId &&  \
      (instanceTypes.isEmpty() || instanceTypes.contains(it.instanceType)) &&  \
      (platforms.isEmpty() || platforms.contains(it.platform)) &&  \
      (availabilityZones.isEmpty() || availabilityZones.contains(it.availabilityZone)) &&  \
      (rangeStart == null || it.logTime >= rangeStart) &&  \
      (rangeEnd == null || it.logTime <= rangeEnd) &&  \
      tagFilterMatches(it.tags, tags)
    }.collect {
      InstanceLog log ->
        new HourLogForEquals(
          hours: 1, instanceId: log.instanceId, instanceType: log.instanceType, platform: log.platform,
          region: log.region, availabilityZone: log.availabilityZone, logTime: log.logTime, tags: log.tags.collect({
          InstanceTag tag -> new TagForEquals(key: tag.key, value: tag.value)
        }))
    };
  }

  private static boolean tagFilterMatches(Collection<TagForEquals> tags, Collection<TagForEquals> filterTags) {
    if (filterTags.isEmpty()) return true; // if no tags to match in filter, success
    return !Collections.disjoint(tags, filterTags); // otherwise at least one tag must be a filter tag
  }

  static String verify() {
    if (!"cassandra".equals(CassandraSessionManager.DB_TO_USE) && !"euca-cassandra".equals(CassandraSessionManager.DB_TO_USE)) {
      return "Error: not configured to use cassandra";
    }
    try {
      InstanceLogs.getInstance().append(records);
      List<List<InstanceTag>> listsOfTags = [TAGS_1, TAGS_2, TAGS_3, TAGS_4, TAGS_5, TAGS_6, TAGS_7, TAGS_8, TAGS_9, BAD_TAGS_1, BAD_TAGS_2];

      int tagPos = 0;

      for (String accountId : [ACCOUNT_1, ACCOUNT_2, BAD_ACCOUNT]) {
        for (Date rangeStart : [LOG_TIME_1, LOG_TIME_2, LOG_TIME_3, LOG_TIME_4, LOG_TIME_5, null]) {
          for (Date rangeEnd : [LOG_TIME_1, LOG_TIME_2, LOG_TIME_3, LOG_TIME_4, LOG_TIME_5, null]) {
            for (String instanceType : [TYPE_1, TYPE_2, TYPE_3, BAD_TYPE, null]) {
              for (String platform : [PLATFORM_1, PLATFORM_2, BAD_PLATFORM, null]) {
                for (String availabilityZone : [AZ_1, AZ_2, AZ_3, AZ_4, BAD_AZ, null]) {
                  for (int i = 0; i < 3; i++) {
                    List<InstanceTag> tags = listsOfTags.get(tagPos++ % listsOfTags.size());
                    InstanceUsageFilters filters = new InstanceUsageFilters();
                    if (instanceType != null) {
                      filters.member.add(new InstanceUsageFilter(type: "instance_type", key: instanceType))
                    }
                    if (platform != null) {
                      filters.member.add(new InstanceUsageFilter(type: "platform", key: platform))
                    }
                    if (availabilityZone != null) {
                      filters.member.add(new InstanceUsageFilter(type: "availability_zone", key: availabilityZone));
                    }
                    for (InstanceTag tag : tags) {
                      filters.member.add(new InstanceUsageFilter(type: "tag", key: tag.key, value: tag.value));
                    }
                    assert (

                      InstanceLogs.getInstance().queryHourly(
                        accountId, rangeStart, rangeEnd, filters
                      ).collect {
                        InstanceHourLog it ->
                          new HourLogForEquals(hours: it.hours, instanceId: it.instanceId, instanceType: it.instanceType,
                            platform: it.platform, region: it.region, availabilityZone: it.availabilityZone,
                            logTime: it.logTime,
                            tags: it.tags.collect {
                              InstanceTag tag -> new TagForEquals(key: tag.key, value: tag.value)
                            }
                          )
                      }.toSet()
                        ==

                        queryHourly(records, accountId, rangeStart, rangeEnd, filters).toSet()
                    )
                  }
                }
              }
            }
          }
        }
      }
      for (String accountId : [ACCOUNT_1, ACCOUNT_2]) {
        CassandraSessionManager.doWithSession {
          it.execute(
            new SimpleStatement("DELETE FROM eucalyptus_billing.instance_log where account_id=?", accountId)
          )
        };
        for (String instanceType : [TYPE_1, TYPE_2, TYPE_3]) {
          CassandraSessionManager.doWithSession {
            it.execute(
              new SimpleStatement("DELETE FROM eucalyptus_billing.instance_log_by_inst_type where account_id=? AND instance_type=?", accountId, instanceType)
            )
          };
          for (String platform : [PLATFORM_1, PLATFORM_2]) {
            CassandraSessionManager.doWithSession {
              it.execute(
                new SimpleStatement("DELETE FROM eucalyptus_billing.instance_log_by_inst_type_and_platform where account_id=? AND instance_type=? AND platform=?",
                  accountId, instanceType, platform)
              )
            };
            for (String availabilityZone : [AZ_1, AZ_2, AZ_3, AZ_4]) {
              CassandraSessionManager.doWithSession {
                it.execute(
                  new SimpleStatement("DELETE FROM eucalyptus_billing.instance_log_by_inst_type_platform_and_az where account_id=? AND instance_type=? " +
                    "AND platform=? AND availability_zone=?",
                    accountId, instanceType, platform, availabilityZone)
                )
              };
            }
          }
          for (String availabilityZone : [AZ_1, AZ_2, AZ_3, AZ_4]) {
            CassandraSessionManager.doWithSession {
              it.execute(
                new SimpleStatement("DELETE FROM eucalyptus_billing.instance_log_by_inst_type_and_az where account_id=? AND instance_type=? AND availability_zone=?",
                  accountId, instanceType, availabilityZone)
              )
            };
          }
        }
        for (String platform : [PLATFORM_1, PLATFORM_2]) {
          CassandraSessionManager.doWithSession {
            it.execute(
              new SimpleStatement("DELETE FROM eucalyptus_billing.instance_log_by_platform where account_id=? AND platform=?",
                accountId, platform)
            )
          };
          for (String availabilityZone : [AZ_1, AZ_2, AZ_3, AZ_4]) {
            CassandraSessionManager.doWithSession {
              it.execute(
                new SimpleStatement("DELETE FROM eucalyptus_billing.instance_log_by_platform_and_az where account_id=? AND platform=? AND availability_zone=?",
                  accountId, platform, availabilityZone)
              )
            };
          }
        }
        for (String availabilityZone : [AZ_1, AZ_2, AZ_3, AZ_4]) {
          CassandraSessionManager.doWithSession {
            it.execute(
              new SimpleStatement("DELETE FROM eucalyptus_billing.instance_log_by_az where account_id=? AND availability_zone=?",
                accountId, availabilityZone)
            )
          };
        }
      }
    } catch (Exception e) {
      return "Error: " + e.getMessage() == null ? e.getClass().getName() : e.getMessage();
    }
    return "Success"
  }
}

com.eucalyptus.portal.instanceusage.CassandraInstanceLogsVerificationImpl.verify();
