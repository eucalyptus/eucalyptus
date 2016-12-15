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
package com.eucalyptus.simplequeue.async;

import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.simpleworkflow.common.stateful.NotifyClientUtils;
import com.eucalyptus.util.Consumer;
import org.apache.log4j.Logger;

/**
 *
 */
public class NotifyClient {
  private static class QueueChannelWrapper implements NotifyClientUtils.ChannelWrapper {
    Queue queue;

    private QueueChannelWrapper(Queue queue) {
      this.queue = queue;
    }

    @Override
    public String getChannelName() {
      return queue.getArn();
    }
  }
  private static final Logger logger = Logger.getLogger(NotifyClient.class);

  public static void notifyQueue(final Queue queue) {
    NotifyClientUtils.notifyChannel(new QueueChannelWrapper(queue));
  }

  public static void pollQueue(final Queue queue,
                                  final long timeout,
                                  final Consumer<Boolean> resultConsumer) throws Exception {
    NotifyClientUtils.pollChannel(new QueueChannelWrapper(queue), timeout, resultConsumer);
  }
}
