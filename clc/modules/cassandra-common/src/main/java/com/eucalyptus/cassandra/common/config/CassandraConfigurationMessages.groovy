/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
@GroovyAddClassUUID
package com.eucalyptus.cassandra.common.config

import com.eucalyptus.config.DeregisterComponentResponseType;
import com.eucalyptus.config.DeregisterComponentType;
import com.eucalyptus.config.DescribeComponentsResponseType;
import com.eucalyptus.config.DescribeComponentsType;
import com.eucalyptus.config.ModifyComponentAttributeResponseType;
import com.eucalyptus.config.ModifyComponentAttributeType;
import com.eucalyptus.config.RegisterComponentResponseType;
import com.eucalyptus.config.RegisterComponentType
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

public class RegisterCassandraType extends RegisterComponentType { }
public class RegisterCassandraResponseType extends RegisterComponentResponseType { }
public class DeregisterCassandraType extends DeregisterComponentType { }
public class DeregisterCassandraResponseType extends DeregisterComponentResponseType { }
public class ModifyCassandraAttributeType extends ModifyComponentAttributeType { }
public class ModifyCassandraAttributeResponseType extends ModifyComponentAttributeResponseType { }
public class DescribeCassandraType extends DescribeComponentsType { }
public class DescribeCassandraResponseType extends DescribeComponentsResponseType { }
