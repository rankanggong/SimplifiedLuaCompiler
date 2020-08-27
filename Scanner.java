
package cop5556fa19;


import static cop5556fa19.Token.Kind.*;

import java.io.IOException;
import java.io.Reader;

// the additional class
import java.util.HashMap;
import cop5556fa19.Token.Kind;


public class Scanner { // the name should be changed
	Reader r;

	int p=0,l=0; //  p represents position and l represents line
	char inputC; // transfer input to char
	String inputS = ""; // the result of Int Literal, String Literal, Keywords and Name
	String result; //refresh the inputS
	
	
	HashMap<String,Kind> keywords = new HashMap<String,Kind>(){
		private static final long serialVersionUID = 1L;{ // Serial UID for this hash map
		put("and",KW_and);
		put("break",KW_break);
		put("do",KW_do);
		put("else",KW_else);
		put("elseif",KW_elseif);
		put("end",KW_end);
		put("false",KW_false);
		put("for",KW_for);
		put("function",KW_function);
		put("goto",KW_goto);
		put("if",KW_if);
		put("in",KW_in);
		put("local",KW_local);
		put("nil",KW_nil);
		put("not",KW_not);
		put("or",KW_or);
		put("repeat",KW_repeat);
		put("return",KW_return);
		put("then",KW_then);
		put("true",KW_true);
		put("until",KW_until);
		put("while",KW_while);
	}}; // add up all keywords
	
	@SuppressWarnings("serial")
	public static class LexicalException extends Exception {	
		public LexicalException(String arg0) {
			super(arg0);
		}
	}
	
	public Scanner(Reader r) throws IOException { // the name should be changed
		this.r = r;
	}
	
	public boolean isKeyword(String keyword) throws Exception{ //  
		return keywords.containsKey(keyword);
	}
	
	public boolean isWhiteSpace(char whiteSpace) { // judge white space
		if(whiteSpace == ' ') {
			return true;
		}
		else if(whiteSpace == '\r'|| whiteSpace == '\n') {
			l++;
			p = 0;
			return true;
		}
		else if(whiteSpace == '\f' || whiteSpace == '\t') {
			return true;
		}
		else
			return false;
	}
	
	public boolean isName(char name,boolean isFirst) throws Exception{ // judge whether the content is name
		if(isFirst == false && ((name>='a'&& name<='z')||(name>='A'&& name<='Z')||name == '_'||name == '$'||name>='0'&& name<='9')) // name can not start with digits
			return true;
		else if(isFirst == true && ((name>='a'&& name<='z')||(name>='A'&& name<='Z')||name == '_'||name == '$'))
			return true;
		else
			return false;
	}
	
	public boolean isDigit(char digit,boolean isFirst) throws Exception{ // judge digit
		if(digit<='9' && digit>'0' && isFirst == true)
			return true;
		else if(digit<='9' && digit>='0' && isFirst == false)
			return true;
		else 
			return false;
	}
	
	public boolean isString(char str,char first) throws Exception{ // judge String Literal and add the character to InputS
		if(str != first) {
			if(str == '\\') {
				str = (char) r.read();
				switch(str) {
				case 'a': 
					inputS += String.valueOf((char)7);
					return true;
				case 'b':
					inputS += "\b";
					return true;
				case 'f':
					inputS += "\f";
					return true;
				case'n':
					inputS += "\n";
					return true;
				case 'r':
					inputS += "\r";
					return true;
				case 't':
					inputS += "\t";
					return true;
				case 'v':
					inputS += String.valueOf((char)11);
					return true;
				case '\"':
					inputS += "\"";
					return true;
				case '\'':
					inputS += "'";
					return true;
				case '\\':
					inputS += "\\";
					return true;
				default: throw new LexicalException("Error: incoorect Escape Sequence (line:"+l+")"+"(pos:"+p+")");
				}
			}
			else { 
				inputS += String.valueOf(str);
				return true;
			}
		}
		else
			return false;
	}
	
	public Token getNext() throws Exception {
		    inputC = (char) r.read();
		    
		    while(isWhiteSpace(inputC)) { // delete all white space
		    	inputC = (char) r.read();
		    }
		    
		    if(isName(inputC,true)) { // input keywords and name
		    	 do{
		    		inputS += String.valueOf(inputC);
		    		r.mark(1);
		    		inputC = (char) r.read();
		    	}while(isName(inputC,false));
		    	r.reset();
		    	if(isKeyword(inputS)) {	
		    		result = inputS;
			    	inputS = "";
	    			return new Token(keywords.get(result),result,p++,l++); }// return keywords
		    	if(!isKeyword(inputS)){
		    		result = inputS;
			    	inputS = "";
			    	return new Token(NAME,result,p++,l++);
		    	}// return names
		    }
		    		  
		    if(inputC == '-') { // delete comment
		    	r.mark(1);
		    	if((char)r.read() == '-') {
		    		do{
		    			inputC = (char) r.read();
		    		}while(!isWhiteSpace(inputC));
		    		inputC = (char) r.read();
				    
				    while(isWhiteSpace(inputC)) { // delete all white space after comment
				    	inputC = (char) r.read();
				    }
		    	}
		    	else {
		    		r.reset();
		    		return new Token(OP_MINUS,"-",p++,l++);
		    	}
		    }  
		    
			if(isName(inputC,true)) { // input keywords and name
					    	 do{
					    		inputS += String.valueOf(inputC);
					    		r.mark(1);
					    		inputC = (char) r.read();
					    		if(isKeyword(inputS)) {	
					    		result = inputS;
						    	inputS = "";
				    			return new Token(keywords.get(result),result,p++,l++); }// return keywords
					    	}while(isName(inputC,false));
					    	r.reset();
					    	if(!isKeyword(inputS)){
					    		result = inputS;
						    	inputS = "";
						    	return new Token(NAME,result,p++,l++);
					    	}// return names
					    }

		    
		    else if(inputC == '0') // get integer literal
		    	return new Token(INTLIT,"0",p++,l++);
		    else if(isDigit(inputC,true)) {
		    	do {
		    		inputS += String.valueOf(inputC);
		    		r.mark(1);
		    		inputC = (char) r.read();
		    	}while(isDigit(inputC,false));
		    	r.reset();
		    	result = inputS;
		    	inputS = "";
		    	return new Token(INTLIT,result,p++,l++);		    	
		    }
		    
		    else if(inputC == '"') { // get String Literal
		    	 do{
		    		inputC = (char) r.read();
		    	}while(isString(inputC,'"'));
		    	result = inputS;
			    inputS = "";
		    	if(inputC == '"')
		    		return new Token(STRINGLIT,"\""+result+"\"",p++,l++);
		    	else
		    		throw new LexicalException("Error: lacking \" at the end of Stirng Literal (line:"+l+")"+"(pos:"+p+")");
		    }
		    
		    else if(inputC == '\'') { 
		    	 do{
		    		inputC = (char) r.read();
		    	}while(isString(inputC,'\''));
		    	result = inputS;
			    inputS = "";
		    	if(inputC == '\'')
		    		return new Token(STRINGLIT,"'"+result+"'",p++,l++);
		    	else
		    		throw new LexicalException("Error: lacking \" at the end of Stirng Literal (line:"+l+")"+"(pos:"+p+")");
		    }
		    
		    else { // get other token
		    	switch(inputC){
		    	case (char) -1:
		    		return new Token(EOF,"eof",p++,l++);
		    	case '+':
		    		return new Token(OP_PLUS,"+",p++,l);
		    	case '*':
		    		return new Token(OP_TIMES,"*",p++,l);
		    	case '/':
		    		r.mark(1);
		    		if(r.read() == '/')
		    			return new Token(OP_DIVDIV,"//",p++,l);
		    		else
		    			r.reset();
		    			return new Token(OP_DIV,"/",p++,l);
		    	case '%':
		    		return new Token(OP_MOD,"%",p++,l);
		    	case '^':
	                return new Token(OP_POW,"^",p++,l);
		    	case '#':
		    		return new Token(OP_HASH,"#",p++,l);
		    	case '&':
		    		return new Token(BIT_AMP,"&",p++,l);
		    	case '~':
		    		r.mark(1);
		    		if(r.read() == '=')
		    			return new Token(REL_NOTEQ,"~=",p++,l);
		    		else
		    			r.reset();
		    			return new Token(BIT_XOR,"~",p++,l);
		    	case '|':
		    		return new Token(BIT_OR,"|",p++,l);
		    	case '<':
		    		r.mark(1);
		    		switch((char)r.read()) {
		    		case '<':
		    			return new Token(BIT_SHIFTL,"<<",p++,l);
		    		case '=':
		    			return new Token(REL_LE,"<=",p++,l);
		    		default:
		    			r.reset();
		    			return new Token(REL_LT,"<",p++,l);
		    		}
		    	case '>':
		    		r.mark(1);
		    		switch((char)r.read()) {		    		
		    		case '>':
		    			return new Token(BIT_SHIFTR,">>",p++,l);
		    		case '=':
		    			return new Token(REL_GE,">=",p++,l);
		    		default:
		    			r.reset();
		    			return new Token(REL_GT,">",p++,l);		
		    		}
		    	case '=':
		    		r.mark(1);
		    		if((char)r.read() == '=')
		    			return new Token(REL_EQEQ,"==",p++,l);
		    		else
		    			r.reset();
		    			return new Token(ASSIGN,"=",p++,l);		    		
		    	case '(':
		    		return new Token(LPAREN,"(",p++,l);
		    	case ')':
		    		return new Token(RPAREN,")",p++,l);
		    	case '{':
		    		return new Token(LCURLY,"{",p++,l);
		    	case '}':
		    		return new Token(RCURLY,"}",p++,l);
		    	case '[':
		    		return new Token(LSQUARE,"[",p++,l);
		    	case ']':
		    		return new Token(RSQUARE,"]",p++,l);
		    	case ':':
		    		r.mark(1);
		    		if(r.read() == ':')
		    			return new Token(COLONCOLON,"::",p++,l);
		    		else
		    			r.reset();
		    			return new Token(COLON,":",p++,l);
		    	case ';':
		    		return new Token(SEMI,";",p++,l);
		    	case ',':
		    		return new Token(COMMA,",",p++,l);
		    	case '.':
		    		r.mark(1);
		    		if((char)r.read() == '.'){
		    			r.mark(1);
		    			if(r.read() == '.')
		    				return new Token(DOTDOTDOT,"...",p++,l);
		    			else 
		    				r.reset();
		    				return new Token(DOTDOT,"..",p++,l);
		    		}
		    		else
		    			r.reset();
		    			return new Token(DOT,".",p++,l);		    				
		    	}
	    
	    	}
		    throw new LexicalException("Eorror: the content you input is illegal!");
	    }
}