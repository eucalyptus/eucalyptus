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
package com.eucalyptus.simplequeue.ws;

import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import org.apache.log4j.Logger;

public class SimpleQueueQueryBinding extends BaseQueryBinding<OperationParameter> {
  private static final Logger LOG = Logger.getLogger(SimpleQueueQueryBinding.class);
  static final String SIMPLEQUEUE_NAMESPACE_PATTERN = "http://queue.amazonaws.com/doc/2012-11-05/";  //TODO:GEN2OOLS: replace version with pattern : %s
  static final String SIMPLEQUEUE_DEFAULT_VERSION = "2012-11-05";              //TODO:GEN2OOLS: replace with correct default API version
  static final String SIMPLEQUEUE_DEFAULT_NAMESPACE = String.format(SIMPLEQUEUE_NAMESPACE_PATTERN, SIMPLEQUEUE_DEFAULT_VERSION);

  public SimpleQueueQueryBinding() {
    super(SIMPLEQUEUE_NAMESPACE_PATTERN, SIMPLEQUEUE_DEFAULT_VERSION, OperationParameter.Action, OperationParameter.Operation );
  }

}
