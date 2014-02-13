/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.imaging;

import javax.persistence.*;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.groovy.JsonSlurper;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import edu.ucsb.eucalyptus.msgs.ConversionTask;

/**
 * Entity base-class for the persisted state associated with the progress in execution of an image
 * conversion task.
 * 
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Table( name = "metadata_imaging_tasks" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "metadata_imaging_tasks_discriminator",
                      discriminatorType = DiscriminatorType.STRING )
@DiscriminatorValue( value = "metadata_imaging_task" )
public class ImagingTask extends UserMetadata<ImportTaskState> implements ImagingMetadata.ImagingTaskMetadata {
  @Transient
  // save task in JSON
  private ConversionTask task;
  @Column( name = "bytes_processed" )
  private final Long     bytesProcessed;
  @Type( type = "org.hibernate.type.StringClobType" )
  @Lob
  // so there is not need to worry about length of the JSON
  @Column( name = "task_in_json" )
  private String         taskInJSON;

  protected ImagingTask( ) {
    this(null,null);
    task = null;
  }
  
  ImagingTask( String displayName ) {
    this( null, displayName );
  }
  
  ImagingTask( OwnerFullName owner, String displayName ) {
    super( owner, displayName );
    this.bytesProcessed = null;
  }
  
  static ImagingTask named(final OwnerFullName owner){
    return new ImagingTask(owner, null);
  }
  
  static ImagingTask named(){
    return new ImagingTask();
  }
  
  
  protected ImagingTask( OwnerFullName ownerFullName, String volumeTaskId, ConversionTask task, ImportTaskState state, long bytesProcessed ) {
    super( ownerFullName, volumeTaskId );
    this.task = task;
    this.setState( state );
    this.bytesProcessed = bytesProcessed;
  }
  
  public ConversionTask getTask( ) {
    return task;
  }
  
  public long getBytesProcessed( ) {
    return bytesProcessed;
  }
  
  @PrePersist
  /* package */void serializeTaskToJSON( ) {
    if ( this.task != null ) {
      taskInJSON = ( JSONSerializer.toJSON( task.toJSON( ) ) ).toString( );
    }
  }
  
  @PostLoad
  public void createTaskFromJSON( ) {
    // recreate task from JSON string
    if ( this.taskInJSON != null ) {
      JsonSlurper jsonSlurper = new JsonSlurper( );
      JSONObject taskObject = ( JSONObject ) jsonSlurper.parseText( this.taskInJSON );
      this.task = new ConversionTask( taskObject );
    }
  }
  
  public String getTaskInJons( ) {
    return taskInJSON;
  }
  
  @Override
  public String getPartition( ) {
    return null;
  }
  
  @Override
  public FullName getFullName( ) {
    return null;
  }

}
