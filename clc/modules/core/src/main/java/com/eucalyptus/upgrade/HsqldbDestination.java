package com.eucalyptus.upgrade;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.LocalDatabaseBootstrapper;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.Components;

public class HsqldbDestination implements DatabaseDestination {
	private static Logger LOG = Logger.getLogger( HsqldbDestination.class );

	public void initialize( ) throws Exception {
		/** Bring up the new destination database **/
		Components.lookup( Component.db ).buildService( );
		LocalDatabaseBootstrapper db = ( LocalDatabaseBootstrapper ) LocalDatabaseBootstrapper.getInstance( );
		db.load(Stage.DatabaseInit);
		db.start();
	}
}
