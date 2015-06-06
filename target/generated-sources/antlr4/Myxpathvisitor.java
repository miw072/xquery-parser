import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.*;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.*;
import java.io.*;

import org.w3c.dom.*;
public class Myxpathvisitor  extends xpathBaseVisitor<ArrayList<Node>> {
	ArrayList<Node> currentList;
	DOMParser parser;
	Document document;
	Stack<HashMap<String, ArrayList<Node>>> ctxStack;

public Myxpathvisitor(){
	currentList = new ArrayList<Node>();
	parser = new DOMParser();
	ctxStack = new Stack<HashMap<String, ArrayList<Node>>>();
    try{
    	DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
    	document = domBuilder.newDocument();
    }catch (Exception e){
    	System.out.println("Error creating document!");
    }
}
/***********************************************************************
* Xquery
***********************************************************************/	
// xqVar
public ArrayList<Node> visitXqVar(xpathParser.XqVarContext ctx) {
	if (ctxStack.empty()){
		System.out.println("No Context Set!");
		System.exit(1);
	}
	String varKey = ctx.Var().getText();
	HashMap<String,ArrayList<Node>> currCtxMap = ctxStack.peek();
	if (!currCtxMap.containsKey(varKey)){
		System.out.println(varKey);
		System.out.println("No such Variable!");
		System.exit(1);
	}
	return currCtxMap.get(varKey);
}
// xq in	
public ArrayList<Node> visitXqin(xpathParser.XqinContext ctx) {
	return visit(ctx.xq());
}	

//xq ap
public ArrayList<Node> visitXqap(xpathParser.XqapContext ctx) {
	return visit(ctx.ap());
}


private ArrayList<Node> flwr(int level,int num,ArrayList<Node> curr, String name, xpathParser.XqflwrContext ctx) {
	HashMap<String,ArrayList<Node>> currMap = getCtxMap();
	ArrayList<Node> item;
	ArrayList<Node> result = new ArrayList<Node>();
	if (level>=(num-1)){
		for (int i=0;i<curr.size();i++){
			item = new ArrayList<Node>();
			item.add(curr.get(i));
			currMap.put(name, item);
			ctxStack.push(currMap);
			if (ctx.letClause()!=null){
				visit(ctx.letClause());
			}
			ArrayList<Node> condResult = visit(ctx.whereClause());
			
			if (condResult.size()!=0){
				result.addAll(visit(ctx.returnClause()));
			}
			if (ctx.letClause()!=null) {
				ctxStack.pop(); // let clause	
			}		
			ctxStack.pop();	
		}
	}
	else{

		for (int i=0;i<curr.size();i++){
			item = new ArrayList<Node>();
			item.add(curr.get(i));
			currMap.put(name, item);
			ctxStack.push(currMap);
			ArrayList<Node> nextVar = visit(ctx.forClause().xq(level+1));
			String nextName = ctx.forClause().Var(level+1).getText();
			result.addAll(flwr(level+1,num,nextVar,nextName,ctx));
			ctxStack.pop();
		}
	}
	return result;
}

/*****************************************************************************************
 * JOIN down
 *****************************************************************************************/
public ArrayList<Node> visitXqjoin( xpathParser.XqjoinContext ctx){
	ArrayList<Node> result = visit(ctx.joinClause());
	return result;
}

public ArrayList<Node> visitJoinClause(xpathParser.JoinClauseContext ctx){
	ArrayList<Node> result = new ArrayList<Node>();
	ArrayList<Node> xq1 = visit(ctx.xq(0));
	ArrayList<Node> xq2 = visit(ctx.xq(1));
	ArrayList<String> var1 = new ArrayList<String>();
	ArrayList<String> var2 = new ArrayList<String>();
	int num1 = ctx.jvar(0).ID().size();
	int num2 = ctx.jvar(1).ID().size();
	if (num1!=num2){
		System.exit(-1);
	}
	
	for (int i=0;i<num1;i++){
		var1.add(ctx.jvar(0).ID(i).getText());
		var2.add(ctx.jvar(1).ID(i).getText());
	}
	
	ArrayList<Integer> varIndex1 = findIndex(xq1.get(0).getChildNodes(),var1);
	ArrayList<Integer> varIndex2 = findIndex(xq2.get(0).getChildNodes(),var2);
	
	HashMap<String, ArrayList<Node>> map = fillMap(xq1, varIndex1.get(0));
	
	//first join
	for (Node curr : xq2){
		String key = curr.getChildNodes().item(varIndex2.get(0)).getTextContent();
		if (map.containsKey(key)){
			ArrayList<Node> joinList = map.get(key);
			
			for (Node toAdd : joinList){
				result.add(crossProduct(curr, toAdd));
			}
		}
	}
	
	//joinsize > 1, process remaining
	if (num1 > 1){
		for (Node toCheck : result){
			boolean error = false;
			NodeList child = toCheck.getChildNodes();
			
			for (int i = 1; i < num1; i++){
				int index1 = varIndex1.get(i);
				int index2 = varIndex2.get(i) + xq2.get(0).getChildNodes().getLength();
				
				String s1 = child.item(index1).getTextContent();
				String s2 = child.item(index2).getTextContent();
				
				if (!s1.equals(s2)){
					error = true;
				}
			}
			
			if (error){
				result.remove(toCheck);
			}
		}
	}
	
	
	return result;
}

private Node crossProduct(Node tuple,Node toAdd){
	NodeList l1 = tuple.getChildNodes();
	NodeList l2 = toAdd.getChildNodes();
	//System.out.println("l1's length: " + l1.getLength());
	//System.out.println("l2's length: " + l2.getLength());
	Element curr = document.createElement("tuple");
	
	while (l1.getLength() > 0){
		curr.appendChild(l1.item(0));
	}
	
	while (l2.getLength() > 0){
		curr.appendChild(l2.item(0));
	}
	
	//System.out.println("curr's length: " + curr.getChildNodes().getLength());
	
	return curr;
	
}


private HashMap<String, ArrayList<Node>> fillMap( ArrayList<Node> nodes, int index) {
	HashMap<String, ArrayList<Node>> result = new HashMap<String, ArrayList<Node>>();
	for (Node node : nodes){
		String name = node.getChildNodes().item(index).getTextContent();
		if (result.containsKey(name)){
			result.get(name).add(node);
		}
		else {
			ArrayList<Node> newitem = new ArrayList<Node> ();
			newitem.add(node);
			result.put(name,newitem);
		}
	}
	return result;
	
}

private ArrayList<Integer> findIndex(NodeList nodes, ArrayList<String> var){
	ArrayList<Integer> result = new ArrayList<Integer>();
	for (int j=0;j<var.size();j++){
		String name = var.get(j);
		int i=0;
		for (; i< nodes.getLength(); i++){
			 if (nodes.item(i).getNodeName().equals(name)){
				 result.add(i);
				 break;
			 }
		}
		if (i==nodes.getLength()){
			System.exit(-1);
		}
	}
	return result;
}

/*****************************************************************************************
 * JOIN above
 *****************************************************************************************/

//xq flwr
public ArrayList<Node> visitXqflwr(xpathParser.XqflwrContext ctx) {
	int numVar = ctx.forClause().Var().size();
	ArrayList<Node> startVar = visit(ctx.forClause().xq(0));
	String varName = ctx.forClause().Var(0).getText();
	return flwr(0,numVar,startVar,varName, ctx);
}

//xq make Construct
public ArrayList<Node> visitXqConstruct(@NotNull xpathParser.XqConstructContext ctx) {
	ArrayList<Node> children = visit(ctx.xq());
	String name = ctx.ID().get(0).getText(); //fix me tag == /tag
	Element curr = document.createElement(name);
	for (int i=0; i<children.size();i++){
		if (children.get(i)!=null && children.get(i).getNodeType()!=Node.ATTRIBUTE_NODE){
			if (children.get(i).getNodeType()!=Node.DOCUMENT_NODE)
				curr.appendChild(document.importNode(children.get(i), true));
			else {
				Node temp = children.get(i).getFirstChild();
				curr.appendChild(document.importNode(temp, true));
			}
		}
	}
	ArrayList<Node> result = new ArrayList<Node>();
	result.add(curr);
	return result;
}

//xq xq make text
public ArrayList<Node> visitXqString(@NotNull xpathParser.XqStringContext ctx){
	String name = ctx.String().getText(); //fix me tag == /tag
	name = name.substring(1, name.length()-1);
	Text curr = document.createTextNode(name);
	ArrayList<Node> result = new ArrayList<Node>();
	result.add(curr);
	return result;
}


//xq xqlet
public ArrayList<Node> visitXqlet(@NotNull xpathParser.XqletContext ctx){
	visit(ctx.letClause());
	ArrayList<Node> result = new ArrayList<Node>();
	result.addAll(visit(ctx.xq()));
	ctxStack.pop();
	return result;
}

//xq xq/rp
public ArrayList<Node> visitXqslrp(@NotNull xpathParser.XqslrpContext ctx) {
	ArrayList<Node> xqresult = visit(ctx.xq());
	setContext(currentList,xqresult);
	return visit(ctx.rp());	
}

// xq xq//rp
public ArrayList<Node> visitXqdslrp(@NotNull xpathParser.XqdslrpContext ctx) {
	ArrayList<Node> xqresult = visit(ctx.xq());
	setContext(currentList,xqresult);
	ArrayList<Node> context = new ArrayList<Node> (currentList);
	ArrayList<Node> temp = getChildren();
	while(temp.size()>0){
		context.addAll(temp);
		temp = getChildren();
	}
	setContext(currentList,context);
	xqresult = visit(ctx.rp());
	setContext(currentList,unique(xqresult));	
	return unique(xqresult);
	
}


//xq xqandxq
public ArrayList<Node> visitXqandxq(@NotNull xpathParser.XqandxqContext ctx) {
	ArrayList<Node> result1 = visit(ctx.xq(0));
	ArrayList<Node> result2 = visit(ctx.xq(1));
	ArrayList<Node> result =new ArrayList<Node>(result1);
	result.addAll(result2);
	return result;
}
/***********************************************************************
* Clauses
***********************************************************************/
/*public ArrayList<Node> visitForClause(@NotNull xpathParser.ForClauseContext ctx) {
	int num = ctx.Var().size();
	HashMap<String,ArrayList<Node>> currCtxMap = getCtxMap();
	ArrayList<Node> result = new ArrayList<Node>();
	for (int i=0;i<num;i++){
		String varName = ctx.Var(i).getText();
		result = visit(ctx.xq(i));
		currCtxMap.put(varName, result);
	}
	ctxStack.push(currCtxMap);
	return result;	
}	
*/
public ArrayList<Node> visitLet(xpathParser.LetContext ctx) {
	int num = ctx.Var().size();
	HashMap<String,ArrayList<Node>> currCtxMap =getCtxMap();
	ArrayList<Node> result = new ArrayList<Node>();
	ctxStack.push(currCtxMap);
	for (int i=0;i<num;i++){
		String varName = ctx.Var(i).getText();
		result = visit(ctx.xq(i));
		currCtxMap.put(varName, result);
	}
	return result;	
}

/*public ArrayList<Node> visitLetempty(xpathParser.LetemptyContext ctx){
	HashMap<String,ArrayList<Node>> currCtxMap =getCtxMap();
	ArrayList<Node> result = new ArrayList<Node>();
	ctxStack.push(currCtxMap);
	return result;
}*/

public ArrayList<Node> visitWhere(xpathParser.WhereContext ctx) {
	return visit(ctx.cond());
}

public ArrayList<Node> visitWhereempty(xpathParser.WhereemptyContext ctx) {
	ArrayList<Node> result = new ArrayList<Node>();
	result.add(document);
	return result;
}

public ArrayList<Node> visitReturnClause(xpathParser.ReturnClauseContext ctx) {
	return visit(ctx.xq());
}

/***********************************************************************
 * COND 
 ***********************************************************************/	
//cond is
public ArrayList<Node> visitCondis(xpathParser.CondisContext ctx){
	//ArrayList<Node> originList = new ArrayList<Node>(currentList);
	ArrayList<Node> result0 = visit(ctx.xq(0));
	//setContext(currentList,originList);
	ArrayList<Node> result1 = visit(ctx.xq(1));
	//setContext(currentList,originList);
	if (isequal(result0,result1)){
		return result0;
	}
	result0.clear();
	return result0;
}

//cond is sym
public ArrayList<Node> visitCondissym(xpathParser.CondissymContext ctx){
	//ArrayList<Node> originList = new ArrayList<Node>(currentList);
	ArrayList<Node> result0 = visit(ctx.xq(0));
	//setContext(currentList,originList);
	ArrayList<Node> result1 = visit(ctx.xq(1));
	//setContext(currentList,originList);
	if (isequal(result0,result1)){
		return result0;
	}
	result0.clear();
	return result0;
}

//cond eq sym
public ArrayList<Node> visitCondeqsym( xpathParser.CondeqsymContext ctx) {
	//ArrayList<Node> originList = new ArrayList<Node>(currentList);
	ArrayList<Node> result0 = visit(ctx.xq(0));
	//setContext(currentList,originList);
	ArrayList<Node> result1 = visit(ctx.xq(1));
	//setContext(currentList,originList);
	if (copyequal(result0,result1)){
		return result0;
	}
	result0.clear();
	return result0;
}

//cond eq
public ArrayList<Node> visitCondeq( xpathParser.CondeqContext ctx) {
	//ArrayList<Node> originList = new ArrayList<Node>(currentList);
	ArrayList<Node> result0 = visit(ctx.xq(0));
	//setContext(currentList,originList);
	ArrayList<Node> result1 = visit(ctx.xq(1));
	//setContext(currentList,originList);
	if (copyequal(result0,result1)){
		return result0;
	}
	result0.clear();
	return result0;
}

//cond empty
public ArrayList<Node> visitCondept( xpathParser.CondeptContext ctx){
	//ArrayList<Node> originList = new ArrayList<Node>(currentList);
	ArrayList<Node> result = visit(ctx.xq());
	//setContext(currentList,originList);
	if (result.size()>0) result.clear();
	else result.add(document);
	return result;
}

//cond in
public ArrayList<Node> visitCondin(xpathParser.CondinContext ctx){
	return visit(ctx.cond());
}

//cond and
public ArrayList<Node> visitCondand(xpathParser.CondandContext ctx){
	ArrayList<Node> result0 = visit(ctx.cond(0));
	ArrayList<Node> result1 = visit(ctx.cond(1));
	if (result0.size()==0){
		return result0;
	}
	else if (result1.size()==0){
		return result1;
	}
	return result0;
}

//cond or
public ArrayList<Node> visitCondor(xpathParser.CondorContext ctx){
	ArrayList<Node> result0 = visit(ctx.cond(0));
	ArrayList<Node> result1 = visit(ctx.cond(1));
	if (result0.size()!=0){
		return result0;
	}
	else if (result1.size()!=0){
		return result1;
	}
	return result0;
}

//cond not
public ArrayList<Node> visitCondnot(xpathParser.CondnotContext ctx){
	ArrayList<Node> result = visit(ctx.cond());
	if (result.size()>0) result.clear();
	else result.add(document);
	return result;
}

//helper function for some
private ArrayList<Node> some(int level, int num, ArrayList<Node> curr, String name, xpathParser.CondsomeContext ctx){
	HashMap<String,ArrayList<Node>> currMap = getCtxMap();
	ArrayList<Node> item;
	ArrayList<Node> result = new ArrayList<Node>();
	if (level>=(num-1)){
		for (int i=0;i<curr.size();i++){
			item = new ArrayList<Node>();
			item.add(curr.get(i));
			currMap.put(name, item);
			ctxStack.push(currMap);	
			ArrayList<Node> condResult = visit(ctx.cond());
			
			if (condResult.size()!=0){
				result.addAll(condResult);
			}			
			ctxStack.pop();	
		}
	}
	else{

		for (int i=0;i<curr.size();i++){
			item = new ArrayList<Node>();
			item.add(curr.get(i));
			currMap.put(name, item);
			ctxStack.push(currMap);
			ArrayList<Node> nextVar = visit(ctx.xq(level+1));
			String nextName = ctx.Var(level+1).getText();
			result = some(level+1,num,nextVar,nextName,ctx);
			ctxStack.pop();
			if (result.size() != 0){
				break;
			}
		}
	}
	return result;
}

//cond some
public ArrayList<Node> visitCondsome(xpathParser.CondsomeContext ctx){
	int numVar = ctx.Var().size();
	ArrayList<Node> startVar = visit(ctx.xq(0));
	String varName = ctx.Var(0).getText();
	return some(0,numVar,startVar,varName, ctx);
}
/***********************************************************************
 * AP 
 ***********************************************************************/	
	//absolute path slash
	public ArrayList<Node> visitApsl(xpathParser.ApslContext ctx){
		String xmlFile = ctx.String().getText();
		xmlFile = xmlFile.substring(1, xmlFile.length()-1);
		//System.out.println("Enter apsl");
		Document mydocument;
		try {
	         parser.parse(xmlFile);
	         mydocument = parser.getDocument();
	 		 currentList.add(mydocument);
	    } catch (SAXException e) {
	         System.err.println (e);
	    } catch (IOException e) {
	         System.err.println (e);
	    }
		return visit(ctx.rp());
	}
	
	//absolute path double slash
	public ArrayList<Node> visitApdsl(xpathParser.ApdslContext ctx){
		String xmlFile = ctx.String().getText();
		xmlFile = xmlFile.substring(1, xmlFile.length()-1);
		//System.out.println("Enter apdsl");
		Document mydocument;
		try {
	         parser.parse(xmlFile);
	         mydocument = parser.getDocument();
	 		 currentList.add(mydocument);
	    } catch (SAXException e) {
	         System.err.println (e);
	    } catch (IOException e) {
	         System.err.println (e);
	    }
		ArrayList<Node> context = new ArrayList<Node> (currentList);
		ArrayList<Node> temp = getChildren();	
		while(temp.size()>0){
			context.addAll(temp);
			temp = getChildren();
		}
		setContext(currentList,context);
		return visit(ctx.rp());
	} 
	
/***********************************************************************
 * RP 
 ***********************************************************************/	
	
	//relative path tag
	public ArrayList<Node> visitRptag(xpathParser.RptagContext ctx){
		//System.out.println("Enter rptag");
		ArrayList<Node> result = new ArrayList<Node>();
		String tagName = ctx.ID().getText();
		for (int i = 0; i < currentList.size(); i++){
			NodeList curr = currentList.get(i).getChildNodes();
			for (int j = 0; j < curr.getLength(); j++){
				if (curr.item(j).getNodeType() == Node.ELEMENT_NODE){
					if (curr.item(j).getNodeName().equalsIgnoreCase(tagName)){
						result.add(curr.item(j));
					}
				}
			}
		}
		setContext(currentList,result);
		return result;
	}
	
	//relative path slash
	public ArrayList<Node> visitRpsrp(xpathParser.RpsrpContext ctx){
		//System.out.println("Enter rpsrp");
		ArrayList<Node> result = new ArrayList<Node>();
		result = visit(ctx.rp(0));
		setContext(currentList,result);
		result = visit(ctx.rp(1));
		setContext(currentList,unique(result));
		
		return unique(result);
	}
	
	//relative path double slash rp
	public ArrayList<Node> visitRpdsrp(xpathParser.RpdsrpContext ctx) {
		//System.out.println("Enter rpdsrp");
		ArrayList<Node> result = new ArrayList<Node>();
		result = visit(ctx.rp(0));
		setContext(currentList,result);
		ArrayList<Node> context = new ArrayList<Node> (currentList);
		ArrayList<Node> temp = getChildren();
		while(temp.size()>0){
			context.addAll(temp);
			temp = getChildren();
		}
		setContext(currentList,context);
		result = visit(ctx.rp(1));
		setContext(currentList,unique(result));
		
		return unique(result);
	}
	
	// relative path ()
	public ArrayList<Node> visitRpin(xpathParser.RpinContext ctx) {
		return visit(ctx.rp());
	} 
	//relative path current
	public ArrayList<Node> visitRpcurr(xpathParser.RpcurrContext ctx){
		ArrayList<Node> result = new ArrayList<Node>(currentList);
		return result;
	}
	
	//relative path star(children)
	public ArrayList<Node> visitRpchildren(xpathParser.RpchildrenContext ctx){
		return getChildren();
	}
	
	//relative path text()
	public ArrayList<Node> visitRptext(xpathParser.RptextContext ctx){
		return getText();
	}
	
	//relative path ..(parent)
	public ArrayList<Node> visitRpparent(xpathParser.RpparentContext ctx){
		return getParent();
	}
	
	//relative path comma(and)
	public ArrayList<Node> visitRpandrp(xpathParser.RpandrpContext ctx){
		ArrayList<Node> originList = new ArrayList<Node>(currentList);
		ArrayList<Node> result0 = new ArrayList<Node>();
		ArrayList<Node> result1 = new ArrayList<Node>();
		result0 = visit(ctx.rp(0));
		setContext(currentList,originList);
		result1 = visit(ctx.rp(1));
		ArrayList<Node> result = new ArrayList<Node>();
		result.addAll(result0);
		result.addAll(result1);
		setContext(currentList,result);
		return result;
	}
	
	//relative path filter
	public ArrayList<Node> visitRpfilter(xpathParser.RpfilterContext ctx) {
		//System.out.println("Enter rpfilter");
		ArrayList<Node> result = new ArrayList<Node>();
		visit(ctx.rp());
		ArrayList<Node> curr = new ArrayList<Node>(currentList);
		for (int i=0;i<curr.size();i++){
			setContext(currentList,curr.get(i));
			if (visit(ctx.fun()).size()>0){
				result.add(curr.get(i));
			}
		}
		setContext(currentList,result);
		return result;
	}
	
	//relative path attribute
	public ArrayList<Node> visitRpattr(xpathParser.RpattrContext ctx){
		ArrayList<Node> result = new ArrayList<Node>();
		String attrName = ctx.ID().getText();
		for (int i = 0; i < currentList.size(); i++){
			Node a = currentList.get(i).getAttributes().getNamedItem(attrName);
			if (a != null) result.add(a);
		}
		setContext(currentList,result);
		return result;		
	}
/***********************************************************************
 * filter 
 ***********************************************************************/
	// filter filter[rp]
	public ArrayList<Node> visitFilterrp(xpathParser.FilterrpContext ctx) {
		//System.out.println("Enter rpfiler");
		ArrayList<Node> originList = new ArrayList<Node>(currentList);
		ArrayList<Node> result = visit(ctx.rp());
		setContext(currentList,originList);
		return result;
	}
	// filter not filter
	public ArrayList<Node> visitFilternot(xpathParser.FilternotContext ctx){
		//System.out.println("Enter rpfilernot");
		ArrayList<Node> originList = new ArrayList<Node>(currentList);
		ArrayList<Node> result = visit(ctx.fun());
		setContext(currentList,originList);
		if (result.size()>0) result.clear();
		else result.add(document);
		return result;
	}
	// filter (filter)
	public ArrayList<Node> visitFilterin(xpathParser.FilterinContext ctx){
		return visit(ctx.fun());		
	}
	// filter filter and filter
	public ArrayList<Node> visitFilterand(xpathParser.FilterandContext ctx){
		ArrayList<Node> result0 = visit(ctx.fun(0));
		ArrayList<Node> result1 = visit(ctx.fun(1));
		if (result0.size()==0){
			return result0;
		}
		else if (result1.size()==0){
			return result1;
		}
		return result0;
	}
	//filter filter or filter
	public ArrayList<Node> visitFilteror(xpathParser.FilterorContext ctx) {
		ArrayList<Node> result0 = visit(ctx.fun(0));
		ArrayList<Node> result1 = visit(ctx.fun(1));
		if (result0.size()!=0){
			return result0;
		}
		else if (result1.size()!=0){
			return result1;
		}
		return result0;
	}
	
	//filter rp == rp
	public ArrayList<Node> visitFilterissym(xpathParser.FilterissymContext ctx) {
		ArrayList<Node> originList = new ArrayList<Node>(currentList);
		ArrayList<Node> result0 = visit(ctx.rp(0));
		setContext(currentList,originList);
		ArrayList<Node> result1 = visit(ctx.rp(1));
		setContext(currentList,originList);
		if (isequal(result0,result1)){
			return result0;
		}
		result0.clear();
		return result0;
	} 
	
	//filter rp is rp
	public ArrayList<Node> visitFilteris(xpathParser.FilterisContext ctx) {
		ArrayList<Node> originList = new ArrayList<Node>(currentList);
		ArrayList<Node> result0 = visit(ctx.rp(0));
		setContext(currentList,originList);
		ArrayList<Node> result1 = visit(ctx.rp(1));
		setContext(currentList,originList);
		if (isequal(result0,result1)){
			return result0;
		}
		result0.clear();
		return result0;
	}
	//filter rp = rq
	public ArrayList<Node> visitFiltereqsym( xpathParser.FiltereqsymContext ctx) {
		ArrayList<Node> originList = new ArrayList<Node>(currentList);
		ArrayList<Node> result0 = visit(ctx.rp(0));
		setContext(currentList,originList);
		ArrayList<Node> result1 = visit(ctx.rp(1));
		setContext(currentList,originList);
		if (copyequal(result0,result1)){
			return result0;
		}
		result0.clear();
		return result0;
	}
	
	//filter rp eq rp
	public ArrayList<Node> visitFiltereq( xpathParser.FiltereqContext ctx) {
		ArrayList<Node> originList = new ArrayList<Node>(currentList);
		ArrayList<Node> result0 = visit(ctx.rp(0));
		setContext(currentList,originList);
		ArrayList<Node> result1 = visit(ctx.rp(1));
		setContext(currentList,originList);
		if (copyequal(result0,result1)){
			return result0;
		}
		result0.clear();
		return result0;
	}
/***************************************************************************
 *Helper 
 ***************************************************************************/	
	private HashMap<String,ArrayList<Node>> getCtxMap(){
		if (ctxStack.empty())
			return new HashMap<String,ArrayList<Node>>();
		else
			return new HashMap<String,ArrayList<Node>>(ctxStack.peek());
	}
	
	private void setContext(ArrayList<Node>curr,ArrayList<Node> change) {
		curr.clear();
		curr.addAll(change);
	}
	
	private void setContext(ArrayList<Node>curr,Node change) {
		curr.clear();
		curr.add(change);
	}
	
	private boolean isequal(ArrayList<Node> result0,ArrayList<Node> result1){
		if (result0.size()>0 && result1.size()>0){
			for (int i=0;i<result0.size();i++){
				for (int j=0;j<result1.size();j++){
					if (result0.get(i).isSameNode(result1.get(j))){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private boolean copyequal(ArrayList<Node> result0,ArrayList<Node> result1){
		if (result0.size()>0 && result1.size()>0){
			for (int i=0;i<result0.size();i++){
				for (int j=0;j<result1.size();j++){
					if (result0.get(i).isEqualNode(result1.get(j))){
						return true;
					}
				}
			}
		}
		return false;
	}

	
	private ArrayList<Node> getChildren(){
		ArrayList<Node> result = new ArrayList<Node>();
		for (int i = 0; i < currentList.size(); i++){
			NodeList curr = currentList.get(i).getChildNodes();
			for (int j = 0; j < curr.getLength(); j++){
				if (curr.item(j).getNodeType() == Node.ELEMENT_NODE){
					result.add(curr.item(j));
				}
			}
		}
		setContext(currentList,result);
		return result;
	}
	
	private ArrayList<Node> getText(){
		ArrayList<Node> result = new ArrayList<Node>();
		for (int i = 0; i < currentList.size(); i++){
			NodeList curr = currentList.get(i).getChildNodes();
			for (int j = 0; j < curr.getLength(); j++){
				if (curr.item(j).getNodeType() == Node.TEXT_NODE){
					result.add(curr.item(j));
				}
			}
		}
		setContext(currentList,result);
		return result;
	}
	
	private ArrayList<Node> getParent(){
		ArrayList<Node> result = new ArrayList<Node>();
		for (int i = 0; i < currentList.size(); i++){
			Node curr = currentList.get(i).getParentNode(); // TO DO: if element?
			if (curr!=null)
				result.add(curr);
		}
		setContext(currentList,unique(result));
		return unique(result);
	}
	
	
	private ArrayList<Node> unique(ArrayList<Node> arr){
		ArrayList<Node> set = new ArrayList<Node>();
		for (int i = 0; i < arr.size(); i++ ){
			Node n = arr.get(i);
			if(!set.contains(n)){
				set.add(n);
			}
		}
		return set;
	}
	
	
	public static void main(String[] args) throws Exception{
		FileInputStream fin = new FileInputStream("xptest");
		ANTLRInputStream input = new ANTLRInputStream(fin);
		// create a lexer that feeds off of input CharStream
		xpathLexer lexer = new xpathLexer(input);
		// create a buffer of tokens pulled from the lexer
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// create a parser that feeds off the tokens buffer
		xpathParser parser = new xpathParser(tokens);
		ParseTree tree = parser.xq(); // begin parsing at init rule
		Myxpathvisitor eval = new Myxpathvisitor();
		JoinHelper helper = new JoinHelper();
		helper.visit(tree);
		ArrayList<Node> rs = eval.visit(tree);
		
		Element rootElement = eval.document.createElement("myoutput");
		eval.document.appendChild(rootElement);
		for (int i = 0; i < rs.size(); i++){
			//System.out.println(rs.get(i).getNodeName());
			//System.out.println(rs.get(i).getNodeValue());
			if (rs.get(i)!=null && rs.get(i).getNodeType()!= Node.ATTRIBUTE_NODE){
				if (rs.get(i).getNodeType()!=Node.DOCUMENT_NODE)
					rootElement.appendChild(eval.document.importNode(rs.get(i), true));
				else {
					Node temp = rs.get(i).getFirstChild();
					rootElement.appendChild(eval.document.importNode(temp, true));
				}
			}
			
		}		
		if (fin!=null) fin.close();
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(eval.document);
		StreamResult result = new StreamResult(new File("output.xml"));
		transformer.transform(source, result);
	}
}