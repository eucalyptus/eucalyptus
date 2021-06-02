/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.PropertyChangeListeners;
import java.util.function.Supplier;

@ConfigurableClass(root = "services.loadbalancingv2.limit", description = "Parameters for loadbalancing limits")
public class Loadbalancingv2LimitProperties {

  public enum Limit {
    application_load_balancers(() ->
        APPLICATION_LOAD_BALANCERS),
    certificates_per_application_load_balancer(() ->
        CERTIFICATES_PER_APPLICATION_LOAD_BALANCER),
    certificates_per_network_load_balancer(() ->
        CERTIFICATES_PER_NETWORK_LOAD_BALANCER),
    condition_values_per_alb_rule(() ->
        CONDITION_VALUES_PER_ALB_RULE),
    condition_wildcards_per_alb_rule(() ->
        CONDITION_WILDCARDS_PER_ALB_RULE),
    gateway_load_balancers(() ->
        20),
    gateway_load_balancers_per_vpc(() ->
        10),
    geneve_target_groups(() ->
        50),
    listeners_per_application_load_balancer(() ->
        LISTENERS_PER_APPLICATION_LOAD_BALANCER),
    listeners_per_network_load_balancer(() ->
        LISTENERS_PER_NETWORK_LOAD_BALANCER),
    network_load_balancer_enis_per_vpc(() ->
        NETWORK_LOAD_BALANCER_ENIS_PER_VPC),
    network_load_balancers(() ->
        NETWORK_LOAD_BALANCERS),
    rules_per_application_load_balancer(() ->
        RULES_PER_APPLICATION_LOAD_BALANCER),
    target_groups(() ->
        TARGET_GROUPS),
    target_groups_per_action_on_application_load_balancer(() ->
        TARGET_GROUPS_PER_ACTION_ON_APPLICATION_LOAD_BALANCER),
    target_groups_per_action_on_network_load_balancer(() ->
        TARGET_GROUPS_PER_ACTION_ON_NETWORK_LOAD_BALANCER),
    target_groups_per_application_load_balancer(() ->
        TARGET_GROUPS_PER_APPLICATION_LOAD_BALANCER),
    target_id_registrations_per_application_load_balancer(() ->
        TARGET_ID_REGISTRATIONS_PER_APPLICATION_LOAD_BALANCER),
    targets_per_application_load_balancer(() ->
        TARGETS_PER_APPLICATION_LOAD_BALANCER),
    targets_per_availability_zone_per_gateway_load_balancer(() ->
        TARGETS_PER_AVAILABILITY_ZONE_PER_GATEWAY_LOAD_BALANCER),
    targets_per_availability_zone_per_network_load_balancer(() ->
        TARGETS_PER_AVAILABILITY_ZONE_PER_NETWORK_LOAD_BALANCER),
    targets_per_network_load_balancer(() ->
        TARGETS_PER_NETWORK_LOAD_BALANCER),
    targets_per_target_group(() ->
        TARGETS_PER_TARGET_GROUP),
    ;

    private final Supplier<Integer> limitSupplier;

    Limit(final Supplier<Integer> limitSupplier) {
      this.limitSupplier = limitSupplier;
    }

    public String display() {
      return name().replace('_', '-');
    }

    public int value() {
      return limitSupplier.get();
    }
  }

  @ConfigurableField(
      initial = "50",
      description = "Maximum number of application load balancers.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int APPLICATION_LOAD_BALANCERS = 50;

  @ConfigurableField(
      initial = "25",
      description = "Maximum number of certificates per application load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int CERTIFICATES_PER_APPLICATION_LOAD_BALANCER = 25;

  @ConfigurableField(
      initial = "25",
      description = "Maximum number of certificates per network load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int CERTIFICATES_PER_NETWORK_LOAD_BALANCER = 25;

  @ConfigurableField(
      initial = "5",
      description = "Maximum number of condition values per application load balancer rule.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int CONDITION_VALUES_PER_ALB_RULE = 5;

  @ConfigurableField(
      initial = "5",
      description = "Maximum number of condition wildcards per application load balancer rule.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int CONDITION_WILDCARDS_PER_ALB_RULE = 5;

  @ConfigurableField(
      initial = "50",
      description = "Maximum number of listeners per application load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int LISTENERS_PER_APPLICATION_LOAD_BALANCER = 50;

  @ConfigurableField(
      initial = "50",
      description = "Maximum number of listeners per network load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int LISTENERS_PER_NETWORK_LOAD_BALANCER = 50;

  @ConfigurableField(
      initial = "300",
      description = "Maximum number of network load balancer ENIs per VPC.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int NETWORK_LOAD_BALANCER_ENIS_PER_VPC = 300;

  @ConfigurableField(
      initial = "50",
      description = "Maximum number of network load balancers.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int NETWORK_LOAD_BALANCERS = 50;

  @ConfigurableField(
      initial = "100",
      description = "Maximum number of rules per application load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int RULES_PER_APPLICATION_LOAD_BALANCER = 100;

  @ConfigurableField(
      initial = "3000",
      description = "Maximum number of target groups.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGET_GROUPS = 3000;

  @ConfigurableField(
      initial = "5",
      description = "Maximum number of target groups per application load balancer action.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGET_GROUPS_PER_ACTION_ON_APPLICATION_LOAD_BALANCER = 5;

  @ConfigurableField(
      initial = "1",
      description = "Maximum number of target groups per network load balancer action..",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGET_GROUPS_PER_ACTION_ON_NETWORK_LOAD_BALANCER = 1;

  @ConfigurableField(
      initial = "100",
      description = "Maximum number of target groups per application load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGET_GROUPS_PER_APPLICATION_LOAD_BALANCER = 100;

  @ConfigurableField(
      initial = "100",
      description = "Maximum number of target identifier registrations per application load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGET_ID_REGISTRATIONS_PER_APPLICATION_LOAD_BALANCER = 100;

  @ConfigurableField(
      initial = "1000",
      description = "Maximum number of targets per application load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGETS_PER_APPLICATION_LOAD_BALANCER = 1000;

  @ConfigurableField(
      initial = "300",
      description = "Maximum number of targets per zone per gateway load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGETS_PER_AVAILABILITY_ZONE_PER_GATEWAY_LOAD_BALANCER = 300;

  @ConfigurableField(
      initial = "500",
      description = "Maximum number of targets per zone per network load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGETS_PER_AVAILABILITY_ZONE_PER_NETWORK_LOAD_BALANCER = 500;

  @ConfigurableField(
      initial = "3000",
      description = "Maximum number of targets per network load balancer.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGETS_PER_NETWORK_LOAD_BALANCER = 3000;

  @ConfigurableField(
      initial = "1000",
      description = "Maximum number of targets per target group.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int TARGETS_PER_TARGET_GROUP = 1000;

  @ConfigurableField(
      initial = "50",
      description = "Maximum number of user defined tags for a resource.",
      changeListener = PropertyChangeListeners.IsNonNegativeInteger.class)
  public static volatile int MAX_TAGS = 50;

}
