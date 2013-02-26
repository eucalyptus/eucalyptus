/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.activities;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.autoscaling.activities.EucalyptusClient;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;

public class EucalyptusActivityTasks {
	private static final Logger LOG = Logger.getLogger( EucalyptusActivityTask.class );
	private EucalyptusActivityTasks() {}
	private static  EucalyptusActivityTasks _instance = new EucalyptusActivityTasks();
	public static EucalyptusActivityTasks getInstnace(){
		return _instance;
	}
	
	public void launchInstance(final String availabilityZone, final String imageId, final String instanceType){
		LOG.info("launching instances at zone="+availabilityZone+", imageId="+imageId);
		EucalyptusLaunchInstanceTask launchTask = new EucalyptusLaunchInstanceTask(availabilityZone, imageId, instanceType);
		launchTask.dispatch(new ActivityContext() {
			@Override
			public String getUserId(){
				try{
					return Accounts.lookupSystemAdmin( ).getUserId();
				}catch(AuthException ex){
					throw Exceptions.toUndeclared(ex);
				}
			}
			@Override
			public EucalyptusClient getEucalyptusClient(){
				 try {
				      final EucalyptusClient client = new EucalyptusClient( this.getUserId() );
				      client.init();
				      return client;
				    } catch ( Exception e ) {
				      throw Exceptions.toUndeclared( e );
				    }
			}
		});
	}
	
	private interface ActivityContext {
	  String getUserId();
	  EucalyptusClient getEucalyptusClient();
	}
	
	private class EucalyptusLaunchInstanceTask extends EucalyptusActivityTask<RunInstancesResponseType> {
		private final String availabilityZone;
		private final String imageId;
		private final String instanceType;
		private final AtomicReference<List<String>> instanceIds = new AtomicReference<List<String>>(
		    Collections.<String>emptyList()
	    );

		private EucalyptusLaunchInstanceTask(final String availabilityZone, final String imageId, final String instanceType ) {
			this.availabilityZone = availabilityZone;
			this.imageId = imageId;
			this.instanceType = instanceType;
		}

	    private RunInstancesType runInstances( 
	    		final String availabilityZone,
	            final int attemptToLaunch ) 
	    {
	    	OwnerFullName systemAcct = AccountFullName.getInstance(Principals.systemAccount( ));
	     	LOG.info("runInstances with zone="+availabilityZone+", account="+systemAcct);
	     		       	
		    final LaunchConfiguration launchConfiguration = 
	    		  LaunchConfiguration.create(systemAcct, "launch_config_loadbalacing", 
	    		  this.imageId, this.instanceType);
		    final RunInstancesType runInstances = TypeMappers.transform( launchConfiguration, RunInstancesType.class );
		    runInstances.setAvailabilityZone( availabilityZone );
		    runInstances.setMaxCount( attemptToLaunch );
		    return runInstances;
	    }
	    
	    @Override
	    void dispatchInternal( final ActivityContext context,
	                           final Callback.Checked<RunInstancesResponseType> callback ) {
	      final EucalyptusClient client = context.getEucalyptusClient();
	      client.dispatch( runInstances( availabilityZone, 1 ), callback );
	    }

	    @Override
	    void dispatchSuccess( final ActivityContext context,
	                          final RunInstancesResponseType response ) {
	      final List<String> instanceIds = Lists.newArrayList();
	      for ( final RunningInstancesItemType item : response.getRsvInfo().getInstancesSet() ) {
	        instanceIds.add( item.getInstanceId() );
	      }

	      this.instanceIds.set( ImmutableList.copyOf( instanceIds ) );
	    }

	    List<String> getInstanceIds() {
	      return instanceIds.get();
	    }
	}
	
	private abstract class EucalyptusActivityTask <RES extends BaseMessage>{
	    private volatile boolean dispatched = false;
	
	    protected EucalyptusActivityTask(){}
	
	    final CheckedListenableFuture<Boolean> dispatch( final ActivityContext context ) {
	      try {
	        final CheckedListenableFuture<Boolean> future = Futures.newGenericeFuture();
	        dispatchInternal( context, new Callback.Checked<RES>(){
	          @Override
	          public void fireException( final Throwable throwable ) {
	            try {
	              dispatchFailure( context, throwable );
	            } finally {
	              future.set( false );
	            }
	          }
	
	          @Override
	          public void fire( final RES response ) {
	            try {
	              dispatchSuccess( context, response );
	            } finally {
	              future.set( true );
	            }
	          }
	        } );
	        dispatched = true;
	        return future;
	      } catch ( Exception e ) {
	        LOG.error( e, e );
	      }
	      return Futures.predestinedFuture( false );
	    }
	
	    abstract void dispatchInternal( ActivityContext context, Callback.Checked<RES> callback );
	
	    void dispatchFailure( ActivityContext context, Throwable throwable ) {
	      // error, assume no instances run for now
	      LOG.error( "Loadbalancer activity error", throwable ); //TODO:STEVE: Remove failure logging and record in scaling activity details/description
	    }
	
	    abstract void dispatchSuccess( ActivityContext context, RES response );
	}
}
