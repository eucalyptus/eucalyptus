/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.images;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.groovy.JsonSlurper;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.entities.AbstractPersistent;

import edu.ucsb.eucalyptus.msgs.ConversionTask;
/**
 * Entity base-class for the persisted state associated with the progress in execution of an image
 * conversion task.
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Table( name = "imaging_tasks" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ImagingTask implements Serializable {
	@Transient // save task in JSON
	private ConversionTask task;
	@Column( name = "state" )
	private final ImportTaskState state;
	@Column( name = "bytes_processed" )
	private final Long bytesProcessed;
	@Column( name = "update_time" )
	private final Long updateTime;
	@Id @Column(name="task_id", nullable=false)
	private String id;
	@Type(type="org.hibernate.type.StringClobType")
	@Lob // so there is not need to worry about length of the JSON
	@Column( name = "task_in_json")
	private String taskInJSON;
	
	public ImagingTask() {
		id = null;
		task = null;
		state = null;
		bytesProcessed = null;
		updateTime = null;
	}
	
	public ImagingTask(ConversionTask task, ImportTaskState state, long bytesProcessed) {
		this.id = task.getConversionTaskId();
		this.task = task;
		this.state = state;
		updateTime = System.currentTimeMillis();
		this.bytesProcessed = bytesProcessed;
	}
	
	public ConversionTask getTask() {
		return task;
	}
	
	public ImportTaskState getState() {
		return state;
	}
	
	public long getUpdateTime() {
		return updateTime;
	}
	
	public long getBytesProcessed() {
		return bytesProcessed;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		if (id == null)
			throw new IllegalArgumentException("id cant be null");
		this.id = id;
		this.task.setConversionTaskId(id);
	}
	
	/*package*/ void serializeTaskToJSON() {
		taskInJSON = (JSONSerializer.toJSON(task.toJSON())).toString();
	}
	
	public void createTaskFromJSON() {
		// recreate task from JSON string
		JsonSlurper jsonSlurper = new JsonSlurper();
		JSONObject taskObject = (JSONObject)jsonSlurper.parseText(this.taskInJSON);
		this.task = new ConversionTask(taskObject);
	}

	public String getTaskInJons() {
		return taskInJSON;
	}
}
