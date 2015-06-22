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
package com.eucalyptus.resources;

import java.util.List;

import org.springframework.util.StringUtils;

import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.VmTypeDetails;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.resources.client.EucalyptusClient;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.net.HostSpecifier;

public class PropertyChangeListeners {
  public static class EmiChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      if(t.getValue()!=null && t.getValue().equals(newValue))
        return;
      if (newValue == null || !CloudMetadatas.isMachineImageIdentifier(newValue))
        throw new ConfigurablePropertyException("Invalid EMI ID");
      try {
          final List<ImageDetails> images = Ec2Client.getInstance()
              .describeImages(null, Lists.newArrayList(newValue));
          if (images == null || images.size() <= 0)
            throw new Exception(
                "No such EMI is found in the system");
          if (!images.get(0).getImageId().equalsIgnoreCase(newValue))
            throw new Exception(
                "No such EMI is found in the system");
      } catch (final Exception e) {
        throw new ConfigurablePropertyException("Could not change EMI ID to " +
            newValue + " due to: " + e.getMessage());
      }
    }
  }

  public static class InstanceTypeChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      if(t.getValue()!=null && t.getValue().equals(newValue))
        return;
      try {
        if (newValue == null || newValue.isEmpty())
            throw new EucalyptusCloudException("Instance type cannot be unset");
        List<VmTypeDetails> types = EucalyptusClient.getInstance().describeVMTypes();
        boolean found = false;
        for(VmTypeDetails type:types){
          if (type.getName().equals(newValue)) {
            found = true;
            break;
          }
        }
        if (!found)
          throw new ConfigurablePropertyException("Invalid instance type");
      } catch (final Exception e) {
        throw new ConfigurablePropertyException("Could not change instance type to "
              + newValue + " due to: " + e.getMessage());
      }
    }
  }

  public static class PositiveNumberChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      if(t.getValue()!=null && t.getValue().equals(newValue))
        return;
      try {
        final int newExp = Integer.parseInt(newValue);
        if (newExp <= 0)
          throw new Exception();
      } catch (final Exception ex) {
        throw new ConfigurablePropertyException(
            "The value must be number type and bigger than 0");
      }
    }
  }

  public static class NTPServerChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      if(newValue.contains(",")) {
        final String[] addresses = newValue.split(",");
        if((addresses.length-1) != StringUtils.countOccurrencesOf(newValue, ","))
          throw new ConfigurablePropertyException("Invalid address");

        for(final String address : addresses){
          if(!HostSpecifier.isValid(String.format("%s.com",address)))
            throw new ConfigurablePropertyException("Invalid address");
        }
      } else {
        final String address = newValue;
        if(address != null && ! address.equals("")){
          if(!HostSpecifier.isValid(String.format("%s.com", address)))
            throw new ConfigurablePropertyException("Invalid address");
        }
      }
    }
  }

  public static class AvailabilityZonesChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, final String zones)
        throws ConfigurablePropertyException {
      if(t.getValue()!=null && t.getValue().equals(zones))
        return;
      if (zones == null || zones.length() == 0) {
        return;
      }

      final List<String> availabilityZones = Lists.newArrayList();
      if (zones.contains(",")) {
        final String[] tokens = zones.split(",");
        if ((tokens.length - 1) != StringUtils.countOccurrencesOf(zones, ","))
          throw new ConfigurablePropertyException("Invalid availability zones");
        for (final String zone : tokens)
          availabilityZones.add(zone);
      } else {
        availabilityZones.add(zones);
      }
      final List<String> clusterNames = Lists.newArrayList();
      try {
        final List<ClusterInfoType> clusters = Ec2Client
            .getInstance().describeAvailabilityZones(null, false);
        for (final ClusterInfoType cluster : clusters) {
          clusterNames.add(cluster.getZoneName());
        }
      } catch (final Exception ex) {
        throw new ConfigurablePropertyException(
            "Faield to check availability zones", ex);
      }
      for (final String zone : availabilityZones) {
        if (!clusterNames.contains(zone))
          throw new ConfigurablePropertyException(zone
              + " is not found in availability zones");
      }
    }
  }
}
