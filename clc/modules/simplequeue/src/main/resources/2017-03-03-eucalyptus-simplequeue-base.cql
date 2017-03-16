-----------------------------------------------------------------------
-- (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation; version 3 of the License.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program.  If not, see http://www.gnu.org/licenses/.
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
