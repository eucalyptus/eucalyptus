/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.simpleworkflow;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
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
  @Type(type="text")
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
