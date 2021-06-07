/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common;

import spock.lang.Specification

/**
 *
 */
class Loadbalancingv2MessageValidationSpecification extends Specification {

  def 'should accept valid load balancer names'() {
    expect: 'name matches validation regex'
    Loadbalancingv2MessageValidation.FieldRegexValue.LOADBALANCING_NAME.pattern().matcher(name).matches()

    where:
    _ | name
    _ | 'a'
    _ | 'ab'
    _ | 'abc'
    _ | 'abcd'
    _ | 'abcde'
    _ | 'abcdef'
    _ | 'abcdefghijklmnopqrstuvwzyz012'
    _ | 'abcdefghijklmnopqrstuvwzyz0123'
    _ | 'abcdefghijklmnopqrstuvwzyz01234'
    _ | 'abcdefghijklmnopqrstuvwzyz012345'
    _ | 'a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p1'
    _ | 'a-b'
    _ | 'a--b'
    _ | 'a---b'
    _ | 'ainternal-a'
    _ | 'a-internal-a'
  }

  def 'should reject invalid load balancer names'() {
    expect: 'name does not match validation regex'
    !Loadbalancingv2MessageValidation.FieldRegexValue.LOADBALANCING_NAME.pattern().matcher(name).matches()

    where:
    _ | name
    _ | ''
    _ | 'abcdefghijklmnopqrstuvwzyz0123456'
    _ | '-'
    _ | '-a-'
    _ | '-a'
    _ | 'a-'
    _ | '-ab'
    _ | 'ab-'
    _ | '-abc'
    _ | 'abc-'
    _ | '-abcdefghijklmnopqrstuvwzyz012'
    _ | 'abcdefghijklmnopqrstuvwzyz012-'
    _ | '-abcdefghijklmnopqrstuvwzyz0123'
    _ | 'abcdefghijklmnopqrstuvwzyz0123-'
    _ | '-abcdefghijklmnopqrstuvwzyz01234'
    _ | 'abcdefghijklmnopqrstuvwzyz01234-'
  }

  def 'should accept valid security policy names'() {
    expect: 'name matches validation regex'
    Loadbalancingv2MessageValidation.FieldRegexValue.SECURITY_POLICY.pattern().matcher(name).matches()

    where:
    _ | name
    _ | 'ELBSecurityPolicy-2016-08'
    _ | 'ELBSecurityPolicy-TLS-1-0-2015-04'
    _ | 'ELBSecurityPolicy-TLS-1-1-2017-01'
    _ | 'ELBSecurityPolicy-TLS-1-2-2017-01'
    _ | 'ELBSecurityPolicy-TLS-1-2-Ext-2018-06'
    _ | 'ELBSecurityPolicy-FS-2018-06'
    _ | 'ELBSecurityPolicy-FS-1-1-2019-08'
    _ | 'ELBSecurityPolicy-FS-1-2-2019-08'
    _ | 'ELBSecurityPolicy-FS-1-2-Res-2019-08'
    _ | 'ELBSecurityPolicy-2015-05'
    _ | 'ELBSecurityPolicy-FS-1-2-Res-2020-10'
  }

  def 'should reject invalid security policy names'() {
    expect: 'name does not match validation regex'
    !Loadbalancingv2MessageValidation.FieldRegexValue.SECURITY_POLICY.pattern().matcher(name).matches()

    where:
    _ | name
    _ | 'ELBSecurityPolicy-'
    _ | ''
    _ | ' ELBSecurityPolicy-TLS-1-1-2017-01'
  }

  def 'should accept valid redirect port or pattern'() {
    expect: 'port matches validation regex'
    Loadbalancingv2MessageValidation.FieldRegexValue.REDIRECT_PORT.pattern().matcher(port).matches()

    where:
    _ | port
    _ | '1'
    _ | '9'
    _ | '10'
    _ | '19'
    _ | '80'
    _ | '100'
    _ | '199'
    _ | '443'
    _ | '999'
    _ | '1000'
    _ | '9999'
    _ | '10000'
    _ | '59999'
    _ | '60000'
    _ | '64999'
    _ | '65000'
    _ | '65499'
    _ | '65500'
    _ | '65529'
    _ | '65530'
    _ | '65535'
    _ | '#{port}'
  }

  def 'should reject invalid redirect port or pattern'() {
    expect: 'port does not match validation regex'
    !Loadbalancingv2MessageValidation.FieldRegexValue.REDIRECT_PORT.pattern().matcher(port).matches()

    where:
    _ | port
    _ | '-1'
    _ | '0'
    _ | '00'
    _ | '000'
    _ | '0000'
    _ | '00000'
    _ | '01'
    _ | '09'
    _ | '65536'
    _ | '65540'
    _ | '65600'
    _ | '66000'
    _ | '70000'
    _ | '#{por}'
    _ | '#{portt}'
    _ | '#{1}'
  }

  def 'should accept valid code values or range'() {
    expect: 'code matches validation regex'
    Loadbalancingv2MessageValidation.FieldRegexValue.CODE_VALUES_OR_RANGE.pattern().matcher(code).matches()

    where:
    _ | code
    _ | '0'
    _ | '1'
    _ | '9'
    _ | '10'
    _ | '12'
    _ | '19'
    _ | '80'
    _ | '99'
    _ | '200'
    _ | '499'
    _ | '1,1,1,1,1,1'
    _ | '1,2,3,4,5,6,7,8,9'
    _ | '10,100,200,300,400'
    _ | '0-0'
    _ | '0-99'
    _ | '1-99'
    _ | '200-299'
    _ | '200-399'
  }

  def 'should reject invalid code values or range'() {
    expect: 'code does not match validation regex'
    !Loadbalancingv2MessageValidation.FieldRegexValue.CODE_VALUES_OR_RANGE.pattern().matcher(code).matches()

    where:
    _ | code
    _ | '-1'
    _ | '1000'
    _ | '1-'
    _ | '-1'
    _ | '1-1-1'
    _ | '1-1,2-2'
    _ | '1,'
    _ | ',1'
    _ | '1,1,1,1,1,'
    _ | ',1,1,1,1,1'
  }
}