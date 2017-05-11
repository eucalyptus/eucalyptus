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

-- instance usage data --
CREATE TABLE IF NOT EXISTS eucalyptus_billing.instance_log_by_inst_type (
  account_id TEXT,
  instance_id TEXT,
  instance_type TEXT,
  platform TEXT,
  region TEXT,
  availability_zone TEXT,
  log_time TIMESTAMP,
  tags_json LIST<TEXT>,
  natural_id TIMEUUID,
  PRIMARY KEY((account_id, instance_type), log_time, natural_id)
) WITH CLUSTERING ORDER BY (log_time ASC, natural_id ASC);

-- instance usage data --
CREATE TABLE IF NOT EXISTS eucalyptus_billing.instance_log_by_platform (
  account_id TEXT,
  instance_id TEXT,
  instance_type TEXT,
  platform TEXT,
  region TEXT,
  availability_zone TEXT,
  log_time TIMESTAMP,
  tags_json LIST<TEXT>,
  natural_id TIMEUUID,
  PRIMARY KEY((account_id, platform), log_time, natural_id)
) WITH CLUSTERING ORDER BY (log_time ASC, natural_id ASC);

-- instance usage data --
CREATE TABLE IF NOT EXISTS eucalyptus_billing.instance_log_by_az (
  account_id TEXT,
  instance_id TEXT,
  instance_type TEXT,
  platform TEXT,
  region TEXT,
  availability_zone TEXT,
  log_time TIMESTAMP,
  tags_json LIST<TEXT>,
  natural_id TIMEUUID,
  PRIMARY KEY((account_id, availability_zone), log_time, natural_id)
) WITH CLUSTERING ORDER BY (log_time ASC, natural_id ASC);

-- instance usage data --
CREATE TABLE IF NOT EXISTS eucalyptus_billing.instance_log_by_inst_type_and_platform (
  account_id TEXT,
  instance_id TEXT,
  instance_type TEXT,
  platform TEXT,
  region TEXT,
  availability_zone TEXT,
  log_time TIMESTAMP,
  tags_json LIST<TEXT>,
  natural_id TIMEUUID,
  PRIMARY KEY((account_id, instance_type, platform), log_time, natural_id)
) WITH CLUSTERING ORDER BY (log_time ASC, natural_id ASC);

-- instance usage data --
CREATE TABLE IF NOT EXISTS eucalyptus_billing.instance_log_by_platform_and_az (
  account_id TEXT,
  instance_id TEXT,
  instance_type TEXT,
  platform TEXT,
  region TEXT,
  availability_zone TEXT,
  log_time TIMESTAMP,
  tags_json LIST<TEXT>,
  natural_id TIMEUUID,
  PRIMARY KEY((account_id, platform, availability_zone), log_time, natural_id)
) WITH CLUSTERING ORDER BY (log_time ASC, natural_id ASC);

-- instance usage data --
CREATE TABLE IF NOT EXISTS eucalyptus_billing.instance_log_by_inst_type_and_az (
  account_id TEXT,
  instance_id TEXT,
  instance_type TEXT,
  platform TEXT,
  region TEXT,
  availability_zone TEXT,
  log_time TIMESTAMP,
  tags_json LIST<TEXT>,
  natural_id TIMEUUID,
  PRIMARY KEY((account_id, instance_type, availability_zone), log_time, natural_id)
) WITH CLUSTERING ORDER BY (log_time ASC, natural_id ASC);

-- instance usage data --
CREATE TABLE IF NOT EXISTS eucalyptus_billing.instance_log_by_inst_type_platform_and_az (
  account_id TEXT,
  instance_id TEXT,
  instance_type TEXT,
  platform TEXT,
  region TEXT,
  availability_zone TEXT,
  log_time TIMESTAMP,
  tags_json LIST<TEXT>,
  natural_id TIMEUUID,
  PRIMARY KEY((account_id, instance_type, platform, availability_zone), log_time, natural_id)
) WITH CLUSTERING ORDER BY (log_time ASC, natural_id ASC);

