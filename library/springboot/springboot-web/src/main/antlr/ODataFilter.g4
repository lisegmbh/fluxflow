grammar ODataFilter;

filter
    : expr EOF
    ;

expr
    : orExpr
    ;

orExpr
    : andExpr (OR andExpr)*
    ;

andExpr
    : notExpr (AND notExpr)*
    ;

notExpr
    : NOT notExpr
    | inExpr
    | comparisonExpr
    ;

comparisonExpr
    : additiveExpr (compOp additiveExpr)?
    ;

additiveExpr
    : primaryExpr
    ;

primaryExpr
    : literal
    | member
    | functionCall
    | LPAREN expr RPAREN
    ;

inExpr
    : additiveExpr IN LPAREN argumentList RPAREN
    ;

functionCall
    : FUN LPAREN argumentList? RPAREN
    ;

argumentList
    : expr (COMMA expr)*
    ;

member
    : IDENTIFIER (SLASH IDENTIFIER)*
    ;

literal
    : STRING
    | NUMBER
    | BOOLEAN
    | NULL
    ;

compOp
    : EQ
    | NE
    | GT
    | GE
    | LT
    | LE
    ;

AND     : 'and';
OR      : 'or';
NOT     : 'not';
EQ      : 'eq';
NE      : 'ne';
GT      : 'gt';
GE      : 'ge';
LT      : 'lt';
LE      : 'le';
IN      : 'in';
NULL    : 'null';
BOOLEAN : 'true' | 'false';

LPAREN  : '(';
RPAREN  : ')';
COMMA   : ',';
SLASH   : '/';

FUN
    : 'contains'
    ;

IDENTIFIER
    : [A-Za-z_] ([A-Za-z0-9_] | '-' [A-Za-z0-9_])*
    ;

NUMBER
    : '-'? [0-9]+ ('.' [0-9]+)?
    ;

STRING
    : '\'' ( '\'\'' | ~'\'' )* '\''
    ;

WS
    : [ \t\r\n]+ -> skip
    ;