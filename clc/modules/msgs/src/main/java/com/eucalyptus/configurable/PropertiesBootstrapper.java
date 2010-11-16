package com.eucalyptus.configurable;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;

@Provides(Component.configuration)
@RunDuring(Bootstrap.Stage.SystemCredentialsInit)
public class PropertiesBootstrapper extends Bootstrapper {

	@Override
	public boolean load( ) throws Exception {
		ConfigurationProperties.doConfiguration( );
		return true;
	}

	@Override
	public boolean start( ) throws Exception {
		return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
	 */
	@Override
	public boolean enable( ) throws Exception {
		return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
	 */
	@Override
	public boolean stop( ) throws Exception {
		//unload properties
		return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
	 */
	@Override
	public void destroy( ) throws Exception {}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
	 */
	@Override
	public boolean disable( ) throws Exception {
		return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#check()
	 */
	@Override
	public boolean check( ) throws Exception {
		return true;
	}
}
