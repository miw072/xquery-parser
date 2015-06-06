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
public class JoinHelper extends xpathBaseVisitor<String>{

	public String visitXqflwr( xpathParser.XqflwrContext ctx) { 
		int numVar = ctx.forClause().Var().size();
		ArrayList<ArrayList<String>> varList = getVarList(ctx.forClause());
		//System.out.println(varList);
		
		ArrayList<String> xqfor = produceXqFor(varList, ctx.forClause());
		
		ArrayList<ParseTree> InWhere  = flatWhere(ctx.whereClause().getChild(1));	
		//System.out.println(joinVarList(InWhere,varList.get(0),varList.get(1)));
		ArrayList<String> xqwhere = produceXqWhere(varList, joinVarList(InWhere,varList.get(0),varList.get(1)));
		ArrayList<String> xqReturn = produceXqReturn(varList);
		
		return null;
	}
	
	private ArrayList<String> produceXqReturn(ArrayList<ArrayList<String>> group){
		ArrayList<String> result = new ArrayList<String>();
		for (ArrayList<String> l : group){
			String curr = "return <tuple>{";
			for (String s: l){
				 curr += "<" + s.substring(1, s.length()) + ">" + s + "</" + s.substring(1, s.length()) + ">,";
			}
			curr = curr.substring(0, curr.length() - 1) + "}";
			result.add(curr);
		}
		
		System.out.println(result);
		return result;
	}
	
	
	private ArrayList<String> produceXqWhere(ArrayList<ArrayList<String>> group, ArrayList<ArrayList<String>> joinList){
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<String> group1Join = joinList.get(0);
		ArrayList<String> group2Join = joinList.get(1);
		ArrayList<String> group1Where = joinList.get(2);
		ArrayList<String> group2Where = joinList.get(3);
		
		if (group1Join.size() != 0 ){
			String join1 = "[";
			for (String s : group1Join){
				join1 += s + ",";
			}
			join1 = join1.substring(0, join1.length() - 1);
			join1 += "]";
			result.add(join1);
		}
		
		if (group2Join.size() != 0){
			String join2 = "[";
			for (String s : group2Join){
				join2 += s + ",";
			}
			join2 = join2.substring(0, join2.length() - 1);
			join2 += "]";
			result.add(join2);
		}
		
		if (group1Where.size() != 0){
			String where1 = "where ";
			for (String s : group1Where){
				where1 += s + "and ";
			}
			where1 = where1.substring(0, where1.length() - 4);
			result.add(where1);
		}
		
		if (group1Where.size() != 0){
			String where2 = "where ";
			for (String s : group2Where){
				where2 += s + "and ";
			}
			where2 = where2.substring(0, where2.length() - 4);
			result.add(where2);
		}
		
		
		System.out.println(result);
		return result;
	}
	
	
	
	
	private ArrayList<ArrayList<String>> joinVarList(ArrayList<ParseTree> ctx,ArrayList<String> varList1,ArrayList<String> varList2){
		ArrayList<String> firstList = new ArrayList<String>();
		ArrayList<String> secondList = new ArrayList<String>();
		
		ArrayList<String> thirdList = new ArrayList<String>();
		ArrayList<String> forthList = new ArrayList<String>();
		for (ParseTree p: ctx){
			String lType = p.getChild(0).getClass().getName();
			String rType = p.getChild(2).getClass().getName();
			//System.out.println(lType+"   "+rType);
			if (lType.equals("xpathParser$XqVarContext") || rType.equals("xpathParser$XqVarContext")){
				if (lType.equals("xpathParser$XqVarContext") && rType.equals("xpathParser$XqVarContext")){
					String lVar = p.getChild(0).getText();
					String rVar = p.getChild(2).getText();
					int state = inJoins(lVar,rVar,varList1,varList2);
					switch(state){
						case 1:
								firstList.add(lVar);
								secondList.add(rVar);
								break;
						case 2:
								secondList.add(lVar);
								firstList.add(rVar);
								break;
						case 3:
								thirdList.add(lVar+" = "+rVar);
								break;
						case 4:
								forthList.add(lVar+" = "+rVar);
								break;
								
						default:break;
					}			
				}
				else{
					if(lType.equals("xpathParser$XqStringContext")){
						String String = p.getChild(0).getText();
						String Var = p.getChild(2).getText();
						if (varList1.contains(Var)){
							thirdList.add(Var+" = "+String);
						}
						else if (varList2.contains(Var)){
							forthList.add(Var+" = "+String);
						}

					}
					else if (rType.equals("xpathParser$XqStringContext")){
						String Var = p.getChild(0).getText();
						String String = p.getChild(2).getText();
						if (varList1.contains(Var)){
							thirdList.add(Var+" = "+String);
						}
						else if (varList2.contains(Var)){
							forthList.add(Var+" = "+String);
						}
					}
					else {
						System.out.println("Not a join! EQ");
						System.exit(-1);
					}
				}
			}
			else {
				System.out.println("Not a join! EQ must have a VAR");
				System.exit(-1);
			}
		}
		
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		result.add(firstList);
		result.add(secondList);
		result.add(thirdList);
		result.add(forthList);
		
		return result;
	}
	
	private int inJoins(String lVar, String rVar, ArrayList<String> firstList, ArrayList<String> secondList){
		if (firstList.contains(lVar)&& secondList.contains(rVar)){
			return 1;
		}
		else if (firstList.contains(rVar)&& secondList.contains(lVar)){
			return 2;
		}
		else if (firstList.contains(lVar)&&firstList.contains(rVar)){
			return 3;
		}
		else if (secondList.contains(lVar)&&secondList.contains(rVar)){
			return 4;
		}
		return 0;
	}
	
	private ArrayList<String> produceXqFor(ArrayList<ArrayList<String>> list, xpathParser.ForClauseContext ctx){
		int num = ctx.Var().size();
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < list.size(); i++){
			result.add("");
		}
		
		for (int i = 0; i < list.size(); i++){
			for (int j = 0; j < num; j++){
				String name = ctx.Var(j).getText();
				if (list.get(i).contains(name)){
					String content = ctx.xq(j).getText();
					result.set(i, result.get(i).concat(name + " in " + content + ", "));
				}
			}
		}
		
		for (String s : result){
			s = "for " + s;
			s = s.substring(0, s.length() - 2);
			//System.out.println(s);
		}
		
		return result;
	}
	
	
	private ArrayList<ParseTree> flatWhere(ParseTree ctx) {
		ArrayList<ParseTree> result = new ArrayList<ParseTree>();
		if (ctx.getClass().getName().equals("xpathParser$CondandContext")){
			result.addAll(flatWhere(ctx.getChild(0)));
			result.addAll(flatWhere(ctx.getChild(2)));
		}
		else if (ctx.getClass().getName().equals("xpathParser$CondeqsymContext") ||
				ctx.getClass().getName().equals("xpathParser$CondeqContext")){
			result.add(ctx);
		}
		else {
			System.out.println("Not a join! 3");
			System.exit(-1);
		}
			
		return result;
	}
	
	
	private ArrayList<ArrayList<String>> getVarList(xpathParser.ForClauseContext ctx){
		int numVar = ctx.Var().size();
		int numLists =0;
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		for (int i=0; i<numVar;i++){
			String type = ctx.xq(i).getClass().getName();
			//System.out.println(ctx.xq(i).getText());
			if (type.equals("xpathParser$XqapContext")){
				result.add(new ArrayList<String>());
				result.get(numLists).add(ctx.Var(i).getText());
				numLists++;
			}
			else if (type.equals("xpathParser$XqslrpContext") || type.equals("xpathParser$XqdslrpContext")){
				if (ctx.xq(i).getChild(0).getClass().getName().equals("xpathParser$XqVarContext")){
					String name = ctx.xq(i).getChild(0).getText();
					for (ArrayList<String> list: result){
						if (list.contains(name)){
							list.add(ctx.Var(i).getText());
						}
					}
				}
				else{
					System.out.println("Not a join! 1");
					System.exit(-1);
				}
			}
			else {
				System.out.println("Not a join! 2");
				System.exit(-1);
			}
				
		}
		
		return result;
	}
}
