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

package com.eucalyptus.vm;

import com.eucalyptus.util.ByteArray;
import com.google.common.base.Function;

import java.util.List;

import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;

public class SensorsConfigMetadata implements
	Function<MetadataRequest, ByteArray> {

    private static Logger LOG = Logger.getLogger(SensorsConfigMetadata.class);

    private static String getSensorConfig() {

	List<ConfigurableProperty> props = null;

	try {
	    props = PropertyDirectory.getPropertyEntrySet("reporting");

	} catch (Exception ex) {
	    LOG.error("Unable to collect describe sensors configurations", ex);
	}

	StringBuffer configurations = new StringBuffer(props.size());

	for (ConfigurableProperty prop : props) {
	    configurations.append(prop.getFieldName() + " " + prop.getValue() + "\n");
	}

	return configurations.toString();
    }

    @Override
    public ByteArray apply(MetadataRequest arg0) {
	return ByteArray.newInstance(getSensorConfig());
    }
}
