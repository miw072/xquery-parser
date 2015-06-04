/**
 * Define a grammar called Hello
 */
grammar xpath;
@lexer::members {
    boolean ignore=true;
}

xq  : Var 												#xqVar
	| String 									#xqString
	| '('xq ')'											#xqin
	| ap												#xqap
	| xq ',' xq											#xqandxq
	| xq '/' rp											#xqslrp
	| xq '//'rp                                         #xqdslrp
	| '<' ID '>' '{'xq'}' '<''/' ID '>'					#xqConstruct
	| forClause (letClause)? whereClause returnClause		#xqflwr
	| letClause xq										#xqlet
	| joinClause										#xqjoin
	;


joinClause : 'join' '(' xq ',' xq ',' jvar ',' jvar ')';
jvar: '[' ID (',' ID)* ']';
forClause 	: 'for' Var 'in' xq  (',' Var 'in' xq)*
		  	;

letClause 	: 'let' Var ':=' xq (',' Var ':=' xq)*		#let  
			//|											#letempty
			; 

whereClause : 'where' cond								#where
			|											#whereempty
			;

returnClause: 'return' xq								
			;
			
cond		: xq '=' xq									#condeqsym
			| xq 'eq' xq								#condeq
			| xq '==' xq								#condissym
			| xq 'is' xq								#condis
			| 'empty' '(' xq ')'						#condept
			| 'some' Var 'in' xq (',' Var 'in' xq)* 'satisfies' cond	#condsome
			| '('cond')'								#condin
			| cond 'and' cond							#condand
			| cond 'or' cond							#condor
			| 'not' cond								#condnot
			;		


ap  : 'document' '(' String ')' '/' rp          #apsl
	| 'document' '(' String ')' '//' rp         #apdsl
	;


rp	: '*'							    #rpchildren
	| ID                           #rptag
	| '.'							    #rpcurr
	| '..'								#rpparent
	| '(' rp ')'						#rpin
	| rp '/' rp							#rpsrp
	| rp '//' rp						#rpdsrp
	| rp '[' fun ']'					#rpfilter	
	| rp ',' rp							#rpandrp	
	| 'text()'							#rptext
	| '@'ID								#rpattr 
	;

fun	: rp								#filterrp
	| rp '=' rp							#filtereqsym
	| rp 'eq' rp						#filtereq
	| rp '==' rp						#filterissym
	| rp 'is' rp						#filteris
	| '(' fun ')'						#filterin
	| fun 'and' fun						#filterand
	| fun 'or' fun						#filteror
	| 'not' fun							#filternot
	;

/*
 * Tokens
 */


DOCUMENT : 'document';
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
QIAN: '$';
WS : [ \t\r\n]+ { if(ignore) skip(); } ; // skip spaces, tabs, newlines

Var : '$' ID;
ID : [a-zA-Z0-9._!]+;
String : '"' [a-zA-Z0-9._! ,]+ '"';