package cop5556fa19;

import static cop5556fa19.Token.Kind.*;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

import cop5556fa19.AST.Exp;
import cop5556fa19.AST.ExpBinary;
import cop5556fa19.AST.ExpFalse;
import cop5556fa19.AST.ExpFunction;
import cop5556fa19.AST.ExpInt;
import cop5556fa19.AST.ExpName;
import cop5556fa19.AST.ExpNil;
import cop5556fa19.AST.ExpString;
import cop5556fa19.AST.ExpTable;
import cop5556fa19.AST.ExpTrue;
import cop5556fa19.AST.ExpUnary;
import cop5556fa19.AST.ExpVarArgs;
import cop5556fa19.AST.Expressions;
import cop5556fa19.AST.Field;
import cop5556fa19.AST.FieldExpKey;
import cop5556fa19.AST.FieldNameKey;
import cop5556fa19.AST.FieldImplicitKey;
import cop5556fa19.AST.ParList;
import cop5556fa19.AST.Name;
import cop5556fa19.AST.FuncBody;
import cop5556fa19.AST.Block;
import cop5556fa19.Parser.SyntaxException;

class ExpressionParserTest {

	// To make it easy to print objects and turn this output on and off
	static final boolean doPrint = true;

	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}


	
	// creates a scanner, parser, and parses the input.  
	Exp parseAndShow(String input) throws Exception {
		show("parser input:\n" + input); // Display the input
		Reader r = new StringReader(input);
		Scanner scanner = new Scanner(r); // Create a Scanner and initialize it
		Parser parser = new Parser(scanner);  // Create a parser
		Exp e = parser.exp(); // Parse and expression
		show("e=" + e);  //Show the resulting AST
		return e;
	}
	


	@Test
	void testIdent0() throws Exception {
		String input = "x";
		Exp e = parseAndShow(input);
		assertEquals(ExpName.class, e.getClass());
		assertEquals("x", ((ExpName) e).name);
	}

	@Test
	void testIdent1() throws Exception {
		String input = "(x)";
		Exp e = parseAndShow(input);
		assertEquals(ExpName.class, e.getClass());
		assertEquals("x", ((ExpName) e).name);
	}

	@Test
	void testString() throws Exception {
		String input = "\"string\"";
		Exp e = parseAndShow(input);
		assertEquals(ExpString.class, e.getClass());
		assertEquals("string", ((ExpString) e).v);
	}

	@Test
	void testBoolean0() throws Exception {
		String input = "true";
		Exp e = parseAndShow(input);
		assertEquals(ExpTrue.class, e.getClass());
	}

	@Test
	void testBoolean1() throws Exception {
		String input = "false";
		Exp e = parseAndShow(input);
		assertEquals(ExpFalse.class, e.getClass());
	}


	@Test
	void testBinary0() throws Exception {
		String input = "1 + 2";
		Exp e = parseAndShow(input);
		Exp expected = Expressions.makeBinary(1,OP_PLUS,2);
		show("expected="+expected);
		assertEquals(expected,e);
	}
	
	@Test
	void testUnary0() throws Exception {
		String input = "-2";
		Exp e = parseAndShow(input);
		Exp expected = Expressions.makeExpUnary(OP_MINUS, 2);
		show("expected="+expected);
		assertEquals(expected,e);
	}
	
	@Test
	void testUnary1() throws Exception {
		String input = "-*2\n";
		assertThrows(SyntaxException.class, () -> {
		Exp e = parseAndShow(input);
		});	
	}
	

	
	@Test
	void testRightAssoc() throws Exception {
		String input = "\"concat\" .. \"is\"..\"right associative\"";
		Exp e = parseAndShow(input);
		Exp expected = Expressions.makeBinary(
				Expressions.makeExpString("concat")
				, DOTDOT
				, Expressions.makeBinary("is",DOTDOT,"right associative"));
		show("expected=" + expected);
		assertEquals(expected,e);
	}
	
	@Test
	void testLeftAssoc() throws Exception {
		String input = "\"minus\" - \"is\" - \"left associative\"";
		Exp e = parseAndShow(input);
		Exp expected = Expressions.makeBinary(
				Expressions.makeBinary(
						Expressions.makeExpString("minus")
				, OP_MINUS
				, Expressions.makeExpString("is")), OP_MINUS, 
				Expressions.makeExpString("left associative"));
		show("expected=" + expected);
		assertEquals(expected,e);
		
	}
	
	@Test
	void test() throws Exception{ // test keywords
		String input = "nil";
		Exp e = parseAndShow(input);
		Exp expected = new ExpNil(new Token(KW_nil,"nil",0,0));
		show("expected = " + expected);
		assertEquals(expected,e);
	}
	
	@Test
	void test1() throws Exception{ // test parenthesis
		String input = "(2)";
		Exp e = parseAndShow(input); 
		Exp expected = Expressions.makeInt(2);
		show("expected=" + expected);
		assertEquals(expected,e);
	}
	
	@Test
	void testLeftAndRightAssoc() throws Exception {
		String input = "\"...\" + 123 .. \"123 \" + -123";
		Exp e = parseAndShow(input);
		Exp expected = Expressions.makeBinary(Expressions.makeBinary(Expressions.makeExpString("..."), OP_PLUS,
				Expressions.makeInt(123)), DOTDOT, Expressions.makeBinary(Expressions.makeExpString("123 "), OP_PLUS, Expressions.makeExpUnary(OP_MINUS, 123)));
		show("expected=" + expected);
		assertEquals(expected,e);
	}
/**
*	@Test
*	void testFunction() throws Exception {
*		String input = "function (x, y, z...) end .. x + y";
*		Exp e = parseAndShow(input);
*		ParList parList = new ParList(new Token(NAME, "x", 0, 0), Arrays.asList(new Name(new Token(NAME, "x", 0, 0), "x"), new Name(new Token(NAME, "y", 0, 0), "y"), new Name(new Token(NAME, "z", 0, 0), "z")), true);
*		Exp expected = Expressions.makeBinary(new ExpFunction(new Token(Token.Kind.KW_function, "function", 0, 0), new FuncBody(new Token(Token.Kind.LPAREN, "(", 0, 0), parList, new Block(null))), DOTDOT, Expressions.makeBinary(new ExpName(new Token(Token.Kind.NAME, "x", 0, 0)), OP_PLUS,(new ExpName(new Token(Token.Kind.NAME, "y", 0, 0)))));
*		show("expected=" + expected);
*		assertEquals(expected, e);
*	}
**/
	@Test
	void testTableConstructor() throws Exception {
		String input = "{[1 + 2] = 3 + 4, x = 5 ; y + 4 ;} + 3"; 
		Exp e = parseAndShow(input);
		List<Field> fields = new ArrayList<>();
		fields.add(new FieldExpKey(new Token(LSQUARE, "[", 0, 0), Expressions.makeBinary(Expressions.makeInt(1), OP_PLUS, Expressions.makeInt(2)),Expressions.makeBinary(Expressions.makeInt(3),OP_PLUS,Expressions.makeInt(4))));
		fields.add(new FieldNameKey(new Token(NAME, "x", 0, 0), new Name(new Token(NAME, "x", 0, 0), "x"), Expressions.makeInt(5)));
		fields.add(new FieldImplicitKey(new Token(NAME, "y", 0, 0), Expressions.makeBinary(new ExpName(new Token(NAME, "y", 0, 0)), OP_PLUS, Expressions.makeInt(4))));
		show("expected = " + Expressions.makeBinary(new ExpTable(new Token(LSQUARE, "[", 0, 0), fields), OP_PLUS, Expressions.makeInt(3)));
		assertEquals(e, Expressions.makeBinary(new ExpTable(new Token(LSQUARE, "[", 0, 0), fields), OP_PLUS, Expressions.makeInt(3)));
	}
	
	@Test
	void test2() throws Exception{
		String input = "a+b*c+d";
		Exp e = parseAndShow(input);
		
	}
	
	@Test
	void test3() throws Exception{
		String input = "123 + (456 - 789) - 101112";
		Exp e = parseAndShow(input);
		
	}

	@Test
	void test4() throws Exception{
		String input = "123 or 456 and 789 or 1011";
		Exp e = parseAndShow(input);
		
	}
	
	@Test
	void test5() throws Exception{
		String input = "1^ (4+2)*3";
		Exp e = parseAndShow(input);
		
	}
	
	@Test
	void test6() throws Exception{
		String input = "1+2/3 >> 4^5";
		Exp e = parseAndShow(input);
		
	}
	
	@Test
	void test7() throws Exception{
		String input = "(1 <= 2) == (4 >= 3)";
		Exp e = parseAndShow(input);
	}
	
	@Test
	void test8() throws Exception{
		String input = "1~ 2 | 3 & 4";
		Exp e = parseAndShow(input);
		
	}
	@Test
	void test9() throws Exception{
		String input = "(1*2)/(3%4)//5";
		Exp e = parseAndShow(input);
		
	}
	
	@Test
	void test10() throws Exception{
		String input = "function (aa,b) end >> function (test,I,...) end & function(...) end";
		Exp e = parseAndShow(input);
		
	}
	
	@Test
	void test11() throws Exception{
		String input = "{3}";
		Exp e = parseAndShow(input);
		
	}
	
	@Test
	void test12() throws Exception{
		String input = "{[3]=a}";
		Exp e = parseAndShow(input);
		
	}
	
	@Test
	void test13() throws Exception{
		String input = "{}";
		Exp e = parseAndShow(input);
		
	}
	
	@Test
	void test14() throws Exception{
		String input = "function () end";
		Exp e = parseAndShow(input);
	}
	
	
}
