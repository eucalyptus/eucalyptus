/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.context;

import java.util.Objects;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.util.WildcardNameMatcher;

/**
 *
 */
@ComponentNamed
public class ContextWireTapMessageSelector implements MessageSelector {

  private static final WildcardNameMatcher matcher = new WildcardNameMatcher( );

  @Override
  public boolean accept( final Message<?> message ) {
    final Object payload = message.getPayload( );
    final String patternSource = Objects.toString( ServiceContext.CONTEXT_MESSAGE_LOG_WHITELIST, "" );
    return payload != null && (
        matcher.matches( patternSource, payload.getClass( ).getSimpleName( ) ) ||
        matcher.matches( patternSource, payload.getClass( ).getName( ) )
    );
  }
}
