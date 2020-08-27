package interpreter;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import interpreter.StaticSemanticException;

import cop5556fa19.AST.ASTVisitor;
import cop5556fa19.AST.Block;
import cop5556fa19.AST.Chunk;
import cop5556fa19.AST.Exp;
import cop5556fa19.AST.ExpBinary;
import cop5556fa19.AST.ExpFalse;
import cop5556fa19.AST.ExpFunction;
import cop5556fa19.AST.ExpFunctionCall;
import cop5556fa19.AST.ExpInt;
import cop5556fa19.AST.ExpList;
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
import cop5556fa19.AST.FieldList;
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

public class StaticAnalysis implements ASTVisitor{
		
	@Override
	public Object visitExpNil(ExpNil expNil, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpBin(ExpBinary expBin, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitExps(expBin.e0,arg);
		visitExps(expBin.e1,arg);
		return null;
	}

	@Override
	public Object visitUnExp(ExpUnary unExp, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitExps(unExp.e,arg);
		return null;
	}

	@Override
	public Object visitExpInt(ExpInt expInt, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpString(ExpString expString, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpTable(ExpTable expTableConstr, Object arg) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0; i < expTableConstr.fields.size(); i++) {
			visitFields(expTableConstr.fields.get(i),arg);
		}
		return null;
	}

	@Override
	public Object visitExpList(ExpList expList, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitParList(ParList parList, Object arg) throws Exception {
		// TODO Auto-generated method stub
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
	public Object visitName(Name name, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		HashMap<Name,StatLabel> map_Label = new HashMap<>(); // use for storing label inside
		HashMap<Name,StatLabel> map_Out = new HashMap<>();
		if(arg != null) // get the label outside this block
			map_Out = (HashMap) arg;  
		
		for(int j=0; j < block.stats.size(); j++) {
			Stat stat = block.stats.get(j);
			
			if(stat instanceof StatLabel) {
				if(!map_Label.containsKey(((StatLabel)stat).label)) {
					((StatLabel)stat).enclosingBlock = block; // point the enclosingBlock to a label
					((StatLabel)stat).index = j;
					map_Label.put(((StatLabel)stat).label,(StatLabel)stat);
				}
				else
					throw new StaticSemanticException(stat.firstToken,"Error! Have two labels with the same name.");
			}
		}
		
		for(int i=0; i < block.stats.size(); i++) { // visit all statements in a block
			Stat stat = block.stats.get(i);
			
			if(stat instanceof StatGoto) {
				StatLabel label = map_Label.get(((StatGoto)stat).name);
				StatLabel out = map_Out.get(((StatGoto)stat).name);
				if(label != null) {
					((StatGoto)stat).label = label;
				}else if(out != null) {
					((StatGoto)stat).label = out;
				}else
					throw new StaticSemanticException(stat.firstToken,"Error! The Goto doesn't have the corresponding label");
			}
		}
		
		for(int a=0; a < block.stats.size(); a++) { // traverse other statements
			Stat stat = block.stats.get(a);
			if(!map_Label.isEmpty()) {
				visitStats(stat,map_Label);
			}else{
				visitStats(stat,map_Out);
			}
		}
		return null;
	}
	
	public void visitStats(Stat stat, Object arg) throws Exception{
		switch(stat.getClass().getName()) { // get the type of the statements
		case "cop5556fa19.AST.StatAssign":
			visitStatAssign((StatAssign)stat,arg);
			break;
		case "cop5556fa19.AST.StatBreak":
			visitStatBreak((StatBreak)stat,arg);
			break;
		case "cop5556fa19.AST.RetStat":
			visitRetStat((RetStat)stat,arg);
			break;
		case "cop5556fa19.AST.StatDo":
			visitStatDo((StatDo)stat,arg);
			break;
		case "cop5556fa19.AST.StatFor":
			visitStatFor((StatFor)stat,arg);
			break;
		case "cop5556fa19.AST.StatForEach":
			visitStatForEach((StatForEach)stat,arg);
			break;
		case "cop5556fa19.AST.StatFunction":
			visitStatFunction((StatFunction)stat,arg);
			break;
		case "cop5556fa19.AST.StatGoto":
			break;
		case "cop5556fa19.AST.StatIf":
			visitStatIf((StatIf)stat,arg);
			break;
		case "cop5556fa19.AST.StatLabel":			
			break;
		case "cop5556fa19.AST.StatLoacalAssign":
			visitStatLocalAssign((StatLocalAssign)stat,arg);
			break;
		case "cop5556fa19.AST.StatLocalFunc":
			visitStatLocalFunc((StatLocalFunc)stat,arg);
			break;
		case"cop5556fa19.AST.StatRepeat":
			visitStatRepeat((StatRepeat)stat,arg);
			break;
		case "cop5556fa19.AST.StatWhile":
			visitStatWhile((StatWhile)stat,arg);
			break;
			}
	}

	@Override
	public Object visitStatBreak(StatBreak statBreak, Object arg, Object arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatBreak(StatBreak statBreak, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatGoto(StatGoto statGoto, Object arg) throws Exception {
		arg = visitName(statGoto.name,arg);
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatDo(StatDo statDo, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitBlock(statDo.b,arg);
		return null;
	}

	@Override
	public Object visitStatWhile(StatWhile statWhile, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitExps(statWhile.e,arg);
		visitBlock(statWhile.b,arg);
		return null;
	}

	@Override
	public Object visitStatRepeat(StatRepeat statRepeat, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitBlock(statRepeat.b,arg);
		visitExps(statRepeat.e,arg);
		return null;
	}

	@Override
	public Object visitStatIf(StatIf statIf, Object arg) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0; i < statIf.es.size(); i++) {
			visitExps(statIf.es.get(i),arg);
		}
		for(int j=0; j< statIf.bs.size(); j++) {
			visitBlock(statIf.bs.get(j),arg);
		}
		return null;
	}

	@Override
	public Object visitStatFor(StatFor statFor1, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitExpName(statFor1.name,arg);
		visitExps(statFor1.ebeg,arg);
		visitExps(statFor1.eend,arg);
		visitExps(statFor1.einc,arg);
		visitBlock(statFor1.g,arg);
		return null;
	}

	@Override
	public Object visitStatForEach(StatForEach statForEach, Object arg) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0; i < statForEach.names.size(); i++) {
			visitExpName(statForEach.names.get(i),arg);
		}
		for(int j=0; j < statForEach.exps.size(); j++) {
			visitExps(statForEach.exps.get(j),arg);
		}
		return null;
	}

	@Override
	public Object visitFuncName(FuncName funcName, Object arg) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0; i< funcName.names.size(); i++) {
			visitExpName(funcName.names.get(i),arg);
		}
		visitExpName(funcName.afterColon,arg);
		return null;
	}

	@Override
	public Object visitStatFunction(StatFunction statFunction, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitFuncName(statFunction.name,arg);
		visitFuncBody(statFunction.body,arg);
		return null;
	}

	@Override
	public Object visitStatLocalFunc(StatLocalFunc statLocalFunc, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitFuncName(statLocalFunc.funcName,arg);
		visitFuncBody(statLocalFunc.funcBody,arg);
		return null;
	}

	@Override
	public Object visitStatLocalAssign(StatLocalAssign statLocalAssign, Object arg) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0; i<statLocalAssign.nameList.size(); i++) {
			visitExpName(statLocalAssign.nameList.get(i),arg);
		}
		for(int j=0; j<statLocalAssign.expList.size(); j++) {
			visitExps(statLocalAssign.expList.get(j),arg);
		}
		return null;
	}

	@Override
	public Object visitRetStat(RetStat retStat, Object arg) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0; i<retStat.el.size(); i++) {
			visitExps(retStat.el.get(i),arg);
		}
		return null;
	}

	@Override
	public Object visitChunk(Chunk chunk, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitBlock(chunk.block,arg);
		return null;
	}

	@Override
	public Object visitFieldExpKey(FieldExpKey fieldExpKey, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitExps(fieldExpKey.key,arg);
		visitExps(fieldExpKey.value,arg);
		return null;
	}

	@Override
	public Object visitFieldNameKey(FieldNameKey fieldNameKey, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitName(fieldNameKey.name,arg);
		visitExps(fieldNameKey.exp,arg);
		return null;
	}

	@Override
	public Object visitFieldImplicitKey(FieldImplicitKey fieldImplicitKey, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitExps(fieldImplicitKey.exp,arg);
		return null;
	}

	@Override
	public Object visitExpTrue(ExpTrue expTrue, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpFalse(ExpFalse expFalse, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitFuncBody(FuncBody funcBody, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitParList(funcBody.p,arg);
		visitBlock(funcBody.b,arg);
		return null;
	}

	@Override
	public Object visitExpVarArgs(ExpVarArgs expVarArgs, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatAssign(StatAssign statAssign, Object arg) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0; i<statAssign.varList.size(); i++) {
			visitExps(statAssign.varList.get(i),arg);
		}
		for(int j=0; j<statAssign.expList.size(); j++) {
			visitExps(statAssign.expList.get(j),arg);
		}
		return null;
	}

	@Override
	public Object visitExpTableLookup(ExpTableLookup expTableLookup, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitExps(expTableLookup.table,arg);
		visitExps(expTableLookup.key,arg);
		return null;
	}

	@Override
	public Object visitExpFunctionCall(ExpFunctionCall expFunctionCall, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitExps(expFunctionCall.f,arg);
		for(int i=0; i<expFunctionCall.args.size(); i++) {
			visitExps(expFunctionCall.args.get(i),arg);
		}
		return null;
	}

	@Override
	public Object visitLabel(StatLabel statLabel, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitName(statLabel.label,arg);
		return null;
	}

	@Override
	public Object visitFieldList(FieldList fieldList, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpName(ExpName expName, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void visitExps(Exp exp,Object arg) throws Exception{
		switch(exp.getClass().getName()) { // work for different types of expressions
		case "cop5556fa19.AST.ExpBinary":
			visitExpBin((ExpBinary)exp,arg);
			break;
		case "cop5556fa19.AST.ExpFalse":
			visitExpFalse((ExpFalse)exp,arg);
			break;
		case "cop5556fa19.AST.ExpFunction":
			visitFunDef((ExpFunction)exp,arg);
			break;
		case "cop5556fa19.AST.ExpFunctionCall":
			visitExpFunctionCall((ExpFunctionCall)exp,arg);
			break;
		case "cop5556fa19.AST.ExpInt":
			visitExpInt((ExpInt)exp,arg);
			break;
		case "cop5556fa19.AST.ExpList":
			visitExpName((ExpName)exp,arg);
			break;
		case "cop5556fa19.AST.ExpNil":
			visitExpNil((ExpNil)exp,arg);
			break;
		case "cop5556fa19.AST.ExpString":
			visitExpString((ExpString)exp,arg);
			break;
		case "cop5556fa19.AST.ExpTable":
			visitExpTable((ExpTable)exp,arg);
			break;
		case "cop5556fa19.AST.ExpTableLookup":
			visitExpTableLookup((ExpTableLookup)exp,arg);
			break;
		case "cop5556fa19.AST.ExpTrue":
			visitExpTrue((ExpTrue)exp,arg);
			break;
		case "cop5556fa19.AST.ExpUnary":
			visitUnExp((ExpUnary)exp,arg);
			break;
		case "cop5556fa19.AST.ExpVarArgs":
			visitExpVarArgs((ExpVarArgs)exp,arg);
			break;
		case "cop5556fa19.AST.ExpName":
			visitExpName((ExpName)exp,arg);
			break;
		}
	}
	
	public void visitFields(Field field,Object arg) throws Exception{
		switch(field.getClass().getName()) {
		case "cop5556fa19.AST.FieldExpKey":
			visitFieldExpKey((FieldExpKey)field,arg);
			break;
		case "cop5556fa19.AST.FieldImplicitKey":
			visitFieldImplicitKey((FieldImplicitKey)field,arg);
			break;
		case "cop5556fa19.AST.FieldNameKey":
			visitFieldNameKey((FieldNameKey)field,arg);
			break;	
		}
	}
	
}
