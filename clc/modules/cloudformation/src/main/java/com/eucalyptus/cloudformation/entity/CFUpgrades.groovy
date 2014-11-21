package com.eucalyptus.cloudformation.entity

import com.eucalyptus.cloudformation.CloudFormation
import groovy.sql.Sql
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

import java.util.concurrent.Callable

/**
 * Created by ethomas on 11/19/14.
 */
public class CFUpgrades {

  @com.eucalyptus.upgrade.Upgrades.PreUpgrade( value = CloudFormation.class, since = com.eucalyptus.upgrade.Upgrades.Version.v4_1_0 )
  @CompileStatic( TypeCheckingMode.SKIP )
  public static class CloudFormation410PreUpgrade implements Callable<Boolean> {
    private static Logger LOG = Logger.getLogger( CloudFormation410PreUpgrade.class );

    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = com.eucalyptus.upgrade.Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloudformation" );
        sql.execute( "ALTER TABLE stacks ALTER COLUMN stack_policy TYPE text" );
        sql.execute( "ALTER TABLE stacks ALTER COLUMN template_body TYPE text" );
        sql.execute( "ALTER TABLE stack_events ALTER COLUMN physical_resource_id TYPE text" );
        sql.execute( "ALTER TABLE stack_resources ALTER COLUMN physical_resource_id TYPE text" );
        return true;
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return false;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
  }


}
