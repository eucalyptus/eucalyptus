-----------------------------------------------------------------------
-- Copyright 2017 Ent. Services Development Corporation LP
--
-- Redistribution and use of this software in source and binary forms,
-- with or without modification, are permitted provided that the
-- following conditions are met:
--
--   Redistributions of source code must retain the above copyright
--   notice, this list of conditions and the following disclaimer.
--
--   Redistributions in binary form must reproduce the above copyright
--   notice, this list of conditions and the following disclaimer
--   in the documentation and/or other materials provided with the
--   distribution.
--
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
-- "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
-- LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
-- FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
-- COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
-- INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
-- BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
-- LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
-- CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
-- LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
-- ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
-- POSSIBILITY OF SUCH DAMAGE.
-----------------------------------------------------------------------
--
-- Initial tables for eucalyptus_simplequeue keyspace
--

CREATE TABLE IF NOT EXISTS eucalyptus_simplequeue.queues (
  account_id TEXT,
  queue_name TEXT,
  unique_id_per_version TIMEUUID,
  attributes MAP<TEXT, TEXT>,
  partition_token text,
  PRIMARY KEY ((account_id), queue_name)
) WITH CLUSTERING ORDER BY (queue_name ASC);

CREATE TABLE IF NOT EXISTS eucalyptus_simplequeue.queues_by_source_queue (
  source_queue_arn TEXT,
  account_id TEXT,
  queue_name TEXT,
  last_lookup TIMESTAMP,
  PRIMARY KEY ((source_queue_arn), account_id, queue_name)
);

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
