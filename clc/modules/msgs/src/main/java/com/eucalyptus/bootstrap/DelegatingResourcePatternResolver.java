/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.bootstrap;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 *
 */
public class DelegatingResourcePatternResolver implements ResourcePatternResolver {

  private final ResourcePatternResolver delegate;

  public DelegatingResourcePatternResolver( final ResourcePatternResolver delegate ) {
    this.delegate = delegate;
  }

  @Override
  public Resource[] getResources( final String locationPattern ) throws IOException {
    return delegate.getResources( locationPattern );
  }

  @Override
  public Resource getResource( final String location ) {
    return delegate.getResource( location );
  }

  @Override
  public ClassLoader getClassLoader( ) {
    return delegate.getClassLoader( );
  }
}
