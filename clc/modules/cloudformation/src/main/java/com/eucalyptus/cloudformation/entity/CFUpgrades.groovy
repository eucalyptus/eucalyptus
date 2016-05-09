/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.entity

import com.eucalyptus.cloudformation.CloudFormation
import com.eucalyptus.cloudformation.config.CloudFormationConfiguration
import com.eucalyptus.component.ComponentIds
import com.eucalyptus.component.ServiceBuilder
import com.eucalyptus.component.ServiceBuilders
import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.ServiceConfigurations
import com.eucalyptus.empyrean.Empyrean
import com.eucalyptus.entities.Entities
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow
import com.eucalyptus.upgrade.Upgrades
import com.eucalyptus.util.Exceptions
import com.google.common.base.Predicate
import groovy.sql.Sql
import org.apache.log4j.Logger

import javax.annotation.Nullable
import javax.persistence.EntityTransaction
import java.sql.SQLException
import java.util.concurrent.Callable

import static com.eucalyptus.upgrade.Upgrades.EntityUpgrade
import static com.eucalyptus.upgrade.Upgrades.PreUpgrade

/**
 * Created by ethomas on 11/19/14.
 */
class CFUpgrades {

  @PreUpgrade(value = CloudFormation.class, since = Upgrades.Version.v4_1_0)
  static class CloudFormation410PreUpgrade implements Callable<Boolean> {
    private static Logger LOG = Logger.getLogger(CloudFormation410PreUpgrade.class)

    @Override
    Boolean call() throws Exception {
      Sql sql = null
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection("eucalyptus_cloudformation")
        sql.execute("ALTER TABLE stacks ALTER COLUMN stack_id TYPE varchar(400)")
        sql.execute("ALTER TABLE stacks ALTER COLUMN stack_policy TYPE text")
        sql.execute("ALTER TABLE stacks ALTER COLUMN template_body TYPE text")
        sql.execute("ALTER TABLE stack_events ALTER COLUMN stack_id TYPE varchar(400)")
        sql.execute("ALTER TABLE stack_events ALTER COLUMN physical_resource_id TYPE text")
        sql.execute("ALTER TABLE stack_resources ALTER COLUMN stack_id TYPE varchar(400)")
        sql.execute("ALTER TABLE stack_resources ALTER COLUMN physical_resource_id TYPE text")
        return true
      } catch (Exception ex) {
        LOG.error(ex, ex)
        return false
      } finally {
        if (sql != null) {
          sql.close()
        }
      }
    }
  }

  @EntityUpgrade(entities = CloudFormationConfiguration.class, value = CloudFormation.class, since = Upgrades.Version.v4_1_0)
  enum CloudFormation410RegistrationUpgrade implements Predicate<Class> {
    INSTANCE

    protected static final Logger logger = Logger.getLogger(CloudFormation410RegistrationUpgrade)

    @Override
    boolean apply(@Nullable final Class entityClass) {
      try {
        if (!ServiceConfigurations.list(CloudFormation).isEmpty() &&
            ServiceConfigurations.list(SimpleWorkflow).isEmpty()) {
          final String cloudformation = ComponentIds.lookup(CloudFormation).name()
          final String simpleworkflow = ComponentIds.lookup(SimpleWorkflow).name()
          final ServiceBuilder builder = ServiceBuilders.lookup(SimpleWorkflow)
          ServiceConfigurations.list(CloudFormation).each { ServiceConfiguration configuration ->
            final String simpleWorkflowServiceName
            if (configuration.name.equals("${configuration.partition}.${cloudformation}" as String)) {
              simpleWorkflowServiceName = "${configuration.partition}.${simpleworkflow}"
            } else { // use host based naming
              simpleWorkflowServiceName = "${configuration.hostName}_${simpleworkflow}"
            }
            try {
              ServiceConfigurations.lookupByName(SimpleWorkflow, simpleWorkflowServiceName)
              logger.warn("Existing simpleworkflow service found with name: " + simpleWorkflowServiceName)
            } catch (final NoSuchElementException nsee) {
              logger.info("Registering simpleworkflow service on host " + configuration.hostName)
              ServiceConfigurations.store(builder.newInstance(
                  configuration.partition,
                  simpleWorkflowServiceName,
                  configuration.hostName,
                  configuration.port))
            }
          }
        } else {
          logger.info("Not registering simpleworkflow services on upgrade")
        }
      } catch (final Exception e) {
        logger.error("Error registering simpleworkflow services on upgrade", e)
      }
      true
    }
  }

  @PreUpgrade(value = CloudFormation.class, since = Upgrades.Version.v4_3_0)
  static class CloudFormation430PreUpgrade implements Callable<Boolean> {
    private static Logger LOG = Logger.getLogger(CloudFormation430PreUpgrade.class)

    private boolean columnExists(Sql sql, String table, String name) throws SQLException {
      Object[] objectArray = new Object[2];
      objectArray[0] = table;
      objectArray[1] = name;
      return !sql.rows(
          "SELECT column_name FROM information_schema.columns WHERE table_name=? AND column_name=?",
          objectArray).isEmpty();
    }

    @Override
    Boolean call() throws Exception {
      Sql sql = null
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection("eucalyptus_cloudformation");
        if (!columnExists(sql, "stack_resources", "is_created_enough_to_delete")) {
          sql.execute("ALTER TABLE stack_resources ADD COLUMN is_created_enough_to_delete boolean");
        }
        sql.execute("UPDATE stack_resources SET is_created_enough_to_delete = NOT(physical_resource_id IS NULL) WHERE (is_created_enough_to_delete IS NULL)")
        sql.execute("ALTER TABLE stack_resources ALTER COLUMN is_created_enough_to_delete SET NOT NULL");
        if (!columnExists(sql, "stack_resources", "creation_policy_json")) {
          sql.execute("ALTER TABLE stack_resources ADD COLUMN creation_policy_json text");
        }
        if (!columnExists(sql, "stack_resources", "resource_version")) {
          sql.execute("ALTER TABLE stack_resources ADD COLUMN resource_version integer");
        }
        sql.execute("UPDATE stack_resources SET resource_version = 0 WHERE (resource_version IS NULL)")
        if (!columnExists(sql, "stack_resources", "update_type")) {
          sql.execute("ALTER TABLE stack_resources ADD COLUMN update_type varchar(255)");
        }
        if (!columnExists(sql, "stacks", "stack_version")) {
          sql.execute("ALTER TABLE stacks ADD COLUMN stack_version integer");
        }
        sql.execute("UPDATE stacks SET stack_version = 0 WHERE (stack_version IS NULL)")
        if (!columnExists(sql, "stacks", "working_outputs_json")) {
          sql.execute("ALTER TABLE stacks ADD COLUMN working_outputs_json text");
        }
        sql.execute("UPDATE stacks SET working_outputs_json = outputs_json WHERE (working_outputs_json IS NULL)")
        return true
      } catch (Exception ex) {
        LOG.error(ex, ex)
        return false
      } finally {
        if (sql != null) {
          sql.close()
        }
      }
    }
  }

}