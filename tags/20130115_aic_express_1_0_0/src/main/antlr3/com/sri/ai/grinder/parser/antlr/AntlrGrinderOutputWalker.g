tree grammar AntlrGrinderOutputWalker;

options {

    // Default language but name it anyway
    //
    language  = Java;

    // Use the vocabulary generated by the accompanying
    // lexer. Maven knows how to work out the relationship
    // between the lexer and parser and will build the 
    // lexer before the parser. It will also rebuild the
    // parser if the lexer changes.
    //
    tokenVocab = AntlrGrinderParser;
    
    ASTLabelType = CommonTree;
    
    backtrack = true;
    memoize = true;
}

@header {

    package com.sri.ai.grinder.parser.antlr;

    import java.util.ArrayList;

    import com.sri.ai.expresso.api.Expression;
    import com.sri.ai.expresso.core.DefaultSymbol;
    import com.sri.ai.expresso.core.DefaultCompoundSyntaxTree;
}

@members {
/** Makes parser fail on first error. */
//@Override
//protected void mismatch(IntStream input, int ttype, BitSet follow)
//    throws RecognitionException {
//    throw new MismatchedTokenException(ttype, input);
//}

/** Makes parser fail on first error. */
//@Override
//public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow)
//    throws RecognitionException {
//    throw e;
//    //return null;
//}

/** Makes parser fail on first error. */
//@Override
//protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow)
//    throws RecognitionException {
//    throw new MismatchedTokenException(ttype, input);
//}

}

// Alter code generation so catch-clauses get replace with this action.
//@rulecatch {
//catch (RecognitionException e) { throw e; }
//}


/*
    The ANTLR output tree walker is the final step in parsing the LPI grammar.
    The walker converts the AST from ANTLR's native CommonTree node type to
    the expected DefaultCompoundSyntaxTree/DefaultSymbol output.  It walks
    the CommonTree AST and at every node, generates the appropriate output
    node object.

    Adding new grammar rules
    ------------------------
    Add a converter under the expr rule to handle the new node types for the 
    new rules.  Make sure the children of the node types are declared as expr
    to ensure that the walker will invoke the expr rule on all the children.
*/
start returns [Expression value]
    : a=expr EOF { $value = a; }
    ;

expr returns [Expression value]
@init {
    ArrayList<Expression> varargs = new ArrayList<Expression>();
}
    : ^(PREVIOUSMESSAGETO a=expr b=expr)                  { $value = new DefaultCompoundSyntaxTree("previous message to . from .", a, b); }
    | ^(MESSAGETO a=expr b=expr)                          { $value = new DefaultCompoundSyntaxTree("message to . from .", a, b); }
    | ^(SINGLE_ARROW a=expr b =expr)                      { $value = new DefaultCompoundSyntaxTree("->", a, b); }
    | ^(LAMBDA a=expr b=expr)                             { $value = new DefaultCompoundSyntaxTree("lambda . : .", a, b); }
    | ^(IFTHENELSE a=expr b=expr c=expr)                  { $value = new DefaultCompoundSyntaxTree("if . then . else .", a, b, c); }
    | ^(FORALL a=expr b=expr)                             { $value = new DefaultCompoundSyntaxTree("for all . : .", a, b); }
    | ^(THEREEXISTS a=expr b=expr)                        { $value = new DefaultCompoundSyntaxTree("there exists . : .", a, b); }
    | ^(ARROW (a=expr {varargs.add(a); })*)               { $value = new DefaultCompoundSyntaxTree("=>", varargs); }
    | ^(DOUBLE_ARROW (a=expr {varargs.add(a); })*)        { $value = new DefaultCompoundSyntaxTree("<=>", varargs); }
    | ^(OR (a=expr {varargs.add(a); })*)                  { $value = new DefaultCompoundSyntaxTree("or", varargs); }
    | ^(AND (a=expr {varargs.add(a); })*)                 { $value = new DefaultCompoundSyntaxTree("and", varargs); }
    | ^(IS (a=expr {varargs.add(a); })*)                  { $value = new DefaultCompoundSyntaxTree("is", varargs); }
    | ^(EQUAL (a=expr {varargs.add(a); })*)               { $value = new DefaultCompoundSyntaxTree("=", varargs); }
    | ^(NOT_EQUAL (a=expr {varargs.add(a); })*)           { $value = new DefaultCompoundSyntaxTree("!=", varargs); }
    | ^(GREATER_THAN (a=expr {varargs.add(a); })*)        { $value = new DefaultCompoundSyntaxTree(">", varargs); }
    | ^(GREATER_THAN_EQUAL (a=expr {varargs.add(a); })*)  { $value = new DefaultCompoundSyntaxTree(">=", varargs); }
    | ^(LESS_THAN (a=expr {varargs.add(a); })*)           { $value = new DefaultCompoundSyntaxTree("<", varargs); }
    | ^(LESS_THAN_EQUAL (a=expr {varargs.add(a); })*)     { $value = new DefaultCompoundSyntaxTree("<=", varargs); }
    | ^(IN (a=expr {varargs.add(a); })*)                  { $value = new DefaultCompoundSyntaxTree("in", varargs); }
    | ^(UNION (a=expr {varargs.add(a); })*)               { $value = new DefaultCompoundSyntaxTree("union", varargs); }
    | ^(INTERSECTION (a=expr {varargs.add(a); })*)        { $value = new DefaultCompoundSyntaxTree("intersection", varargs); }
    | ^(PLUS (a=expr {varargs.add(a); })*)                { $value = new DefaultCompoundSyntaxTree("+", varargs); }
    | ^(DASH (a=expr {varargs.add(a); })*)                { $value = new DefaultCompoundSyntaxTree("-", varargs); }
    | ^(MINUS (a=expr {varargs.add(a); })*)               { $value = new DefaultCompoundSyntaxTree("minus", varargs); }
    | ^(TIMES (a=expr {varargs.add(a); })*)               { $value = new DefaultCompoundSyntaxTree("*", varargs); }
    | ^(DIVIDE (a=expr {varargs.add(a); })*)              { $value = new DefaultCompoundSyntaxTree("/", varargs); }
    | ^(CARAT (a=expr {varargs.add(a); })*)               { $value = new DefaultCompoundSyntaxTree("^", varargs); }
//    | ^(MINUS a=expr)                                     { $value = new DefaultCompoundSyntaxTree("-", a); }
    | ^(NOT (a=expr {varargs.add(a); })*)                 { $value = new DefaultCompoundSyntaxTree("not", varargs); }
    | ^(CASE (a=expr {varargs.add(a); })*)                { $value = new DefaultCompoundSyntaxTree("case", varargs); }
    | ^(INDEX (a=expr {varargs.add(a); })*)               { $value = new DefaultCompoundSyntaxTree("index of . in .", varargs); }
    | ^(OCCURS (a=expr {varargs.add(a); })*)              { $value = new DefaultCompoundSyntaxTree("occurs in", varargs); }
    | ^(UNDERSCORESET (a=expr {varargs.add(a); })*)       { $value = new DefaultCompoundSyntaxTree(". _{ . : . }", varargs); }
    | ^(UNDERSCORE (a=expr {varargs.add(a); })*)          { $value = new DefaultCompoundSyntaxTree("_", varargs); }
    | ^(NEIGHBORSOF a=expr b=expr)                        { $value = new DefaultCompoundSyntaxTree("neighbors of . from .", a, b); }
    | ^(NEIGHBORSOFVARIABLE a=expr)                       { $value = new DefaultCompoundSyntaxTree("neighbors of variable", a); }
    | ^(NEIGHBORSOFFACTOR a=expr)                         { $value = new DefaultCompoundSyntaxTree("neighbors of factor", a); }
    | ^(VALUEOF a=expr)                                   { $value = new DefaultCompoundSyntaxTree("value of", a); }
    | ^(SQUAREBRACKET a=expr)                             { $value = new DefaultCompoundSyntaxTree("[ . ]", a); }
    | ^(SET a=expr)                                       { $value = new DefaultCompoundSyntaxTree("{ . }", a); }
    | ^(SETCOMPREHENSION1 a=expr b=expr)                  { $value = new DefaultCompoundSyntaxTree("{ . . . }", a, b, null); }
    | ^(SETCOMPREHENSION2 a=expr b=expr c=expr)           { $value = new DefaultCompoundSyntaxTree("{ . . . }", a, b, c); }
    | ^(SETCOMPREHENSION3 a=expr b=expr)                  { $value = new DefaultCompoundSyntaxTree("{ . . . }", null, a, b); }
    | ^(MULTISET a=expr)                                  { $value = new DefaultCompoundSyntaxTree("{{ . }}", a); }
    | ^(MULTISETCOMPREHENSION1 a=expr b=expr)             { $value = new DefaultCompoundSyntaxTree("{{ . . . }}", a, b, null); }
    | ^(MULTISETCOMPREHENSION2 a=expr b=expr c=expr)      { $value = new DefaultCompoundSyntaxTree("{{ . . . }}", a, b, c); }
    | ^(MULTISETCOMPREHENSION3 a=expr b=expr)             { $value = new DefaultCompoundSyntaxTree("{{ . . . }}", null, a, b); }
    | ^(SEQUENCE a=expr)                                  { $value = new DefaultCompoundSyntaxTree("| . |", a); }
    | ^(TUPLE a=expr)                                     { $value = new DefaultCompoundSyntaxTree("( . )", a); }
    | ^(FUNCTION a=expr (b=expr {varargs.add(b); })*)     { $value = new DefaultCompoundSyntaxTree(a, varargs); }
    | ^(SYMBOL ID)                                        { $value = DefaultSymbol.createSymbol($ID.text); }
    | ^(SYMBOL_EXPRESSION a=expr)                         { $value = DefaultSymbol.createSymbol(a); }
    | ^(KLEENE (a=expr { varargs.add(a); })*)             { $value = new DefaultCompoundSyntaxTree("kleene list", varargs); }
    | ^(COMPREHENSION_ON a=expr)                          { $value = new DefaultCompoundSyntaxTree("( on . )", a); }
    | ^(COMPREHENSION_VERT_BAR a=expr)                    { $value = new DefaultCompoundSyntaxTree("|", a); }
    | ^(COLON a=expr b=expr)                              { $value = new DefaultCompoundSyntaxTree(":", a, b); }
    ;


