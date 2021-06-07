/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common;


import spock.lang.Specification

import static com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName.parse
import static com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName.Type.forResourceType

/**
 *
 */
class Loadbalancingv2ResourceNameSpecification extends Specification {

  def 'should support known arn types'() {
    expect: 'arn types are parsed correctly'
    parse( arn ).type == type

    where:
    type            | arn
    'listener'      | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:listener/app/my-load-balancer/50dc6c495c0c9188/f2f7dc8efc522ab2'
    'listener-rule' | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:listener-rule/app/my-load-balancer/50dc6c495c0c9188/f2f7dc8efc522ab2/9683b2d02a6cabee'
    'loadbalancer'  | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:loadbalancer/app/my-load-balancer/50dc6c495c0c9188'
    'targetgroup'   | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:targetgroup/my-targets/73e2d6bc24d8a067'
  }

  def 'should support arn subtypes'() {
    expect: 'arn type and subtype are parsed correctly'
    parse( arn ).type == type
    parse( arn ).getSubType(forResourceType(type).get()) == subType

    where:
    type            | subType | arn
    'listener'      | 'app' | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:listener/app/my-load-balancer/50dc6c495c0c9188/f2f7dc8efc522ab2'
    'listener'      | 'net' | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:listener/net/my-load-balancer/50dc6c495c0c9188/f2f7dc8efc522ab2'
    'listener-rule' | 'app' | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:listener-rule/app/my-load-balancer/50dc6c495c0c9188/f2f7dc8efc522ab2/9683b2d02a6cabee'
    'listener-rule' | 'net' | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:listener-rule/net/my-load-balancer/50dc6c495c0c9188/f2f7dc8efc522ab2/9683b2d02a6cabee'
    'loadbalancer'  | 'app' | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:loadbalancer/app/my-load-balancer/50dc6c495c0c9188'
    'loadbalancer'  | 'net' | 'arn:aws:elasticloadbalancing:eucalyptus:123456789012:loadbalancer/net/my-load-balancer/50dc6c495c0c9188'
  }
}
