/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.reporting.queue;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.*;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.reporting.ReportingBootstrapper;

@Provides(Reporting.class)
@RunDuring(Bootstrap.Stage.PrivilegedConfiguration)
public class QueueBootstrapper extends Bootstrapper
{
	private static Logger log = Logger.getLogger( QueueBootstrapper.class );

	@Override
	public boolean load() throws Exception
	{
		/* NOTE: a workaround was moved to SystemBootstrapper.init() because
		 * log4j brain damage prevents ActiveMQ broker from starting.
		 */
//		try {
//	        QueueFactory.getInstance().startup();
//	        log.info("broker started");
//			return true;			
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			return false;
//		}
		return true;
	}

	@Override
	public boolean start() throws Exception
	{
		return true;
	}

	@Override
	public boolean enable() throws Exception
	{
		return true;
	}

	@Override
	public boolean stop() throws Exception
	{
		return true;
	}

	@Override
	public void destroy() throws Exception
	{
	}

	@Override
	public boolean disable() throws Exception
	{
		return true;
	}

	@Override
	public boolean check() throws Exception
	{
		return true;
	}

}
