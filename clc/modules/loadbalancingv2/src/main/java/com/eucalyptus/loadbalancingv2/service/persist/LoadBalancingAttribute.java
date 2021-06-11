/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LoadBalancingAttribute {

  private static final EnumSet<LoadBalancer.Type> ANY = EnumSet.allOf(LoadBalancer.Type.class);
  private static final EnumSet<LoadBalancer.Type> APP = EnumSet.of(LoadBalancer.Type.application);
  private static final EnumSet<LoadBalancer.Type> NET = EnumSet.of(LoadBalancer.Type.network);
  private static final Set<String> VALUES_BOOLEAN = ImmutableSet.of("true", "false");

  public enum Attribute {
    //
    // Load balancer attributes
    //
    LoadBalancerAccessLogsS3Enabled(ANY, "access_logs.s3.enabled", "false", VALUES_BOOLEAN){
      @Override
      public void validate(
          final String value,
          final Map<String, String> attributes
      ) throws IllegalArgumentException {
        super.validate(value, attributes);
        if("true".equals(value)) {
          if (!attributes.containsKey(LoadBalancerAccessLogsS3Bucket.key()) ||
              attributes.get(LoadBalancerAccessLogsS3Bucket.key()).isEmpty() ) {
            throw new IllegalArgumentException("Bucket required when access logging enabled");
          }
        }
      }
    },
    LoadBalancerAccessLogsS3Bucket(ANY, "access_logs.s3.bucket"),
    LoadBalancerAccessLogsS3Prefix(ANY, "access_logs.s3.prefix"),
    LoadBalancerDeletionProtectionEnabled(ANY, "deletion_protection.enabled", "false", VALUES_BOOLEAN),
    LoadBalancerIdleTimeoutSeconds(APP, "idle_timeout.timeout_seconds", "60", 0, 86400),
    LoadBalancerCrossZoneEnabled(NET, "load_balancing.cross_zone.enabled", "false", VALUES_BOOLEAN),
    LoadBalancerRoutingHttpDesyncMode(APP, "routing.http.desync_mitigation_mode", "defensive", ImmutableSet.of("monitor", "defensive", "strictest")),
    LoadBalancerRoutingHttpDropHeaders(APP, "routing.http.drop_invalid_header_fields.enabled", "false", VALUES_BOOLEAN),
    LoadBalancerRoutingHttp2Enabled(APP, "routing.http2.enabled", "true", VALUES_BOOLEAN),
    LoadBalancerWafFailOpenEnabled(APP, "waf.fail_open.enabled", "false", VALUES_BOOLEAN),

    //
    // Target group attributes
    //
    TargetGroupDeregDelayTimeoutSeconds(ANY, "deregistration_delay.timeout_seconds", "300", 0, 3600),
    TargetGroupDeregDelayConnTermEnabled(NET, "deregistration_delay.connection_termination.enabled", "false", VALUES_BOOLEAN),
    TargetGroupBalancingAlgorithmType(APP, "load_balancing.algorithm.type", "round_robin", ImmutableSet.of("round_robin", "least_outstanding_requests")),
    TargetGroupPreserveClientIpEnabled(NET, "preserve_client_ip.enabled", "false", VALUES_BOOLEAN),
    TargetGroupProxyProtocolV2Enabled(NET, "proxy_protocol_v2.enabled", "false", VALUES_BOOLEAN),
    TargetGroupSlowStartDurationSeconds(APP, "slow_start.duration_seconds", "0", 0, 900),
    TargetGroupStickinessEnabled(ANY, "stickiness.enabled", "false", VALUES_BOOLEAN),
    TargetGroupStickinessAppCookieName(APP, "stickiness.app_cookie.cookie_name") {
      @Override
      public void validate(
          final String value,
          final Map<String, String> attributes
      ) throws IllegalArgumentException {
        super.validate(value, attributes);
        final String upperValue = Objects.toString(value, "").toUpperCase();
        if (upperValue.startsWith("AWSALB") ||
            upperValue.startsWith("AWSALBAPP") ||
            upperValue.startsWith("AWSALBTG")) {
          throw new IllegalArgumentException("Reserved cookie name prefix");
        }
      }
    },
    TargetGroupStickinessAppCookieSeconds(APP, "stickiness.app_cookie.duration_seconds", "86400", 1, 604800),
    TargetGroupStickinessLbCookieSeconds(APP, "stickiness.lb_cookie.duration_seconds", "86400", 1, 604800),
    TargetGroupAppStickinessType(ANY, "stickiness.type", null, ImmutableSet.of("app_cookie", "lb_cookie", "source_ip")),
    ;

    private final Class<? extends Loadbalancingv2Metadata> resourceClass;
    private final EnumSet<LoadBalancer.Type> types;
    private final String key;
    private final String defaultValue;
    private final Predicate<String> validator;

    Attribute(final EnumSet<LoadBalancer.Type> types, final String key) {
      this(types, key, null, __ -> true);
    }

    Attribute(
        final EnumSet<LoadBalancer.Type> types,
        final String key,
        final String defaultValue,
        final Set<String> permittedValues
    ) {
      this(types, key, defaultValue, value -> permittedValues.isEmpty() || permittedValues.contains(value));
    }

    Attribute(
        final EnumSet<LoadBalancer.Type> types,
        final String key,
        final String defaultValue,
        final long minValue,
        final long maxValue
    ) {
      this(types, key, defaultValue, value -> {
        long longValue = Long.parseLong(value);
        return longValue >= minValue && longValue <= maxValue;
      });
    }

    Attribute(
        final EnumSet<LoadBalancer.Type> types,
        final String key,
        final String defaultValue,
        final Predicate<String> validator
    ) {
      this.resourceClass = name().startsWith("LoadBalancer") ?
          Loadbalancingv2Metadata.LoadbalancerMetadata.class :
          Loadbalancingv2Metadata.TargetgroupMetadata.class;
      this.types = types;
      this.key = key;
      this.defaultValue = defaultValue;
      this.validator = validator;
    }

    public String key() {
      return key;
    }

    public String defaultValue() {
      return defaultValue;
    }

    public Boolean booleanValue(final Map<String,String> attributes) {
      final String stringValue = stringValue(attributes);
      return Boolean.parseBoolean(stringValue);
    }

    public Integer integerValue(final Map<String,String> attributes) {
      final String stringValue = stringValue(attributes);
      try {
        return Integer.parseUnsignedInt(stringValue);
      } catch (NumberFormatException ignore){}
      return null;
    }

    public String stringValue(final Map<String,String> attributes) {
      return attributes.getOrDefault(key(), defaultValue());
    }

    public void validate(final String value, final Map<String,String> attributes) throws IllegalArgumentException {
      if (value.length() > 1024) throw new IllegalArgumentException("Value too large");
      if (!validator.test(value)) throw new IllegalArgumentException("Invalid value for " + key());
    }
  }

  public static Set<Attribute> attributes(
      final Class<? extends Loadbalancingv2Metadata> resourceClass,
      final LoadBalancer.Type type
  ) {
    final Set<Attribute> attributes = Sets.newLinkedHashSet();
    for (final Attribute attribute : Attribute.values()) {
      if ((type == null || attribute.types.contains(type)) &&
          resourceClass.equals(attribute.resourceClass)) {
        attributes.add(attribute);
      }
    }
    return attributes;
  }

  public static Map<String, String> defaults(
      @Nonnull final Class<? extends Loadbalancingv2Metadata> resourceClass
  ) {
    return defaults(resourceClass, null);
  }

  public static Map<String, String> defaults(
      @Nonnull  final Class<? extends Loadbalancingv2Metadata> resourceClass,
      @Nullable final LoadBalancer.Type type
  ) {
    final Map<String,String> attributeMap = Maps.newLinkedHashMap();
    for (final Attribute attribute : Attribute.values()) {
      if ((type == null || attribute.types.contains(type)) &&
          resourceClass.equals(attribute.resourceClass)) {
        attributeMap.put(attribute.key(), attribute.defaultValue());
      }
    }
    return attributeMap;
  }

  public static <T extends Throwable> void validate(
      @Nonnull  final Function<String, T> onFail,
      @Nonnull  final Class<? extends Loadbalancingv2Metadata> resourceClass,
      @Nullable final LoadBalancer.Type type,
      @Nonnull  final Map<String,String> attributeMap
  ) throws T {
    try {
      final Set<String> allowedKeys = Sets.newHashSet();
      for (final Attribute attribute : attributes(resourceClass, type)) {
        allowedKeys.add(attribute.key());
        if (attributeMap.containsKey(attribute.key())) {
          attribute.validate(attributeMap.get(attribute.key()), attributeMap);
        }
      }
      if (!allowedKeys.containsAll(attributeMap.keySet())) {
        throw onFail.apply("Unknown attribute key(s)");
      }
    } catch(final IllegalArgumentException e) {
      throw onFail.apply(e.getMessage());
    }
  }
}
