/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import com.eucalyptus.auth.AuthException;

public interface Authorization extends Serializable {

  public static enum EffectType {
    Deny,
    Allow,
    Limit, // extension to IAM for quota
  }
  
  public EffectType getEffect( );
 
  public String getType( );
  
  public Boolean isNotAction( );
  
  public Set<String> getActions( ) throws AuthException;
  
  public Boolean isNotResource( );
  
  public Set<String> getResources( ) throws AuthException;
  
  public List<Condition> getConditions( ) throws AuthException;
  
  public Group getGroup( ) throws AuthException;
  
}
