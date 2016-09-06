/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.simplequeue

import com.eucalyptus.config.DeregisterComponentResponseType
import com.eucalyptus.config.DeregisterComponentType
import com.eucalyptus.config.DescribeComponentsResponseType
import com.eucalyptus.config.DescribeComponentsType
import com.eucalyptus.config.ModifyComponentAttributeResponseType
import com.eucalyptus.config.ModifyComponentAttributeType
import com.eucalyptus.config.RegisterComponentResponseType
import com.eucalyptus.config.RegisterComponentType
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

public class RegisterSimpleQueueType extends RegisterComponentType {}

public class RegisterSimpleQueueResponseType extends RegisterComponentResponseType {}

public class DeregisterSimpleQueueType extends DeregisterComponentType {}

public class DeregisterSimpleQueueResponseType extends DeregisterComponentResponseType {}

public class ModifySimpleQueueAttributeType extends ModifyComponentAttributeType{}

public class ModifySimpleQueueAttributeResponseType extends ModifyComponentAttributeResponseType {}

public class DescribeSimpleQueueType extends DescribeComponentsType {}

public class DescribeSimpleQueueResponseType extends DescribeComponentsResponseType {}
