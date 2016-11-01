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

package com.eucalyptus.simplequeue.workflow;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;

import java.util.Collection;
import java.util.Date;

/**
 * Created by ethomas on 10/25/16.
 */
@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 400,
  defaultTaskStartToCloseTimeoutSeconds = 300)
@Activities(version="2.0")
public interface CloudWatchActivities {
  public Collection<String> getPartitions();
  public void performWork(String partitionInfo);
}
