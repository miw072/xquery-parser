/**
 * Define a grammar called Hello
 */
grammar xpath;


ap  : 'doc' '(' ID ')' '/' rp          #apsl
	| 'doc' '(' ID ')' '//' rp         #apdsl
	;

rp	: '*'							    #rpchildren
	| ID                                #rptag
	| '.'							    #rpcurr
	| '..'								#rpparent
	| '(' rp ')'						#rpin
	| rp '/' rp							#rpsrp
	| rp '//' rp						#rpdsrp
	| rp '[' fun ']'					#rpfilter	
	| rp ',' rp							#rpandrp	
	| 'text()'							#rptext
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

ID : [a-zA-Z0-9._]+;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines

