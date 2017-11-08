/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

