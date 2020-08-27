package cop5556fa19;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cop5556fa19.AST.Block;
import cop5556fa19.AST.Chunk;
import cop5556fa19.AST.Exp;
import cop5556fa19.AST.ExpBinary;
import cop5556fa19.AST.ExpFalse;
import cop5556fa19.AST.ExpFunction;
import cop5556fa19.AST.ExpFunctionCall;
import cop5556fa19.AST.ExpInt;
// import cop5556fa19.AST.ExpList;
import cop5556fa19.AST.ExpName;
import cop5556fa19.AST.ExpNil;
import cop5556fa19.AST.ExpString;
import cop5556fa19.AST.ExpTable;
import cop5556fa19.AST.ExpTableLookup;
import cop5556fa19.AST.ExpTrue;
import cop5556fa19.AST.ExpUnary;
import cop5556fa19.AST.ExpVarArgs;
import cop5556fa19.AST.Field;
import cop5556fa19.AST.FieldExpKey;
import cop5556fa19.AST.FieldImplicitKey;
import cop5556fa19.AST.FieldNameKey;
import cop5556fa19.AST.FuncBody;
import cop5556fa19.AST.FuncName;
import cop5556fa19.AST.Name;
import cop5556fa19.AST.ParList;
import cop5556fa19.AST.RetStat;
import cop5556fa19.AST.Stat;
import cop5556fa19.AST.StatAssign;
import cop5556fa19.AST.StatBreak;
import cop5556fa19.AST.StatDo;
import cop5556fa19.AST.StatFor;
import cop5556fa19.AST.StatForEach;
import cop5556fa19.AST.StatFunction;
import cop5556fa19.AST.StatGoto;
import cop5556fa19.AST.StatIf;
import cop5556fa19.AST.StatLabel;
import cop5556fa19.AST.StatLocalAssign;
import cop5556fa19.AST.StatLocalFunc;
import cop5556fa19.AST.StatRepeat;
import cop5556fa19.AST.StatWhile;
import cop5556fa19.Token.Kind;

import static cop5556fa19.Token.Kind.*;

public class Parser{
	
	@SuppressWarnings("serial")
	class SyntaxException extends Exception {
		Token t;
		
		public SyntaxException(Token t, String message) {
			super(t.line + ":" + t.pos + " " + message);
		}
	}
	
	final Scanner scanner;
	Token t;  //invariant:  this is the next token


	public Parser(Scanner s) throws Exception {
		this.scanner = s;
		t = scanner.getNext(); //establish invariant
	}
	
	public Chunk parse() throws Exception{
		Chunk chunk = chunk();
		if(!isKind(EOF)) {
			throw new SyntaxException(t,"Please end before the end of input");
		}
		return chunk;
	}
	
	Chunk chunk() throws Exception{ // parseAndShow as a Chunk
		Token first = t;
		Block b = block();
		return new Chunk(first,b);
	}
	
	Block block() throws Exception{
		Token first = t;
		List<Stat> stats = new ArrayList<>();
		if(isKind(EOF));
		else {
			while(!isKind(EOF)){
				if(isKind(SEMI)) { 
					consume();
				}
				else if(isKind(KW_return)) // 0 or 1 retstat
					stats.add(retstat());
				else if(isKind(SEMI,NAME,LPAREN,COLONCOLON,KW_break,KW_goto,KW_do,KW_while,KW_repeat,KW_if,KW_for,KW_function,KW_local))
					stats.add(stat());
				else
					break;
			}
		}
		return new Block(first,stats);
	}
	
	Stat stat() throws Exception{
		Token first = t;
		Stat stat = null;
	
		if(isKind(NAME,LPAREN)) { 
			List<Exp> varList = new ArrayList<>();
			List<Exp> expList = new ArrayList<>();
			varList.add(prefix());
			while(isKind(COMMA)) {
				consume();
				varList.add(prefix());
			}
			match(ASSIGN);
			expList.add(exp());
			while(isKind(COMMA)) {
				consume();
				expList.add(exp());
			}
			stat = new StatAssign(first,varList,expList);
		}
		else if(isKind(COLONCOLON)) {
			List<Stat> stats = new ArrayList<>(); 
			Block enclosingBlock = new Block(t,stats);
			int index = 0;
			consume();
			Name label = getname(consume());
			match(COLONCOLON);
			stat = new StatLabel(first,label,enclosingBlock,index);
		}
		else if(isKind(KW_break)) {
			stat = new StatBreak(consume());
		}
		else if(isKind(KW_goto)) {
			consume();
			Name name = getname(consume());
			stat = new StatGoto(first,name);
		}
		else if(isKind(KW_do)) {
			consume();
			Block b = block();
			match(KW_end);
			stat = new StatDo(first,b);
		}
		else if(isKind(KW_while)) {
			consume();
			Exp e = exp();
			match(KW_do);
			Block b = block();
			match(KW_end);
			stat = new StatWhile(first,e,b);
		}
		else if(isKind(KW_repeat)) {
			consume();
			Block b = block();
			match(KW_until);
			Exp e = exp();
			stat = new StatRepeat(first,b,e);
		}
		else if(isKind(KW_if)) {			
			List<Exp> es = new ArrayList<>();
			List<Block> bs = new ArrayList<>();
			consume();
			es.add(exp());
			match(KW_then);
			bs.add(block());
			while(isKind(KW_elseif)) {
				consume();
				es.add(exp());
				match(KW_then);
				bs.add(block());
			}
			if(isKind(KW_else)) {
				consume();
				bs.add(block());
			}
			match(KW_end);
			stat = new StatIf(first,es,bs);
		}
		else if(isKind(KW_for)){
			consume();
			
			List<Exp> expList = new ArrayList<>();
			List<ExpName> names= new ArrayList<>();						
			ExpName n = new ExpName(consume());		
			
			names.add(n);
			if(isKind(ASSIGN)) { 
				Exp einc = null;
				
				consume();
				Exp ebeg = exp();
				match(COMMA);
				Exp eend = exp();
				if(isKind(COMMA)) {
					consume();
					einc = exp();
				}
				match(KW_do);
				Block g = block();
				match(KW_end);
				stat = new StatFor(first,n,ebeg,eend,einc,g);
			}else {
				while(isKind(COMMA)) {
					consume();
					names.add(new ExpName(consume()));
				}	
				match(KW_in);
				expList = listExp();
				match(KW_do);
				Block b = block();
				match(KW_end);
				stat = new StatForEach(first,names,expList,b);
			}
		}
		else if(isKind(KW_function)) {
			consume();
			FuncName name = funcName();
			FuncBody body = functionBody();
			stat = new StatFunction(first,name,body);
		}
		else if(isKind(KW_local)) {
			consume();
			if(isKind(KW_function)) {
				consume();
				FuncName funcName = funcName();
				FuncBody funcBody = functionBody();
				stat = new StatLocalFunc(first,funcName,funcBody);
			}else {
				List<ExpName> nameList = new ArrayList<>();
				List<Exp> expList = new ArrayList<>();
				nameList.add(new ExpName(consume()));
				while(isKind(COMMA)) {
					consume();
					nameList = listExpName();
				}
				if(isKind(ASSIGN)) {
					consume();
					expList = listExp();
				}
				stat = new StatLocalAssign(first,nameList,expList);
			}
		}
		else {
			error(t.kind);
		}
		return stat;
	}
		
	RetStat retstat() throws Exception{
		Token first = t;
		consume();
		List<Exp> e1 = new ArrayList<>();
		if(!isKind(SEMI,EOF))
			e1 = listExp();
		if(isKind(SEMI))
			consume();
		if(!isKind(EOF,KW_end,KW_elseif,KW_else,KW_until))
			error(t.kind);
		return new RetStat(first,e1);
	}
	
	FuncName funcName() throws Exception{
		Token first = t;
		List<ExpName> names = new ArrayList<>();
		ExpName nameAfterColon = null;
		names.add(new ExpName(consume()));
		while(isKind(DOT)) {
			consume();
			names.add(new ExpName(consume()));
		}
		if(isKind(COLON)) {
			consume();
			nameAfterColon = new ExpName(consume());
		}
		return new FuncName(first,names,nameAfterColon);
	}
	
	List<ExpName> listExpName() throws Exception{
		List<ExpName> names= new ArrayList<>();
		names.add(new ExpName(consume()));
		while(isKind(COMMA)) {
			consume();
			names.add(new ExpName(consume()));
		}
		return names;
	}

	Exp exp() throws Exception { 
		Token first = t;
		Exp e1 = binop_12();
		while (isKind(KW_or)) {
			Token op = consume();
			Exp e0 = binop_12();
			e1 = new ExpBinary(first, e1, op, e0);
		}
		return e1;
	}	
	
	Exp binop_12() throws Exception{ // judge or
		Token first = t;
		Exp e1 = binop_11();
		while(isKind(KW_and)) {
			Token op = consume();
			Exp e0 = binop_11();
			e1 = new  ExpBinary(first, e1, op, e0);
		}
		return e1;
	}
	
	Exp binop_11() throws Exception{ // judge and
		Token first = t;
		Exp e1 = binop_10();
		while(isKind(REL_LT,REL_GT,REL_LE,REL_GE,REL_EQEQ,REL_NOTEQ)) {
			Token op = consume();
			Exp e0 = binop_10();
			e1 = new  ExpBinary(first, e1, op, e0);
		}
		return e1;
	}
	
	Exp binop_10() throws Exception{ // judge < > <= >= ~= ==
		Token first = t;
		Exp e1 = binop_9();
		while(isKind(BIT_OR)) {
			Token op = consume();
			Exp e0 = binop_9();
			e1 = new  ExpBinary(first, e1, op, e0);
		}
		return e1;
	}
	
	Exp binop_9() throws Exception{ // judge |
		Token first = t;
		Exp e1 = binop_8();
		while(isKind(BIT_XOR)) {
			Token op = consume();
			Exp e0 = binop_8();
			e1 = new  ExpBinary(first, e1, op, e0);
		}
		return e1;
	}
	
	Exp binop_8() throws Exception{ // judge ~
		Token first = t;
		Exp e1 = binop_7();
		while(isKind(BIT_AMP)) {
			Token op = consume();
			Exp e0 = binop_7();
			e1 = new  ExpBinary(first, e1, op, e0);
		}
		return e1;
	}
	
	Exp binop_7() throws Exception{ // judge &
		Token first = t;
		Exp e1 = binop_6();
		while(isKind(BIT_SHIFTR,BIT_SHIFTL)) {
			Token op = consume();
			Exp e0 = binop_6();
			e1 = new  ExpBinary(first, e1, op, e0);
		}
		return e1;
	}
	Exp binop_6() throws Exception{ // judge << >>
		Token first = t;
		Exp e0 = binop_5();
		while(isKind(DOTDOT)) {
			Token op = consume();
			Exp e1 = binop_6();
			e0 = new  ExpBinary(first, e0, op, e1);
		}
		return e0;
	}
	
	Exp binop_5() throws Exception{ // judge ..
		Token first = t;
		Exp e1 = binop_4();		
		while(isKind(OP_PLUS,OP_MINUS)) {
			Token op = consume();
			Exp e0 = binop_4();
			e1 = new  ExpBinary(first, e1, op, e0);
		}
		return e1;
	}
	
	Exp binop_4() throws Exception{ // judge + -
		Token first = t;
		Exp e1 = null;
		if(isKind(OP_MINUS,KW_not,OP_HASH,BIT_XOR)) {
				e1 = unop();
		}
		else {
			e1= binop_2();
		}
		while(isKind(OP_TIMES,OP_DIV,OP_DIVDIV,OP_MOD)) {
			Token op = consume();
			Exp e0 = null;
			e0= binop_2();
			e1= new  ExpBinary(first, e1, op, e0);
		}
		return e1;
	}
		
	Exp unop() throws Exception{ // unop
		Token first = t;
		Kind op = consume().kind;
		Exp e = null;
		if(isKind(OP_MINUS,KW_not,OP_HASH,BIT_XOR)) {
			e = new ExpUnary(first,op,unop());
		}
		else
			e = new ExpUnary(first,op,binop_2()); 
		return e;
	}
	
	Exp binop_2() throws Exception{ // judge * / // %
		Token first = t;
		Exp e0 = andExp();
		while(isKind(OP_POW)) {
			Token op = consume();
			Exp e1 = binop_2();
			e0 = new  ExpBinary(first, e0, op, e1);
		}
		return e0;
	}
	
private Exp andExp() throws Exception{ // situations besides "exp binop exp"
		// TODO Auto-generated method stub
		Token first = t;
		Exp e = null;
		
		if(isKind(KW_nil)) {
			e = new ExpNil(consume());
		}
		else if(isKind(KW_false)) {
			e = new ExpFalse(consume());
		}
		else if(isKind(KW_true)) {
			e = new ExpTrue(consume());
		}
		else if(isKind(INTLIT)) {
			e = new ExpInt(consume());
		}
		else if(isKind(STRINGLIT)) {
			e = new ExpString(consume());
		}
		else if(isKind(DOTDOTDOT)) {
			e = new ExpVarArgs(consume());
		}
		else if(isKind(NAME,LPAREN)) { // PrefixExp: NAME & (expressions)
			e = prefix();			
		}
		else if(isKind(KW_function)) { // FunctionDef
			consume();
			FuncBody body= functionBody();
			e = new ExpFunction(first,body);
		}
		else if(isKind(LCURLY)) { // Table Constructor
			consume();
			e = fieldlist();
			match(RCURLY);
		}		
		else{
			error(t.kind);  //I find this is a more useful placeholder than returning null.
		}
		return e;
	}

	Exp prefix() throws Exception{ // prefix and var
		Exp e = null;
		if(isKind(NAME)) {
			e = new ExpName(consume());
		}
		else if(isKind(LPAREN)) {
			consume();
			e = exp();
			match(RPAREN);
		}
		if(isKind(LSQUARE,DOT,LPAREN,LCURLY,STRINGLIT,COLON))
			e = prefixTail(e);
		return e;
	}
	
	Exp prefixTail(Exp pre) throws Exception{ // exp table lookup and functional call
		Token first = t;
		Exp e = null;
		if(isKind(LSQUARE)) {
			consume();
			e = exp();
			match(RSQUARE);
			e = new ExpTableLookup(first,pre,e);
			if(isKind(LSQUARE,DOT,LPAREN,LCURLY,STRINGLIT,COLON))
				e = prefixTail(e);
		}
		else if(isKind(DOT,COLON)) {
			consume();
			e = new ExpString(consume());
			e = new ExpTableLookup(first,pre,e);
			if(isKind(LSQUARE,DOT,LPAREN,LCURLY,STRINGLIT,COLON))
				e = prefixTail(e);
			}
		else if(isKind(LPAREN,LCURLY,STRINGLIT)) {
			List<Exp> args = new ArrayList<>();
			args = args();
			e = new  ExpFunctionCall(first,pre,args);
			if(isKind(LSQUARE,DOT,LPAREN,LCURLY,STRINGLIT,COLON))
				e = prefixTail(e);
		}
		/*
		 * else if(isKind(COLON)) {
		 *
		 *	List<Exp> args = new ArrayList<>();
		 *
		 *	consume();
		 *	Name name = getname(consume());
		 *	args = args();
		 *	e = new ExpFunctionCall(first,pre,args,name);
		 *	if(isKind(LSQUARE,DOT,LPAREN,LCURLY,STRINGLIT,COLON))
		 *		e = prefixTail(e);
		 *	}
		*/
		return e;
	}
	
/**	
	ExpFunctionCall expFunctionCall(Exp f) throws Exception{
		Token first = t;
		List<Exp> args = new ArrayList<>();
		if(isKind(LPAREN,LCURLY,STRINGLIT)) {
			args = args();
		}
		else
			error(t.kind);
		return new ExpFunctionCall(first,f,args);
	}

	
	FunctionCall functionCall(Exp func) throws Exception{
		Token first = t;
		List<Exp> args = new ArrayList<>();
		Name name = null;
		if(isKind(COLON)) {
			consume();
			name = getname(consume());
			args = args();
		}
		else
			error(t.kind);
		return new FunctionCall(first,func,args,name);
	}
**/
	
	List<Exp> args() throws Exception{ // work for arguments in functionCall
		List<Exp> args = new ArrayList<>();
		if(isKind(LPAREN)) {
			consume();
			if(!isKind(RPAREN))
				args = listExp();
			match(RPAREN);
		}else if(isKind(LCURLY)) {
			consume();
			args.add(fieldlist());
			match(RCURLY);
		}
		else if(isKind(STRINGLIT)) {
			args.add(new ExpString(consume()));
		}
		return args;
	}
	
	List<Exp> listExp() throws Exception{
		List<Exp> expList = new ArrayList<>();
		expList.add(exp());
		while(isKind(COMMA)) {
			consume();
			expList.add(exp());
		}
		return expList;
	}

	ExpTable fieldlist() throws Exception{ // field list
		Token first = t;
		List<Field> fields = new ArrayList<>();
		if(isKind(RCURLY)) {}else {
			fields.add(field());
			while(isKind(COMMA,SEMI)) { // field sep
				consume();
				if(isKind(RCURLY))
					break;
				else
					fields.add(field());
			}
		}
		return new ExpTable(first,fields);
	}	
	
	Field field() throws Exception{ // Field expression keys & Field name keys & Field implicit keys
		Field f = null;
		if(isKind(LSQUARE)) { // field expression keys 
			f = fieldExpKey();
		}
		else if(isKind(NAME)) { // field name keys or exp with first token of name
			Token first_name = consume();
			if(isKind(ASSIGN))
				f = fieldNameKey(first_name);
			else {
				Exp e0 = new ExpName(first_name);
				if(isKind(RCURLY,COMMA)) {
				}
				else{
					Token op = null;
					Exp e1 = null;
					if(isKind(KW_or)) {
						op = consume();
						e1 = exp();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(KW_and)) {
						op = consume();
						e1 = binop_12();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(REL_LT,REL_GT,REL_LE,REL_GE,REL_EQEQ,REL_NOTEQ)) {
						op = consume();
						e1 = binop_11();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(BIT_OR)){
						op = consume();
						e1 = binop_10();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(BIT_XOR)){
						op = consume();
						e1 = binop_9();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(BIT_AMP)){
						op = consume();
						e1 = binop_8();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(BIT_SHIFTR,BIT_SHIFTL)){
						op = consume();
						e1 = binop_7();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(DOTDOT)) {
						op = consume();
						e1 = binop_6();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(OP_PLUS,OP_MINUS)) {
						op = consume();
						e1 = binop_5();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(OP_TIMES,OP_DIV,OP_DIVDIV,OP_MOD)) {
						op = consume();
						e1 = binop_4();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(OP_POW)) {
						op = consume();
						e1 = binop_2();
						e0 = new ExpBinary(first_name,e0,op,e1);
					}
					else if(isKind(LSQUARE,DOT,LPAREN,LCURLY,STRINGLIT,COLON)) {
						e0 = prefixTail(e0);
					}
					else
						error(t.kind);
				}
				f = new FieldImplicitKey(first_name,e0);
			}
		}
		else {
			f = fieldImplicitKey(); // work for another expression
		}
		return f;
	}
	
	FieldExpKey fieldExpKey() throws Exception{
		Token first = t; 
		consume();
		Exp key = exp();
		match(RSQUARE);
		match(ASSIGN);
		Exp value = exp();
		return new FieldExpKey(first,key,value);
	}
	
	FieldNameKey fieldNameKey(Token first_name) throws Exception{
		Name name = getname(first_name);
		consume();
		Exp exp = exp();
		return new FieldNameKey(first_name,name,exp);
	}
	
	FieldImplicitKey fieldImplicitKey() throws Exception{
		Token first = t;
		Exp exp = exp();
		return new FieldImplicitKey(first,exp);
	}
	
	FuncBody functionBody() throws Exception{ // function body
		Token first = t;
		ParList plist = null;
		Block b = null;
		if(isKind(LPAREN)) {
			consume();
			if(isKind(RPAREN)) {
				consume();
			}else {
				plist = parlist();
				match(RPAREN);
			}
			b = block();
			match(KW_end);
			}
		return new FuncBody(first,plist,b);
	}
	
	ParList parlist() throws Exception{ // parlist
		Token first = t;
		List<Name> names = new ArrayList<>();
		boolean hasvarargs = false;
		
		if(isKind(DOTDOTDOT)) {
			hasvarargs = true;
			consume();
		}else {
			names.add(getname(consume()));
			while(isKind(COMMA)) {  // name list
				consume();
				if(isKind(NAME))
					names.add(getname(consume()));
				else if(isKind(DOTDOTDOT)) {
					hasvarargs = true;
					consume();
				}
			}
		}	
		
		return new ParList(first,names,hasvarargs);
	}
	
	List<Name> listName() throws Exception{
		List<Name> names= new ArrayList<>();
		names.add(getname(consume()));
		while(isKind(COMMA)) {
			consume();
			names.add(getname(consume()));
		}
		return names;
	}
	
	
	Name getname(Token first) throws Exception{ // return the text of a NAME
		return new Name(first,first.getName());
	}

	protected boolean isKind(Kind kind) {
		return t.kind == kind;
	}

	protected boolean isKind(Kind... kinds) {
		for (Kind k : kinds) {
			if (k == t.kind)
				return true;
		}
		return false;
	}

	/**
	 * @param kind
	 * @return
	 * @throws Exception
	 */
	Token match(Kind kind) throws Exception {
		Token tmp = t;
		if (isKind(kind)) {
			consume();
			return tmp;
		}
		error(kind);
		return null; // unreachable
	}

	/**
	 * @param kind
	 * @return
	 * @throws Exception
	 */
	Token match(Kind... kinds) throws Exception {
		Token tmp = t;
		if (isKind(kinds)) {
			consume();
			return tmp;
		}
		StringBuilder sb = new StringBuilder();
		for (Kind kind1 : kinds) {
			sb.append(kind1).append(kind1).append(" ");
		}
		error(kinds);
		return null; // unreachable
	}

	Token consume() throws Exception { // put out the last token 
		Token tmp = t;
        t = scanner.getNext();
		return tmp;
	}
	
	void error(Kind... expectedKinds) throws SyntaxException {
		String kinds = Arrays.toString(expectedKinds);
		String message;
		if (expectedKinds.length == 1) {
			message = "Expected " + kinds + " at " + t.line + ":" + t.pos;
		} else {
			message = "Expected one of" + kinds + " at " + t.line + ":" + t.pos;
		}
		throw new SyntaxException(t, message);
	}

	void error(Token t, String m) throws SyntaxException {
		String message = m + " at " + t.line + ":" + t.pos;
		throw new SyntaxException(t, message);
	}
	


}
