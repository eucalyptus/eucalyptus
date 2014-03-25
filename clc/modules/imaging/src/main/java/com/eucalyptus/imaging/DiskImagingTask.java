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

import javax.annotation.Nullable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.imaging.worker.ImagingServiceProperties;
import com.eucalyptus.util.Dates;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "disk-imaging-task" )
public class DiskImagingTask extends ImagingTask {
  private static Logger LOG  = Logger.getLogger( DiskImagingTask.class );
  
  @Transient
  private DiskImageConversionTask task;
  
  protected DiskImagingTask(){}
  private DiskImagingTask( final OwnerFullName ownerFullName, final DiskImageConversionTask task, 
      final ImportTaskState state){
    super(ownerFullName, task.getConversionTaskId());
    this.setState(state);
    this.task = task;
  }
  
  private DiskImagingTask(final OwnerFullName ownerFullName) {
    super(ownerFullName, null);
  }
  
  public static DiskImagingTask named(final OwnerFullName ownerFullName) {
    final DiskImagingTask task = new DiskImagingTask(ownerFullName);
    return task;
  }
  
  @Override
  protected void createTaskFromJSON() {
    if ( this.taskInJSON != null ) {
      JsonSlurper jsonSlurper = new JsonSlurper( );
      JSONObject taskObject = ( JSONObject ) jsonSlurper.parseText( this.taskInJSON );
      this.task = new DiskImageConversionTask( taskObject );
    }    
  }
  @Override
  public String getTaskExpirationTime() {
    return this.task.getExpirationTime();
  }

  @Override
  public JSONObject toJSON() {
    return this.task.toJSON();
  }

  @Override
  public void setTaskState(String state) {
    this.task.setState(state);    
  }

  @Override
  public String getTaskState() {
    return this.task.getState();
  }

  @Override
  public void setTaskStatusMessage(String msg){
    this.task.setStatusMessage(msg);
  }
  
  @Override
  @PostLoad
  protected void onLoad(){
    super.onLoad();
  }
  
  public DiskImageConversionTask getTask(){
    return this.task;
  }
  
  @TypeMapper
  enum DiskImagingTaskTransform implements Function<ImportImageType, DiskImagingTask> {
    INSTANCE;

    @Nullable
    @Override
    public DiskImagingTask apply(ImportImageType input) {
      final DiskImageConversionTask ct = new DiskImageConversionTask(); 

      String conversionTaskId = ResourceIdentifiers.generateString("import-is");
      conversionTaskId = conversionTaskId.toLowerCase();
      ct.setConversionTaskId( conversionTaskId );
      ct.setExpirationTime( new Date( Dates.hoursFromNow( 
          Integer.parseInt(ImagingServiceProperties.IMPORT_TASK_EXPIRATION_HOURS) ).getTime( ) ).toString( ) );
      ct.setState( ImportTaskState.NEW.getExternalTaskStateName() );
      ct.setStatusMessage( "" );

      final ImportDiskImage image = input.getImage();
      ct.setImportDisk(image);

      final DiskImagingTask newTask = 
          new DiskImagingTask(Contexts.lookup().getUserFullName(), ct, ImportTaskState.NEW);
      newTask.serializeTaskToJSON();
      return newTask;
    }
  }
}
