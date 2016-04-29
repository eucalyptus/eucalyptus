/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import static com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView.name;
import static com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView.subnetId;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults.CheckException;

import com.eucalyptus.loadbalancing.LoadBalancingSystemVpcs;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.VmTypeDetails;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.common.LoadBalancingBackend;
import com.eucalyptus.loadbalancing.LoadBalancerPolicies;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.InstanceProfileSetup;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.SecurityGroupSetup;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.TagCreator;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.HostSpecifier;

/**
 * @author Sang-Min Park
 *
 */
@ConfigurableClass(root = "services.loadbalancing.worker", description = "Parameters controlling loadbalancing")
public class LoadBalancerASGroupCreator extends AbstractEventHandler<LoadbalancingEvent> implements StoredResult<String>{
	private static Logger  LOG     = Logger.getLogger( LoadBalancerASGroupCreator.class );

	public static class ElbEmiChangeListener implements PropertyChangeListener {
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			try {
				if ( newValue instanceof String  ) {
					if(t.getValue()!=null && ! t.getValue().equals(newValue))
						onPropertyChange((String)newValue, null, null, null);
				}
			} catch ( final Exception e ) {
				throw new ConfigurablePropertyException("Could not change EMI ID due to: " + e.getMessage());
			}
		}
	}

	public static class ElbInstanceTypeChangeListener implements PropertyChangeListener {
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			try {
				if ( newValue instanceof String ) {
					if( newValue.equals( "" ) )
						throw new EucalyptusCloudException("Instance type cannot be unset");
					if(t.getValue()!=null && ! t.getValue().equals(newValue))
						onPropertyChange(null, (String)newValue, null, null);
				}
			} catch ( final Exception e ) {
				throw new ConfigurablePropertyException("Could not change instance type due to: " + e.getMessage());
			}
		}
	}

	public static class ElbKeyNameChangeListener implements PropertyChangeListener<String> {
		@Override
		public void fireChange( ConfigurableProperty t, String keyname ) throws ConfigurablePropertyException {
			try {
				if(t.getValue()!=null && !t.getValue().equals(keyname)) {
					if ( keyname != null && !keyname.isEmpty() ) {
						// find out if there are any old elbs are deployed
						boolean oldElbExist = false;
						for (LoadBalancer lb:LoadBalancers.listLoadbalancers()){
							if (!lb.useSystemAccount()) {
								oldElbExist = true;
								break;
							}
						}
						try {
							Ec2Client.getInstance().describeKeyPairs(Accounts.lookupSystemAccountByAlias(
									AccountIdentifiers.ELB_SYSTEM_ACCOUNT ).getUserId( ), Lists.newArrayList(keyname));
						} catch(Exception ex) {
							throw new ConfigurablePropertyException("Could not change key name due to: " + ex.getMessage()
									+ ". Do you have keypair " + keyname + " that belongs to "
									+ AccountIdentifiers.ELB_SYSTEM_ACCOUNT + " account?");
						}
						if (oldElbExist) {
							try {
								Ec2Client.getInstance().describeKeyPairs(null,
										Lists.newArrayList(keyname));
							} catch(Exception ex) {
								throw new ConfigurablePropertyException("Could not change key name due to: " + ex.getMessage()
										+ ". Do you have keypair " + keyname + " that belongs to system account?");
							}
						}
					}
					onPropertyChange(null, null, keyname, null);
				}
			} catch ( final ConfigurablePropertyException e ) {
				throw e;
			} catch ( final Exception e ) {
				throw new ConfigurablePropertyException("Could not change key name due to: " + e.getMessage());
			}
		}
	}

	public static class ElbVmExpirationDaysChangeListener implements PropertyChangeListener<String> {
		@Override
		public void fireChange(ConfigurableProperty t, String newValue)
				throws ConfigurablePropertyException {
			try{
				final int newExp = Integer.parseInt(newValue);
				if(newExp <= 0 )
					throw new Exception();
			}catch(final Exception ex) {
				throw new ConfigurablePropertyException("The value must be number type and bigger than 0");
			}
		}
	}

	private static class AsyncPropertyChanger implements Callable<Boolean> {
		private String emi = null;
		private String instanceType = null;
		private String keyname = null;
		private String initScript = null;

		private AsyncPropertyChanger (final String emi, final String instanceType,
									  final String keyname, String initScript) {
			this.emi = emi;
			this.instanceType = instanceType;
			this.keyname = keyname;
			this.initScript = initScript;
		}

		@Override
		public synchronized Boolean call() throws Exception {
			final List<LoadBalancer> lbs = LoadBalancers.listLoadbalancers();
			for(final LoadBalancer lb : lbs){
				final Collection<LoadBalancerAutoScalingGroupCoreView> groups = lb.getAutoScaleGroups();
				for(final LoadBalancerAutoScalingGroupCoreView asg : groups) {
					if(asg==null || asg.getName()==null)
						continue;

					final String asgName = asg.getName();
					try{
						AutoScalingGroupType asgType = null;
						try{
							final DescribeAutoScalingGroupsResponseType resp =
									EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(asgName), lb.useSystemAccount());

							if(resp.getDescribeAutoScalingGroupsResult() != null &&
									resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups()!=null &&
									resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember()!=null &&
									resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().size()>0){
								asgType = resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().get(0);
							}
						}catch(final Exception ex){
							LOG.warn("can't find autoscaling group named "+asgName);
							continue;
						}
						if(asgType!=null){
							final String lcName = asgType.getLaunchConfigurationName();
							final LaunchConfigurationType lc =
									EucalyptusActivityTasks.getInstance().describeLaunchConfiguration(lcName, lb.useSystemAccount());

							String launchConfigName;
							do{
								launchConfigName = String.format("lc-euca-internal-elb-%s-%s-%s",
										lb.getOwnerAccountNumber(), lb.getDisplayName(), UUID.randomUUID().toString().substring(0, 8));

								if(launchConfigName.length()>255)
									launchConfigName = launchConfigName.substring(0, 255);
							}while(launchConfigName.equals(asgType.getLaunchConfigurationName()));

							final String newEmi = emi != null? emi : lc.getImageId();
							final String newType = instanceType != null? instanceType : lc.getInstanceType();
							String newKeyname = keyname != null ? keyname : lc.getKeyName();

							final String newUserdata = B64.standard.encString(String.format(
									"%s\n%s",
									getCredentialsString(),
									getLoadBalancerUserData(initScript, lb.getOwnerAccountNumber())));

							try{
								EucalyptusActivityTasks.getInstance().createLaunchConfiguration(newEmi, newType, lc.getIamInstanceProfile(),
										launchConfigName, lc.getSecurityGroups().getMember(), newKeyname, newUserdata,
										lc.getAssociatePublicIpAddress( ), lb.useSystemAccount() );
							}catch(final Exception ex){
								throw new EucalyptusCloudException("failed to create new launch config", ex);
							}
							try{
								EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(asgName, null,asgType.getDesiredCapacity(), launchConfigName, lb.useSystemAccount());
							}catch(final Exception ex){
								throw new EucalyptusCloudException("failed to update the autoscaling group", ex);
							}
							try{
								EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(asgType.getLaunchConfigurationName(), lb.useSystemAccount());
							}catch(final Exception ex){
								LOG.warn("unable to delete the old launch configuration", ex);
							}
							// copy all tags from new image to ASG
							if ( emi != null) {
								try {
									final List<ImageDetails> images =
											EucalyptusActivityTasks.getInstance().describeImagesWithVerbose(Lists.newArrayList(emi));
									// image should exist at this point
									for(ResourceTag tag:images.get(0).getTagSet()){
										EucalyptusActivityTasks.getInstance().createOrUpdateAutoscalingTags(tag.getKey(), tag.getValue(), asgName, lb.useSystemAccount());
									}
								} catch (final Exception ex) {
									LOG.warn("unable to propogate tags from image to ASG", ex);
								}
							}
							LOG.debug(String.format("autoscaling group '%s' was updated", asgName));
						}
					}catch(final EucalyptusCloudException ex){
						LOG.error("Failed to apply ELB property changes", ex);
						return false;
					}catch(final Exception ex){
						LOG.error("Failed to apply ELB property changes", ex);
						return false;
					}
				} // for all autoscaling groups of LB
			} // for all LBs
			return true;
		}
	}

	private static void onPropertyChange(final String emi, final String instanceType,
										 final String keyname, String initScript) throws EucalyptusCloudException{
		if (!( Bootstrap.isFinished() && Topology.isEnabled( Compute.class ) ) )
			return;

		// should validate the parameters

		// keyname is validated by caller
		// validate image id
		if(emi!=null){
			try{
				final List<ImageDetails> images =
						EucalyptusActivityTasks.getInstance().describeImagesWithVerbose(Lists.newArrayList(emi));
				if(images == null || images.size()<=0)
					throw new EucalyptusCloudException("No such EMI is found in the system");
				if(! images.get(0).getImageId().toLowerCase().equals(emi.toLowerCase()))
					throw new EucalyptusCloudException("No such EMI is found in the system");
			}catch(final EucalyptusCloudException ex){
				throw ex;
			}catch(final Exception ex){
				throw new EucalyptusCloudException("Failed to verify EMI in the system");
			}
		}
		// validate instance type
		if(instanceType!=null){
			try{
				final List<VmTypeDetails> vmTypes =
						EucalyptusActivityTasks.getInstance().describeInstanceTypes(Lists.newArrayList(instanceType));
				if(vmTypes.size()<=0)
					throw new EucalyptusCloudException("Invalid instance type -- " + instanceType);
			}catch(final EucalyptusCloudException ex){
				throw ex;
			}catch(final Exception ex) {
				throw new EucalyptusCloudException("Failed to verify instance type -- " + instanceType);
			}
		}

		//
		if( !Topology.isEnabledLocally( LoadBalancingBackend.class ) )
			return;

		if ((emi!=null && emi.length()>0) ||
				(instanceType!=null && instanceType.length()>0) ||
				(keyname!=null) || (initScript != null) ){
			Threads.enqueue(LoadBalancingBackend.class, LoadBalancerASGroupCreator.class, 1,
					new AsyncPropertyChanger(emi, instanceType, keyname, initScript));
		}
	}

	public static class ElbNTPServerChangeListener implements PropertyChangeListener {
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			try {
				if ( newValue instanceof String ) {
					if(((String) newValue).contains(",")){
						final String[] addresses = ((String)newValue).split(",");
						if((addresses.length-1) != StringUtils.countOccurrencesOf((String) newValue, ","))
							throw new EucalyptusCloudException("Invalid address");

						for(final String address : addresses){
							if(!HostSpecifier.isValid(String.format("%s.com",address)))
								throw new EucalyptusCloudException("Invalid address");
						}
					}else{
						final String address = (String) newValue;
						if( !address.equals("") ){
							if(!HostSpecifier.isValid(String.format("%s.com", address)))
								throw new EucalyptusCloudException("Invalid address");
						}
					}
				}else
					throw new EucalyptusCloudException("Address is not string type");
			} catch ( final Exception e ) {
				throw new ConfigurablePropertyException("Could not change ntp server address", e);
			}
		}
	}

	public static class ElbAppCookieDurationChangeListener implements PropertyChangeListener {
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			try {
				if ( newValue instanceof String ) {
					try{
						final int appCookieDuration = Integer.parseInt((String)newValue);
						if(appCookieDuration <= 0)
							throw new Exception();
					}catch(final NumberFormatException ex){
						throw new ConfigurablePropertyException("Duration must be in number type and bigger than 0 (in hours)");
					}
				}
			}catch (final ConfigurablePropertyException ex){
				throw ex;
			}catch (final Exception ex) {
				throw new ConfigurablePropertyException("Could not change ELB app cookie duration", ex);
			}
		}
	}

	@ConfigurableField( displayName = "image",
			description = "EMI containing haproxy and the controller",
			initial = "NULL",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ElbEmiChangeListener.class)
	public static String IMAGE = "NULL";

	@ConfigurableField( displayName = "instance_type",
			description = "instance type for loadbalancer instances",
			initial = "m1.small",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ElbInstanceTypeChangeListener.class)
	public static String INSTANCE_TYPE = "m1.small";

	@ConfigurableField( displayName = "keyname",
			description = "keyname to use when debugging loadbalancer VMs",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ElbKeyNameChangeListener.class)
	public static String KEYNAME = null;

	@ConfigurableField( displayName = "ntp_server",
			description = "the address of the NTP server used by loadbalancer VMs",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ElbNTPServerChangeListener.class
	)
	public static String NTP_SERVER = null;

	@ConfigurableField( displayName = "app_cookie_duration",
			description = "duration of app-controlled cookie to be kept in-memory (hours)",
			initial = "24", // 24 hours by default
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ElbAppCookieDurationChangeListener.class)
	public static String APP_COOKIE_DURATION = "24";

	@ConfigurableField( displayName = "expiration_days",
			description = "the days after which the loadbalancer Vms expire",
			initial = "365", // 1 year by default
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ElbVmExpirationDaysChangeListener.class)
	public static String EXPIRATION_DAYS = "365";

	@ConfigurableField(displayName = "init_script",
			description = "bash script that will be executed before service configuration and start up",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = InitScriptChangeListener.class)
	public static String INIT_SCRIPT = null;

	private List<String> launchConfigNames = Lists.newArrayList();
	private List<String> asgNames = Lists.newArrayList();
	private List<String> createdLaunchConfigNames = Lists.newArrayList();
	private List<String> createdAsgNames = Lists.newArrayList();
	private LoadbalancingEvent event = null;
	public LoadBalancerASGroupCreator(EventHandlerChain<? extends LoadbalancingEvent> chain){
		super(chain);
	}

	public static String getCredentialsString() {
		final String credStr = String.format("euca-%s:%s",
				B64.standard.encString("setup-credential"),
				EXPIRATION_DAYS);
		return credStr;
	}

	@Override
	public void apply( LoadbalancingEvent evt) throws EventHandlerException {
		if(IMAGE == null)
			throw new EventHandlerException("Loadbalancer's EMI is not configured");
		this.event = evt;
		Collection<String> eventZones = null;
		Collection<String> eventSecurityGroupIds = Collections.emptySet( );
		Map<String,String> zoneToSubnetIdMap = null;
		final LoadBalancer lbEntity;
		final LoadBalancer.LoadBalancerCoreView lb;
		try{
			lbEntity = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			lb = lbEntity.getCoreView( );
			if ( zoneToSubnetIdMap == null ) {
				zoneToSubnetIdMap = CollectionUtils.putAll(
						Iterables.filter( lbEntity.getZones( ), Predicates.compose( Predicates.notNull( ), subnetId( ) ) ),
						Maps.<String, String>newHashMap( ),
						name( ),
						subnetId( ) );
			}
		}catch(NoSuchElementException ex){
			throw new EventHandlerException("Failed to find the loadbalancer "+evt.getLoadBalancer(), ex);
		}catch(Exception ex){
			throw new EventHandlerException("Failed due to query exception", ex);
		}

		if ( evt instanceof NewLoadbalancerEvent ) {
			eventZones = ((NewLoadbalancerEvent) evt ).getZones( );
			zoneToSubnetIdMap = ((NewLoadbalancerEvent) evt ).getZoneToSubnetIdMap( );
		} else if ( evt instanceof ApplySecurityGroupsEvent ) {
			final Map<String,String> groupIdToNameMap = ( (ApplySecurityGroupsEvent) evt ).getSecurityGroupIdsToNames( );
			eventSecurityGroupIds = groupIdToNameMap == null ? eventSecurityGroupIds : groupIdToNameMap.keySet( );
			eventZones = Lists.newArrayList();
			eventZones.addAll(
					Collections2.transform(
							Collections2.filter(lbEntity.getZones(), new Predicate<LoadBalancerZoneCoreView> () {
								@Override
								public boolean apply(LoadBalancerZoneCoreView arg0) {
									return LoadBalancerZone.STATE.InService.equals(arg0.getState());
								}
							}),
							new Function<LoadBalancerZoneCoreView, String> () {
								@Override
								public String apply(LoadBalancerZoneCoreView arg0) {
									return arg0.getName();
								} }
					));
		}

		if( eventZones == null && eventSecurityGroupIds.isEmpty( ) )
			return;	// do nothing when zone/groups are not specified

		for (final String availabilityZone : eventZones) {
			final String groupName = getAutoScalingGroupName( lb.getOwnerAccountNumber(), lb.getDisplayName(), availabilityZone);
			String launchConfigName = null;
			String launchConfigToDelete = null;
			String instanceProfileName = null;
			try{
				List<String> result = this.chain.findHandler(InstanceProfileSetup.class).getResult();
				instanceProfileName = result.get(0);
			}catch(Exception ex){
				;
			}

			boolean asgFound = false;
			boolean updateLaunchConfig = false;
			try{
				final DescribeAutoScalingGroupsResponseType response =
						EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(groupName), lb.useSystemAccount());

				final List<AutoScalingGroupType> groups =
						response.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
				if(groups.size()>0 && groups.get(0).getAutoScalingGroupName().equals(groupName)){
					asgFound =true;
					launchConfigName = groups.get(0).getLaunchConfigurationName();
					if ( !eventSecurityGroupIds.isEmpty( ) ) {
						final LaunchConfigurationType lc =
								EucalyptusActivityTasks.getInstance().describeLaunchConfiguration( launchConfigName, lb.useSystemAccount() );

						updateLaunchConfig = lc == null ||
								lc.getSecurityGroups( ) == null ||
								!Sets.newHashSet( lc.getSecurityGroups( ).getMember( ) ).equals( Sets.newHashSet( eventSecurityGroupIds ) );
						if ( updateLaunchConfig ) {
							launchConfigToDelete = launchConfigName;
						}
					}
				}
			}catch(final Exception ex){
				asgFound = false;
			}
			final List<String> availabilityZones = Lists.newArrayList( availabilityZone );
			String vpcZoneIdentifier = null;
			String systemVpcZoneIdentifier = null;
			if ( !asgFound || updateLaunchConfig ) {
				try {
					vpcZoneIdentifier = zoneToSubnetIdMap.isEmpty() ?
							null :
							Strings.emptyToNull(Joiner.on(',').skipNulls().join(Iterables.transform(
									availabilityZones,
									Functions.forMap(zoneToSubnetIdMap))));
					if (vpcZoneIdentifier != null)
						systemVpcZoneIdentifier = LoadBalancingSystemVpcs.getSystemVpcSubnetId(vpcZoneIdentifier);
					else
						systemVpcZoneIdentifier = null;
				}catch(final Exception ex) {
					throw new EventHandlerException("Failed to look up subnet ID", ex);
				}

				try{
					Set<String> securityGroupNamesOrIds = null;
					if (systemVpcZoneIdentifier == null ) {
						securityGroupNamesOrIds = Sets.newHashSet(eventSecurityGroupIds);
						if (securityGroupNamesOrIds.isEmpty()) {
							if (!lb.getSecurityGroupIdsToNames().isEmpty()) {
								securityGroupNamesOrIds.addAll(lb.getSecurityGroupIdsToNames().keySet());
							} else {
								final StoredResult<String> sgroupSetup = this.getChain().findHandler(SecurityGroupSetup.class);
								final List<String> group = sgroupSetup.getResult();
								if (!group.isEmpty()) {
									securityGroupNamesOrIds.add(group.get(0));
								}
							}
						}
					}else { // if system VPC is used, use it's security group
						securityGroupNamesOrIds = Sets.newHashSet();
						securityGroupNamesOrIds.add(
								LoadBalancingSystemVpcs.getSecurityGroupId(systemVpcZoneIdentifier)
						);
					}
					final String keyName =
							KEYNAME!=null && KEYNAME.length()>0 ? KEYNAME : null;

					final String userData = B64.standard.encString(String.format("%s\n%s",
							getCredentialsString(),
							getLoadBalancerUserData(INIT_SCRIPT, lb.getOwnerAccountNumber())));

					launchConfigName = getLaunchConfigName (lb.getOwnerAccountNumber(), lb.getDisplayName(), availabilityZone);
					EucalyptusActivityTasks.getInstance().createLaunchConfiguration(IMAGE, INSTANCE_TYPE, instanceProfileName,
							launchConfigName, securityGroupNamesOrIds, keyName, userData,
							zoneToSubnetIdMap.isEmpty( ) ? null : false, lb.useSystemAccount() );
					this.createdLaunchConfigNames.add(launchConfigName);
				}catch(Exception ex){
					throw new EventHandlerException("Failed to create launch configuration", ex);
				}
			}
			this.launchConfigNames.add(launchConfigName);

			Integer capacity = EventHandlerChainNew.getCapacityPerZone();
			if(!asgFound){
				// create autoscaling group with the zone and desired capacity
				try{
					EucalyptusActivityTasks.getInstance().createAutoScalingGroup(groupName, availabilityZones, systemVpcZoneIdentifier,
							capacity, launchConfigName, TagCreator.TAG_KEY, TagCreator.TAG_VALUE, lb.useSystemAccount());
					this.createdAsgNames.add(groupName);
				}catch(Exception ex){
					throw new EventHandlerException("Failed to create autoscaling group", ex);
				}
			}else{
				try{
					EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, availabilityZones, capacity, launchConfigName, lb.useSystemAccount());
				}catch(Exception ex){
					throw new EventHandlerException("Failed to update the autoscaling group", ex);
				}
				if ( launchConfigToDelete != null ) try {
					EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration( launchConfigToDelete, lb.useSystemAccount());
				}catch(final Exception ex) {
					LOG.warn( "unable to delete launch configuration (" + launchConfigToDelete + ")", ex );
				}
			}
			this.asgNames.add(groupName);

			// commit ASG record to the database
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerAutoScalingGroup.class ) ) {
				try {
					final LoadBalancerAutoScalingGroup group =
							Entities.uniqueResult( LoadBalancerAutoScalingGroup.named( lbEntity, availabilityZone ) );
					if ( capacity != null ) group.setCapacity( capacity );
				}catch(NoSuchElementException ex){
					final LoadBalancerAutoScalingGroup group =
							LoadBalancerAutoScalingGroup.newInstance(lbEntity, availabilityZone,
									vpcZoneIdentifier, systemVpcZoneIdentifier, groupName, launchConfigName);
					if ( capacity != null ) group.setCapacity(capacity);
					Entities.persist(group);
				}
				db.commit();
			}catch(final Exception ex){
				throw new EventHandlerException("Failed to commit the database", ex);
			}
		} // end of for all zones
	}

	@Override
	public void rollback() throws EventHandlerException {
		LoadBalancer lb;
		try{
			lb = LoadBalancers.getLoadbalancer(this.event.getContext(), this.event.getLoadBalancer());
		}catch(Exception ex){
			throw new EventHandlerException("Could not find the loadbalancer with name="+this.event.getLoadBalancer(), ex);
		}

		for(final String asgName : this.createdAsgNames) {
			// delete autoscaling group
			try{
				// terminate all instances
				EucalyptusActivityTasks.getInstance().deleteAutoScalingGroup(asgName, true, lb.useSystemAccount());
			}catch(Exception ex){
				LOG.error("failed to delete autoscaling group - "+ asgName);
			}
		}

		for(final String launchConfigName : this.createdLaunchConfigNames) {
			// delete launch config
			try{
				EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(launchConfigName, lb.useSystemAccount());
			}catch(Exception ex){
				LOG.error("failed to delete launch configuration - "+launchConfigName);
			}
		}
	}

	@Override
	public List<String> getResult() {
		final List<String> result= Lists.newArrayList();
		result.addAll(this.asgNames);
		return result;
	}

	static String getLaunchConfigName( final String ownerAccountNumber, final String loadBalancerName, final String availabilityZone ) {
		String newLaunchConfigName =  String.format("lc-euca-internal-elb-%s-%s-%s-%s",
				ownerAccountNumber, loadBalancerName, availabilityZone, UUID.randomUUID().toString().substring(0, 8));
		if(newLaunchConfigName.length()>255)
			newLaunchConfigName = newLaunchConfigName.substring(0, 255);
		return newLaunchConfigName;
	}
	static String getAutoScalingGroupName( final String ownerAccountNumber, final String loadBalancerName, final String availabilityZone ) {
		String groupName = String.format("euca-internal-elb-%s-%s-%s", ownerAccountNumber, loadBalancerName, availabilityZone );
		if(groupName.length()>255)
			groupName = groupName.substring(0, 255);
		return groupName;
	}

	public static String getLoadBalancerUserData(String initScript, final String ownerAccountNumber) {
		Map<String, String> kvMap = new HashMap<String, String>();

		if (NTP_SERVER != null){
			kvMap.put("ntp_server", NTP_SERVER);
		}
		if(APP_COOKIE_DURATION != null){
			kvMap.put("app-cookie-duration", APP_COOKIE_DURATION);
		}

		kvMap.put("elb_service_url", String.format("loadbalancing.%s",DNSProperties.getDomain()));
		kvMap.put("euare_service_url", String.format("euare.%s", DNSProperties.getDomain()));
		kvMap.put("objectstorage_service_url", String.format("objectstorage.%s", DNSProperties.getDomain()));
		kvMap.put("webservice_port", String.format("%d", StackConfiguration.PORT));
		if(ownerAccountNumber!=null)
			kvMap.put("loadbalancer_owner_account", ownerAccountNumber);

		try {
			List<ServiceStatusType> services =
					EucalyptusActivityTasks.getInstance().describeServices("eucalyptus");

			if(services == null || services.size()<=0)
				throw new EucalyptusActivityException("failed to describe eucalyptus services");

			ServiceStatusType service = services.get(0);
			String serviceUrl = service.getServiceId().getUri();

			// parse the service Url: e.g., http://192.168.0.1:8773/services/Eucalyptus
			String tmp = serviceUrl.replace("http://", "").replace("https://", "");
			String host = tmp.substring(0, tmp.indexOf(":"));
			tmp = tmp.replace(host+":", "");
			String port = tmp.substring(0, tmp.indexOf("/"));
			String path = tmp.replace(port+"/", "");
			kvMap.put("eucalyptus_host", host);
			kvMap.put("eucalyptus_port", port);
			kvMap.put("eucalyptus_path", path);
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}

		final StringBuilder sb = new StringBuilder("#!/bin/bash").append("\n");
		if (initScript != null && initScript.length()>0)
			sb.append(initScript);
		sb.append("\n#System generated Load Balancer Servo config\n");
		sb.append("mkdir -p /etc/load-balancer-servo/\n");
		sb.append("yum -y --disablerepo \\* --enablerepo eucalyptus-service-image install load-balancer-servo\n");
		sb.append("echo \"");
		for (String key : kvMap.keySet()) {
			String value = kvMap.get(key);
			sb.append(String.format("\n%s=%s", key, value));
		}
		sb.append("\" > /etc/load-balancer-servo/servo.conf");
		sb.append("\nchown -R servo:servo /etc/load-balancer-servo");
		sb.append("\nservice load-balancer-servo start");
		return sb.toString();
	}

	public static class InitScriptChangeListener implements PropertyChangeListener<String> {
		@Override
		public void fireChange(ConfigurableProperty t, String newValue)
				throws ConfigurablePropertyException {
			try {
				// init script can be empty
				if (t.getValue() != null && !t.getValue().equals(newValue))
					onPropertyChange(null, null, null, (String) newValue);
			} catch (final Exception e) {
				throw new ConfigurablePropertyException("Could not change init script", e);
			}
		}
	}
}
