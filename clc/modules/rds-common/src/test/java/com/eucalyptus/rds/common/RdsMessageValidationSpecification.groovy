/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common

import spock.lang.Specification

/**
 *
 */
class RdsMessageValidationSpecification extends Specification {

  def 'should accept valid database instance identifiers'() {
    expect: 'database instance identifier matches validation regex'
    RdsMessageValidation.
            FieldRegexValue.RDS_DB_INSTANCE_ID.
            pattern().
            matcher(id).
            matches()

    where:
    _ | id
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
    _ | 'ainternal-a'
    _ | 'a-internal-a'
  }

  def 'should reject invalid database instance identifiers'() {
    expect: 'database instance identifier does not match validation regex'
    !RdsMessageValidation.
            FieldRegexValue.RDS_DB_INSTANCE_ID.
            pattern().
            matcher(id).
            matches()

    where:
    _ | id
    _ | ''
    _ | '1'
    _ | '-'
    _ | '-a'
    _ | 'a-'
    _ | '-aaaaa'
    _ | 'aaaaa-'
    _ | 'a--b'
    _ | 'a---b'
  }

  def 'should accept valid database cluster identifiers'() {
    expect: 'database instance cluster matches validation regex'
    RdsMessageValidation.
            FieldRegexValue.RDS_DB_CLUSTER_ID.
            pattern().
            matcher(id).
            matches()

    where:
    _ | id
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
    _ | 'ainternal-a'
    _ | 'a-internal-a'
  }

  def 'should reject invalid database cluster identifiers'() {
    expect: 'database instance cluster does not match validation regex'
    !RdsMessageValidation.
            FieldRegexValue.RDS_DB_CLUSTER_ID.
            pattern().
            matcher(id).
            matches()

    where:
    _ | id
    _ | ''
    _ | '1'
    _ | '-'
    _ | '-a'
    _ | 'a-'
    _ | '-aaaaa'
    _ | 'aaaaa-'
    _ | 'a--b'
    _ | 'a---b'
  }

  def 'should accept valid database snaphot identifiers'() {
    expect: 'database snaphot identifier matches validation regex'
    RdsMessageValidation.
            FieldRegexValue.RDS_DB_SNAPSHOT_ID.
            pattern().
            matcher(id).
            matches()

    where:
    _ | id
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
    _ | 'ainternal-a'
    _ | 'a-internal-a'
  }

  def 'should reject invalid database snaphot identifiers'() {
    expect: 'database snaphot identifier does not match validation regex'
    !RdsMessageValidation.
            FieldRegexValue.RDS_DB_SNAPSHOT_ID.
            pattern().
            matcher(id).
            matches()

    where:
    _ | id
    _ | ''
    _ | '1'
    _ | '-'
    _ | '-a'
    _ | 'a-'
    _ | '-aaaaa'
    _ | 'aaaaa-'
    _ | 'a--b'
    _ | 'a---b'
  }

  def 'should accept valid database parameter group names'() {
    expect: 'database parameter group name matches validation regex'
    RdsMessageValidation.
            FieldRegexValue.RDS_DB_PARAMETER_GROUP_NAME.
            pattern().
            matcher(name).
            matches()

    where:
    _ | name
    _ | 'a'
    _ | 'ab'
    _ | 'a-b-c'
    _ | 'abcdefghijklmnopqrstuvwzyz0'
    _ | 'abcdefghijklmnopqrstuvwzyz012345'
    _ | 'default.postgres10'
  }

  def 'should reject invalid database parameter group names'() {
    expect: 'database parameter group name does not match validation regex'
    !RdsMessageValidation.
            FieldRegexValue.RDS_DB_PARAMETER_GROUP_NAME.
            pattern().
            matcher(name).
            matches()

    where:
    _ | name
    _ | ''
    _ | '?'
    _ | '-a'
    _ | 'a-'
    _ | '-aaaaa'
    _ | 'aaaaa-'
    _ | 'a--b'
    _ | 'a---b'
    _ | 'a b c'
    _ | 'a.b.c'
    _ | 'a_b_c'
  }

  def 'should accept valid database subnet group names'() {
    expect: 'database subnet group name matches validation regex'
    RdsMessageValidation.
            FieldRegexValue.RDS_DB_SUBNET_GROUP_NAME.
            pattern().
            matcher(name).
            matches()

    where:
    _ | name
    _ | 'a'
    _ | 'ab'
    _ | 'a b c'
    _ | 'a.b.c'
    _ | 'a_b_c'
    _ | 'a-b-c'
    _ | 'abcdefghijklmnopqrstuvwzyz0'
    _ | 'abcdefghijklmnopqrstuvwzyz012345'
  }

  def 'should reject invalid database subnet group names'() {
    expect: 'database subnet group name does not match validation regex'
    !RdsMessageValidation.
            FieldRegexValue.RDS_DB_SUBNET_GROUP_NAME.
            pattern().
            matcher(name).
            matches()

    where:
    _ | name
    _ | ''
    _ | '?'
  }

  def 'should accept valid database master username'() {
    expect: 'database master username matches validation regex'
    RdsMessageValidation.
            FieldRegexValue.RDS_DB_MASTERUSERNAME.
            pattern().
            matcher(name).
            matches()

    where:
    _ | name
    _ | 'a'
    _ | 'A'
    _ | '1'
    _ | 'abcdefghijklmnopqrstuvwzyzABCDEFGHIJKLMNOPQRSTUVWZYZ0123456789'
  }

  def 'should reject invalid database master username'() {
    expect: 'database master username does not match validation regex'
    !RdsMessageValidation.
            FieldRegexValue.RDS_DB_MASTERUSERNAME.
            pattern().
            matcher(name).
            matches()

    where:
    _ | name
    _ | ''
    _ | ' '
    _ | '?'
    _ | '?a'
    _ | 'a?'
    _ | '.'
  }

  def 'should accept valid database master password'() {
    expect: 'database master password matches validation regex'
    RdsMessageValidation.
            FieldRegexValue.RDS_DB_MASTERPASSWORD.
            pattern().
            matcher(password).
            matches()

    where:
    _ | password
    _ | 'abcdefgh'
    _ | '12345678'
    _ | '12345678a'
    _ | '#$525!_\\<>,.'
    _ | 'this is a passphrase'
  }

  def 'should reject invalid database master password'() {
    expect: 'database master password does not match validation regex'
    !RdsMessageValidation.
            FieldRegexValue.RDS_DB_MASTERPASSWORD.
            pattern().
            matcher(password).
            matches()

    where:
    _ | password
    _ | ''
    _ | 'a'
    _ | '1'
    _ | '1234567'
    _ | 'abcdef'
    _ | '12345678/'
    _ | '/12345678'
    _ | '1234/5678'
    _ | '12345678@'
    _ | '@12345678'
    _ | '1234@5678'
    _ | '12345678"'
    _ | '"12345678'
    _ | '1234"5678'
  }

}