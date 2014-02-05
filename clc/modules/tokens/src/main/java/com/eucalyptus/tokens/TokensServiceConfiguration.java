/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.tokens;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.util.Strings;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 *
 */
@ConfigurableClass(root = "tokens", description = "Parameters controlling tokens service")
public class TokensServiceConfiguration {

  private static final Logger logger = Logger.getLogger( TokensServiceConfiguration.class );

  @ConfigurableField(
      initial = "",
      description = "Actions to enable (ignored if empty)",
      changeListener = TokensServicePropertyChangeListener.class )
  public static volatile String enabledActions = "";

  @ConfigurableField(
      initial = "",
      description = "Actions to disable",
      changeListener = TokensServicePropertyChangeListener.class )
  public static volatile String disabledActions = "";

  private static final AtomicReference<Set<String>> enabledActionsSet = new AtomicReference<>( Collections.<String>emptySet() );
  private static final AtomicReference<Set<String>> disabledActionsSet = new AtomicReference<>( Collections.<String>emptySet() );

  public static Set<String> getEnabledActions( ) {
    return enabledActionsSet.get( );
  }

  public static Set<String> getDisabledActions( ) {
    return disabledActionsSet.get( );
  }

  public static final class TokensServicePropertyChangeListener implements PropertyChangeListener {
    @SuppressWarnings( "unchecked" )
    @Override
    public void fireChange( final ConfigurableProperty configurableProperty,
                            final Object newValue ) throws ConfigurablePropertyException {
      try {
        final Splitter splitter =
            Splitter.on( CharMatcher.WHITESPACE.or( CharMatcher.anyOf( ",;|" ) ) ).trimResults( ).omitEmptyStrings( );
        final String fieldName = configurableProperty.getField().getName() + "Set";
        final Field field = TokensServiceConfiguration.class.getDeclaredField( fieldName );
        field.setAccessible( true );
        final Set<String> value =
            ImmutableSet.copyOf( Iterables.transform( splitter.split( String.valueOf( newValue ) ), Strings.lower( ) ) );
        logger.info( "Tokens service configuration updated " + configurableProperty.getDisplayName( ) + ": " + value );
        ((AtomicReference<Set<String>>)field.get( null )).set( value );
      } catch ( NoSuchFieldException | IllegalAccessException  e ) {
        logger.error( "Error updating token service configuration for " + configurableProperty.getDisplayName( ), e );
      }
    }
  }
}
