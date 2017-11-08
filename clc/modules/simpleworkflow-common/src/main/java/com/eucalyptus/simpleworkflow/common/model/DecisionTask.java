/*************************************************************************
 * Copyright 2014 Ent. Services Development Corporation LP
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2015 Amazon.com, Inc. or its affiliates.
 *   All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is
 *   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 *   ANY KIND, either express or implied. See the License for the specific
 *   language governing permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simpleworkflow.common.model;

import java.io.Serializable;

/**
 * <p>
 * A structure that represents a decision task. Decision tasks are sent
 * to deciders in order for them to make decisions.
 * </p>
 */
public class DecisionTask extends SimpleWorkflowMessage implements Serializable {
    /**
     * The opaque string used as a handle on the task. This token is used by
     * workers to communicate progress and response information back to the
     * system about the task.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 1024<br/>
     */
    private String taskToken;

    /**
     * The ID of the <code>DecisionTaskStarted</code> event recorded in the
     * history.
     */
    private Long startedEventId;

    /**
     * The workflow execution for which this decision task was created.
     */
    private WorkflowExecution workflowExecution;

    /**
     * The type of the workflow execution for which this decision task was
     * created.
     */
    private WorkflowType workflowType;

    /**
     * A paginated list of history events of the workflow execution. The
     * decider uses this during the processing of the decision task.
     */
    private com.amazonaws.internal.ListWithAutoConstructFlag<HistoryEvent> events;

    /**
     * If a <code>NextPageToken</code> was returned by a previous call, there
     * are more results available. To retrieve the next page of results, make
     * the call again using the returned token in <code>nextPageToken</code>.
     * Keep all other arguments unchanged. <p>The configured
     * <code>maximumPageSize</code> determines how many results can be
     * returned in a single call.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     */
    private String nextPageToken;

    /**
     * The ID of the DecisionTaskStarted event of the previous decision task
     * of this workflow execution that was processed by the decider. This can
     * be used to determine the events in the history new since the last
     * decision task received by the decider.
     */
    private Long previousStartedEventId;

    /**
     * The opaque string used as a handle on the task. This token is used by
     * workers to communicate progress and response information back to the
     * system about the task.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 1024<br/>
     *
     * @return The opaque string used as a handle on the task. This token is used by
     *         workers to communicate progress and response information back to the
     *         system about the task.
     */
    public String getTaskToken() {
        return taskToken;
    }
    
    /**
     * The opaque string used as a handle on the task. This token is used by
     * workers to communicate progress and response information back to the
     * system about the task.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 1024<br/>
     *
     * @param taskToken The opaque string used as a handle on the task. This token is used by
     *         workers to communicate progress and response information back to the
     *         system about the task.
     */
    public void setTaskToken(String taskToken) {
        this.taskToken = taskToken;
    }
    
    /**
     * The opaque string used as a handle on the task. This token is used by
     * workers to communicate progress and response information back to the
     * system about the task.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 1024<br/>
     *
     * @param taskToken The opaque string used as a handle on the task. This token is used by
     *         workers to communicate progress and response information back to the
     *         system about the task.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DecisionTask withTaskToken(String taskToken) {
        this.taskToken = taskToken;
        return this;
    }

    /**
     * The ID of the <code>DecisionTaskStarted</code> event recorded in the
     * history.
     *
     * @return The ID of the <code>DecisionTaskStarted</code> event recorded in the
     *         history.
     */
    public Long getStartedEventId() {
        return startedEventId;
    }
    
    /**
     * The ID of the <code>DecisionTaskStarted</code> event recorded in the
     * history.
     *
     * @param startedEventId The ID of the <code>DecisionTaskStarted</code> event recorded in the
     *         history.
     */
    public void setStartedEventId(Long startedEventId) {
        this.startedEventId = startedEventId;
    }
    
    /**
     * The ID of the <code>DecisionTaskStarted</code> event recorded in the
     * history.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param startedEventId The ID of the <code>DecisionTaskStarted</code> event recorded in the
     *         history.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DecisionTask withStartedEventId(Long startedEventId) {
        this.startedEventId = startedEventId;
        return this;
    }

    /**
     * The workflow execution for which this decision task was created.
     *
     * @return The workflow execution for which this decision task was created.
     */
    public WorkflowExecution getWorkflowExecution() {
        return workflowExecution;
    }
    
    /**
     * The workflow execution for which this decision task was created.
     *
     * @param workflowExecution The workflow execution for which this decision task was created.
     */
    public void setWorkflowExecution(WorkflowExecution workflowExecution) {
        this.workflowExecution = workflowExecution;
    }
    
    /**
     * The workflow execution for which this decision task was created.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param workflowExecution The workflow execution for which this decision task was created.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DecisionTask withWorkflowExecution(WorkflowExecution workflowExecution) {
        this.workflowExecution = workflowExecution;
        return this;
    }

    /**
     * The type of the workflow execution for which this decision task was
     * created.
     *
     * @return The type of the workflow execution for which this decision task was
     *         created.
     */
    public WorkflowType getWorkflowType() {
        return workflowType;
    }
    
    /**
     * The type of the workflow execution for which this decision task was
     * created.
     *
     * @param workflowType The type of the workflow execution for which this decision task was
     *         created.
     */
    public void setWorkflowType(WorkflowType workflowType) {
        this.workflowType = workflowType;
    }
    
    /**
     * The type of the workflow execution for which this decision task was
     * created.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param workflowType The type of the workflow execution for which this decision task was
     *         created.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DecisionTask withWorkflowType(WorkflowType workflowType) {
        this.workflowType = workflowType;
        return this;
    }

    /**
     * A paginated list of history events of the workflow execution. The
     * decider uses this during the processing of the decision task.
     *
     * @return A paginated list of history events of the workflow execution. The
     *         decider uses this during the processing of the decision task.
     */
    public java.util.List<HistoryEvent> getEvents() {
      if (events == null) {
        events = new com.amazonaws.internal.ListWithAutoConstructFlag<HistoryEvent>();
        events.setAutoConstruct(true);
      }
      return events;
    }
    
    /**
     * A paginated list of history events of the workflow execution. The
     * decider uses this during the processing of the decision task.
     *
     * @param events A paginated list of history events of the workflow execution. The
     *         decider uses this during the processing of the decision task.
     */
    public void setEvents(java.util.Collection<HistoryEvent> events) {
        if (events == null) {
            this.events = null;
            return;
        }
        com.amazonaws.internal.ListWithAutoConstructFlag<HistoryEvent> eventsCopy = new com.amazonaws.internal.ListWithAutoConstructFlag<HistoryEvent>(events.size());
        eventsCopy.addAll(events);
        this.events = eventsCopy;
    }
    
    /**
     * A paginated list of history events of the workflow execution. The
     * decider uses this during the processing of the decision task.
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if
     * any). Use {@link #setEvents(java.util.Collection)} or {@link
     * #withEvents(java.util.Collection)} if you want to override the
     * existing values.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param events A paginated list of history events of the workflow execution. The
     *         decider uses this during the processing of the decision task.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DecisionTask withEvents(HistoryEvent... events) {
        if (getEvents() == null) setEvents(new java.util.ArrayList<HistoryEvent>(events.length));
        for (HistoryEvent value : events) {
            getEvents().add(value);
        }
        return this;
    }
    
    /**
     * A paginated list of history events of the workflow execution. The
     * decider uses this during the processing of the decision task.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param events A paginated list of history events of the workflow execution. The
     *         decider uses this during the processing of the decision task.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DecisionTask withEvents(java.util.Collection<HistoryEvent> events) {
        if (events == null) {
            this.events = null;
        } else {
            com.amazonaws.internal.ListWithAutoConstructFlag<HistoryEvent> eventsCopy = new com.amazonaws.internal.ListWithAutoConstructFlag<HistoryEvent>(events.size());
            eventsCopy.addAll(events);
            this.events = eventsCopy;
        }

        return this;
    }

    /**
     * If a <code>NextPageToken</code> was returned by a previous call, there
     * are more results available. To retrieve the next page of results, make
     * the call again using the returned token in <code>nextPageToken</code>.
     * Keep all other arguments unchanged. <p>The configured
     * <code>maximumPageSize</code> determines how many results can be
     * returned in a single call.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     *
     * @return If a <code>NextPageToken</code> was returned by a previous call, there
     *         are more results available. To retrieve the next page of results, make
     *         the call again using the returned token in <code>nextPageToken</code>.
     *         Keep all other arguments unchanged. <p>The configured
     *         <code>maximumPageSize</code> determines how many results can be
     *         returned in a single call.
     */
    public String getNextPageToken() {
        return nextPageToken;
    }
    
    /**
     * If a <code>NextPageToken</code> was returned by a previous call, there
     * are more results available. To retrieve the next page of results, make
     * the call again using the returned token in <code>nextPageToken</code>.
     * Keep all other arguments unchanged. <p>The configured
     * <code>maximumPageSize</code> determines how many results can be
     * returned in a single call.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     *
     * @param nextPageToken If a <code>NextPageToken</code> was returned by a previous call, there
     *         are more results available. To retrieve the next page of results, make
     *         the call again using the returned token in <code>nextPageToken</code>.
     *         Keep all other arguments unchanged. <p>The configured
     *         <code>maximumPageSize</code> determines how many results can be
     *         returned in a single call.
     */
    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
    
    /**
     * If a <code>NextPageToken</code> was returned by a previous call, there
     * are more results available. To retrieve the next page of results, make
     * the call again using the returned token in <code>nextPageToken</code>.
     * Keep all other arguments unchanged. <p>The configured
     * <code>maximumPageSize</code> determines how many results can be
     * returned in a single call.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     *
     * @param nextPageToken If a <code>NextPageToken</code> was returned by a previous call, there
     *         are more results available. To retrieve the next page of results, make
     *         the call again using the returned token in <code>nextPageToken</code>.
     *         Keep all other arguments unchanged. <p>The configured
     *         <code>maximumPageSize</code> determines how many results can be
     *         returned in a single call.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DecisionTask withNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
        return this;
    }

    /**
     * The ID of the DecisionTaskStarted event of the previous decision task
     * of this workflow execution that was processed by the decider. This can
     * be used to determine the events in the history new since the last
     * decision task received by the decider.
     *
     * @return The ID of the DecisionTaskStarted event of the previous decision task
     *         of this workflow execution that was processed by the decider. This can
     *         be used to determine the events in the history new since the last
     *         decision task received by the decider.
     */
    public Long getPreviousStartedEventId() {
        return previousStartedEventId;
    }
    
    /**
     * The ID of the DecisionTaskStarted event of the previous decision task
     * of this workflow execution that was processed by the decider. This can
     * be used to determine the events in the history new since the last
     * decision task received by the decider.
     *
     * @param previousStartedEventId The ID of the DecisionTaskStarted event of the previous decision task
     *         of this workflow execution that was processed by the decider. This can
     *         be used to determine the events in the history new since the last
     *         decision task received by the decider.
     */
    public void setPreviousStartedEventId(Long previousStartedEventId) {
        this.previousStartedEventId = previousStartedEventId;
    }
    
    /**
     * The ID of the DecisionTaskStarted event of the previous decision task
     * of this workflow execution that was processed by the decider. This can
     * be used to determine the events in the history new since the last
     * decision task received by the decider.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param previousStartedEventId The ID of the DecisionTaskStarted event of the previous decision task
     *         of this workflow execution that was processed by the decider. This can
     *         be used to determine the events in the history new since the last
     *         decision task received by the decider.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DecisionTask withPreviousStartedEventId(Long previousStartedEventId) {
        this.previousStartedEventId = previousStartedEventId;
        return this;
    }

    /**
     * Returns a string representation of this object; useful for testing and
     * debugging.
     *
     * @return A string representation of this object.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getTaskToken() != null) sb.append("TaskToken: " + getTaskToken() + ",");
        if (getStartedEventId() != null) sb.append("StartedEventId: " + getStartedEventId() + ",");
        if (getWorkflowExecution() != null) sb.append("WorkflowExecution: " + getWorkflowExecution() + ",");
        if (getWorkflowType() != null) sb.append("WorkflowType: " + getWorkflowType() + ",");
        if (getEvents() != null) sb.append("Events: " + getEvents() + ",");
        if (getNextPageToken() != null) sb.append("NextPageToken: " + getNextPageToken() + ",");
        if (getPreviousStartedEventId() != null) sb.append("PreviousStartedEventId: " + getPreviousStartedEventId() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getTaskToken() == null) ? 0 : getTaskToken().hashCode()); 
        hashCode = prime * hashCode + ((getStartedEventId() == null) ? 0 : getStartedEventId().hashCode()); 
        hashCode = prime * hashCode + ((getWorkflowExecution() == null) ? 0 : getWorkflowExecution().hashCode()); 
        hashCode = prime * hashCode + ((getWorkflowType() == null) ? 0 : getWorkflowType().hashCode()); 
        hashCode = prime * hashCode + ((getEvents() == null) ? 0 : getEvents().hashCode()); 
        hashCode = prime * hashCode + ((getNextPageToken() == null) ? 0 : getNextPageToken().hashCode()); 
        hashCode = prime * hashCode + ((getPreviousStartedEventId() == null) ? 0 : getPreviousStartedEventId().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof DecisionTask == false) return false;
        DecisionTask other = (DecisionTask)obj;
        
        if (other.getTaskToken() == null ^ this.getTaskToken() == null) return false;
        if (other.getTaskToken() != null && other.getTaskToken().equals(this.getTaskToken()) == false) return false; 
        if (other.getStartedEventId() == null ^ this.getStartedEventId() == null) return false;
        if (other.getStartedEventId() != null && other.getStartedEventId().equals(this.getStartedEventId()) == false) return false; 
        if (other.getWorkflowExecution() == null ^ this.getWorkflowExecution() == null) return false;
        if (other.getWorkflowExecution() != null && other.getWorkflowExecution().equals(this.getWorkflowExecution()) == false) return false; 
        if (other.getWorkflowType() == null ^ this.getWorkflowType() == null) return false;
        if (other.getWorkflowType() != null && other.getWorkflowType().equals(this.getWorkflowType()) == false) return false; 
        if (other.getEvents() == null ^ this.getEvents() == null) return false;
        if (other.getEvents() != null && other.getEvents().equals(this.getEvents()) == false) return false; 
        if (other.getNextPageToken() == null ^ this.getNextPageToken() == null) return false;
        if (other.getNextPageToken() != null && other.getNextPageToken().equals(this.getNextPageToken()) == false) return false; 
        if (other.getPreviousStartedEventId() == null ^ this.getPreviousStartedEventId() == null) return false;
        if (other.getPreviousStartedEventId() != null && other.getPreviousStartedEventId().equals(this.getPreviousStartedEventId()) == false) return false; 
        return true;
    }
}
    