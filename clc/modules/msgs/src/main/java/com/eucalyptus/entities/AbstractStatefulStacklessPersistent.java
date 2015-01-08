/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
