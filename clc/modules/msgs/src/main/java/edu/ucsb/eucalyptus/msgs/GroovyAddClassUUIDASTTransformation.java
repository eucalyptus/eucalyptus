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
package edu.ucsb.eucalyptus.msgs;

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
      if ( cNode.getDeclaredField( "$EUCA_CLASS_UUID" ) == null ) {
        final FieldNode field = new FieldNode(
            "$EUCA_CLASS_UUID",
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
            new ClassNode(String.class),
            new ClassNode(cNode.getClass()),
            new ConstantExpression( UUID.randomUUID( ).toString( ) )
        );

        cNode.addField( field );
      }
    }
    
  }

}
