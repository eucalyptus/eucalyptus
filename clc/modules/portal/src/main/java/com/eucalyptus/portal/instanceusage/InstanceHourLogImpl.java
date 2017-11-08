/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.portal.instanceusage;

import com.eucalyptus.portal.workflow.InstanceLog;
import com.eucalyptus.portal.workflow.InstanceTag;

import java.util.Date;
import java.util.List;

public class InstanceHourLogImpl implements InstanceHourLog {
  private InstanceLog log = null;
  private Long hours = 0L;
  public InstanceHourLogImpl(final InstanceLog log, final long hours) {
    this.log = log;
    this.hours = hours;
  }

  @Override
  public String getInstanceId() {
    return log.getInstanceId();
  }

  @Override
  public String getInstanceType() {
    return log.getInstanceType();
  }

  @Override
  public String getPlatform() {
    return log.getPlatform();
  }

  @Override
  public String getRegion() {
    return log.getRegion();
  }

  @Override
  public String getAvailabilityZone() {
    return log.getAvailabilityZone();
  }

  @Override
  public List<InstanceTag> getTags() {
    return log.getTags();
  }

  @Override
  public Date getLogTime() {
    return log.getLogTime();
  }

  @Override
  public void setLogTime(final Date date) {
    log.setLogTime(date);
  }

  @Override
  public Long getHours() {
    return this.hours;
  }

  @Override
  public void setHours(long hours) {
    this.hours = hours;
  }

}
