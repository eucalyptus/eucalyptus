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
import java.util.concurrent.ConcurrentHashMap;

import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults.CheckException;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

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
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.ResourceTag;
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
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.common.LoadBalancingBackend;
import com.eucalyptus.loadbalancing.LoadBalancerPolicies;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.InstanceProfileSetup;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.SecurityGroupSetup;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.TagCreator;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
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
					throw new ConfigurablePropertyException("Could not change EMI ID", e);
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
					throw new ConfigurablePropertyException("Could not change instance type", e);
			    }
			}
	}
	
	public static class ElbKeyNameChangeListener implements PropertyChangeListener {
		   @Override
		   public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			    try {
			      if ( newValue instanceof String ) {	  
			    	  if(t.getValue()!=null && ! t.getValue().equals(newValue))
			    		  onPropertyChange(null, null, (String)newValue, null);
			      }
			    } catch ( final Exception e ) {
					throw new ConfigurablePropertyException("Could not change key name", e);
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
	
	private static void onPropertyChange(final String emi, final String instanceType,
	    final String keyname, String initScript) throws EucalyptusCloudException{
		if (!( Bootstrap.isFinished() &&
		          Topology.isEnabledLocally( LoadBalancingBackend.class ) &&
		          Topology.isEnabled( Eucalyptus.class ) ) )
			return;
		
		// should validate the parameters
		if(emi!=null){
			try{
				final List<ImageDetails> images =
					EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(emi));
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
		
		if(keyname != null && ! keyname.equals("")){
			try{
				final List<DescribeKeyPairsResponseItemType> keypairs =
						EucalyptusActivityTasks.getInstance().describeKeyPairs(Lists.newArrayList(keyname));
				if(keypairs ==null || keypairs.size()<=0)
					throw new EucalyptusCloudException("No such keypair is found in the system");
				if(! keypairs.get(0).getKeyName().equals(keyname))
					throw new EucalyptusCloudException("No such keypair is found in the system");
			}catch(final EucalyptusCloudException ex){
				throw ex;
			}catch(final Exception ex){
				throw new EucalyptusCloudException("Failed to verify the keyname in the system");
			}
		}
		
		if((emi!=null && emi.length()>0) || (instanceType!=null && instanceType.length()>0) || (keyname!=null && keyname.length()>0) || (initScript != null) ){
			// 
			final List<LoadBalancer> lbs = LoadBalancers.listLoadbalancers();
			for(final LoadBalancer lb : lbs){
				final LoadBalancerAutoScalingGroupCoreView asg = lb.getAutoScaleGroup();
				if(asg==null || asg.getName()==null)
					continue;

				final String asgName = asg.getName();
				try{
					AutoScalingGroupType asgType = null;
					try{
						final DescribeAutoScalingGroupsResponseType resp = EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(asgName));
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
						final LaunchConfigurationType lc = EucalyptusActivityTasks.getInstance().describeLaunchConfiguration(lcName);

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
                getLoadBalancerUserData(initScript)));
   
						try{
							EucalyptusActivityTasks.getInstance().createLaunchConfiguration(newEmi, newType, lc.getIamInstanceProfile(), 
									launchConfigName, lc.getSecurityGroups().getMember(), newKeyname, newUserdata,
									Boolean.TRUE.equals( lc.getAssociatePublicIpAddress( ) ) );
						}catch(final Exception ex){
							throw new EucalyptusCloudException("failed to create new launch config", ex);
						}
						try{
							EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(asgName, null,asgType.getDesiredCapacity(), launchConfigName);
						}catch(final Exception ex){
							throw new EucalyptusCloudException("failed to update the autoscaling group", ex);
						}
						try{
							EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(asgType.getLaunchConfigurationName());
						}catch(final Exception ex){
							LOG.warn("unable to delete the old launch configuration", ex);
						}	
	          // copy all tags from image to ASG
	          try {
	            final List<ImageDetails> images =
	                EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(emi));
	            // image should exist at this point
	            for(ResourceTag tag:images.get(0).getTagSet())
	              EucalyptusActivityTasks.getInstance().createOrUpdateAutoscalingTags(tag.getKey(), tag.getValue(), asgName);
	          } catch (final Exception ex) {
	            LOG.warn("unable to propogate tags from image to ASG", ex);
	          }
						LOG.debug(String.format("autoscaling group '%s' was updated", asgName));
					}
				}catch(final EucalyptusCloudException ex){
					throw ex;
				}catch(final Exception ex){
					throw new EucalyptusCloudException("Unable to update the autoscaling group", ex);
				}
			}
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
	 
	@Provides(LoadBalancingBackend.class)
	@RunDuring(Bootstrap.Stage.Final)
	@DependsLocal(LoadBalancingBackend.class)
	public static class LoadBalancingPropertyBootstrapper extends Bootstrapper.Simple {

	  private static LoadBalancingPropertyBootstrapper singleton;
	  private static final Callable<String> imageNotConfiguredFaultRunnable =
	      Faults.forComponent( LoadBalancingBackend.class ).havingId( 1014 ).logOnFirstRun();

	  public static Bootstrapper getInstance( ) {
	    synchronized ( LoadBalancingPropertyBootstrapper.class ) {
	      if ( singleton == null ) {
	        singleton = new LoadBalancingPropertyBootstrapper( );
	        LOG.info( "Creating Load Balancing Bootstrapper instance." );
	      } else {
	        LOG.info( "Returning Load Balancing Bootstrapper instance." );
	      }
	    }
	    return singleton;
	  }
	  
	  private static int CheckCounter =0 ;
	  private static boolean EmiCheckResult = true;
	  @Override
    public boolean check( ) throws Exception {
      if ( CloudMetadatas.isMachineImageIdentifier( LoadBalancerASGroupCreator.IMAGE ) ) {
        if( CheckCounter >= 3 && Topology.isEnabled( Eucalyptus.class ) ){
          try{
            final List<ImageDetails> emis =
                EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(LoadBalancerASGroupCreator.IMAGE));
            EmiCheckResult = LoadBalancerASGroupCreator.IMAGE.equals( emis.get( 0 ).getImageId() );
          }catch(final Exception ex){
            EmiCheckResult=false;
          }
          CheckCounter = 0;
        }else
          CheckCounter++;
        return EmiCheckResult;
        } else {
        try {
          //GRZE: do this bit in the way that it allows getting the information with out needing to spelunk log files.
          final ServiceConfiguration localService = Components.lookup( LoadBalancingBackend.class ).getLocalServiceConfiguration( );
          final CheckException ex = Faults.failure( localService, imageNotConfiguredFaultRunnable.call( ).split("\n")[1] );
          Faults.submit( localService, localService.lookupStateMachine().getTransitionRecord(), ex );
        } catch ( Exception e ) {
          LOG.debug( e );
        }
        return false;
       }
    }
	  
	  @Override
	  public boolean enable( ) throws Exception {
	    if (!super.enable())
	      return false;
	    try{
	      LoadBalancerPolicies.initialize();
	    }catch(final Exception ex){
	      LOG.error("Unable to initialize ELB policy types", ex);
	      return false;
	    }
	    return true;
	  }
	}

		
	private int capacityPerZone = 1;
	private String launchConfigName = null;
	private String asgName = null;
	public LoadBalancerASGroupCreator(EventHandlerChain<? extends LoadbalancingEvent> chain, int capacityPerZone){
		super(chain);
		this.capacityPerZone = capacityPerZone;
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

		Collection<String> eventZones = null;
		Collection<String> eventSecurityGroupIds = Collections.emptySet( );
		Map<String,String> zoneToSubnetIdMap = null;
		if ( evt instanceof NewLoadbalancerEvent ) {
			eventZones = ((NewLoadbalancerEvent) evt ).getZones( );
			zoneToSubnetIdMap = ((NewLoadbalancerEvent) evt ).getZoneToSubnetIdMap( );
		} else if ( evt instanceof ApplySecurityGroupsEvent ) {
			final Map<String,String> groupIdToNameMap = ( (ApplySecurityGroupsEvent) evt ).getSecurityGroupIdsToNames( );
			eventSecurityGroupIds = groupIdToNameMap == null ? eventSecurityGroupIds : groupIdToNameMap.keySet( );
		}

		if( eventZones == null && eventSecurityGroupIds.isEmpty( ) )
			return;	// do nothing when zone/groups are not specified

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
		
		String newlaunchConfigName = String.format("lc-euca-internal-elb-%s-%s-%s",
				lb.getOwnerAccountNumber(), lb.getDisplayName(), UUID.randomUUID().toString().substring(0, 8));
		if(newlaunchConfigName.length()>255)
			newlaunchConfigName = newlaunchConfigName.substring(0, 255);
		String launchConfigName = newlaunchConfigName;
		String launchConfigToDelete = null;

		String groupName = getAutoScalingGroupName( lb.getOwnerAccountNumber(), lb.getDisplayName() );

		String instanceProfileName = null;
		try{
			List<String> result = this.chain.findHandler(InstanceProfileSetup.class).getResult();
			instanceProfileName = result.get(0);
		}catch(Exception ex){
		}
		
		boolean asgFound = false;
		boolean updateLaunchConfig = false;
		try{
			final DescribeAutoScalingGroupsResponseType response = 
					EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(groupName));
			final List<AutoScalingGroupType> groups =
					response.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
			if(groups.size()>0 && groups.get(0).getAutoScalingGroupName().equals(groupName)){
				asgFound =true;
				launchConfigName = groups.get(0).getLaunchConfigurationName();
				if ( !eventSecurityGroupIds.isEmpty( ) ) {
					final LaunchConfigurationType lc = EucalyptusActivityTasks.getInstance().describeLaunchConfiguration( launchConfigName );
					updateLaunchConfig = lc == null ||
							lc.getSecurityGroups( ) == null ||
							!Sets.newHashSet( lc.getSecurityGroups( ).getMember( ) ).equals( Sets.newHashSet( eventSecurityGroupIds ) );
					if ( updateLaunchConfig ) {
						launchConfigToDelete = launchConfigName;
						launchConfigName = newlaunchConfigName;
					}
				}
			}
		}catch(final Exception ex){
			asgFound = false;
		}
		
		// create launch config based on the parameters
		if ( !asgFound || updateLaunchConfig ) {
			try{
				final Set<String> securityGroupNamesOrIds = Sets.newHashSet( eventSecurityGroupIds );
				if ( securityGroupNamesOrIds.isEmpty( ) ) {
					if ( !lb.getSecurityGroupIdsToNames().isEmpty() ) {
						securityGroupNamesOrIds.addAll( lb.getSecurityGroupIdsToNames().keySet() );
					} else {
						final StoredResult<String> sgroupSetup = this.getChain().findHandler( SecurityGroupSetup.class );
						final List<String> group = sgroupSetup.getResult();
						if ( !group.isEmpty() ) {
							securityGroupNamesOrIds.add( group.get( 0 ) );
						}
					}
				}
				final String keyName =
						KEYNAME!=null && KEYNAME.length()>0 ? KEYNAME : null;

				final String userData = B64.standard.encString(String.format("%s\n%s",
				    getCredentialsString(),
				    getLoadBalancerUserData(INIT_SCRIPT)));

				EucalyptusActivityTasks.getInstance().createLaunchConfiguration(IMAGE, INSTANCE_TYPE, instanceProfileName,
						launchConfigName, securityGroupNamesOrIds, keyName, userData, !zoneToSubnetIdMap.isEmpty( ) );
				this.launchConfigName = launchConfigName;
			}catch(Exception ex){
				throw new EventHandlerException("Failed to create launch configuration", ex);
			}
		}

		Integer capacity;
		if(!asgFound){
			// create autoscaling group with zones and desired capacity
			if ( eventZones == null ) {
				throw new EventHandlerException("Availability zones required to create auto scaling group.");
			}
			try{
				final List<String> availabilityZones = Lists.newArrayList( eventZones );
				final String vpcZoneIdentifier = zoneToSubnetIdMap.isEmpty( ) ?
						null :
						Strings.emptyToNull( Joiner.on( ',' ).skipNulls().join( Iterables.transform(
								availabilityZones,
								Functions.forMap( zoneToSubnetIdMap ) ) ) );
				capacity = availabilityZones.size() * this.capacityPerZone;
				EucalyptusActivityTasks.getInstance().createAutoScalingGroup(groupName, availabilityZones, vpcZoneIdentifier,
						capacity, launchConfigName, TagCreator.TAG_KEY, TagCreator.TAG_VALUE);
				this.asgName = groupName;
			}catch(Exception ex){
				throw new EventHandlerException("Failed to create autoscaling group", ex);
			}
		}else{
			try{
				final List<String> availabilityZones = eventZones == null ? null : Lists.newArrayList( eventZones );
				capacity = availabilityZones == null ? null : availabilityZones.size() * this.capacityPerZone;
				EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, availabilityZones, capacity, launchConfigName);
			}catch(Exception ex){
				throw new EventHandlerException("Failed to update the autoscaling group", ex);
			}
			if ( launchConfigToDelete != null ) try {
				EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration( launchConfigToDelete );
			}catch(final Exception ex) {
				LOG.warn( "unable to delete launch configuration (" + launchConfigToDelete + ")", ex );
			}
			this.asgName = groupName;
			this.launchConfigName = launchConfigName;
		}

		// commit ASG record to the database
		try ( final TransactionResource db = Entities.transactionFor( LoadBalancerAutoScalingGroup.class ) ) {
			try {
				final LoadBalancerAutoScalingGroup group = Entities.uniqueResult( LoadBalancerAutoScalingGroup.named( lbEntity ) );
				if ( capacity != null ) group.setCapacity( capacity );
			}catch(NoSuchElementException ex){
				final LoadBalancerAutoScalingGroup group = LoadBalancerAutoScalingGroup.newInstance(lbEntity, groupName, this.launchConfigName);
				if ( capacity != null ) group.setCapacity(capacity);
				Entities.persist(group);
			}
			db.commit();
		}catch(Exception ex){
			throw new EventHandlerException("Failed to commit the database", ex);
		}
	}

	@Override
	public void rollback() throws EventHandlerException {
		
		// delete autoscaling group
		if(this.asgName != null){
			try{
				// terminate all instances
				EucalyptusActivityTasks.getInstance().deleteAutoScalingGroup(this.asgName, true);
			}catch(Exception ex){
				LOG.error("failed to delete autoscaling group - "+this.asgName);
			}
		}
		
		// delete launch config
		if(this.launchConfigName != null){
			try{
				EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(this.launchConfigName);
			}catch(Exception ex){
				LOG.error("failed to delete launch configuration - "+this.launchConfigName);
			}
		}
	}

	@Override
	public List<String> getResult() {
		List<String> result= Lists.newArrayList();
		if(this.launchConfigName != null)
			result.add(this.launchConfigName);
		if(this.asgName != null)
			result.add(this.asgName);
		return result;
	}

	static String getAutoScalingGroupName( final String ownerAccountNumber, final String loadBalancerName ) {
		String groupName = String.format("asg-euca-internal-elb-%s-%s", ownerAccountNumber, loadBalancerName );
		if(groupName.length()>255)
			groupName = groupName.substring(0, 255);
		return groupName;
	}

	public static String getLoadBalancerUserData(String initScript) {
    Map<String, String> kvMap = new HashMap<String, String>();
    if (NTP_SERVER != null)
      kvMap.put("ntp_server", NTP_SERVER);

    if(APP_COOKIE_DURATION != null){
      kvMap.put("app-cookie-duration", APP_COOKIE_DURATION);
    }

    kvMap.put("elb_service_url", String.format("loadbalancing.%s",DNSProperties.DOMAIN));
    kvMap.put("euare_service_url", String.format("euare.%s", DNSProperties.DOMAIN));
    
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
    sb.append("\ntouch /var/lib/load-balancer-servo/ntp.lock");
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
