/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.crypto.util;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.CallbackLookup;
import org.apache.ws.security.message.DOMCallbackLookup;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverContext;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;
import org.w3c.dom.Element;
import com.google.common.base.Objects;

/**
 * Resolve elements by ID within a document, supports ws-utility identifiers.
 */
public class WssIdResolver extends ResourceResolverSpi {

  @Override
  public XMLSignatureInput engineResolveURI( final ResourceResolverContext context ) throws ResourceResolverException {
    final String id = context.uriToResolve.substring( 1 );
    final CallbackLookup callbackLookup = new DOMCallbackLookup( context.attr.getOwnerDocument( ) );
    Element referencedElement = null;
    try {
        referencedElement = callbackLookup.getElement( id, null, true );
    } catch ( WSSecurityException ex ) {
        error( context, ex.getMessage( ) );
    }
    
    if (referencedElement == null) {
      error( context, "Error resolving reference: " + context.uriToResolve );
    }
  
    final XMLSignatureInput result = new XMLSignatureInput( referencedElement );
    result.setMIMEType( "text/xml" );
    result.setSourceURI( Objects.firstNonNull( context.baseUri, "" ) + context.uriToResolve );
    return result;
  }

  @Override
  public boolean engineCanResolveURI( final ResourceResolverContext context ) {
    return context.uriToResolve != null && 
        context.uriToResolve.startsWith( "#" ) && 
        !context.uriToResolve.startsWith("#xpointer(");
  }

  private static void error( final ResourceResolverContext context,
                             final String details ) throws ResourceResolverException {
    throw new ResourceResolverException(
        "empty",
        new Object[]{ details },
        context.attr,
        context.baseUri );
  }
}

