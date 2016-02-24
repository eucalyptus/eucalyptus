/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.simpleworkflow.common.model.WorkflowEventAttributes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Strings;
import com.google.common.base.Objects;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_simpleworkflow" )
@Table( name = "swf_workflow_history_event", indexes = {
    @Index( name = "swf_workflow_history_event_execution_id_idx", columnList = "workflow_execution_id" )
} )
public class WorkflowHistoryEvent extends AbstractPersistent {
  private static final long serialVersionUID = 1L;

  @ManyToOne
  @JoinColumn( name = "workflow_execution_id", nullable = false, updatable = false )
  private WorkflowExecution workflowExecution;

  @Column( name = "event_order", nullable = false, updatable = false )
  private Long eventOrder;

  @Column( name = "event_type", nullable = false, updatable = false )
  private String eventType;

  @Column( name = "event_attributes", nullable = false, updatable = false )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  private String eventAttributes;

  protected WorkflowHistoryEvent( ) {
  }

  public static WorkflowHistoryEvent create( final WorkflowExecution execution,
                                             final WorkflowEventAttributes attributes ) {
    final WorkflowHistoryEvent workflowHistoryEvent = new WorkflowHistoryEvent( );
    workflowHistoryEvent.setWorkflowExecution( execution );
    workflowHistoryEvent.setEventType( Strings.trimSuffix( "EventAttributes", attributes.getClass( ).getSimpleName( ) ) ); //TODO:STEVE: add unit test to enforce this convention
    workflowHistoryEvent.setEventAttributes( SwfJsonUtils.writeObjectAsString( attributes ) );
    workflowHistoryEvent.updateTimeStamps( );
    return workflowHistoryEvent;
  }

  @SuppressWarnings( "unchecked" )
  public WorkflowEventAttributes toAttributes( ) {
    try {
      return SwfJsonUtils.readObject(
          getEventAttributes(),
          (Class<? extends WorkflowEventAttributes>)
              Class.forName( WorkflowEventAttributes.class.getPackage( ).getName( ) +
                  "." + getEventType( ) + "EventAttributes" ) );
    } catch ( Exception e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  public Long getEventId( ) {
    return Objects.firstNonNull( getEventOrder( ), 0L ) + 1L;
  }

  public WorkflowExecution getWorkflowExecution( ) {
    return workflowExecution;
  }

  public void setWorkflowExecution( final WorkflowExecution workflowExecution ) {
    this.workflowExecution = workflowExecution;
  }

  public Long getEventOrder( ) {
    return eventOrder;
  }

  public void setEventOrder( final Long eventOrder ) {
    this.eventOrder = eventOrder;
  }

  public String getEventType( ) {
    return eventType;
  }

  public void setEventType( final String eventType ) {
    this.eventType = eventType;
  }

  public String getEventAttributes( ) {
    return eventAttributes;
  }

  public void setEventAttributes( final String eventAttributes ) {
    this.eventAttributes = eventAttributes;
  }
}
