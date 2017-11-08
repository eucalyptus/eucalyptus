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
-- Initial tables for eucalyptus_billing keyspace
--

CREATE TABLE IF NOT EXISTS eucalyptus_billing.aws_records (
  account_id TEXT,
  service TEXT,
  operation TEXT,
  usage_type TEXT,
  resource TEXT,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  usage_value TEXT,
  natural_id TIMEUUID,
  operation_usage_type_concat TEXT,
  PRIMARY KEY((account_id, service), end_time, natural_id)
) WITH CLUSTERING ORDER BY (end_time ASC, natural_id ASC);

CREATE INDEX IF NOT EXISTS aws_records_operation_idx ON eucalyptus_billing.aws_records (operation);

CREATE INDEX IF NOT EXISTS aws_records_usage_type_idx ON eucalyptus_billing.aws_records (usage_type);

CREATE INDEX IF NOT EXISTS aws_records_operation_usage_type_idx
  ON eucalyptus_billing.aws_records (operation_usage_type_concat);

CREATE INDEX IF NOT EXISTS aws_records_operation_usage_type_idx
  ON eucalyptus_billing.aws_records (operation_usage_type_concat);

-- prefix index (to allow LIKE with % in suffix, requires cassandra 3.4 (I think) or later.
CREATE CUSTOM INDEX IF NOT EXISTS aws_records_usage_type_prefix_idx ON eucalyptus_billing.aws_records (usage_type) USING 'org.apache.cassandra.index.sasi.SASIIndex';
CREATE CUSTOM INDEX IF NOT EXISTS aws_records_operation_usage_type_prefix_idx ON eucalyptus_billing.aws_records (operation_usage_type_concat) USING 'org.apache.cassandra.index.sasi.SASIIndex';

-- separate table for records as it may not be a low cardinality value (secondary index otherwise)
CREATE TABLE IF NOT EXISTS eucalyptus_billing.aws_records_by_resource (
  account_id TEXT,
  service TEXT,
  operation TEXT,
  usage_type TEXT,
  resource TEXT,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  usage_value TEXT,
  natural_id TIMEUUID,
  operation_usage_type_concat TEXT,
  PRIMARY KEY((account_id, service, resource), end_time, natural_id)
) WITH CLUSTERING ORDER BY (end_time ASC, natural_id ASC);

CREATE INDEX IF NOT EXISTS aws_records_by_resource_operation_idx
  ON eucalyptus_billing.aws_records_by_resource (operation);

CREATE INDEX IF NOT EXISTS aws_records_by_resource_usage_type_idx
  ON eucalyptus_billing.aws_records_by_resource (usage_type);

CREATE INDEX IF NOT EXISTS aws_records_by_resource_operation_usage_type_idx
  ON eucalyptus_billing.aws_records_by_resource (operation_usage_type_concat);

-- prefix index (to allow LIKE with % in suffix, requires cassandra 3.4 (I think) or later.
CREATE CUSTOM INDEX IF NOT EXISTS aws_records_by_resource_usage_type_prefix_idx ON eucalyptus_billing.aws_records_by_resource (usage_type) USING 'org.apache.cassandra.index.sasi.SASIIndex';
CREATE CUSTOM INDEX IF NOT EXISTS aws_records_by_resource_operation_usage_type_prefix_idx ON eucalyptus_billing.aws_records_by_resource (operation_usage_type_concat) USING 'org.apache.cassandra.index.sasi.SASIIndex';


-- instance usage data --
CREATE TABLE IF NOT EXISTS eucalyptus_billing.instance_log (
  account_id TEXT,
  instance_id TEXT,
  instance_type TEXT,
  platform TEXT,
  region TEXT,
  availability_zone TEXT,
  log_time TIMESTAMP,
  -- a list of tags in json format  TODO: user defined types?
  tags_json LIST<TEXT>,
  natural_id TIMEUUID,
  PRIMARY KEY((account_id), log_time, natural_id)
) WITH CLUSTERING ORDER BY (log_time ASC, natural_id ASC);