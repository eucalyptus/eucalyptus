/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.entities;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * Identical to AbstractStatefulPersistent, but without the stack-trace element.
 * @param <STATE>
 */
@MappedSuperclass
public abstract class AbstractStatefulStacklessPersistent<STATE extends Enum<STATE>> extends AbstractPersistent {
    @Transient
    private static final long serialVersionUID = 1L;

    @Column( name = "metadata_state" )
    @Enumerated( EnumType.STRING )
    STATE                     state;

    @Column( name = "metadata_last_state" )
    @Enumerated( EnumType.STRING )
    STATE                     lastState;

    @Column( name = "metadata_display_name" )
    protected String          displayName;

    protected AbstractStatefulStacklessPersistent() {
        super( );
    }

    protected AbstractStatefulStacklessPersistent(final STATE state, final String displayName) {
        super( );
        this.state = state;
        this.displayName = displayName;
    }

    protected AbstractStatefulStacklessPersistent(final String displayName) {
        super( );
        this.displayName = displayName;
    }

    public STATE getState( ) {
        return this.state;
    }

    public void setState( final STATE state ) {
        if ( state != null && this.state != null && !state.equals( this.state ) ) {
            this.lastState = this.state;
        } else if ( state != null && this.state == null ) {
            this.lastState = state;
        } else if ( state == null && this.state != null ) {
            this.lastState = this.state;
        }
        this.state = state;
    }

    @Override
    public int hashCode( ) {
        final int prime = 31;
        int result = super.hashCode( );
        result = prime * result + ( ( this.displayName == null )
                ? 0
                : this.displayName.hashCode( ) );
        return result;
    }

    public String getDisplayName( ) {
        return this.displayName;
    }

    public void setDisplayName( final String displayName ) {
        this.displayName = displayName;
    }

    public final String getName( ) {
        return this.getDisplayName( );
    }

    public STATE getLastState( ) {
        return this.lastState;
    }

    public void setLastState( STATE lastState ) {
        this.lastState = lastState;
    }

}
