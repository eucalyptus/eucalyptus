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
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow
import com.eucalyptus.upgrade.Upgrades
import com.google.common.base.Predicate

import javax.annotation.Nullable

import static com.eucalyptus.upgrade.Upgrades.EntityUpgrade
import static com.eucalyptus.upgrade.Upgrades.PreUpgrade
import groovy.sql.Sql
import org.apache.log4j.Logger

import java.util.concurrent.Callable

/**
 * Created by ethomas on 11/19/14.
 */
class CFUpgrades {

  @PreUpgrade( value = CloudFormation.class, since = Upgrades.Version.v4_1_0 )
  static class CloudFormation410PreUpgrade implements Callable<Boolean> {
    private static Logger LOG = Logger.getLogger( CloudFormation410PreUpgrade.class )

    @Override
    Boolean call( ) throws Exception {
      Sql sql = null
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloudformation" )
        sql.execute( "ALTER TABLE stacks ALTER COLUMN stack_id TYPE varchar(400)" )
        sql.execute( "ALTER TABLE stacks ALTER COLUMN stack_policy TYPE text" )
        sql.execute( "ALTER TABLE stacks ALTER COLUMN template_body TYPE text" )
        sql.execute( "ALTER TABLE stack_events ALTER COLUMN stack_id TYPE varchar(400)" )
        sql.execute( "ALTER TABLE stack_events ALTER COLUMN physical_resource_id TYPE text" )
        sql.execute( "ALTER TABLE stack_resources ALTER COLUMN stack_id TYPE varchar(400)" )
        sql.execute( "ALTER TABLE stack_resources ALTER COLUMN physical_resource_id TYPE text" )
        return true
      } catch ( Exception ex ) {
        LOG.error( ex, ex )
        return false
      } finally {
        if ( sql != null ) {
          sql.close( )
        }
      }
    }
  }

  @EntityUpgrade( entities = CloudFormationConfiguration.class, value = CloudFormation.class, since = Upgrades.Version.v4_1_0 )
  enum CloudFormation410RegistrationUpgrade implements Predicate<Class> {
    INSTANCE

    protected static final Logger logger = Logger.getLogger( CloudFormation410RegistrationUpgrade )

    @Override
    boolean apply( @Nullable final Class entityClass ) {
      try {
        if ( !ServiceConfigurations.list( CloudFormation ).isEmpty( ) &&
            ServiceConfigurations.list( SimpleWorkflow ).isEmpty( ) ) {
          final String cloudformation = ComponentIds.lookup( CloudFormation ).name( )
          final String simpleworkflow = ComponentIds.lookup( SimpleWorkflow ).name( )
          final ServiceBuilder builder = ServiceBuilders.lookup( SimpleWorkflow )
          ServiceConfigurations.list( CloudFormation ).each{ ServiceConfiguration configuration ->
            final String simpleWorkflowServiceName
            if ( configuration.name.equals( "${configuration.partition}.${cloudformation}" as String ) ) {
              simpleWorkflowServiceName = "${configuration.partition}.${simpleworkflow}"
            } else { // use host based naming
              simpleWorkflowServiceName = "${configuration.hostName}_${simpleworkflow}"
            }
            try {
              ServiceConfigurations.lookupByName( SimpleWorkflow, simpleWorkflowServiceName )
              logger.warn( "Existing simpleworkflow service found with name: " + simpleWorkflowServiceName )
            } catch ( final NoSuchElementException nsee ) {
              logger.info( "Registering simpleworkflow service on host " + configuration.hostName )
              ServiceConfigurations.store( builder.newInstance(
                  configuration.partition,
                  simpleWorkflowServiceName,
                  configuration.hostName,
                  configuration.port ) )
            }
          }
        } else {
          logger.info( "Not registering simpleworkflow services on upgrade" )
        }
      } catch ( final Exception e ) {
        logger.error( "Error registering simpleworkflow services on upgrade", e )
      }
      true
    }
  }
}
