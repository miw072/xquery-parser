/**
 * Define a grammar called Hello
 */
grammar xpath;


ap  	: 'doc' '(' ID ')' '/' rp
	| 'doc' '(' ID ')' '//' rp 
	;

rp	: '*'
	| ID
	| '.'
	| '..'
	| '(' rp ')'
	| rp '/' rp
	| rp '//' rp
	| rp '[' fun ']'
	| rp ',' rp
	| 'text()'
	;

fun	: rp
	| rp '=' rp
	| rp 'eq' rp
	| rp '==' rp
	| rp 'is' rp
	| '(' fun ')'
	| fun 'and' fun
	| fun 'or' fun
	| 'not' fun
	;

/*
 * Tokens
 */


DOCUMENT : 'doc';
TXT : 'text()';
EQ : 'eq';
IS : 'is';
AND: 'and';
OR: 'or';
NOT: 'not';

WILD : '*';
COMMA: ',';
DOT: '.';
DDOT: '..';
EQSYM: '=';
ISSYM: '==';
SLASH: '/';
DSLASH: '//';
LPAREN: '(';
RPAREN: ')';

ID : [a-zA-Z0-9]+;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines

