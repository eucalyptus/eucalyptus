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
