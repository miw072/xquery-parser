import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.SAXException;

import java.util.*;
import java.io.*;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Myxpathvisitor  extends xpathBaseVisitor<ArrayList<Node>> {
	ArrayList<Node> currentList = new ArrayList<Node>();
	DOMParser parser = new DOMParser();
	
	//absolute path slash
	public ArrayList<Node> visitApsl(xpathParser.ApslContext ctx){
		String xmlFile = ctx.ID().getText();
		
		try {
	         parser.parse(xmlFile);
	         Document document = parser.getDocument();
	 		 currentList.add(document);
	    } catch (SAXException e) {
	         System.err.println (e);
	    } catch (IOException e) {
	         System.err.println (e);
	    }
		return visit(ctx.rp());
	}
	
	//relative path tag
	public ArrayList<Node> visitRptag(xpathParser.RptagContext ctx){
		ArrayList<Node> result = new ArrayList<Node>();
		String tagName = ctx.ID().getText();
		for (int i = 0; i < currentList.size(); i++){
			NodeList curr = currentList.get(i).getChildNodes();
			for (int j = 0; j < curr.getLength(); j++){
				if (curr.item(j).getNodeType() == Node.ELEMENT_NODE){
					if (curr.item(j).getNodeName().equals(tagName)){
						result.add(curr.item(j));
					}
				}
			}
		}
		currentList = result;
		return result;
	}
	
	//relative path slash
	public ArrayList<Node> visitRpsrp(xpathParser.RpsrpContext ctx){
		ArrayList<Node> result = new ArrayList<Node>();
		result = visit(ctx.rp(0));
		currentList = result;
		result = visit(ctx.rp(1));
		currentList = unique(result);
		
		return unique(result);
	}
	
	//relative path current
	public ArrayList<Node> visitRpcurr(xpathParser.RpcurrContext ctx){
		return currentList;
	}
	
	//relative path star(children)
	public ArrayList<Node> visitRpchildren(xpathParser.RpchildrenContext ctx){
		return getChildren();
	}
	
	//relative path ..(parent)
	public ArrayList<Node> visitRpparent(xpathParser.RpparentContext ctx){
		return getParent();
	}
	
	//relative path comma(and)
	public ArrayList<Node> visitRpandrp(xpathParser.RpandrpContext ctx){
		ArrayList<Node> result0 = new ArrayList<Node>();
		ArrayList<Node> result1 = new ArrayList<Node>();
		result0 = visit(ctx.rp(0));
		result1 = visit(ctx.rp(1));
		ArrayList<Node> result = new ArrayList<Node>(result0);
		result.addAll(result1);
		currentList = result;
		return result;
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
		currentList = result;
		return result;
	}
	
	private ArrayList<Node> getParent(){
		ArrayList<Node> result = new ArrayList<Node>();
		for (int i = 0; i < currentList.size(); i++){
			Node curr = currentList.get(i).getParentNode(); // TO DO: if element?
			result.add(curr);
		}
		currentList = unique(result);
		return unique(result);
	}
	
	
	private ArrayList<Node> unique(ArrayList<Node> arr){
		HashSet<Node> set = new HashSet<Node>();
		for (int i = 0; i < arr.size(); i++ ){
			Node n = arr.get(i);
			if(set.contains(n)){
				arr.remove(i);
			}else{
				set.add(n);
			}
		}
		return arr;
	}
	
	
	public static void main(String[] args) throws Exception{
		
		ANTLRInputStream input = new ANTLRInputStream(System.in);
		// create a lexer that feeds off of input CharStream
		xpathLexer lexer = new xpathLexer(input);
		// create a buffer of tokens pulled from the lexer
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// create a parser that feeds off the tokens buffer
		xpathParser parser = new xpathParser(tokens);
		ParseTree tree = parser.ap(); // begin parsing at init rule
		Myxpathvisitor eval = new Myxpathvisitor();
		ArrayList<Node> rs = eval.visit(tree);
		
		for (int i = 0; i < rs.size(); i++){
			System.out.println(rs.get(i).getNodeName());
		}	
	}
}