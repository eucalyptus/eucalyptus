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
import com.eucalyptus.autoscaling.activities.DispatchingClient;
import com.eucalyptus.autoscaling.activities.EucalyptusClient;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EmpyreanMessage;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesItemType;
/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EucalyptusActivityTasks {
	private static final Logger LOG = Logger.getLogger( EucalyptusActivityTask.class );
	private EucalyptusActivityTasks() {}
	private static  EucalyptusActivityTasks _instance = new EucalyptusActivityTasks();
	public static EucalyptusActivityTasks getInstance(){
		return _instance;
	}

	private interface ActivityContext<TM extends BaseMessage, TC extends ComponentId> {
	  String getUserId();
	  DispatchingClient<TM, TC> getClient();
	}
	
	private class EmpyreanSystemActivity implements ActivityContext<EmpyreanMessage, Empyrean>{

		@Override
		public String getUserId() {
			try{
				return Accounts.lookupSystemAdmin( ).getUserId();
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}

		@Override
		public DispatchingClient<EmpyreanMessage, Empyrean> getClient() {
			// TODO Auto-generated method stub
			try{
				final EmpyreanClient client = 
						new EmpyreanClient(this.getUserId());
				client.init();
				return client;
			}catch(Exception e){
				throw Exceptions.toUndeclared(e);
			}
		}
	}
	private class EucalyptusSystemActivity implements ActivityContext<EucalyptusMessage, Eucalyptus>{
		@Override
		public String getUserId(){
			// TODO: SPARK: Impersonation?
			try{
				/// ASSUMING LB SERVICE HAS ACCESS TO LOCAL DB
				return Accounts.lookupSystemAdmin( ).getUserId();
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
		
		@Override
		public DispatchingClient<EucalyptusMessage, Eucalyptus> getClient(){
			 try {
			     // final DispatchingClient<BaseMessage, ComponentId> client = 
				 final EucalyptusClient client = 
			    		  new EucalyptusClient( this.getUserId() );
			      client.init();
			      return client;
			    } catch ( Exception e ) {
			      throw Exceptions.toUndeclared( e );
			    }
		}
	}
	
	public List<String> launchInstances(final String availabilityZone, final String imageId, 
			final String instanceType, int numInstance){
		return launchInstances(availabilityZone, imageId, instanceType, numInstance, null);
	}
	public List<String> launchInstances(final String availabilityZone, final String imageId, 
				final String instanceType, int numInstance, final String userData){
		LOG.info("launching instances at zone="+availabilityZone+", imageId="+imageId);
		final EucalyptusLaunchInstanceTask launchTask = new EucalyptusLaunchInstanceTask(availabilityZone, imageId, instanceType);
		if(userData!=null)
			launchTask.setUserData(userData);
		final CheckedListenableFuture<Boolean> result = launchTask.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				final List<String> instances = launchTask.getInstanceIds();
				return instances;
			}else
				throw new EucalyptusActivityException("failed to launch the instance");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public List<String> terminateInstances(final List<String> instances){
		LOG.info(String.format("terminating %d instances", instances.size()));
		if(instances.size() <=0)
			return instances;
		
		final EucalyptusTerminateInstanceTask terminateTask = new EucalyptusTerminateInstanceTask(instances);
		final CheckedListenableFuture<Boolean> result = terminateTask.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				final List<String> terminated = terminateTask.getTerminatedInstances();
				return terminated;
			}else
				throw new EucalyptusActivityException("failed to terminate the instances");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public List<ServiceStatusType> describeServices(final String componentType){
		LOG.info("calling describe-services -T "+componentType);
		final EucalyptusDescribeServicesTask serviceTask = new EucalyptusDescribeServicesTask(componentType);
		final CheckedListenableFuture<Boolean> result = serviceTask.dispatch(new EmpyreanSystemActivity());
		try{
			if(result.get()){
				return serviceTask.getServiceDetais();
			}else
				throw new EucalyptusActivityException("failed to describe services");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	private class EucalyptusDescribeServicesTask extends EucalyptusActivityTask<EmpyreanMessage, Empyrean> {
		private String componentType = null;
		private List<ServiceStatusType> services = null; 
		private EucalyptusDescribeServicesTask(final String componentType){
			this.componentType = componentType;
		}
		
		private DescribeServicesType describeServices(){
			final DescribeServicesType req = new DescribeServicesType();
		    req.setByServiceType(this.componentType);
		    return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EmpyreanMessage, Empyrean> context,
				Checked<EmpyreanMessage> callback) {
			final DispatchingClient<EmpyreanMessage,Empyrean> client = context.getClient();
			client.dispatch(describeServices(), callback);
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EmpyreanMessage, Empyrean> context, EmpyreanMessage response) {
			// TODO Auto-generated method stub
			final DescribeServicesResponseType resp = (DescribeServicesResponseType) response;
			this.services = resp.getServiceStatuses();
		}
		
		public List<ServiceStatusType> getServiceDetais(){
			return this.services;
		}
	}
	
	private class EucalyptusTerminateInstanceTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
		private final List<String> instanceIds;
		private final AtomicReference<List<String>> terminatedIds = new AtomicReference<List<String>>();
		private EucalyptusTerminateInstanceTask(final List<String> instanceId){
			this.instanceIds = instanceId;
		}
		private TerminateInstancesType terminateInstances(){
			final TerminateInstancesType req = new TerminateInstancesType();
			req.setInstancesSet(Lists.newArrayList(this.instanceIds));
			return req;
		}
		 
		@Override
		void dispatchInternal( ActivityContext<EucalyptusMessage,Eucalyptus> context, Callback.Checked<EucalyptusMessage> callback){
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(terminateInstances(), callback);
		}
		    
		
		@Override
		void dispatchSuccess( ActivityContext<EucalyptusMessage,Eucalyptus> context, EucalyptusMessage response ){
			TerminateInstancesResponseType resp = (TerminateInstancesResponseType) response;
			this.terminatedIds.set(Lists.transform(resp.getInstancesSet(), 
					new Function<TerminateInstancesItemType, String>(){
						@Override
						public String apply(TerminateInstancesItemType item){
							return item.getInstanceId();
						}
					}));
		}
		
		List<String> getTerminatedInstances(){
			return this.terminatedIds.get();
		}

	}
	
	private class EucalyptusLaunchInstanceTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		private final String availabilityZone;
		private final String imageId;
		private final String instanceType;
		private String userData = null;
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
		    if(availabilityZone != null)
		    	runInstances.setAvailabilityZone( availabilityZone );
		    runInstances.setMaxCount( attemptToLaunch );
		    runInstances.setAddressingType("private");
		    if(this.userData!=null)
		    	runInstances.setUserData(this.userData);
		    return runInstances;
	    }
	    
	    @Override
	    void dispatchInternal( final ActivityContext<EucalyptusMessage, Eucalyptus> context,
	                           final Callback.Checked<EucalyptusMessage> callback ) {
	      final DispatchingClient<EucalyptusMessage,Eucalyptus> client = context.getClient();
	      client.dispatch( runInstances( availabilityZone, 1 ), callback );
	    }

	    @Override
	    void dispatchSuccess( final ActivityContext<EucalyptusMessage, Eucalyptus> context,
	                          final EucalyptusMessage response ) {
	      final List<String> instanceIds = Lists.newArrayList();
	      RunInstancesResponseType resp = (RunInstancesResponseType) response;
	      for ( final RunningInstancesItemType item : resp.getRsvInfo().getInstancesSet() ) {
	        instanceIds.add( item.getInstanceId() );
	      }

	      this.instanceIds.set( ImmutableList.copyOf( instanceIds ) );
	    }
	    
	    void setUserData(String userData){
	    	this.userData = userData;
	    }
	    List<String> getInstanceIds() {
	      return instanceIds.get();
	    }
	}
	
	private abstract class EucalyptusActivityTask <TM extends BaseMessage, TC extends ComponentId>{
	    private volatile boolean dispatched = false;
	
	    protected EucalyptusActivityTask(){}
	
	    final CheckedListenableFuture<Boolean> dispatch( final ActivityContext<TM,TC> context ) {
	      try {
	        final CheckedListenableFuture<Boolean> future = Futures.newGenericeFuture();
	        dispatchInternal( context, new Callback.Checked<TM>(){
	          @Override
	          public void fireException( final Throwable throwable ) {
	            try {
	              dispatchFailure( context, throwable );
	            } finally {
	              future.set( false );
	            }
	          }
	
	          @Override
	          public void fire( final TM response ) {
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
	
	    abstract void dispatchInternal( ActivityContext<TM,TC> context, Callback.Checked<TM> callback );
	
	    void dispatchFailure( ActivityContext<TM,TC> context, Throwable throwable ) {
	      // error, assume no instances run for now
	      LOG.error( "Loadbalancer activity error", throwable ); //TODO:STEVE: Remove failure logging and record in scaling activity details/description
	    }
	
	    abstract void dispatchSuccess( ActivityContext<TM,TC> context, TM response );
	}
}
