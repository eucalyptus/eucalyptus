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

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.simplequeue.SimpleQueue;
import com.eucalyptus.simplequeue.config.SimpleQueueConfiguration;
import com.eucalyptus.ws.server.SoapPipeline;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
@ComponentPart( SimpleQueue.class )
public class SimpleQueueSoapPipeline extends SoapPipeline {

  public SimpleQueueSoapPipeline() {
    super(
      "cloudwatch-soap",
      SimpleQueue.class,
      SimpleQueueConfiguration.SERVICE_PATH,
      SimpleQueueQueryBinding.SIMPLEQUEUE_DEFAULT_NAMESPACE,
      "http://queue.amazonaws.com/doc/\\d\\d\\d\\d-\\d\\d-\\d\\d/");
  }
}
