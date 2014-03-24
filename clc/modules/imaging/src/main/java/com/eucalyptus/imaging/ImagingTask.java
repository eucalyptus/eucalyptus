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

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import net.sf.json.JSONSerializer;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;

/**
 * @author Sang-Min Park
 *
 * ImagingTask is the abstract class where task id (display_name), task state, 
 * and task description in JSON are managed.
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Table( name = "metadata_imaging_tasks" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "metadata_imaging_tasks_discriminator",
                      discriminatorType = DiscriminatorType.STRING )
@DiscriminatorValue( value = "metadata_imaging_task" )
public class ImagingTask extends UserMetadata<ImportTaskState> 
  implements ImagingMetadata.ImagingTaskMetadata, IConversionTask {
  private static Logger LOG  = Logger.getLogger( ImagingTask.class );

  @Type( type = "org.hibernate.type.StringClobType" )
  @Lob
  // so there is not need to worry about length of the JSON
  @Column( name = "metadata_task_in_json" )
  protected String         taskInJSON;
  
  @Column( name = "metadata_state_message")
  private String  stateReason;
  
  @Column( name = "metadata_worker_id")
  private String workerId;
  
  protected ImagingTask( ) {
    this(null,null);
  }
  
  ImagingTask( String displayName ) {
    this( null, displayName );
  }
  
  ImagingTask( OwnerFullName owner, String displayName ) {
    super( owner, displayName );
  }
  static ImagingTask named(final ImportTaskState state) {
    final ImagingTask task = new ImagingTask(null,null);
    task.setState(state);
    return task;
  }

  static ImagingTask named(OwnerFullName owner, final String taskId){
    return new ImagingTask(owner, taskId);
  }
  
  static ImagingTask named(final String taskId){
    return new ImagingTask(null, taskId);
  }
  
  static ImagingTask named(){
    return new ImagingTask();
  }

  public void setStateReason(final String reason){
    this.stateReason = reason;
  }
  
  public String getStateReason(){
    return this.stateReason;
  }
  
  public void cleanUp() {
    throw new UnsupportedOperationException();
  }
  
  @PrePersist
  protected void serializeTaskToJSON( ) {
    taskInJSON = ( JSONSerializer.toJSON( this.toJSON( ) ) ).toString( );
  }
  
  protected void createTaskFromJSON( ){
    throw new UnsupportedOperationException();
  }
  
  public String getTaskInJsons( ) {
    return taskInJSON;
  }
  
  public void setTaskInJsons(final String taskInJson){
    this.taskInJSON= taskInJson;
  }
  
  public Date getExpirationTime(){
    try{
      return (new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")).parse(this.getTaskExpirationTime());
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }
  
  public void setWorkerId(final String workerId){
    this.workerId = workerId;
  }
  
  public String getWorkerId(){
    return this.workerId;
  }
  
  @Override
  public String toString() {
    return getClass().getName() + "[name: " + displayName + ", state: " + getState() + "]";
  }
  
  @Override
  public String getPartition( ) {
    return null;
  }
  
  @Override
  public FullName getFullName( ) {
    return null;
  }
  
  protected void onLoad(){
    createTaskFromJSON();
  }

  @Override
  public String getTaskExpirationTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public JSONObject toJSON() {
    throw new UnsupportedOperationException();  }

  @Override
  public void setTaskState(String state) {
    throw new UnsupportedOperationException();    
  }

  @Override
  public String getTaskState() {
    throw new UnsupportedOperationException();  
  }
  
  @Override
  public void setTaskStatusMessage(String msg){
    throw new UnsupportedOperationException();  
  }
}
