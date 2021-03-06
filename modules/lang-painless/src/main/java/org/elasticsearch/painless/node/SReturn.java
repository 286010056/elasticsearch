/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless.node;

import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.lookup.PainlessLookupUtility;
import org.elasticsearch.painless.phase.UserTreeVisitor;
import org.elasticsearch.painless.symbol.Decorations.AllEscape;
import org.elasticsearch.painless.symbol.Decorations.Internal;
import org.elasticsearch.painless.symbol.Decorations.LoopEscape;
import org.elasticsearch.painless.symbol.Decorations.MethodEscape;
import org.elasticsearch.painless.symbol.Decorations.Read;
import org.elasticsearch.painless.symbol.Decorations.TargetType;
import org.elasticsearch.painless.symbol.SemanticScope;

/**
 * Represents a return statement.
 */
public class SReturn extends AStatement {

    private final AExpression expressionNode;

    public SReturn(int identifier, Location location, AExpression expressionNode) {
        super(identifier, location);

        this.expressionNode = expressionNode;
    }

    public AExpression getExpressionNode() {
        return expressionNode;
    }

    @Override
    public <Scope> void visit(UserTreeVisitor<Scope> userTreeVisitor, Scope scope) {
        userTreeVisitor.visitReturn(this, scope);
    }

    @Override
    public <Scope> void visitChildren(UserTreeVisitor<Scope> userTreeVisitor, Scope scope) {
        if (expressionNode != null) {
            expressionNode.visit(userTreeVisitor, scope);
        }
    }

    @Override
    void analyze(SemanticScope semanticScope) {
        if (expressionNode == null) {
            if (semanticScope.getReturnType() != void.class) {
                throw getLocation().createError(new ClassCastException("Cannot cast from " +
                        "[" + semanticScope.getReturnCanonicalTypeName() + "] to " +
                        "[" + PainlessLookupUtility.typeToCanonicalTypeName(void.class) + "]."));
            }
        } else {
            semanticScope.setCondition(expressionNode, Read.class);
            semanticScope.putDecoration(expressionNode, new TargetType(semanticScope.getReturnType()));
            semanticScope.setCondition(expressionNode, Internal.class);
            AExpression.analyze(expressionNode, semanticScope);
            expressionNode.cast(semanticScope);
        }

        semanticScope.setCondition(this, MethodEscape.class);
        semanticScope.setCondition(this, LoopEscape.class);
        semanticScope.setCondition(this, AllEscape.class);
    }
}
