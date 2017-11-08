/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.imaging.backend;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import com.eucalyptus.entities.AbstractStatefulPersistent;

/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Table( name = "metadata_imaging_workers" )
public class ImagingWorker extends AbstractStatefulPersistent<ImagingWorker.STATE> {
  enum STATE {
    RUNNING, RETIRING, DECOMMISSIONED,  
  }
  private ImagingWorker() {
    super(null, null);
  }
  
  private ImagingWorker(final String instanceId){
    super(null, instanceId);
  }
  
  public ImagingWorker(final STATE state, final String instanceId){
    super(state, instanceId);
  }
  
  public static ImagingWorker named(){
    return new ImagingWorker();
  }
  
  public static ImagingWorker named(final String workerId){
    return new ImagingWorker(workerId);
  }
  
  @Column ( name = "metadata_conversion_update_time")
  private Date lastUpdateTime; 
  
  @Column ( name = "metadata_availability_zone")
  private String availabilityZone;
  
  public void setWorkerUpdateTime(){
    this.lastUpdateTime =  new Date();
  }
  
  public Date getWorkerUpdateTime(){
    return this.lastUpdateTime;
  }
  
  public void setAvailabilityZone(final String zone){
    this.availabilityZone = zone;
  }
  
  public String getAvailabilityZone(){
    return this.availabilityZone;
  }
}
