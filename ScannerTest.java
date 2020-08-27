package cop5556fa19;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import cop5556fa19.Scanner.LexicalException;

import static cop5556fa19.Token.Kind.*;

public class ScannerTest {
	
	//I like this to make it easy to print objects and turn this output on and off
	static boolean doPrint = true;
	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}
	
	

	 /**
	  * Example showing how to get input from a Java string literal.
	  * 
	  * In this case, the string is empty.  The only Token that should be returned is an EOF Token.  
	  * 
	  * This test case passes with the provided skeleton, and should also pass in your final implementation.
	  * Note that calling getNext again after having reached the end of the input should just return another EOF Token.
	  * 
	  */
	@Test 
	void test0() throws Exception {
		Reader r = new StringReader("");
		Scanner s = new Scanner(r);
		Token t;
		show(t= s.getNext()); 
		assertEquals(EOF, t.kind);
		show(t= s.getNext());
		assertEquals(EOF, t.kind);
	}

	/**
	 * Example showing how to create a test case to ensure that an exception is thrown when illegal input is given.
	 * 
	 * This "@" character is illegal in the final scanner (except as part of a String literal or comment). So this
	 * test should remain valid in your complete Scanner.
	 */
	@Test
	void test1() throws Exception {
		Reader r = new StringReader("@");
		Scanner s = new Scanner(r);
        assertThrows(LexicalException.class, ()->{
		   s.getNext();
        });
	}
	
	/**
	 * Example showing how to read the input from a file.  Otherwise it is the same as test1.
	 *
	 */
	@Test
	void test2() throws Exception {
		String file = "testInputFiles\\test2.input"; 
		Reader r = new BufferedReader(new FileReader(file));
		Scanner s = new Scanner(r);
        assertThrows(LexicalException.class, ()->{
		   s.getNext();
        });
	}
	

	
	/**
	 * Another example.  This test case will fail with the provided code, but should pass in your completed Scanner.
	 * @throws Exception
	 */
	@Test
	void test3() throws Exception {
		Reader r = new StringReader(",,::==");
		Scanner s = new Scanner(r);
		Token t;
		show(t= s.getNext());
		assertEquals(t.kind,COMMA);
		assertEquals(t.text,",");
		show(t = s.getNext());
		assertEquals(t.kind,COMMA);
		assertEquals(t.text,",");
		
		show(t = s.getNext());
		assertEquals(t.kind,COLONCOLON);
		assertEquals(t.text,"::");
		
		show(t = s.getNext());
		assertEquals(t.kind,REL_EQEQ);
		assertEquals(t.text,"==");
	}
	
	@Test
	void test4() throws Exception {
		Reader r = new StringReader("\n \f \t \t babd_$ \'fsd\\nfs\';and \"sdfsdfs\\fbdfsd\" --sfsdfsdfsd 01478990");
		Scanner s = new Scanner(r);
		Token t;
		show(t= s.getNext());
		assertEquals(t.kind,NAME);
		assertEquals(t.text,"babd_$");
		
		show(t = s.getNext());
		assertEquals(t.kind,STRINGLIT);
		assertEquals(t.text,"\'fsd\nfs\'");
		
		show(t = s.getNext());
		assertEquals(t.kind,SEMI);
		assertEquals(t.text,";");
		
		show(t = s.getNext());
		assertEquals(t.kind,KW_and);
		assertEquals(t.text,"and");			
		
		show(t = s.getNext());
		assertEquals(t.kind,STRINGLIT);
		assertEquals(t.text,"\"sdfsdfs\fbdfsd\"");
		
		show(t = s.getNext());
		assertEquals(t.kind,INTLIT);
		assertEquals(t.text,"0");
		
		show(t = s.getNext());
		assertEquals(t.kind,INTLIT);
		assertEquals(t.text,"1478990");
	}
	
	@Test 
	void test5() throws Exception{
		Reader r = new StringReader("()");
		Scanner s = new Scanner(r);
		Token t;
		show(t = s.getNext());
		assertEquals(t.kind,LPAREN);
		assertEquals(t.text,"(");
		show(t = s.getNext());
		assertEquals(t.kind,RPAREN);
		assertEquals(t.text,")");		
	}
	
	@Test 
	void test6() throws Exception{
		Reader r = new StringReader(  "x = { \\\\\\\\nprint");
		Scanner s = new Scanner(r);
		Token t;
		show(t = s.getNext());		
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		/*show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());*/
		
	}
	
	
}
