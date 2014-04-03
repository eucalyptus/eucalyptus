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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Faults;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.common.LoadBalancingBackend;
import com.eucalyptus.loadbalancing.LoadBalancerPolicies;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.InstanceProfileSetup;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.SecurityGroupSetup;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.TagCreator;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
import com.google.common.net.HostSpecifier;

import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;

/**
 * @author Sang-Min Park
 *
 */
@ConfigurableClass(root = "loadbalancing", description = "Parameters controlling loadbalancing")
public class LoadBalancerASGroupCreator extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String>{
	private static Logger  LOG     = Logger.getLogger( LoadBalancerASGroupCreator.class );

	public static class ElbEmiChangeListener implements PropertyChangeListener {
		   @Override
		   public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			    try {
			          if ( newValue instanceof String  ) {
				    	  if(t.getValue()!=null && ! t.getValue().equals(newValue))
				    		  onPropertyChange((String)newValue, null, null);
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
			    	  if(newValue == null || ((String) newValue).equals(""))
			    		  throw new EucalyptusCloudException("Instance type cannot be unset");
			    	  if(t.getValue()!=null && ! t.getValue().equals(newValue))
						 onPropertyChange(null, (String)newValue, null);
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
			    		  onPropertyChange(null, null, (String)newValue);
			      }
			    } catch ( final Exception e ) {
					throw new ConfigurablePropertyException("Could not change key name", e);
			    }
			}
	}
	
	private static void onPropertyChange(final String emi, final String instanceType, final String keyname) throws EucalyptusCloudException{
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
		
		if(instanceType != null){
			;
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
		
		if((emi!=null && emi.length()>0) || (instanceType!=null && instanceType.length()>0) || (keyname!=null && keyname.length()>0)){
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

						String launchConfigName = null;
						do{
							launchConfigName = String.format("lc-euca-internal-elb-%s-%s-%s", 
									lb.getOwnerAccountNumber(), lb.getDisplayName(), UUID.randomUUID().toString().substring(0, 8));

							if(launchConfigName.length()>255)
								launchConfigName = launchConfigName.substring(0, 255);
						}while(launchConfigName.equals(asgType.getLaunchConfigurationName()));

						final String newEmi = emi != null? emi : lc.getImageId();
						final String newType = instanceType != null? instanceType : lc.getInstanceType();
						String newKeyname = keyname != null ? keyname : lc.getKeyName();

						try{
							EucalyptusActivityTasks.getInstance().createLaunchConfiguration(newEmi, newType, lc.getIamInstanceProfile(), 
									launchConfigName, lc.getSecurityGroups().getMember().get(0), newKeyname, lc.getUserData());
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
						if(address != null && ! address.equals("")){
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
         throw new ConfigurablePropertyException("Could not change instance type", ex);
       }
   }
}

	@ConfigurableField( displayName = "loadbalancer_emi", 
        description = "EMI containing haproxy and the controller",
        initial = "NULL", 
        readonly = false,
        type = ConfigurableFieldType.KEYVALUE,
        changeListener = ElbEmiChangeListener.class)
	public static String LOADBALANCER_EMI = "NULL";
	
	@ConfigurableField( displayName = "loadbalancer_instance_type", 
		description = "instance type for loadbalancer instances",
		initial = "m1.small", 
		readonly = false,
		type = ConfigurableFieldType.KEYVALUE,
		changeListener = ElbInstanceTypeChangeListener.class)
	public static String LOADBALANCER_INSTANCE_TYPE = "m1.small";
	
	@ConfigurableField( displayName = "loadbalancer_vm_keyname", 
			description = "keyname to use when debugging loadbalancer VMs",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ElbKeyNameChangeListener.class)
	public static String LOADBALANCER_VM_KEYNAME = null;
	
	@ConfigurableField( displayName = "loadbalancer_vm_ntp_server", 
			description = "the address of the NTP server used by loadbalancer VMs", 
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ElbNTPServerChangeListener.class
			)
	public static String LOADBALANCER_VM_NTP_SERVER = null;
	
	@ConfigurableField( displayName = "loadbalancer_app_cookie_duration",
	    description = "duration of app-controlled cookie to be kept in-memory (hours)",
	    initial = "24", // 24 hours by default 
	    readonly = false,
	    type = ConfigurableFieldType.KEYVALUE,
	    changeListener = ElbAppCookieDurationChangeListener.class)
	public static String LOADBALANCER_APP_COOKIE_DURATION = "24";
	
	@Provides(LoadBalancingBackend.class)
	@RunDuring(Bootstrap.Stage.Final)
	@DependsLocal(LoadBalancingBackend.class)
	public static class LoadBalancingPropertyBootstrapper extends Bootstrapper.Simple {

	  private static LoadBalancingPropertyBootstrapper singleton;
	  private static final Runnable imageNotConfiguredFaultRunnable =
	      Faults.forComponent( LoadBalancingBackend.class ).havingId( 1014 ).logOnFirstRun( );

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

	  @Override
    public boolean check( ) throws Exception {
      if ( CloudMetadatas.isMachineImageIdentifier( LoadBalancerASGroupCreator.LOADBALANCER_EMI ) ) {
        return true;
      } else {
        imageNotConfiguredFaultRunnable.run( );
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

		
	private NewLoadbalancerEvent event = null;
	private LoadBalancer loadbalancer = null;
	private int capacityPerZone = 1;
	
	private String launchConfigName = null;
	private String asgName = null;
	private LoadBalancerASGroupCreator(
			EventHandlerChain<NewLoadbalancerEvent> chain) {
		super(chain);
	}
	public LoadBalancerASGroupCreator(EventHandlerChain<NewLoadbalancerEvent> chain, int capacityPerZone){
		this(chain);
		this.capacityPerZone = capacityPerZone;
	}

	@Override
	public void apply(NewLoadbalancerEvent evt) throws EventHandlerException {
		if(LOADBALANCER_EMI == null)
			throw new EventHandlerException("Loadbalancer's EMI is not configured");
		this.event = evt;
		if(evt.getZones() == null || evt.getZones().size() <= 0)
			return;	// do nothing when zone is not specified
		
		LoadBalancer lb = null;
		try{
			lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			this.loadbalancer = lb;
		}catch(NoSuchElementException ex){
			throw new EventHandlerException("Failed to find the loadbalancer "+evt.getLoadBalancer(), ex);
		}catch(Exception ex){
			throw new EventHandlerException("Failed due to query exception", ex);
		}
		
		// create user data to supply to haproxy tooling
		InstanceUserDataBuilder userDataBuilder  = null;
		try{
			userDataBuilder= new DefaultInstanceUserDataBuilder();
		}catch(Exception ex){
			throw new EventHandlerException("failed to create service parameters", ex);
		}
		
		String launchConfigName = String.format("lc-euca-internal-elb-%s-%s-%s", 
				lb.getOwnerAccountNumber(), lb.getDisplayName(), UUID.randomUUID().toString().substring(0, 8));
		if(launchConfigName.length()>255)
			launchConfigName = launchConfigName.substring(0, 255);
		
		String groupName = String.format("asg-euca-internal-elb-%s-%s", lb.getOwnerAccountNumber(), lb.getDisplayName());
		if(groupName.length()>255)
			groupName = groupName.substring(0, 255);
		
		String instanceProfileName = null;
		try{
			List<String> result = this.chain.findHandler(InstanceProfileSetup.class).getResult();
			instanceProfileName = result.get(0);
		}catch(Exception ex){
			;
		}
		
		boolean asgFound = false;
		try{
			final DescribeAutoScalingGroupsResponseType response = 
					EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(groupName));
			final List<AutoScalingGroupType> groups =
					response.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
			if(groups.size()>0 && groups.get(0).getAutoScalingGroupName().equals(groupName)){
				asgFound =true;
				launchConfigName = groups.get(0).getLaunchConfigurationName();
			}
		}catch(final Exception ex){
			asgFound = false;
		}
		
		// create launch config based on the parameters
		int capacity =1;
		if(!asgFound){
			try{
				StoredResult<String> sgroupSetup = this.getChain().findHandler(SecurityGroupSetup.class);
				final List<String> group = sgroupSetup.getResult();
				final String sgroupName = group.size()>0 ? group.get(0) : null;
				final String keyName = 
						LOADBALANCER_VM_KEYNAME!=null && LOADBALANCER_VM_KEYNAME.length()>0 ? LOADBALANCER_VM_KEYNAME : null;
				final String userData = B64.standard.encString(String.format("%s\n%s", 
				    "euca-"+B64.standard.encString("setup-credential"),
				    userDataBuilder.build()));
				
				EucalyptusActivityTasks.getInstance().createLaunchConfiguration(LOADBALANCER_EMI, LOADBALANCER_INSTANCE_TYPE, instanceProfileName,
						launchConfigName, sgroupName, keyName, userData);
				this.launchConfigName = launchConfigName;
			}catch(Exception ex){
				throw new EventHandlerException("Failed to create launch configuration", ex);
			}
			
			// create autoscaling group with zones and desired capacity
			try{
				final List<String> availabilityZones = Lists.newArrayList(evt.getZones());
				capacity = availabilityZones.size() * this.capacityPerZone;
				EucalyptusActivityTasks.getInstance().createAutoScalingGroup(groupName, availabilityZones, 
						capacity, launchConfigName, TagCreator.TAG_KEY, TagCreator.TAG_VALUE);
				this.asgName = groupName;
			}catch(Exception ex){
				throw new EventHandlerException("Failed to create autoscaling group", ex);
			}
		}else{
			try{
				final List<String> availabilityZones = Lists.newArrayList(evt.getZones());
				capacity = availabilityZones.size() * this.capacityPerZone;
				EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, availabilityZones, capacity);
			}catch(Exception ex){
				throw new EventHandlerException("Failed to update the autoscaling group", ex);
			}
			this.asgName = groupName;
			this.launchConfigName = launchConfigName;
		}
		
		// commit ASG record to the database
		final EntityTransaction db = Entities.get( LoadBalancerAutoScalingGroup.class );
		try{
			Entities.uniqueResult(LoadBalancerAutoScalingGroup.named(lb));
			db.commit(); // should not happen
		}catch(NoSuchElementException ex){
			final LoadBalancerAutoScalingGroup group = LoadBalancerAutoScalingGroup.newInstance(lb, groupName, this.launchConfigName);
			group.setCapacity(capacity);
			Entities.persist(group);
			db.commit();
		}catch(Exception ex){
			db.rollback();
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
		// TODO Auto-generated method stub
		List<String> result= Lists.newArrayList();
		if(this.launchConfigName != null)
			result.add(this.launchConfigName);
		if(this.asgName != null)
			result.add(this.asgName);
		return result;
	}

	private class InstanceUserDataWithCredential extends DefaultInstanceUserDataBuilder{
		private UserFullName user = null;
		InstanceUserDataWithCredential(UserFullName requestingUser){
			/// credentials
			this.user = requestingUser;
			/// TODO: SPARK: Assuming that the ELB service has an access to database
			try{
				/// ASSUMING LB SERVICE HAS ACCESS TO LOCAL DB
				final User adminUser= Accounts.lookupSystemAdmin( );
				final List<AccessKey> adminKeys = adminUser.getKeys();
				if(adminKeys.size() <= 0)
					throw new EucalyptusActivityException("No access key is found for the admin user");
				final AccessKey adminKey = adminKeys.get(0);
				this.add("access_key", adminKey.getAccessKey());
				this.add("secret_key", adminKey.getSecretKey());
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}
	
	private class DefaultInstanceUserDataBuilder implements InstanceUserDataBuilder {
		ConcurrentHashMap<String,String> dataDict= null;
		protected DefaultInstanceUserDataBuilder(){
			dataDict = new ConcurrentHashMap<String,String>();
			// describe-services to retrieve the info:
			try{
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
				this.add("eucalyptus_host", host);
				this.add("eucalyptus_port", port);
				this.add("eucalyptus_path", path);
				
				this.add("elb_host", host);
				this.add("elb_port", port);	/// elb service path
				if(LOADBALANCER_VM_NTP_SERVER != null && LOADBALANCER_VM_NTP_SERVER.length()>0){
					this.add("ntp_server", LOADBALANCER_VM_NTP_SERVER);
				}
				if(LOADBALANCER_APP_COOKIE_DURATION != null){
				  this.add("app-cookie-duration", LOADBALANCER_APP_COOKIE_DURATION);
				}
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
		
		protected void add(String name, String value){
			dataDict.put(name, value);
		}
		
		@Override
		public String build(){
			StringBuilder sb  = new StringBuilder();
			for (String key : dataDict.keySet()){
				String value = dataDict.get(key);
				sb.append(String.format("%s=%s;", key, value));
			}
			return sb.toString();
		}
	}
	interface InstanceUserDataBuilder {
		String build();
	}
}
