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
package edu.ucsb.eucalyptus.msgs;

import java.lang.reflect.Modifier;
import java.util.UUID;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.PackageNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import com.google.common.base.Charsets;
import groovyjarjarasm.asm.Opcodes;

/**
 * ASTTransformation that adds a UUID field to classes in the source unit.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GroovyAddClassUUIDASTTransformation implements ASTTransformation {

  public void visit( final ASTNode[] nodes, 
                     final SourceUnit sourceUnit) {
    if ( null == nodes || nodes.length < 2 ) return;
    if (!(nodes[0] instanceof AnnotationNode)) return;
    if (!(nodes[1] instanceof PackageNode)) return;

    for ( ClassNode cNode : sourceUnit.getAST().getClasses() ) {
      if ( !Modifier.isInterface( cNode.getModifiers( ) ) && cNode.getDeclaredField( "$EUCA_CLASS_UUID" ) == null ) {
        final FieldNode field = new FieldNode(
            "$EUCA_CLASS_UUID",
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
            new ClassNode(String.class),
            new ClassNode(cNode.getClass()),
            new ConstantExpression( UUID.nameUUIDFromBytes( cNode.getName().getBytes( Charsets.UTF_8 ) ).toString( ) )
        );

        cNode.addField( field );

        // Added so Groovy thinks it has already added the "__timeStamp__239_neverHappen$TIMESTAMP" field and skips it
        final FieldNode timeTagField = new FieldNode(
            "__timeStamp",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            new ClassNode(Long.class),
            new ClassNode(cNode.getClass()),
            new ConstantExpression( 0 )
        );

        cNode.addField( timeTagField );
       }
    }
  }

}
