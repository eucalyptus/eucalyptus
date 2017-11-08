/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cassandra.common.util

import org.junit.Assert
import org.junit.Test

import java.text.ParseException

/**
 *
 */
class CqlUtilTest {

  @Test
  void testBasicParse( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT 1; SELECT 1; SELECT 1;''' );
    Assert.assertEquals( 'Statements', ['SELECT 1','SELECT 1','SELECT 1'], statements )
  }

  @Test
  void testSingleLineSlashQuote( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT 1; \n//SELECT 1;\n SELECT 1;''' );
    Assert.assertEquals( 'Statements', ['SELECT 1','SELECT 1'], statements )
  }

  @Test
  void testSingleLineDashQuote( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT 1;\n--SELECT 1;\nSELECT 1;''' );
    Assert.assertEquals( 'Statements', ['SELECT 1','SELECT 1'], statements )
  }

  @Test
  void testMultiLineDashQuote( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT 1; /*\n\nSELECT 1\n;\n  * -- */ SELECT 1;''' );
    Assert.assertEquals( 'Statements', ['SELECT 1','SELECT 1'], statements )
  }

  @Test
  void testSingleQuoted( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT '1';''' );
    Assert.assertEquals( 'Statements', ['''SELECT '1\''''], statements )
  }

  @Test
  void testDoubleQuoted( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT "1";''' );
    Assert.assertEquals( 'Statements', ['SELECT "1"'], statements )
  }

  @Test
  void testSingleQuotedEscape( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT '1''2';''' );
    Assert.assertEquals( 'Statements', ['''SELECT '1'2\''''], statements )
  }

  @Test
  void testDoubleQuotedEscape( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT "1""2";''' );
    Assert.assertEquals( 'Statements', ['SELECT "1"2"'], statements )
  }

  @Test
  void testLastLineDashComment( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT 1; --''' );
    Assert.assertEquals( 'Statements', ['SELECT 1'], statements )
  }

  @Test
  void testLastLineSlashComment( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT 1; \n//''' );
    Assert.assertEquals( 'Statements', ['SELECT 1'], statements )
  }

  @Test
  void testCommentAfterQuote( ) {
    List<String> statements = CqlUtil.splitCql( '''SELECT "1"/*blah*/, "2"; ''' );
    Assert.assertEquals( 'Statements', ['SELECT "1", "2"'], statements )
  }

  @Test( expected = ParseException )
  void testUnterminatedComment( ) {
    CqlUtil.splitCql( '''SELECT 1; /*''' );
    Assert.fail( 'Expected unterminated comment failure' )
  }

  @Test( expected = ParseException )
  void testUnterminatedSingleQuote( ) {
    CqlUtil.splitCql( '''SELECT '1;''' );
    Assert.fail( 'Expected unterminated quote failure' )
  }

  @Test( expected = ParseException )
  void testUnterminatedDoubleQuote( ) {
    CqlUtil.splitCql( '''SELECT "1;''' );
    Assert.fail( 'Expected unterminated quote failure' )
  }

  @Test( expected = ParseException )
  void testUnterminatedStatement( ) {
    CqlUtil.splitCql( '''SELECT 1''' );
    Assert.fail( 'Expected unterminated statement failure' )
  }

  @Test
  void testLineNumberInParseError( ) {
    try {
      CqlUtil.splitCql( '''SELECT 1\n\n\n''' );
      Assert.fail( 'Expected unterminated statement failure' )
    } catch ( ParseException e ) {
      println( e )
      Assert.assertEquals( "Error message", 'Unterminated statement: SELECT 1, line 3', e.message )
    }
  }

  @Test
  void testExample( ) {
    String script = '''\
      CREATE TABLE IF NOT EXISTS eucalyptus_simplequeue.queues_by_partition (
        partition_token TEXT,
        account_id TEXT,
        queue_name TEXT,
        last_lookup TIMESTAMP,
        PRIMARY KEY ((partition_token), account_id, queue_name)
      );

      CREATE TABLE IF NOT EXISTS eucalyptus_simplequeue.messages (
        account_id TEXT,
        queue_name TEXT,
        partition_token TEXT,
        message_id TIMEUUID,
        message_json TEXT,
        send_time_secs BIGINT,
        receive_count INT,
        total_receive_count INT,
        expiration_timestamp TIMESTAMP,
        is_delayed BOOLEAN,
        is_invisible BOOLEAN,
        PRIMARY KEY ((account_id, queue_name, partition_token), message_id)
      );

      CREATE INDEX IF NOT EXISTS messages_is_delayed_idx ON eucalyptus_simplequeue.messages (is_delayed);

      CREATE INDEX IF NOT EXISTS messages_is_invisible_idx ON eucalyptus_simplequeue.messages (is_invisible);
    '''.stripIndent( )

    List<String> statements = CqlUtil.splitCql( script );
    Assert.assertEquals( 'Statements', [
        '''\
        CREATE TABLE IF NOT EXISTS eucalyptus_simplequeue.queues_by_partition (
          partition_token TEXT,
          account_id TEXT,
          queue_name TEXT,
          last_lookup TIMESTAMP,
          PRIMARY KEY ((partition_token), account_id, queue_name)
        )'''.stripIndent( ),
        '''\
        CREATE TABLE IF NOT EXISTS eucalyptus_simplequeue.messages (
          account_id TEXT,
          queue_name TEXT,
          partition_token TEXT,
          message_id TIMEUUID,
          message_json TEXT,
          send_time_secs BIGINT,
          receive_count INT,
          total_receive_count INT,
          expiration_timestamp TIMESTAMP,
          is_delayed BOOLEAN,
          is_invisible BOOLEAN,
          PRIMARY KEY ((account_id, queue_name, partition_token), message_id)
        )'''.stripIndent( ),
        '''CREATE INDEX IF NOT EXISTS messages_is_delayed_idx ON eucalyptus_simplequeue.messages (is_delayed)''',
        '''CREATE INDEX IF NOT EXISTS messages_is_invisible_idx ON eucalyptus_simplequeue.messages (is_invisible)'''
    ], statements )
  }

}
