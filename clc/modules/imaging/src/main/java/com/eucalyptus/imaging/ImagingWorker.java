/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.imaging;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.entities.AbstractStatefulPersistent;

/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Table( name = "metadata_imaging_workers" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ImagingWorker extends AbstractStatefulPersistent<ImagingWorker.STATE> {
  enum STATE {
    RUNNING, DECOMMISSIONED,  
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
  
  public void setWorkerUpdateTime(){
    this.lastUpdateTime =  new Date();
  }
  
  public Date getWorkerUpdateTime(){
    return this.lastUpdateTime;
  }
}
