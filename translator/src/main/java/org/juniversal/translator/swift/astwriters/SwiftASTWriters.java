/*
 * Copyright (c) 2011-2014, Microsoft Mobile
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.juniversal.translator.swift.astwriters;

import org.eclipse.jdt.core.dom.*;
import org.juniversal.translator.core.*;
import org.juniversal.translator.swift.SwiftTranslator;

import java.util.List;


public class SwiftASTWriters extends ASTWriters {
    private SwiftTranslator swiftTranslator;

    public SwiftASTWriters(SwiftTranslator swiftTranslator) {
        this.swiftTranslator = swiftTranslator;

        addDeclarationWriters();

        addStatementWriters();

        addExpressionWriters();

        // Simple name
        addWriter(SimpleName.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                SimpleName simpleName = (SimpleName) node;

                context.matchAndWrite(simpleName.getIdentifier());
            }
        });
    }

    /**
     * Add visitors for class, method, field, and type declarations.
     */
    private void addDeclarationWriters() {
        // TODO: Implement this
        // Compilation unit
        addWriter(CompilationUnit.class, new CompilationUnitWriter(this));

        // TODO: Implement this
        // Type (class/interface) declaration
        addWriter(TypeDeclaration.class, new TypeDeclarationWriter(this));

        // TODO: Implement this
        // Method declaration (which includes implementation)
        addWriter(MethodDeclaration.class, new MethodDeclarationWriter(this));

        // TODO: Implement this
        // Field declaration
/*
        addWriter(FieldDeclaration.class, new FieldDeclarationWriter(this));
*/

        // TODO: Implement this
        // Variable declaration fragment
        addWriter(VariableDeclarationFragment.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) node;

                // TODO: Handle syntax with extra dimensions on array
                if (variableDeclarationFragment.getExtraDimensions() > 0)
                    context.throwSourceNotSupported("\"int foo[]\" syntax not currently supported; use \"int[] foo\" instead");

                if (context.isWritingVariableDeclarationNeedingStar())
                    context.write("*");

                writeNode(context, variableDeclarationFragment.getName());

                Expression initializer = variableDeclarationFragment.getInitializer();
                if (initializer != null) {
                    context.copySpaceAndComments();
                    context.matchAndWrite("=");

                    context.copySpaceAndComments();
                    writeNode(context, initializer);
                }
            }
        });

        // TODO: Implement this
        // Single variable declaration (used in parameter list & catch clauses)
        addWriter(SingleVariableDeclaration.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) node;

                // TODO: Handle syntax with extra dimensions on array
                if (singleVariableDeclaration.getExtraDimensions() > 0)
                    context.throwSourceNotSupported("\"int foo[]\" syntax not currently supported; use \"int[] foo\" instead");

                // TODO: Handle final & varargs

                SimpleName name = singleVariableDeclaration.getName();
                context.setPositionToStartOfNode(name);
                writeNode(context, name);
                context.write(": ");

                int endOfNamePosition = context.getPosition();

                Type type = singleVariableDeclaration.getType();
                context.setPositionToStartOfNode(type);
                writeNode(context, type);

                context.setPosition(endOfNamePosition);

                // TODO: Handle initializer
                Expression initializer = singleVariableDeclaration.getInitializer();
                if (initializer != null)
                    throw new JUniversalException("Unexpected initializer present for SingleVariableDeclaration");
            }
        });

        // TODO: Implement this
        // Simple type
        addWriter(SimpleType.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                SimpleType simpleType = (SimpleType) node;

                Name name = simpleType.getName();
                if (name instanceof QualifiedName) {
                    QualifiedName qualifiedName = (QualifiedName) name;

                    context.write(ASTWriterUtil.getNamespaceNameForPackageName(qualifiedName.getQualifier()));
                    context.setPositionToEndOfNode(qualifiedName.getQualifier());

                    context.copySpaceAndComments();
                    context.matchAndWrite(".", "::");
                    context.matchAndWrite(qualifiedName.getName().getIdentifier());
                } else {
                    SimpleName simpleName = (SimpleName) name;

                    context.matchAndWrite(simpleName.getIdentifier());
                }
            }
        });

        // TODO: Implement this
        // Parameterized type
        addWriter(ParameterizedType.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ParameterizedType parameterizedType = (ParameterizedType) node;

                writeNode(context, parameterizedType.getType());

                context.copySpaceAndComments();
                context.matchAndWrite("<");

                boolean first = true;
                List<?> typeArguments = parameterizedType.typeArguments();
                for (Object typeArgumentObject : typeArguments) {
                    Type typeArgument = (Type) typeArgumentObject;

                    if (!first) {
                        context.copySpaceAndComments();
                        context.matchAndWrite(",");
                    }

                    context.copySpaceAndComments();
                    writeNode(context, typeArgument);

                    first = false;
                }

                context.matchAndWrite(">");
            }
        });

        // TODO: Implement this
        // Array type
        addWriter(ArrayType.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ArrayType arrayType = (ArrayType) node;

                context.write("Array<");
                writeNode(context, arrayType.getElementType());
                context.skipSpaceAndComments();
                context.write(">");

                context.match("[");
                context.skipSpaceAndComments();

                context.match("]");
            }
        });

        // Primitive type
        addWriter(PrimitiveType.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                PrimitiveType primitiveType = (PrimitiveType) node;

                PrimitiveType.Code code = primitiveType.getPrimitiveTypeCode();
                if (code == PrimitiveType.BYTE)
                    context.matchAndWrite("byte", "Int8");
                else if (code == PrimitiveType.SHORT)
                    context.matchAndWrite("short", "Int16");
                else if (code == PrimitiveType.CHAR)
                    context.matchAndWrite("char", "Character");
                // TODO: For now, map 32 bit Java int to Int type in Swift, which can be 32 or 64 bits.
                // Later probably add @PreserveJavaIntSemantics annotation to force 32 bit + no overflow checking
                else if (code == PrimitiveType.INT)
                    context.matchAndWrite("int", "Int");
                else if (code == PrimitiveType.LONG) {
                    context.matchAndWrite("long", "Int64");
                } else if (code == PrimitiveType.FLOAT)
                    context.matchAndWrite("float", "Float");
                else if (code == PrimitiveType.DOUBLE)
                    context.matchAndWrite("double", "Double");
                else if (code == PrimitiveType.BOOLEAN)
                    context.matchAndWrite("boolean", "Bool");
                else if (code == PrimitiveType.VOID)
                    throw new JUniversalException("Should detect void return types before it gets here");
                else
                    context.throwInvalidAST("Unknown primitive type: " + code);
            }
        });
    }

    /**
     * Add visitors for the different kinds of statements.
     */
    private void addStatementWriters() {
        // TODO: Implement this
        // Block
        addWriter(Block.class, new SwiftASTWriter(this) {
            @SuppressWarnings("unchecked")
            @Override
            public void write(Context context, ASTNode node) {
                Block block = (Block) node;

                context.matchAndWrite("{");

                boolean firstStatement = true;
                for (Statement statement : (List<Statement>) block.statements()) {
                    // If the first statement is a super constructor invocation, we skip it since
                    // it's included as part of the method declaration in C++. If a super
                    // constructor invocation is a statement other than the first, which it should
                    // never be, we let that error out since writeNode won't find a match for it.
                    if (firstStatement && statement instanceof SuperConstructorInvocation)
                        context.setPositionToEndOfNodeSpaceAndComments(statement);
                    else {
                        context.copySpaceAndComments();
                        writeNode(context, statement);
                    }

                    firstStatement = false;
                }

                context.copySpaceAndComments();
                context.matchAndWrite("}");
            }
        });

        // TODO: Implement this
        // Empty statement (";")
        addWriter(EmptyStatement.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                context.matchAndWrite(";");
            }
        });

        // TODO: Implement this
        // Expression statement
        addWriter(ExpressionStatement.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ExpressionStatement expressionStatement = (ExpressionStatement) node;

                writeNode(context, expressionStatement.getExpression());

                context.copySpaceAndComments();
                context.matchAndWrite(";");
            }
        });

        // If statement
        addWriter(IfStatement.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                IfStatement ifStatement = (IfStatement) node;

                int ifColumn = context.getTargetColumn();
                context.matchAndWrite("if");

                context.copySpaceAndCommentsEnsuringDelimiter();

                writeConditionNoParens(ifStatement.getExpression(), context);

                writeStatementEnsuringBraces(ifColumn, false, ifStatement.getThenStatement(), context);

                Statement elseStatement = ifStatement.getElseStatement();
                if (elseStatement != null) {
                    context.copySpaceAndComments();

                    context.matchAndWrite("else");
                    writeStatementEnsuringBraces(ifColumn, true, ifStatement.getElseStatement(), context);
                }
            }
        });

        // While statement
        addWriter(WhileStatement.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                WhileStatement whileStatement = (WhileStatement) node;

                int whileColumn = context.getTargetColumn();
                context.matchAndWrite("while");

                context.copySpaceAndCommentsEnsuringDelimiter();

                writeConditionNoParens(whileStatement.getExpression(), context);

                writeStatementEnsuringBraces(whileColumn, false, whileStatement.getBody(), context);
            }
        });

        // Do while statement
        addWriter(DoStatement.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                DoStatement doStatement = (DoStatement) node;

                int doColumn = context.getTargetColumn();
                context.matchAndWrite("do");

                writeStatementEnsuringBraces(doColumn, false, doStatement.getBody(), context);

                context.copySpaceAndComments();
                context.matchAndWrite("while");

                context.copySpaceAndComments();
                writeConditionNoParens(doStatement.getExpression(), context);

                context.copySpaceAndComments();
                context.matchAndWrite(";");
            }
        });

        // TODO: Implement this
        // Continue statement
        addWriter(ContinueStatement.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ContinueStatement continueStatement = (ContinueStatement) node;

                if (continueStatement.getLabel() != null)
                    context.throwSourceNotSupported("continue statement with a label isn't supported as that construct doesn't exist in C++; change the code to not use a label");

                context.matchAndWrite("continue");

                context.copySpaceAndComments();
                context.matchAndWrite(";");
            }
        });

        // TODO: Implement this
        // Break statement
        addWriter(BreakStatement.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                BreakStatement breakStatement = (BreakStatement) node;

                if (breakStatement.getLabel() != null)
                    context.throwSourceNotSupported("break statement with a label isn't supported as that construct doesn't exist in C++; change the code to not use a label");

                context.matchAndWrite("break");

                context.copySpaceAndComments();
                context.matchAndWrite(";");
            }
        });

        // TODO: Implement this
        // For statement
/*
        addWriter(ForStatement.class, new ForStatementWriter(this));
*/

        // TODO: Implement this
        // Return statement
        addWriter(ReturnStatement.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ReturnStatement returnStatement = (ReturnStatement) node;

                context.matchAndWrite("return");

                Expression expression = returnStatement.getExpression();
                if (expression != null) {
                    context.copySpaceAndComments();
                    writeNode(context, returnStatement.getExpression());
                }

                context.copySpaceAndComments();
                context.matchAndWrite(";");
            }
        });

        // TODO: Implement this
        // Local variable declaration statement
        addWriter(VariableDeclarationStatement.class, new VariableDeclarationWriter(this));

        // TODO: Implement this
        // Throw statement
        addWriter(ThrowStatement.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ThrowStatement throwStatement = (ThrowStatement) node;

                context.matchAndWrite("throw");
                context.copySpaceAndComments();

                writeNode(context, throwStatement.getExpression());
                context.copySpaceAndComments();

                context.matchAndWrite(";");
            }
        });

        // TODO: Implement this
        // Delegating constructor invocation
        addWriter(ConstructorInvocation.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                context.throwSourceNotSupported("Delegating constructors aren't currently supported; for now you have to change the code to not use them (e.g. by adding an init method)");
            }
        });
    }

    /**
     * Add visitors for the different kinds of expressions.
     */
    private void addExpressionWriters() {

        // TODO: Implement this
        // Assignment expression
/*
        addWriter(Assignment.class, new AssignmentWriter(this));
*/

        // TODO: Implement this
        // Method invocation
        addWriter(MethodInvocation.class, new MethodInvocationWriter(this));

        // TODO: Implement this
        // Super Method invocation
        addWriter(SuperMethodInvocation.class, new MethodInvocationWriter(this));

        // TODO: Implement this
        // Class instance creation
        addWriter(ClassInstanceCreation.class, new ClassInstanceCreationWriter(this));

        // TODO: Implement this
        // Array creation
/*
        addWriter(ArrayCreation.class, new ArrayCreationWriter(this));
*/

        // TODO: Implement this
        // Variable declaration expression (used in a for statement)
        addWriter(VariableDeclarationExpression.class, new VariableDeclarationWriter(this));

        // Infix expression
        addWriter(InfixExpression.class, new InfixExpressionWriter(this));

        // Prefix expression
        addWriter(PrefixExpression.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                PrefixExpression prefixExpression = (PrefixExpression) node;

                PrefixExpression.Operator operator = prefixExpression.getOperator();
                if (operator == PrefixExpression.Operator.INCREMENT)
                    context.matchAndWrite("++");
                else if (operator == PrefixExpression.Operator.DECREMENT)
                    context.matchAndWrite("--");
                else if (operator == PrefixExpression.Operator.PLUS)
                    context.matchAndWrite("+");
                else if (operator == PrefixExpression.Operator.MINUS)
                    context.matchAndWrite("-");
                else if (operator == PrefixExpression.Operator.COMPLEMENT)
                    context.matchAndWrite("~");
                else if (operator == PrefixExpression.Operator.NOT)
                    context.matchAndWrite("!");
                else context.throwInvalidAST("Unknown prefix operator type: " + operator);

                // In Swift there can't be any whitespace or comments between a unary prefix operator & its operand, so
                // strip it, not copying anything here
                context.skipSpaceAndComments();

                writeNode(context, prefixExpression.getOperand());
            }
        });

        // Postfix expression
        addWriter(PostfixExpression.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                PostfixExpression postfixExpression = (PostfixExpression) node;

                writeNode(context, postfixExpression.getOperand());

                // In Swift there can't be any whitespace or comments between a postfix operator & its operand, so
                // strip it, not copying anything here
                context.skipSpaceAndComments();

                PostfixExpression.Operator operator = postfixExpression.getOperator();
                if (operator == PostfixExpression.Operator.INCREMENT)
                    context.matchAndWrite("++");
                else if (operator == PostfixExpression.Operator.DECREMENT)
                    context.matchAndWrite("--");
                else context.throwInvalidAST("Unknown postfix operator type: " + operator);
            }
        });

        // TODO: Implement this
        // instanceof expression
        addWriter(InstanceofExpression.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                InstanceofExpression instanceofExpression = (InstanceofExpression) node;

                context.write("INSTANCEOF(");

                Expression expression = instanceofExpression.getLeftOperand();
                writeNode(context, expression);

                context.skipSpaceAndComments();
                context.match("instanceof");

                context.skipSpaceAndComments();
                Type type = instanceofExpression.getRightOperand();
                writeNode(context, type);

                context.write(")");
            }
        });

        // conditional expression
        addWriter(ConditionalExpression.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ConditionalExpression conditionalExpression = (ConditionalExpression) node;

                writeNode(context, conditionalExpression.getExpression());

                context.copySpaceAndComments();
                context.matchAndWrite("?");

                context.copySpaceAndComments();
                writeNode(context, conditionalExpression.getThenExpression());

                context.copySpaceAndComments();
                context.matchAndWrite(":");

                context.copySpaceAndComments();
                writeNode(context, conditionalExpression.getElseExpression());
            }
        });

        // TODO: Implement this
        // this
        addWriter(ThisExpression.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ThisExpression thisExpression = (ThisExpression) node;

                // TODO: Handle qualified this expressions; probably need to do from parent invoking
                // node & disallow qualified this accesses if not field reference / method
                // invocation; it's allowed otherwise in Java but I don't think it does anything
                // MyClass.this.   -->   this->MyClass::
                if (thisExpression.getQualifier() != null)
                    throw new JUniversalException("Qualified this expression isn't supported yet");

                context.matchAndWrite("this");
            }
        });

        // TODO: Implement this
        // Field access
        addWriter(FieldAccess.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                FieldAccess fieldAccess = (FieldAccess) node;

                writeNode(context, fieldAccess.getExpression());
                context.copySpaceAndComments();

                context.matchAndWrite(".", "->");

                writeNode(context, fieldAccess.getName());
            }
        });

        // TODO: Implement this
        // Array access
        addWriter(ArrayAccess.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ArrayAccess arrayAccess = (ArrayAccess) node;

                writeNode(context, arrayAccess.getArray());
                context.copySpaceAndComments();

                context.matchAndWrite("[");
                context.copySpaceAndComments();

                writeNode(context, arrayAccess.getIndex());
                context.copySpaceAndComments();

                context.matchAndWrite("]");
            }
        });

        // TODO: Implement this
        // Qualified name
        addWriter(QualifiedName.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                QualifiedName qualifiedName = (QualifiedName) node;

                // TODO: Figure out the other cases where this can occur & make them all correct

                // Here assume that a QualifiedName refers to field access; if it refers to a type,
                // the caller should catch that case itself and ensure it never gets here

                writeNode(context, qualifiedName.getQualifier());
                context.copySpaceAndComments();

                context.matchAndWrite(".", "->");

                writeNode(context, qualifiedName.getName());
            }
        });

        // TODO: Implement this
        // Parenthesized expression
        addWriter(ParenthesizedExpression.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) node;

                context.matchAndWrite("(");
                context.copySpaceAndComments();

                writeNode(context, parenthesizedExpression.getExpression());
                context.copySpaceAndComments();

                context.matchAndWrite(")");
            }
        });

        // TODO: Implement this
        // Cast expression
        addWriter(CastExpression.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                CastExpression castExpression = (CastExpression) node;

                context.matchAndWrite("(", "static_cast<");

                context.copySpaceAndComments();
                writeNode(context, castExpression.getType());

                context.copySpaceAndComments();
                context.matchAndWrite(")", ">");

                // Skip just whitespace as that's not normally present here in C++ unlike Java, but
                // if there's a newline or comment, preserve that
                context.skipSpaceAndComments();
                context.copySpaceAndComments();

                // Write out the parentheses unless by chance the casted expression already includes
                // them
                boolean needParentheses = !(castExpression.getExpression() instanceof ParenthesizedExpression);
                if (needParentheses)
                    context.write("(");
                writeNode(context, castExpression.getExpression());
                if (needParentheses)
                    context.write(")");
            }
        });

        // Number literal
        addWriter(NumberLiteral.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                NumberLiteral numberLiteral = (NumberLiteral) node;
                String token = numberLiteral.getToken();

                boolean isOctal = false;
                if (token.length() >= 2 && token.startsWith("0")) {
                    char secondChar = token.charAt(1);
                    if (secondChar >= '0' && secondChar <= '7') {
                        isOctal = true;
                    }
                }

                // Swift prefixes octal with 0o whereas Java just uses a leading 0
                if (isOctal) {
                    context.write("0o");
                    context.write(token.substring(1));
                } else context.write(token);

                context.match(token);
            }
        });

        // Boolean literal
        addWriter(BooleanLiteral.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                BooleanLiteral booleanLiteral = (BooleanLiteral) node;
                context.matchAndWrite(booleanLiteral.booleanValue() ? "true" : "false");
            }
        });

        // Character literal
        addWriter(CharacterLiteral.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                CharacterLiteral characterLiteral = (CharacterLiteral) node;

                // TODO: Map character escape sequences
                // TODO: Handle conversion of characters to integers where that's normally implicit in Java

                context.write("Character(\"" + characterLiteral.charValue() + "\")");
                context.match(characterLiteral.getEscapedValue());
            }
        });

        // Null literal
        addWriter(NullLiteral.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                context.matchAndWrite("null", "nil");
            }
        });

        // TODO: Implement this
        // String literal
        addWriter(StringLiteral.class, new SwiftASTWriter(this) {
            @Override
            public void write(Context context, ASTNode node) {
                StringLiteral stringLiteral = (StringLiteral) node;

                context.write("new String(" + stringLiteral.getEscapedValue() + "L)");
                context.match(stringLiteral.getEscapedValue());
            }
        });
    }

    private void writeConditionNoParens(Expression expression, Context context) {
        context.match("(");
        context.skipSpaceAndComments();

        writeNode(context, expression);

        context.skipSpaceAndComments();
        context.match(")");
    }

    private void writeStatementEnsuringBraces(int blockStartColumn, boolean forceSeparateLine,
                                              Statement statement, Context context) {
        if (statement instanceof Block) {
            context.copySpaceAndComments();
            writeNode(context, statement);
        } else {
            if (context.startsOnSameLine(statement)) {
                if (forceSeparateLine) {
                    context.write(" {\n");

                    context.writeSpacesUntilColumn(blockStartColumn);
                    context.writeSpaces(context.getPreferredIndent());
                    context.skipSpacesAndTabs();

                    context.copySpaceAndComments();

                    writeNode(context, statement);

                    context.copySpaceAndCommentsUntilEOL();
                    context.setKnowinglyProcessedTrailingSpaceAndComments(true);
                    context.writeln();

                    context.writeSpacesUntilColumn(blockStartColumn);
                    context.write("}");
                } else {
                    context.copySpaceAndCommentsEnsuringDelimiter();

                    context.write("{ ");
                    writeNode(context, statement);
                    context.write(" }");
                }
            } else {
                context.write(" {");

                context.copySpaceAndComments();

                writeNode(context, statement);

                context.copySpaceAndCommentsUntilEOL();
                context.setKnowinglyProcessedTrailingSpaceAndComments(true);
                context.writeln();

                context.writeSpacesUntilColumn(blockStartColumn);
                context.write("}");
            }
        }
    }

    /**
     * Write out a type, when it's used (as opposed to defined).
     *
     * @param type    type to write
     * @param context context
     */
    public void writeType(Type type, Context context, boolean useRawPointer) {
        boolean referenceType = !type.isPrimitiveType();

        if (!referenceType)
            writeNode(context, type);
        else {
            if (useRawPointer) {
                writeNode(context, type);
                context.write("*");
            } else {
                context.write("ptr< ");
                writeNode(context, type);
                context.write(" >");
            }
        }
    }

    @Override
    public SwiftTranslator getTranslator() {
        return swiftTranslator;
    }
}
