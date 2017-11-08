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
 * Provides details of the <code>MarkerRecorded</code> event.
 * </p>
 */
public class MarkerRecordedEventAttributes implements Serializable, WorkflowEventAttributes {

    /**
     * The name of the marker.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    private String markerName;

    /**
     * Details of the marker (if any).
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     */
    private String details;

    /**
     * The ID of the <code>DecisionTaskCompleted</code> event corresponding
     * to the decision task that resulted in the <code>RecordMarker</code>
     * decision that requested this marker. This information can be useful
     * for diagnosing problems by tracing back the chain of events leading up
     * to this event.
     */
    private Long decisionTaskCompletedEventId;

    /**
     * The name of the marker.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return The name of the marker.
     */
    public String getMarkerName() {
        return markerName;
    }
    
    /**
     * The name of the marker.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param markerName The name of the marker.
     */
    public void setMarkerName(String markerName) {
        this.markerName = markerName;
    }
    
    /**
     * The name of the marker.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param markerName The name of the marker.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public MarkerRecordedEventAttributes withMarkerName(String markerName) {
        this.markerName = markerName;
        return this;
    }

    /**
     * Details of the marker (if any).
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @return Details of the marker (if any).
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * Details of the marker (if any).
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @param details Details of the marker (if any).
     */
    public void setDetails(String details) {
        this.details = details;
    }
    
    /**
     * Details of the marker (if any).
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @param details Details of the marker (if any).
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public MarkerRecordedEventAttributes withDetails(String details) {
        this.details = details;
        return this;
    }

    /**
     * The ID of the <code>DecisionTaskCompleted</code> event corresponding
     * to the decision task that resulted in the <code>RecordMarker</code>
     * decision that requested this marker. This information can be useful
     * for diagnosing problems by tracing back the chain of events leading up
     * to this event.
     *
     * @return The ID of the <code>DecisionTaskCompleted</code> event corresponding
     *         to the decision task that resulted in the <code>RecordMarker</code>
     *         decision that requested this marker. This information can be useful
     *         for diagnosing problems by tracing back the chain of events leading up
     *         to this event.
     */
    public Long getDecisionTaskCompletedEventId() {
        return decisionTaskCompletedEventId;
    }
    
    /**
     * The ID of the <code>DecisionTaskCompleted</code> event corresponding
     * to the decision task that resulted in the <code>RecordMarker</code>
     * decision that requested this marker. This information can be useful
     * for diagnosing problems by tracing back the chain of events leading up
     * to this event.
     *
     * @param decisionTaskCompletedEventId The ID of the <code>DecisionTaskCompleted</code> event corresponding
     *         to the decision task that resulted in the <code>RecordMarker</code>
     *         decision that requested this marker. This information can be useful
     *         for diagnosing problems by tracing back the chain of events leading up
     *         to this event.
     */
    public void setDecisionTaskCompletedEventId(Long decisionTaskCompletedEventId) {
        this.decisionTaskCompletedEventId = decisionTaskCompletedEventId;
    }
    
    /**
     * The ID of the <code>DecisionTaskCompleted</code> event corresponding
     * to the decision task that resulted in the <code>RecordMarker</code>
     * decision that requested this marker. This information can be useful
     * for diagnosing problems by tracing back the chain of events leading up
     * to this event.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param decisionTaskCompletedEventId The ID of the <code>DecisionTaskCompleted</code> event corresponding
     *         to the decision task that resulted in the <code>RecordMarker</code>
     *         decision that requested this marker. This information can be useful
     *         for diagnosing problems by tracing back the chain of events leading up
     *         to this event.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public MarkerRecordedEventAttributes withDecisionTaskCompletedEventId(Long decisionTaskCompletedEventId) {
        this.decisionTaskCompletedEventId = decisionTaskCompletedEventId;
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
        if (getMarkerName() != null) sb.append("MarkerName: " + getMarkerName() + ",");
        if (getDetails() != null) sb.append("Details: " + getDetails() + ",");
        if (getDecisionTaskCompletedEventId() != null) sb.append("DecisionTaskCompletedEventId: " + getDecisionTaskCompletedEventId() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getMarkerName() == null) ? 0 : getMarkerName().hashCode()); 
        hashCode = prime * hashCode + ((getDetails() == null) ? 0 : getDetails().hashCode()); 
        hashCode = prime * hashCode + ((getDecisionTaskCompletedEventId() == null) ? 0 : getDecisionTaskCompletedEventId().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof MarkerRecordedEventAttributes == false) return false;
        MarkerRecordedEventAttributes other = (MarkerRecordedEventAttributes)obj;
        
        if (other.getMarkerName() == null ^ this.getMarkerName() == null) return false;
        if (other.getMarkerName() != null && other.getMarkerName().equals(this.getMarkerName()) == false) return false; 
        if (other.getDetails() == null ^ this.getDetails() == null) return false;
        if (other.getDetails() != null && other.getDetails().equals(this.getDetails()) == false) return false; 
        if (other.getDecisionTaskCompletedEventId() == null ^ this.getDecisionTaskCompletedEventId() == null) return false;
        if (other.getDecisionTaskCompletedEventId() != null && other.getDecisionTaskCompletedEventId().equals(this.getDecisionTaskCompletedEventId()) == false) return false; 
        return true;
    }

    @Override
    public void attach(HistoryEvent historyEvent) {
      historyEvent.setMarkerRecordedEventAttributes(this);
    }
}
    