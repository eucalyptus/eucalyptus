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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import net.sf.json.JSONSerializer;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.imaging.common.ImagingMetadata;
import com.eucalyptus.imaging.ImportTaskProperties;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;

/**
 * @author Sang-Min Park
 *
 * ImagingTask is the abstract class where task id (display_name), task state, 
 * and task description in JSON are managed.
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Table( name = "metadata_imaging_tasks" )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "metadata_imaging_tasks_discriminator",
                      discriminatorType = DiscriminatorType.STRING )
@DiscriminatorValue( value = "metadata_imaging_task" )
public class ImagingTask extends UserMetadata<ImportTaskState> 
  implements ImagingMetadata.ImagingTaskMetadata, IConversionTask {
  private static Logger LOG  = Logger.getLogger( ImagingTask.class );

  @Type(type="text")
  // so there is not need to worry about length of the JSON
  @Column( name = "metadata_task_in_json" )
  protected String         taskInJSON;
  
  @Column( name = "metadata_state_message")
  private String  stateReason;
  
  @Column( name = "metadata_worker_id")
  private String workerId;
  
  @Column ( name = "metadata_worker_timeout")
  private Date workerTimeOut  = null;
  
  @Column ( name = "metadata_timeout_counter")
  private Integer timeOutCounter = null;
  
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

  static ImagingTask named(AccountFullName acctOwner, final String taskId){
    final ImagingTask task = new ImagingTask(null, taskId);
    task.setOwnerAccountNumber(acctOwner.getAccountNumber());
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
  
  public boolean cleanUp() {
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
    
    Calendar cal = Calendar.getInstance(); // creates calendar
    cal.setTime(new Date()); // sets calendar time/date
    cal.add(Calendar.MINUTE, Integer.parseInt(ImportTaskProperties.IMPORT_TASK_TIMEOUT_MINUTES)); // adds one hour
    this.workerTimeOut = cal.getTime(); //
  }
  
  public String getWorkerId(){
    return this.workerId;
  }
  
  public boolean isTimedOut(){
    if(this.workerTimeOut!=null){
      return this.workerTimeOut.before(new Date());
    }
    return false;
  }
  
  public void resetTimeout(){
    this.workerTimeOut = null;
  }
  
  public void incrementTimeout(){
    if(this.timeOutCounter == null)
      this.timeOutCounter = 0;
    this.timeOutCounter++;
  }
  
  public int getTimeoutCount(){
    if(this.timeOutCounter==null)
      this.timeOutCounter = 0;
    return this.timeOutCounter;
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
