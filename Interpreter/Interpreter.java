package interpreter;

import java.io.Reader;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

// import cop5556fa19.BuildSymbolTable;
import static cop5556fa19.Token.Kind;
import cop5556fa19.Token;
import cop5556fa19.Parser;
import cop5556fa19.Scanner;
import cop5556fa19.AST.*;
import interpreter.StaticAnalysis;
import interpreter.built_ins.print;
import interpreter.built_ins.println;
import interpreter.built_ins.toNumber;
import interpreter.ASTVisitorAdapter.TypeException;


public class Interpreter extends ASTVisitorAdapter{

	LuaTable _G; //global environment

	/* Instantiates and initializes global environment
	 * 
	 * Initially, the "standard library" routines implemented in Java are loaded.  For this assignment,
	 * this is just print and println.  
	 * 
	 * These functions impl
	 */
	void init_G() {
		_G = new LuaTable();
		_G.put("print", new print());
		_G.put("println", new println());
		_G.put("toNumber", new toNumber());
	}
	
	ASTNode root; //useful for debugging
		
	public Interpreter() {
		init_G();
	}
	

	
	@SuppressWarnings("unchecked")
	public List<LuaValue> load(Reader r) throws Exception {
		Scanner scanner = new Scanner(r); 
		Parser parser = new Parser(scanner);
		Chunk chunk = parser.parse();
		root = chunk;
		//Perform static analysis to prepare for goto.  Uncomment after u
		StaticAnalysis hg = new StaticAnalysis();
		chunk.visit(hg,null);	
		//Interpret the program and return values returned from chunk.visit
		List<LuaValue> vals = (List<LuaValue>) chunk.visit(this,_G);		
		return vals;
	}
	
	@Override
	public Object visitChunk(Chunk chunk,Object arg) throws Exception{
		Object vals = visitBlock(chunk.block,arg);
		if(vals == null)
			return null;
		
		return vals;
	}
	
	@Override
	public Object visitBlock(Block block,Object arg) throws Exception{
		// TODO
		Object vals = null;
		if(block.stats.size() == 0); // work for the empty situation
		else { 
			boolean isBreak = false;
			Stat lastStat = block.stats.get(block.stats.size()-1);
			for(int i=0; i < block.stats.size(); i++) {
				Stat stat = block.stats.get(i);
				
				if(stat instanceof RetStat) { // break and return will stop the procedures
					vals = (List<LuaValue>)visitStats(stat,arg);
					break;
				}
				else if(stat instanceof StatBreak) {
					vals = -1;
					break;
				}
				else if(stat instanceof StatGoto) {
					vals = (List<LuaValue>)visitStats(stat,arg);
					break;
				}
				else {
					Object res = visitStats(stat,arg);
					if(res instanceof Integer && arg instanceof Boolean) { // when arg is true is should break the while or the repeat
						isBreak = true;
						vals = res;
						break;
					}else if(res instanceof Integer) {
						isBreak = false;
						vals = res;
					}
					else if(res != null) {
						vals = res;
						break;
					}
				}
			}
			if(isBreak == true && lastStat instanceof RetStat) {
				vals = (List<LuaValue>)visitStats(lastStat,arg);
			}
		}
		return vals;
	}
	
	
	@Override
	public Object visitStatAssign(StatAssign statAssign, Object arg) throws Exception{
		for(int i=0; i < statAssign.varList.size(); i++) {
			Exp var = statAssign.varList.get(i);
			
			if(i < statAssign.expList.size()) { 
				Exp exp = statAssign.expList.get(i);
				LuaValue exp_value = (LuaValue) visitExps(exp,arg);
				
				if(var instanceof ExpName) {
					_G.put(new LuaString(((ExpName)var).name),exp_value);
				}else if(var instanceof ExpTableLookup){
					_G.putImplicit((LuaTable)visitExpTableLookup((ExpTableLookup)var,exp_value));
				}else {
					throw new TypeException(statAssign.firstToken,"Error");
				}
				
			}else {
				if(var instanceof ExpName) {
					_G.put(new LuaString(((ExpName)var).name),LuaNil.nil);
				}else if(var instanceof ExpTableLookup){
					_G.putImplicit((LuaTable)visitExpTableLookup((ExpTableLookup)var,LuaNil.nil));
				}else {
					throw new TypeException(statAssign.firstToken,"Error");
				}
			}
		}
		return null;
	}
	
	@Override
	public Object visitStatIf(StatIf statIf, Object arg) throws Exception{
		Object vals = null;
		boolean end = false;
		for(int i = 0; i < statIf.es.size(); i++) {
			Exp exp = statIf.es.get(i);
			Block block = statIf.bs.get(i);
			
			if(torf(exp,arg)) {
				Object res = visitBlock(block,arg);
				if(res != null)
					vals = res;
				end = true;
				break;
			}
		}
		Block block = statIf.bs.get(statIf.es.size()-1);
		if(statIf.bs.size() > statIf.es.size() && end == false) {
			if(visitBlock(block,arg) != null)
				vals = (List<LuaValue>)visitBlock(block,arg);
		}
		return vals;
	}
	
	@Override
	public Object visitStatWhile(StatWhile statWhile, Object arg) throws Exception{
		Object vals = null;
		Block block = statWhile.b;
		 while(torf(statWhile.e,arg)) {
			 Object res =  visitBlock(block,true);
			 if(res instanceof Integer || res instanceof ArrayList) {// When it's in a StatWhile, it will get a LuaValue list 
				 break;
			 }
			 else if(res != null) {
					vals = (LuaValue)res;
					break;
			 }
		 }
		 return vals;
	}
	
	@Override
	public Object visitStatRepeat(StatRepeat statRepeat, Object arg) throws Exception{
		Object vals = null;
		Block block = statRepeat.b;
		 while(!torf(statRepeat.e,true)) {
			 Object res =  visitBlock(block,arg);
			 if(res instanceof Integer || res instanceof ArrayList) {
				 break;
			 }
			 else if(res != null) {
					vals = (LuaValue)res;
					break;
			 }
		 }
		 return vals;
	}
	
	@Override
	public Object visitStatGoto(StatGoto statGoto, Object arg) throws Exception{
		StatLabel label = statGoto.label;
		 return visitStatsAfterLabel(label,arg);
	}
	
	public Object visitStatsAfterLabel(StatLabel statLabel, Object arg) throws Exception{
		Object vals = null;
		Block b = statLabel.enclosingBlock;
		int index = statLabel.index;
		
		for(int i = index; i < b.stats.size(); i++) {
			Stat stat = b.stats.get(i);
			
			if(stat instanceof RetStat) { // break and return will stop the procedures
				vals = (List<LuaValue>)visitStats(stat,arg);
				break;
			}
			else if(stat instanceof StatBreak) 
				break;
			else if(stat instanceof StatGoto) {
				vals = (List<LuaValue>)visitStats(stat,arg);
				break;
			}
			else {
				if(visitStats(stat,arg) != null) {
					vals = (List<LuaValue>)visitStats(stat,arg);
					break;
				}
			}
		}
		 return vals;
	}
	
	@Override
	public Object visitStatDo(StatDo statDo, Object arg) throws Exception{
		Object vals = null;
		Object res = visitBlock(statDo.b,arg);
		if(res != null) 
			vals = res;
		return vals;
	}
	
	@Override
	public Object visitRetStat(RetStat retStat, Object arg) throws Exception {
		List<LuaValue> vals = new ArrayList<>();
		for(int i = 0; i < retStat.el.size(); i++) {
			Exp exp = retStat.el.get(i);
			LuaValue res = (LuaValue) visitExps(exp,arg);
			
			if(res instanceof LuaInt) {
				vals.add((LuaInt)res);
			}
			else if(res instanceof LuaString) {
				if(_G.get((LuaString)res) == LuaNil.nil)
					vals.add(res);
				else
					vals.add(_G.get((LuaString)res));
			}
			else if(res instanceof LuaTable) {
				if(_G.get((LuaTable)res) == LuaNil.nil)
					vals.add(res);
				else
					vals.add(_G.get((LuaTable)res));
			}
			else if(res instanceof LuaNil) {
				vals.add(res);
			}
			else {
				vals.add(res);
			}
		}
		return vals;
	}
	
	@Override
	public Object visitExpNil(ExpNil expNil, Object arg){
		// TODO Auto-generated method stub
		return LuaNil.nil;
	}

	@Override
	public Object visitExpBin(ExpBinary expBin, Object arg) throws Exception {
		LuaValue vals = null;
		LuaValue e0 = (LuaValue) visitExps(expBin.e0,arg);
		Kind op = expBin.op;
		LuaValue e1 = (LuaValue) visitExps(expBin.e1,arg);
		switch(op) {
		case OP_PLUS:
			if(e0 instanceof LuaInt)
				vals = new LuaInt(((LuaInt)e0).v + ((LuaInt)e1).v) ;
			else if (e0 instanceof LuaString) {
				if((((LuaString)e0).value).matches("[0-9]+") && (((LuaString)e1).value).matches("[0-9]+"))
					vals = new LuaInt(Integer.parseInt(((LuaString)e0).value) + Integer.parseInt(((LuaString)e1).value));
				else
					throw new TypeException(expBin.firstToken,"Error");}
			break;
		case OP_MINUS:
			if(e0 instanceof LuaInt)
				vals = new LuaInt(((LuaInt)e0).v - ((LuaInt)e1).v) ;
			else if (e0 instanceof LuaString) {
				if((((LuaString)e0).value).matches("[0-9]+") && (((LuaString)e1).value).matches("[0-9]+"))
					vals = new LuaInt(Integer.parseInt(((LuaString)e0).value) - Integer.parseInt(((LuaString)e1).value));
				else
					throw new TypeException(expBin.firstToken,"Error");}
			break;
		case OP_TIMES:
			if(e0 instanceof LuaInt)
				vals = new LuaInt(((LuaInt)e0).v * ((LuaInt)e1).v) ;
			else if (e0 instanceof LuaString) {
				if((((LuaString)e0).value).matches("[0-9]+") && (((LuaString)e1).value).matches("[0-9]+"))
					vals = new LuaInt(Integer.parseInt(((LuaString)e0).value) * Integer.parseInt(((LuaString)e1).value));
				else
					throw new TypeException(expBin.firstToken,"Error");}
			break;
		case OP_DIV:
			if(e0 instanceof LuaInt)
				vals = new LuaInt(((LuaInt)e0).v / ((LuaInt)e1).v) ;
			else if (e0 instanceof LuaString) {
				if((((LuaString)e0).value).matches("[0-9]+") && (((LuaString)e1).value).matches("[0-9]+"))
					vals = new LuaInt(Integer.parseInt(((LuaString)e0).value) / Integer.parseInt(((LuaString)e1).value));
				else
					throw new TypeException(expBin.firstToken,"Error");}
			break;
		case OP_DIVDIV:
			if(e0 instanceof LuaInt)
				vals = new LuaInt(Math.floorDiv(((LuaInt)e0).v, ((LuaInt)e1).v)) ;
			else if (e0 instanceof LuaString) {
				if((((LuaString)e0).value).matches("[0-9]+") && (((LuaString)e1).value).matches("[0-9]+"))
					vals = new LuaInt(Math.floorDiv(Integer.parseInt(((LuaString)e0).value), Integer.parseInt(((LuaString)e1).value)));
				else
					throw new TypeException(expBin.firstToken,"Error");}
			break;
		case OP_MOD:
			if(e0 instanceof LuaInt)
				vals = new LuaInt(((LuaInt)e0).v % ((LuaInt)e1).v) ;
			else if (e0 instanceof LuaString) {
				if((((LuaString)e0).value).matches("[0-9]+") && (((LuaString)e1).value).matches("[0-9]+"))
					vals = new LuaInt(Integer.parseInt(((LuaString)e0).value) % Integer.parseInt(((LuaString)e1).value));
				else
					throw new TypeException(expBin.firstToken,"Error");}
			break;
		case OP_POW:
			if(e0 instanceof LuaInt)
				vals = new LuaInt((int)Math.pow(((LuaInt)e0).v, ((LuaInt)e1).v)) ;
			else if (e0 instanceof LuaString) {
				if((((LuaString)e0).value).matches("[0-9]+") && (((LuaString)e1).value).matches("[0-9]+"))
					vals = new LuaInt((int)Math.pow(Integer.parseInt(((LuaString)e0).value), Integer.parseInt(((LuaString)e1).value)));
				else
					throw new TypeException(expBin.firstToken,"Error");
			}
			break;
		case BIT_AMP:
			vals = new LuaInt(((LuaInt)e0).v & ((LuaInt)e1).v);
			break;
		case BIT_OR:
			vals = new LuaInt(((LuaInt)e0).v | ((LuaInt)e1).v);
			break;
		case BIT_XOR:
			vals = new LuaInt(((LuaInt)e0).v ^ ((LuaInt)e1).v);
			break;
		case BIT_SHIFTL:
			vals = new LuaInt(((LuaInt)e0).v << ((LuaInt)e1).v);
			break;
		case BIT_SHIFTR:
			vals = new LuaInt(((LuaInt)e0).v >> ((LuaInt)e1).v);
			break;
		case REL_EQEQ:
			if(e0.getClass() == e1.getClass()) {
				if(e0 instanceof LuaInt)
					vals = new LuaBoolean(((LuaInt)e0).v==((LuaInt)e1).v);
				else if(e0 instanceof LuaString)
					vals = new LuaBoolean(((LuaString)e0).value==((LuaString)e1).value);
				else if(e0 instanceof LuaTable)
					vals = new LuaBoolean(((LuaTable)e0).equals(((LuaString)e1).value));
				else if(e0 instanceof LuaBoolean)
					vals = new LuaBoolean(((LuaString)e0).value==((LuaString)e1).value);
				else // the last one is that two values are LuaNil
					vals = new LuaBoolean(true);
			}
			else
				throw new TypeException(expBin.firstToken,"Error!");
			break;
		case REL_NOTEQ:
			if(e0.getClass() == e1.getClass())
				vals = new LuaBoolean((LuaValue)e0 != (LuaValue)e1);
			else
				throw new TypeException(expBin.firstToken,"Error!");
			break;
		case REL_LT:
			if(e1.getClass().getName() == "interpreter.LuaInt" && e1.getClass().getName() == "interpreter.LuaInt")
				vals = new LuaBoolean(((LuaInt)e0).v < ((LuaInt)e1).v);
			else if(e1.getClass().getName() == "interpreter.LuaString" && e1.getClass().getName() == "interpreter.LuaString") {
				if(((LuaString)e0).value.compareTo(((LuaString)e1).value) < 0)
					vals = new LuaBoolean(true);
				else 
					vals = new LuaBoolean(false);
			}
			else
				throw new TypeException(expBin.firstToken,"Error!");
			break;
		case REL_GT:
			if(e0.getClass().getName() == "interpreter.LuaInt" && e1.getClass().getName() == "interpreter.LuaInt")
				vals = new LuaBoolean(((LuaInt)e0).v > ((LuaInt)e1).v);
			else if(e1.getClass().getName() == "interpreter.LuaString" || e1.getClass().getName() == "interpreter.LuaString") {
				if(((LuaString)e0).value.compareTo(((LuaString)e1).value) > 0)
					vals = new LuaBoolean(true);
				else 
					vals = new LuaBoolean(false);
			}
			else
				throw new TypeException(expBin.firstToken,"Error!");
			break;
		case REL_LE:
			if(e1.getClass().getName() == "interpreter.LuaInt" && e1.getClass().getName() == "interpreter.LuaInt")
				vals = new LuaBoolean(((LuaInt)e0).v <= ((LuaInt)e1).v);
			else if(e1.getClass().getName() == "interpreter.LuaString" && e1.getClass().getName() == "interpreter.LuaString") {
				if(((LuaString)e0).value.compareTo(((LuaString)e1).value) <= 0)
					vals = new LuaBoolean(true);
				else 
					vals = new LuaBoolean(false);
			}
			else
				throw new TypeException(expBin.firstToken,"Error!");
			break;
		case REL_GE:
			if(e1.getClass().getName() == "interpreter.LuaInt" && e1.getClass().getName() == "interpreter.LuaInt")
				vals = new LuaBoolean(((LuaInt)e0).v >= ((LuaInt)e1).v);
			else if(e1.getClass().getName() == "interpreter.LuaString" && e1.getClass().getName() == "interpreter.LuaString") {
				if(((LuaString)e0).value.compareTo(((LuaString)e1).value) >= 0)
					vals = new LuaBoolean(true);
				else 
					vals = new LuaBoolean(false);
			}
			else
				throw new TypeException(expBin.firstToken,"Error!");
			break;
		case KW_and:
			if(e0 instanceof LuaBoolean && e1 instanceof LuaBoolean)
				vals = new LuaBoolean(((LuaBoolean)e0).value && ((LuaBoolean)e1).value);
			else if((e0 instanceof LuaBoolean && ((LuaBoolean)e0).value == false) || e0 instanceof LuaNil) // "and" isn't only a boolean operator
				vals = e0;
			else 
				vals = e1;
			break;
		case KW_or:
			if(e0 instanceof LuaBoolean && e1 instanceof LuaBoolean)
				vals = new LuaBoolean(((LuaBoolean)e0).value || ((LuaBoolean)e1).value);
			else if((e0 instanceof LuaBoolean && ((LuaBoolean)e0).value == false) || e0 instanceof LuaNil) // "and" isn't only a boolean operator
				vals = e1;
			else 
				vals = e0;
			break;
		case DOTDOT:
			if(e0 instanceof LuaInt && e1 instanceof LuaInt) {
				vals = new LuaString(String.valueOf(((LuaInt)e0).v)+String.valueOf(((LuaInt)e1).v));
			}else if(e0 instanceof LuaInt && e1 instanceof LuaString) {
				vals = new LuaString(String.valueOf(((LuaInt)e0).v)+((LuaString)e1).value);
			}else if(e0 instanceof LuaString && e1 instanceof LuaInt) {
				vals = new LuaString(((LuaString)e0).value+String.valueOf(((LuaInt)e1).v));
			}else if(e0 instanceof LuaString && e1 instanceof LuaString) {
				vals = new LuaString(((LuaString)e0).value+((LuaString)e1).value);
			}else
				throw new TypeException(expBin.firstToken,"Error!");
			break;
		}
		return vals;
	}

	@Override
	public Object visitUnExp(ExpUnary unExp, Object arg) throws Exception {
		LuaValue vals = null;
		Kind op = unExp.op;
		LuaValue e = (LuaValue)visitExps(unExp.e,arg);
		
		switch(op) {
		case OP_MINUS:
			vals = new LuaInt(-((LuaInt)e).v);
			break;
		case BIT_XOR:
			vals = new LuaInt(~((LuaInt)e).v);
			break;
		case KW_not:
			vals = new LuaBoolean(!((LuaBoolean)e).value);
			break;
		case OP_HASH:
			if(e instanceof LuaString) {
				vals = new LuaInt(((LuaString)e).value.length());
			}
			else
				throw new TypeException(unExp.firstToken,"Error!");
			break;
		}
		return vals;
	}

	@Override
	public Object visitParList(ParList parList, Object arg) throws Exception {
		for(int i=0; i < parList.nameList.size(); i++) {
			visitName(parList.nameList.get(i),arg);
		}
		return null;
	}

	@Override
	public Object visitFunDef(ExpFunction funcDec, Object arg) throws Exception {
		visitFuncBody(funcDec.body,arg);
		return null;
	}

	@Override
	public Object visitName(Name name, Object arg){
		return _G.get(new LuaString(name.name));
	}
	
	@Override
	public Object visitExpInt(ExpInt expInt, Object arg){
		return new LuaInt(expInt.v); 
	}
	
	@Override
	public Object visitExpString(ExpString expString, Object arg){
		return new LuaString(expString.v); 
	}	
	
	@Override
	public Object visitExpTableLookup(ExpTableLookup expTableLookup, Object arg) throws Exception {
		//TODO
		LuaValue table = (LuaValue) visitExps(expTableLookup.table,arg);
		if(table instanceof LuaTable) {
			LuaTable luaTable = (LuaTable) table;
			LuaValue key = (LuaValue) visitExps(expTableLookup.key,arg);
			if(arg == null) {
				return luaTable.get(key);
			}else {
				luaTable.put(key,(LuaValue)arg);
				return null;
			}
		}else {
			throw new TypeException(expTableLookup.firstToken,"Error");
		}
	}
	
	@Override
	public Object visitExpFunctionCall(ExpFunctionCall expFunctionCall, Object arg) throws Exception {
		//TODO
		Exp f = expFunctionCall.f;
		List<Exp> args = expFunctionCall.args;
		List<LuaValue> argument = new ArrayList<>();
		List<LuaValue> vals = new ArrayList<>();
		JavaFunction func = (JavaFunction) _G.get(((ExpName)f).name);
		for(int i=0; i < args.size(); i++) {
			argument.add((LuaValue) visitExps(args.get(i),null));
		}
		vals = func.call(argument);
		return vals;
	}
	
	@Override
	public Object visitExpTable(ExpTable expTableConstr, Object arg) throws Exception {
		//TODO
		LuaTable vals = new LuaTable();
		if(expTableConstr.fields.size() == 0);
		else {
			for(int i=0; i < expTableConstr.fields.size(); i++) {
				Field field = expTableConstr.fields.get(i);
				if(field instanceof FieldImplicitKey) { // ImplicitKey
					vals.putImplicit((LuaValue)visitExps(((FieldImplicitKey)field).exp,arg));
				}
				else if(field instanceof FieldExpKey){ // FieldExpKey
					LuaValue key = (LuaValue) visitExps(((FieldExpKey)field).key,arg);
					LuaValue value = (LuaValue) visitExps(((FieldExpKey)field).value,arg);
					vals.put(key,value);
				}
				else{ // FieldNameKey
					LuaValue exp = (LuaValue) visitExps(((FieldNameKey)field).exp,arg);
					vals.put(new LuaString(((FieldNameKey)field).name.name),exp);		
				}				
			}
		}
		return vals;
	}

	@Override
	public Object visitExpTrue(ExpTrue expTrue, Object arg) {
		return new LuaBoolean(true);
	}

	@Override
	public Object visitExpFalse(ExpFalse expFalse, Object arg) {
		return new LuaBoolean(false);
	}

	@Override
	public Object visitFuncBody(FuncBody funcBody, Object arg) throws Exception {
		visitParList(funcBody.p,arg);
		visitBlock(funcBody.b,arg);
		return null;
	}
	
	@Override
	public Object visitExpName(ExpName expName, Object arg) {
		return _G.get(new LuaString(expName.name));
	}

	
	public Object visitStats(Stat stat,Object arg) throws Exception{
		Object vals = null;
		switch(stat.getClass().getName()) {
		case "cop5556fa19.AST.StatAssign":
			visitStatAssign((StatAssign)stat,arg);
			break;
		case "cop5556fa19.AST.StatIf":
			vals = visitStatIf((StatIf)stat,arg);
			break;
		case "cop5556fa19.AST.StatWhile":
			vals = visitStatWhile((StatWhile)stat,arg);
			break;
		case "cop5556fa19.AST.StatRepeat":
			vals = visitStatRepeat((StatRepeat)stat,arg);
			break;
		case "cop5556fa19.AST.StatBreak":
			break;
		case "cop5556fa19.AST.StatLabel":
			break;	
		case "cop5556fa19.AST.StatGoto":
			vals = visitStatGoto((StatGoto)stat,arg);
			break;
		case "cop5556fa19.AST.RetStat":
			vals = visitRetStat((RetStat)stat,arg);
			break;
		case "cop5556fa19.AST.StatDo":
			vals = visitStatDo((StatDo)stat,arg);
			break;
		}
		return vals;
	}
	
	public Object visitExps(Exp exp,Object arg) throws Exception{
		Object vals = null;
		
		switch(exp.getClass().getName()) { // work for different types of expressions
		case "cop5556fa19.AST.ExpBinary":
			vals = visitExpBin((ExpBinary)exp,arg);
			break;
		case "cop5556fa19.AST.ExpFalse":
			vals = visitExpFalse((ExpFalse)exp,arg);
			break;
		case "cop5556fa19.AST.ExpFunction":
			vals = visitFunDef((ExpFunction)exp,arg);
			break;
		case "cop5556fa19.AST.ExpFunctionCall":
			vals = visitExpFunctionCall((ExpFunctionCall)exp,arg);
			if(((List<LuaValue>)vals).isEmpty())
				vals = LuaNil.nil;
			else
				vals = ((List<LuaValue>)vals).get(0);
			break;
		case "cop5556fa19.AST.ExpInt":
			vals = visitExpInt((ExpInt)exp,arg);
			break;
		case "cop5556fa19.AST.ExpList":
			vals = visitExpName((ExpName)exp,arg);
			break;
		case "cop5556fa19.AST.ExpNil":
			vals = visitExpNil((ExpNil)exp,arg);
			break;
		case "cop5556fa19.AST.ExpString":
			vals = visitExpString((ExpString)exp,arg);
			break;
		case "cop5556fa19.AST.ExpTable":
			vals = visitExpTable((ExpTable)exp,arg);
			break;
		case "cop5556fa19.AST.ExpTableLookup":
			vals = visitExpTableLookup((ExpTableLookup)exp,arg);
			break;
		case "cop5556fa19.AST.ExpTrue":
			vals = visitExpTrue((ExpTrue)exp,arg);
			break;
		case "cop5556fa19.AST.ExpUnary":
			vals = visitUnExp((ExpUnary)exp,arg);
			break;
		case "cop5556fa19.AST.ExpVarArgs":
			vals = visitExpVarArgs((ExpVarArgs)exp,arg);
			break;
		case "cop5556fa19.AST.ExpName":
			vals = visitExpName((ExpName)exp,arg);
			break;
		}
		return vals;
	}
	
	public boolean torf(Exp exp,Object arg) throws Exception{// use to return the boolean value of a expression
		boolean res = false;
		LuaValue val = (LuaValue)visitExps(exp,arg);
		if(val instanceof LuaBoolean) {
			res = ((LuaBoolean)val).value;
		}
		else if(val == LuaNil.nil)
			res = false;
		else
			res = true;
		return res;
	}
}
