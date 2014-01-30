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
package com.eucalyptus.imaging.setup;


import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.AddRoleToInstanceProfileType;
import com.eucalyptus.auth.euare.CreateInstanceProfileResponseType;
import com.eucalyptus.auth.euare.CreateInstanceProfileType;
import com.eucalyptus.auth.euare.CreateRoleResponseType;
import com.eucalyptus.auth.euare.CreateRoleType;
import com.eucalyptus.auth.euare.InstanceProfileType;
import com.eucalyptus.auth.euare.ListInstanceProfilesResponseType;
import com.eucalyptus.auth.euare.ListInstanceProfilesType;
import com.eucalyptus.auth.euare.ListRolesResponseType;
import com.eucalyptus.auth.euare.ListRolesType;
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.AutoScalingGroupNames;
import com.eucalyptus.autoscaling.common.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.AvailabilityZones;
import com.eucalyptus.autoscaling.common.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.DescribeLaunchConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.DescribeLaunchConfigurationsType;
import com.eucalyptus.autoscaling.common.LaunchConfigurationNames;
import com.eucalyptus.autoscaling.common.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.LaunchConfigurationsType;
import com.eucalyptus.autoscaling.common.SecurityGroups;
import com.eucalyptus.autoscaling.common.TagType;
import com.eucalyptus.autoscaling.common.Tags;
import com.eucalyptus.autoscaling.common.UpdateAutoScalingGroupType;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.Signatures;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.imaging.Imaging;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vmtypes.DescribeInstanceTypesResponseType;
import com.eucalyptus.vmtypes.DescribeInstanceTypesType;
import com.eucalyptus.vmtypes.VmTypeDetails;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.CreateSecurityGroupType;
import edu.ucsb.eucalyptus.msgs.CreateTagsType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.ResourceTag;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;

@ConfigurableClass(root = "imaging",  description = "Parameters controlling image conversion tasks")
public class ImagingASGroupCreator {
	private static Logger  LOG = Logger.getLogger( ImagingASGroupCreator.class );

	@ConfigurableField( displayName = "imaging_emi",
	        description = "EMI containing to be named service",
	        initial = "NULL",
	        readonly = false,
	        changeListener = EmiChangeListener.class)
	public static String IMAGING_EMI = "Not set";
		
	@ConfigurableField( displayName = "imaging_instance_type", 
			description = "instance type for imaging instances",
			initial = "m1.small", 
			readonly = false,
			changeListener = InstanceTypeChangeListener.class)
	public static String IMAGING_INSTANCE_TYPE = "m1.small";

	@ConfigurableField( displayName = "imaging_vm_keyname",
			description = "keyname to use when debugging imager VM",
			readonly = false,
			changeListener = KeyNameChangeListener.class)
	public static String IMAGING_VM_KEYNAME = null;

	public static class EmiChangeListener implements PropertyChangeListener<String> {
		@Override
		public void fireChange( ConfigurableProperty t, String newValue ) throws ConfigurablePropertyException {
			try {
				if(t.getValue()!=null && ! t.getValue().equals(newValue)) {
					// checks that EMI exists
					final List<ImageDetails> images = describeImages(Lists.newArrayList(newValue));
					if(images == null || images.size()<=0)
						throw new EucalyptusCloudException("No such EMI is found in the system");
					if(! images.get(0).getImageId().toLowerCase().equals(newValue.toLowerCase()))
						throw new EucalyptusCloudException("No such EMI is found in the system");
				}
		    } catch (Exception e) {
				throw new ConfigurablePropertyException("Could not change EMI ID. " + e.getMessage());
		    }
		}
	}
	
	public static class InstanceTypeChangeListener implements PropertyChangeListener<String> {
		@Override
		public void fireChange( ConfigurableProperty t, String newValue ) throws ConfigurablePropertyException {
			try {
				if(newValue == null || newValue.equals(""))
					throw new EucalyptusCloudException("Instance type cannot be unset");
				if(t.getValue()!=null && ! t.getValue().equals(newValue)) {
					// checks that the passed instance type exists
					final List<VmTypeDetails> allTypes = describeVMTypes();
					boolean typeExists = false;
					String newTypeName = newValue.toLowerCase();
					for (VmTypeDetails detail:allTypes){
						if (detail.getName().equals(newTypeName)){
							typeExists = true;
							break;
						}
					}
					if (!typeExists)
						throw new EucalyptusCloudException("Invalid instance type '" + newValue + "'");
					String oldTypeName = IMAGING_INSTANCE_TYPE;
					IMAGING_INSTANCE_TYPE = newTypeName;
					changeLaunchConfiguration(IMAGING_VM_KEYNAME, IMAGING_VM_KEYNAME, newTypeName, oldTypeName);
				}
			} catch ( final Exception e ) {
				throw new ConfigurablePropertyException("Could not change instance type. " + e.getMessage());
			}
		}
	}
	
	public static class KeyNameChangeListener implements PropertyChangeListener<String> {
		@Override
		public void fireChange( ConfigurableProperty t, String newValue ) throws ConfigurablePropertyException {
			try {
				if(newValue == null || newValue.equals("")) {
					LOG.warn("Unset imaging_vm_keyname");
					String oldKeyName = IMAGING_VM_KEYNAME;
					IMAGING_VM_KEYNAME = null;
					changeLaunchConfiguration(null, oldKeyName, IMAGING_INSTANCE_TYPE, IMAGING_INSTANCE_TYPE);
					return;
				}
				if(t.getValue()!=null && ! t.getValue().equals(newValue)) {
					String newKeyName = newValue.toLowerCase();
					// checks that the passed key exists
					List<DescribeKeyPairsResponseItemType> allKeys = describeKeyPairs(Lists.newArrayList(newKeyName));
					boolean keyExist = false;
					for (DescribeKeyPairsResponseItemType detail:allKeys){
						if (detail.getKeyName().equals(newKeyName)){
							keyExist = true;
							break;
						}
					}
					if (!keyExist)
						throw new EucalyptusCloudException("Invalid key '" + newKeyName + "'");
					String oldKeyName = IMAGING_VM_KEYNAME;
					IMAGING_VM_KEYNAME = newKeyName;
					changeLaunchConfiguration(newKeyName, oldKeyName, IMAGING_INSTANCE_TYPE, IMAGING_INSTANCE_TYPE);
				}
			} catch ( final Exception e ) {
				throw new ConfigurablePropertyException("Could not change keyname. " + e.getMessage());
			}
		}
	}

	@Provides(Imaging.class)
	@RunDuring(Bootstrap.Stage.Final)
	@DependsLocal(Imaging.class)
	public static class ImagingPropertyBootstrapper extends Bootstrapper.Simple {

		private static ImagingPropertyBootstrapper singleton;

		public static Bootstrapper getInstance() {
			synchronized ( ImagingPropertyBootstrapper.class ) {
				if ( singleton == null ) {
					singleton = new ImagingPropertyBootstrapper( );
					LOG.info( "Creating Imaging Bootstrapper instance." );
				} else {
					LOG.debug( "Returning Imaging Balancing Bootstrapper instance." );
				}
		    }
		    return singleton;
		}
		
		@Override
		public boolean check() throws Exception {
			if ( IMAGING_EMI != null && IMAGING_EMI.startsWith("emi-") ) {
				return true;
			} else {
				//TODO: the name of the service is TBD, change message later
				LOG.debug("Imaging service EMI property is unset.  \"\n" +
						"              + \"Use euca-modify-property -p imaging.imaging_emi=<imaging service emi> \"\n" +
						"              + \"where the emi should point to the image provided in the eucalyptus-TBD package.\" ");
				return false;
			}
		}
		  
		@Override
		public boolean enable() throws Exception {
			if (!super.enable())
				return false;
		    try{
				// IAM policy
				RoleType role = createIAMRoleIfDoesNotExists();
				// instance profile
				createInstanceProfileIfDoesNotExists(role.getRoleName());
				//TODO: add security group
				createSecurityGroupIfDoesNotExists();
				// create ASG
				createASGroupIfDoesNotExist(DEFAULT_GROUP_NAME, getAvailableAZ());
		    }catch(Exception ex){
		    	LOG.error(ex.getMessage());
		    	return false;
		    }
		    return true;
		}
	}

	//TODO: update name later
	private static final String DEFAULT_ROLE_PATH_PREFIX = "/internal/imager";
	private static final String DEFAULT_ROLE_NAME = "imager-vm";
	private static final String DEFAULT_ASSUME_ROLE_POLICY =
			"{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":[\"ec2.amazonaws.com\"]},\"Action\":[\"sts:AssumeRole\"]}]}";

	public static final String DEFAULT_INSTANCE_PROFILE_PATH_PREFIX="/internal/imager";
	public static final String DEFAULT_INSTANCE_PROFILE_NAME = "imager-vm";

	public static final String TAG_KEY = "Name";
	public static final String TAG_VALUE = "imager-resources";

	private static String LAUNCH_CONFIG_BASE_NAME = String.format("euca-internal-%s", getId("imager"));
	private static String ASG_GROUP_NAME = String.format("euca-internal-%s", getId("imager"));
	private static String DEFAULT_GROUP_NAME = String.format("euca-internal-%s", getId("imager"));

	private static String getId(String in) {
		return Signatures.SHA1WithRSA.trySign( Eucalyptus.class, in.getBytes( ) ).substring(0, 8);
	}

	private static List<String> getAvailableAZ() throws EucalyptusCloudException {
		List<String> zones = new ArrayList<String>();
		try{
			List<ClusterInfoType> res = describeAvailabilityZones();
			for(ClusterInfoType r:res)
				zones.add(r.getZoneName());
		}catch(Exception ex){
			throw new EucalyptusCloudException("Can't get AZs");
		}
		return zones;
	}

	private static RoleType createIAMRoleIfDoesNotExists() throws EucalyptusCloudException {
		RoleType role = null;
		// list-roles.
		try{
			List<RoleType> result = listRoles(DEFAULT_ROLE_PATH_PREFIX);
			if(result != null){
				for(RoleType r : result){
					if(DEFAULT_ROLE_NAME.equals(r.getRoleName())){
						role = r;
						break;
					}
				}
			}
		}catch(Exception ex){
			throw new EucalyptusCloudException("Failed to list IAM roles", ex);
		}

		// if no role found, create a new role with assume-role policy for imager
		if(role==null){
			try{
				LOG.info("Creating default role for Imager");
				role = createRole(DEFAULT_ROLE_NAME, DEFAULT_ROLE_PATH_PREFIX, DEFAULT_ASSUME_ROLE_POLICY);
			}catch(Exception ex){
				throw new EucalyptusCloudException("Failed to create the role for Imager VMs");
			}
		}
		return role;
	}

	private static InstanceProfileType createInstanceProfileIfDoesNotExists(String iamRoleName) throws EucalyptusCloudException {
		InstanceProfileType instanceProfile = null;
		// list instance profiles
		try{
			// check if the instance profile for ELB VM is found
			List<InstanceProfileType> instanceProfiles = listInstanceProfiles(DEFAULT_INSTANCE_PROFILE_PATH_PREFIX);
			for(InstanceProfileType ip : instanceProfiles){
				if(DEFAULT_INSTANCE_PROFILE_NAME.equals(ip.getInstanceProfileName())){
					instanceProfile = ip;
					break;
				}
			}
		}catch(Exception ex){
			throw new EucalyptusCloudException("Failed to list instance profiles", ex);
		}
		
		// create new if needed
		if(instanceProfile == null){
			try{
				LOG.info("Creating default instance profile for Imager");
				instanceProfile = createInstanceProfile(DEFAULT_INSTANCE_PROFILE_NAME,
								DEFAULT_INSTANCE_PROFILE_PATH_PREFIX);
			}catch(Exception ex){
				throw new EucalyptusCloudException("Failed to create instance profile", ex);
			}
		}

		List<RoleType> roles = instanceProfile.getRoles().getMember();
		boolean roleFound = false;
		for(RoleType role : roles){
			if(role.getRoleName().equals(iamRoleName)){
				roleFound=true;
				break;
			}
		}
		if (!roleFound) {
			try{
				addRoleToInstanceProfile(instanceProfile.getInstanceProfileName(), iamRoleName);
			}catch(Exception ex2){
				throw new EucalyptusCloudException("Failed to add role to the instance profile", ex2);
			}
		}
		return instanceProfile;
	}

	private static void createSecurityGroupIfDoesNotExists() throws EucalyptusCloudException {
		String groupDesc = "Group for eucalyptus imager";
		// check if there's an existing group with the same name
		boolean groupFound = false;
		List<SecurityGroupItemType> groups = describeSecurityGroups(Lists.newArrayList(DEFAULT_GROUP_NAME));
		if(groups!=null && groups.size()>0){
			final SecurityGroupItemType current = groups.get(0);
			if(DEFAULT_GROUP_NAME.equals(current.getGroupName())){
				groupFound=true;
			}
		}

		// create a new security group
		if(! groupFound){
			try{
				LOG.info("Creating default security group for Imager");
				createSecurityGroup(DEFAULT_GROUP_NAME, groupDesc);
			}catch(Exception ex){
				throw new EucalyptusCloudException("Failed to create the security group for imager", ex);
			}
			// tag it
			try{
				createTags(TAG_KEY, TAG_VALUE, Lists.newArrayList(DEFAULT_GROUP_NAME));
			}catch(final Exception ex){
				LOG.warn("could not tag the security group", ex);
			}
		}
	}

	private static String createLaunchConfigName(String base, String keyName, String vmType) {
		return String.format("%s-%s-%s", base, keyName != null ? getId(keyName): "no-key", vmType);
	}

	private static void changeLaunchConfiguration(String newKeyName, String oldKeyName, String newVMType, String oldVMType) {
		// find LaunchConfiguration and ASG
		try {
			String oldLCName = createLaunchConfigName(LAUNCH_CONFIG_BASE_NAME, oldKeyName, oldVMType);
			LaunchConfigurationType launchConfig = describeLaunchConfiguration(oldLCName);
			List<AutoScalingGroupType> members =
					describeAutoScalingGroups(Lists.newArrayList(ASG_GROUP_NAME));
			int desiredCapacity = 0;
			if (launchConfig != null && members!= null) {
				for(AutoScalingGroupType type:members){
					if (LAUNCH_CONFIG_BASE_NAME.equals(type.getLaunchConfigurationName()))
						desiredCapacity = type.getDesiredCapacity();
				}
				// create new configuration
				String newLCName = createLaunchConfiguration(newKeyName, newVMType);
				// update ASG
				updateAutoScalingGroup(ASG_GROUP_NAME, getAvailableAZ(), desiredCapacity, newLCName);
				// delete old configuration
				deleteLaunchConfiguration(oldLCName);
			}
		} catch(Exception ex) {
			LOG.warn("Error updating launch configuration", ex);
		}
	}

	private static void createASGroupIfDoesNotExist(String sgroupName, List<String> availabilityZones) throws EucalyptusCloudException {
		boolean asgFound = false;
		try{
			List<AutoScalingGroupType> groups = describeAutoScalingGroups(Lists.newArrayList(ASG_GROUP_NAME));
			if(groups.size()>0 && groups.get(0).getAutoScalingGroupName().equals(ASG_GROUP_NAME)){
				asgFound = true;
			}
		}catch(Exception ex){
			asgFound = false;
		}

		// create launch config based on the parameters, one VM per AZ
		int capacity = 1;
		if(!asgFound){
			String newLCName = createLaunchConfiguration(IMAGING_VM_KEYNAME, IMAGING_INSTANCE_TYPE);
			// create auto-scaling group with zones and desired capacity
			try{
				LOG.info("Creating Imager's ASG");
				createAutoScalingGroup(ASG_GROUP_NAME, availabilityZones,
						capacity, newLCName, TAG_KEY, TAG_VALUE);
			}catch(Exception ex){
				throw new EucalyptusCloudException("Failed to create autoscaling group", ex);
			}
		}
	}

	private static String createLaunchConfiguration(String keyName, String vmType) throws EucalyptusCloudException {
		// check if launch config already exists
		String name = createLaunchConfigName(LAUNCH_CONFIG_BASE_NAME, keyName, vmType);
		if (describeLaunchConfiguration(name) != null)
			return name;
		// create user data
		InstanceUserDataBuilder userDataBuilder  = null;
		try{
			userDataBuilder = new DefaultInstanceUserDataBuilder();
		}catch(Exception ex){
			throw new EucalyptusCloudException( "Failed to create service parameters" );
		}
		String userData = B64.standard.encString(String.format("euca-%s\n%s",
			    B64.standard.encString("setup-credential"),
			    userDataBuilder.build()));
		LOG.info("Creating launch configuration " + name + " for Imager's ASG");
		createLaunchConfiguration(IMAGING_EMI, vmType, DEFAULT_INSTANCE_PROFILE_NAME,
				name, DEFAULT_GROUP_NAME, keyName, userData);
		return name;
	}

	// Helper methods
	private static List<ImageDetails> describeImages(final ArrayList<String> imageIds) throws EucalyptusCloudException {
		try {
			DescribeImagesType msg = new DescribeImagesType();
			msg.setImagesSet(imageIds);
			DescribeImagesResponseType reply = sendSyncToComonent(Eucalyptus.class, msg);
			return reply.getImagesSet();
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to describe images", e );
		}
	}

	private static List<VmTypeDetails> describeVMTypes() throws EucalyptusCloudException {
		try {
			DescribeInstanceTypesType msg = new DescribeInstanceTypesType();
			DescribeInstanceTypesResponseType reply = sendSyncToComonent(Eucalyptus.class, msg);
			return reply.getInstanceTypeDetails();
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to describe VM types", e );
		}
	}

	private static List<DescribeKeyPairsResponseItemType> describeKeyPairs(final ArrayList<String> keys) throws EucalyptusCloudException {
		try {
			DescribeKeyPairsType msg = new DescribeKeyPairsType();
			msg.setKeySet(keys);
			DescribeKeyPairsResponseType reply = sendSyncToComonent(Eucalyptus.class, msg);
			return reply.getKeySet();
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to describe keys", e );
		}
	}

	private static List<ClusterInfoType> describeAvailabilityZones() throws EucalyptusCloudException {
		try {
			DescribeAvailabilityZonesType msg = new DescribeAvailabilityZonesType();
			DescribeAvailabilityZonesResponseType reply = sendSyncToComonent(Eucalyptus.class, msg);
			return reply.getAvailabilityZoneInfo();
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to describe AZs", e );
		}
	}

	private static RoleType createRole(final String roleName, final String path, final String assumeRolePolicy) throws EucalyptusCloudException {
		try {
			CreateRoleType msg = new CreateRoleType();
			msg.setRoleName(roleName);
			msg.setPath(path);
			msg.setAssumeRolePolicyDocument(assumeRolePolicy);
			CreateRoleResponseType reply = sendSyncToComonent(Euare.class, msg);
			return reply.getCreateRoleResult().getRole();
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to create role", e );
		}
	}

	private static List<InstanceProfileType> listInstanceProfiles(final String pathPrefix) throws EucalyptusCloudException {
		try {
			ListInstanceProfilesType msg = new ListInstanceProfilesType();
			msg.setPathPrefix(pathPrefix);
			ListInstanceProfilesResponseType reply = sendSyncToComonent(Euare.class, msg);
			return reply.getListInstanceProfilesResult().getInstanceProfiles().getMember();
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to list instance profiles", e );
		}
	}

	private static InstanceProfileType createInstanceProfile(final String profileName, final String path) throws EucalyptusCloudException {
		try {
			CreateInstanceProfileType msg = new CreateInstanceProfileType();
			msg.setInstanceProfileName(profileName);
			msg.setPath(path);
			CreateInstanceProfileResponseType reply = sendSyncToComonent(Euare.class, msg);
			return reply.getCreateInstanceProfileResult().getInstanceProfile();
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to creat instance profile", e );
		}
	}

	private static void addRoleToInstanceProfile(final String instanceProfileName, final String roleName) throws EucalyptusCloudException {
		try {
			AddRoleToInstanceProfileType msg = new AddRoleToInstanceProfileType();
			msg.setRoleName(roleName);
			msg.setInstanceProfileName(instanceProfileName);
			sendSyncToComonent(Euare.class, msg);
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to add role to instance profile", e );
		}
	}

	private static void createSecurityGroup(String groupName, String groupDesc) throws EucalyptusCloudException {
		try {
			CreateSecurityGroupType msg = new CreateSecurityGroupType();
			msg.setGroupName(groupName);
			msg.setGroupDescription(groupDesc);
			sendSyncToComonent(Eucalyptus.class, msg);
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to create security group", e );
		}
	}

	private static List<SecurityGroupItemType> describeSecurityGroups(ArrayList<String> groups) throws EucalyptusCloudException {
		try {
			DescribeSecurityGroupsType msg = new DescribeSecurityGroupsType();
			msg.setSecurityGroupSet(groups);
			DescribeSecurityGroupsResponseType reply = sendSyncToComonent(Eucalyptus.class, msg);
			return reply.getSecurityGroupInfo();
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to describe security group", e );
		}
	}

	private static LaunchConfigurationType describeLaunchConfiguration(String configurationName) throws EucalyptusCloudException {
		try {
			DescribeLaunchConfigurationsType msg = new DescribeLaunchConfigurationsType();
			final LaunchConfigurationNames names = new LaunchConfigurationNames();
			names.setMember(Lists.newArrayList(configurationName));
			msg.setLaunchConfigurationNames(names);
			DescribeLaunchConfigurationsResponseType reply = sendSyncToComonent(AutoScaling.class, msg);
			if (reply.getDescribeLaunchConfigurationsResult() != null
					&& reply.getDescribeLaunchConfigurationsResult().getLaunchConfigurations().getMember().size() != 0)
				return reply.getDescribeLaunchConfigurationsResult().getLaunchConfigurations().getMember().get(0);
			else
				return null;
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to describe lunch configuration", e );
		}
	}

	private static List<AutoScalingGroupType> describeAutoScalingGroups(ArrayList<String> groupNames) throws EucalyptusCloudException {
		try {
			DescribeAutoScalingGroupsType msg = new DescribeAutoScalingGroupsType();
			AutoScalingGroupNames names = new AutoScalingGroupNames();
			names.setMember(Lists.<String>newArrayList());
			names.getMember().addAll(groupNames);
			msg.setAutoScalingGroupNames(names);
			DescribeAutoScalingGroupsResponseType reply = sendSyncToComonent(AutoScaling.class, msg);
			if (reply.getDescribeAutoScalingGroupsResult() != null
				&& reply.getDescribeAutoScalingGroupsResult().getAutoScalingGroups() != null)
				return reply.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
			else
				return null;
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to describe auto-scalling groups", e );
		}
	}

	private static void deleteLaunchConfiguration(String configurationName) throws EucalyptusCloudException {
		try {
			DeleteLaunchConfigurationType msg = new DeleteLaunchConfigurationType();
			msg.setLaunchConfigurationName(configurationName);
			sendSyncToComonent(AutoScaling.class, msg);
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to delete launch configuration", e );
		}
	}

	private static void createLaunchConfiguration(String imageId, String instanceType, String instanceProfileName,
			String launchConfigName, String securityGroup, String keyName, String userData) throws EucalyptusCloudException {
		try {
			CreateLaunchConfigurationType msg = new CreateLaunchConfigurationType();
			msg.setImageId(imageId);
			msg.setInstanceType(instanceType);
			if(instanceProfileName!=null)
				msg.setIamInstanceProfile(instanceProfileName);
			if(keyName!=null)
				msg.setKeyName(keyName);
			msg.setLaunchConfigurationName(launchConfigName);
			SecurityGroups groups = new SecurityGroups(Lists.<String>newArrayList(securityGroup));
			msg.setSecurityGroups(groups);
			msg.setUserData(userData);
			sendSyncToComonent(AutoScaling.class, msg);
		} catch (Exception e) {
			LOG.warn(String.format("Failed to create launch configuration with image id:%s, type:%s, profile name:%s,"
					+ "launch config name:%s, security group:%s, key name:%s, user data: %s", imageId, instanceType,
					instanceProfileName, launchConfigName, securityGroup, keyName, userData));
			throw new EucalyptusCloudException( "Failed to create launch configuration due to " + e.getMessage() );
		}
	}

	private static void updateAutoScalingGroup(String groupName, List<String> zones, int capacity, String launchConfigName)
			throws EucalyptusCloudException {
		try {
			UpdateAutoScalingGroupType msg = new UpdateAutoScalingGroupType();
			msg.setAutoScalingGroupName(groupName);
			if(zones!=null && zones.size()>0)
				msg.setAvailabilityZones(new AvailabilityZones(zones));
			if(capacity!=0){
				msg.setDesiredCapacity(capacity);
				msg.setMaxSize(capacity);
				msg.setMinSize(capacity);
			}
			msg.setLaunchConfigurationName(launchConfigName);
			sendSyncToComonent(AutoScaling.class, msg);
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to update auto scaling group", e );
		}
	}

	private static void createAutoScalingGroup(String groupName, List<String> availabilityZones,
			int capacity, String launchConfigName, String tagKey, String tagValue) throws EucalyptusCloudException {
		try {
			CreateAutoScalingGroupType msg = new CreateAutoScalingGroupType();
			msg.setAutoScalingGroupName(groupName);
			AvailabilityZones zones = new AvailabilityZones(availabilityZones);
			msg.setAvailabilityZones(zones);
			msg.setDesiredCapacity(capacity);
			msg.setMaxSize(capacity);
			msg.setMinSize(capacity);
			msg.setHealthCheckType("EC2");
			msg.setLaunchConfigurationName(launchConfigName);
			final Tags tags = new Tags();
			final TagType tag = new TagType();
			tag.setKey(tagKey);
			tag.setValue(tagValue);
			tag.setPropagateAtLaunch(true);
			tag.setResourceType("auto-scaling-group");
			tag.setResourceId(groupName);
			tags.setMember(Lists.newArrayList(tag));
			msg.setTags(tags);
			sendSyncToComonent(AutoScaling.class, msg);
		} catch (Exception e) {
			LOG.warn(String.format("Failed to create auto scalling group name:%s, capacity:%d, launch config name:%s,"
					+ "tagKey:%s, tagValue:%s", groupName, capacity, launchConfigName, tagKey, tagValue));
			throw new EucalyptusCloudException( "Failed to create auto scalling group", e );
		}
	}

	private static List<RoleType> listRoles(String pathPrefix) throws EucalyptusCloudException {
		try {
			ListRolesType msg = new ListRolesType();
			msg.setPathPrefix(pathPrefix);
			ListRolesResponseType reply = sendSyncToComonent(Euare.class, msg);
			return reply.getListRolesResult().getRoles().getMember();
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to list roles", e );
		}
	}

	private static void createTags(String tagKey, String tagValue, List<String> resources) throws EucalyptusCloudException {
		try {
			CreateTagsType msg = new CreateTagsType();
			msg.setResourcesSet(Lists.newArrayList(resources));
			ResourceTag tag = new ResourceTag();
			tag.setKey(tagKey);
			tag.setValue(tagValue);
			msg.setTagSet(Lists.newArrayList(tag));
			sendSyncToComonent(Eucalyptus.class, msg);
		} catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to create tag", e );
		}
	}

	private static <A extends BaseMessage, B extends BaseMessage> B 
		sendSyncToComonent(Class<? extends ComponentId> compClass, A msg) throws Exception {
	    msg.setEffectiveUserId(Accounts.lookupSystemAdmin().getUserId());
	    return AsyncRequests.sendSync(Topology.lookup(compClass), msg);
	}

	private static class DefaultInstanceUserDataBuilder implements InstanceUserDataBuilder {
		ConcurrentHashMap<String,String> dataDict = null;
		protected DefaultInstanceUserDataBuilder(){
			dataDict = new ConcurrentHashMap<String,String>();
			try{
				URI serviceUrl = Topology.lookup( Eucalyptus.class ).getUri();
				this.add("eucalyptus_host", serviceUrl.getHost());
				this.add("eucalyptus_port", Integer.toString(serviceUrl.getPort()));
				this.add("eucalyptus_path", serviceUrl.getPath());
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