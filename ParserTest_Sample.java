package cop5556fa19;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import cop5556fa19.Parser;
import cop5556fa19.Parser.SyntaxException;
import cop5556fa19.AST.ASTNode;
import cop5556fa19.AST.Block;
import cop5556fa19.AST.Chunk;
import cop5556fa19.AST.Exp;
import cop5556fa19.AST.ExpBinary;
import cop5556fa19.AST.ExpFalse;
import cop5556fa19.AST.ExpFunction;
import cop5556fa19.AST.ExpFunctionCall;
import cop5556fa19.AST.ExpInt;
import cop5556fa19.AST.ExpName;
import cop5556fa19.AST.ExpNil;
import cop5556fa19.AST.ExpString;
import cop5556fa19.AST.ExpTable;
import cop5556fa19.AST.ExpTrue;
import cop5556fa19.AST.ExpVarArgs;
import cop5556fa19.AST.Expressions;
import cop5556fa19.AST.Field;
import cop5556fa19.AST.FieldExpKey;
import cop5556fa19.AST.FieldImplicitKey;
import cop5556fa19.AST.ParList;
import cop5556fa19.AST.Stat;
import cop5556fa19.AST.StatAssign;
import cop5556fa19.AST.StatBreak;
import cop5556fa19.AST.StatDo;
import cop5556fa19.AST.StatGoto;
import cop5556fa19.AST.StatLabel;
import cop5556fa19.Scanner;
import cop5556fa19.Token;

import static cop5556fa19.Token.Kind.*;

class ParserTest_Sample {

	// To make it easy to print objects and turn this output on and off
	static final boolean doPrint = true;
//	static final boolean doPrint = false;

	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}
	
	// creates a scanner, parser, and parses the input by calling exp().  
	Exp parseExpAndShow(String input) throws Exception {
		show("parser input:\n" + input); // Display the input
		Reader r = new StringReader(input);
		Scanner scanner = new Scanner(r); // Create a Scanner and initialize it
		Parser parser = new Parser(scanner);
		Exp e = parser.exp();
		show("e=" + e);
		return e;
	}	
	
	
	// creates a scanner, parser, and parses the input by calling block()  
	Block parseBlockAndShow(String input) throws Exception {
		show("parser input:\n" + input); // Display the input
		Reader r = new StringReader(input);
		Scanner scanner = new Scanner(r); // Create a Scanner and initialize it
		Parser parser = new Parser(scanner);
		Method method = Parser.class.getDeclaredMethod("block");
		method.setAccessible(true);
		Block b = (Block) method.invoke(parser);
		show("b=" + b);
		return b;
	}	
	
	
	//creates a scanner, parser, and parses the input by calling parse()
	//this corresponds to the actual use case of the parser
	Chunk parseAndShow(String input) throws Exception {
		show("parser input:\n" + input); // Display the input
		Reader r = new StringReader(input);
		Scanner scanner = new Scanner(r); // Create a Scanner and initialize it
		Parser parser = new Parser(scanner);
		Chunk c = parser.parse();
		show("c="+c);
		return c;
	}
	
	@Test
	void testEmpty1() throws Exception {
		String input = "";
		Block b = parseBlockAndShow(input);
		Block expected = Expressions.makeBlock();
		assertEquals(expected, b);
	}
	
	@Test
	void testEmpty2() throws Exception {
		String input = "";
		ASTNode n = parseAndShow(input);
		Block b = Expressions.makeBlock();
		Chunk expected = new Chunk(b.firstToken,b);
		assertEquals(expected,n);
	}
	
	@Test
	void testAssign1() throws Exception {
		String input = "a=b";
		Block b = parseBlockAndShow(input);		
		List<Exp> lhs = Expressions.makeExpList(Expressions.makeExpNameGlobal("a"));
		List<Exp> rhs = Expressions.makeExpList(Expressions.makeExpNameGlobal("b"));
		StatAssign s = Expressions.makeStatAssign(lhs,rhs);
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,b);
	}
	
	@Test
	void testAssignChunk1() throws Exception {
		String input = "a=b";
		ASTNode c = parseAndShow(input);		
		List<Exp> lhs = Expressions.makeExpList(Expressions.makeExpNameGlobal("a"));
		List<Exp> rhs = Expressions.makeExpList(Expressions.makeExpNameGlobal("b"));
		StatAssign s = Expressions.makeStatAssign(lhs,rhs);
		Block b = Expressions.makeBlock(s);
		Chunk expected = new Chunk(b.firstToken,b);
		assertEquals(expected,c);
	}
	

	@Test
	void testMultiAssign1() throws Exception {
		String input = "a,c=8,9";
		Block b = parseBlockAndShow(input);		
		List<Exp> lhs = Expressions.makeExpList(
					Expressions.makeExpNameGlobal("a")
					,Expressions.makeExpNameGlobal("c"));
		Exp e1 = Expressions.makeExpInt(8);
		Exp e2 = Expressions.makeExpInt(9);
		List<Exp> rhs = Expressions.makeExpList(e1,e2);
		StatAssign s = Expressions.makeStatAssign(lhs,rhs);
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,b);		
	}
	

	

	@Test
	void testMultiAssign3() throws Exception {
		String input = "a,c=8,f(x)";
		Block b = parseBlockAndShow(input);		
		List<Exp> lhs = Expressions.makeExpList(
					Expressions.makeExpNameGlobal("a")
					,Expressions.makeExpNameGlobal("c"));
		Exp e1 = Expressions.makeExpInt(8);
		List<Exp> args = new ArrayList<>();
		args.add(Expressions.makeExpNameGlobal("x"));
		Exp e2 = Expressions.makeExpFunCall(Expressions.makeExpNameGlobal("f"),args, null);
		List<Exp> rhs = Expressions.makeExpList(e1,e2);
		StatAssign s = Expressions.makeStatAssign(lhs,rhs);
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,b);			
	}
	

	
	@Test
	void testAssignToTable() throws Exception {
		String input = "g.a.b = 3";
		Block bl = parseBlockAndShow(input);
		ExpName g = Expressions.makeExpNameGlobal("g");
		ExpString a = Expressions.makeExpString("a");
		Exp gtable = Expressions.makeExpTableLookup(g,a);
		ExpString b = Expressions.makeExpString("b");
		Exp v = Expressions.makeExpTableLookup(gtable, b);
		Exp three = Expressions.makeExpInt(3);		
		Stat s = Expressions.makeStatAssign(Expressions.makeExpList(v), Expressions.makeExpList(three));;
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,bl);
	}
	
	@Test
	void testAssignTableToVar() throws Exception {
		String input = "x = g.a.b";
		Block bl = parseBlockAndShow(input);
		ExpName g = Expressions.makeExpNameGlobal("g");
		ExpString a = Expressions.makeExpString("a");
		Exp gtable = Expressions.makeExpTableLookup(g,a);
		ExpString b = Expressions.makeExpString("b");
		Exp e = Expressions.makeExpTableLookup(gtable, b);
		Exp v = Expressions.makeExpNameGlobal("x");		
		Stat s = Expressions.makeStatAssign(Expressions.makeExpList(v), Expressions.makeExpList(e));;
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,bl);
	}
	

	
	@Test
	void testmultistatements6() throws Exception {
		String input = "x = g.a.b ; ::mylabel:: do  y = 2 goto mylabel f=a(0,200) end break"; //same as testmultistatements0 except ;
		ASTNode c = parseAndShow(input);
		ExpName g = Expressions.makeExpNameGlobal("g");
		ExpString a = Expressions.makeExpString("a");
		Exp gtable = Expressions.makeExpTableLookup(g,a);
		ExpString b = Expressions.makeExpString("b");
		Exp e = Expressions.makeExpTableLookup(gtable, b);
		Exp v = Expressions.makeExpNameGlobal("x");		
		Stat s0 = Expressions.makeStatAssign(v,e);
		StatLabel s1 = Expressions.makeStatLabel("mylabel");
		Exp y = Expressions.makeExpNameGlobal("y");
		Exp two = Expressions.makeExpInt(2);
		Stat s2 = Expressions.makeStatAssign(y,two);
		Stat s3 = Expressions.makeStatGoto("mylabel");
		Exp f = Expressions.makeExpNameGlobal("f");
		Exp ae = Expressions.makeExpNameGlobal("a");
		Exp twohundred = Expressions.makeExpInt(200);
		Exp zero = Expressions.makeExpInt(0);
		List<Exp> args = Expressions.makeExpList(zero, twohundred);
		ExpFunctionCall fc = Expressions.makeExpFunCall(ae, args, null);		
		StatAssign s4 = Expressions.makeStatAssign(f,fc);
		StatDo statdo = Expressions.makeStatDo(s2,s3,s4);
		StatBreak statBreak = Expressions.makeStatBreak();
		Block expectedBlock = Expressions.makeBlock(s0,s1,statdo,statBreak);
		Chunk expectedChunk = new Chunk(expectedBlock.firstToken, expectedBlock);
		assertEquals(expectedChunk,c);
	}
	
	@Test
	void semitest() throws Exception{
		String input = ";";
		ASTNode c = parseAndShow(input);
	}
	
	@Test
	void multistatementtest() throws Exception{
		String input = "repeat x = 2 function slfjs (dfds,... ) x = f(x) end return x ; until a";
		ASTNode c = parseAndShow(input);
	}
	
	@Test
	void multistatementtest1() throws Exception{
		String input = "if x==1 then local function slfjs (dfds,... ) x = f(x) local x = 1 end elseif as>=3 then ; else fe=3 end";
		ASTNode c = parseAndShow(input);
	}
	
	@Test
	void multistatementtest2() throws Exception{
		String input = "while {[x>=1] = y<1 ; x = 3;} do x : sdf \"fsdsd\" [name] = name ; end ";
		ASTNode c = parseAndShow(input);
	}
	
	@Test
	void expFunctionCalltest() throws Exception{
		String input = "  x : sdf \"fsdsd\" [name] ";
		Exp c = parseExpAndShow(input);
	}
	
	@Test
	void multistatementtest3() throws Exception{
		String input = "while {[x>=1] = y<1 ; x = 3;} do x : sdf \"fsdsd\" [name] = name ; end";
		ASTNode c = parseAndShow(input);
	}
	
	@Test
	void multistatementtest4() throws Exception{
		String input = "if x then x=3 elseif y then y=4 elseif true then x=10 else y=11 end";
		ASTNode c = parseAndShow(input);
	}
	
	@Test
	void multistatementtest5() throws Exception{
		String input = "if x==1 then ; elseif x==2 then ; end";
		ASTNode c = parseAndShow(input);
	}
	
	@Test
	void multistatementtest6() throws Exception{
		String input = "for name = ne_$d1e, false,sdf do function sdfds.sdfsd:fsdf2 (); end end";
		Block c = parseBlockAndShow(input);
	}
	
	@Test
	void multistatementtest7() throws Exception{
		String input = "for name = name , nil,sdf do ; end";
		Block c = parseBlockAndShow(input);
	}
	
	@Test
	void multistatementtest8() throws Exception{
		String input = "x = false,DSF";
		Block c = parseBlockAndShow(input);
	}
	
	@Test
	void multistatementtest9() throws Exception{
		String input = "false==f";
		Exp c = parseExpAndShow(input);
	}
	
	@Test
	void multiassign2() throws Exception{
		String input = "a,c = 8,f()";
		Block b = parseBlockAndShow(input);
	}
	
	@Test
	void testleftAssoc() throws Exception{
		String input = "\"minus\" - \"is\" - \"left associative\"";
		Exp e = parseExpAndShow(input);
	}
	
	@Test
	void testfunctiondef1() throws Exception{
		String input = "function(aa,b) end >> function(test,I,...) end & function(...)end";
		Exp e = parseExpAndShow(input);
	}
	
	@Test
	void testleft() throws Exception{
		String input = "- -2";
		Exp e = parseExpAndShow(input);
	}
	
	@Test
	void testTable3() throws Exception{
		String input = "{3,a-3*b}";
		Exp e = parseExpAndShow(input);
	}
	
	@Test
	void testFuncCallStatement0() throws Exception{
		String input = "x=f();";
		Block b = parseBlockAndShow(input);
	}
	
	@Test
	void testFuncDec2() throws Exception{
		String input = "function (...) end";
		Exp e = parseExpAndShow(input);
	}
	@Test
	void testFuncDec3() throws Exception{
		String input = "2*4";
		Exp e = parseExpAndShow(input);
	}
	
	@Test
	void returntest() throws Exception{
		String input = "x = print(t[1][3])";
		ASTNode c = parseAndShow(input);
	}
}

