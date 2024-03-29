/*
 * Copyright (c) 2013, SRI International
 * All rights reserved.
 * Licensed under the The BSD 3-Clause License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * http://opensource.org/licenses/BSD-3-Clause
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of the aic-expresso nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sri.ai.grinder.library.equality.cardinality.helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.library.Disequality;
import com.sri.ai.grinder.library.Equality;
import com.sri.ai.grinder.library.boole.And;
import com.sri.ai.grinder.library.boole.Or;
import com.sri.ai.grinder.library.equality.formula.FormulaToCNF;
import com.sri.ai.grinder.library.equality.formula.FormulaUtil;
import com.sri.ai.grinder.library.equality.formula.PropositionalCNFListener;

@Beta
public class FormulaToSharpSAT {
	
	public static void convertToSharpSAT(Expression formula, RewritingProcess process, PropositionalCNFListener conversionListener) {
		Expression cnfFormula = FormulaToCNF.convertToExponentialCNF(formula, process);
		int minimumTypeSize = FormulaUtil.getConstants(cnfFormula, process).size() + Expressions.getVariables(cnfFormula, process).size();
		
		convertToSharpSAT(cnfFormula, minimumTypeSize, process, conversionListener);
	}
	
	public static void convertToSharpSAT(Expression formula, int typeSize, RewritingProcess process, PropositionalCNFListener conversionListener) {
		boolean stopConversion = false;
		Expression cnfFormula = FormulaToCNF.convertToExponentialCNF(formula, process);
		
		if (cnfFormula.equals(Expressions.TRUE)) {
			conversionListener.end(PropositionalCNFListener.EndState.TRIVIAL_TAUTOLOGY);
		}
		else if (cnfFormula.equals(Expressions.FALSE)) {
			conversionListener.end(PropositionalCNFListener.EndState.TRIVIAL_CONTRADICTION);
		}
		else {
			Map<Expression, Integer> constIds = getConstants(cnfFormula, process);
			Map<Expression, Integer> varIds   = getVariables(cnfFormula, process);
			
			// Converting the problem to a propositional problem: Assume we are
			// X1=X2 or X1!=a1 where |type(X1)|=|type(X2)|=3. 
			// So, F is "X1=X2 or X1!=a1". First, we name the 
			// other two elements in the type as a2 and a3. 
			// Then, we define the following propositional variables: 
			//	v1: X1 = a1
			//	v2: X1 = a2
			//	v3: X1 = a3
			//	v4: X2 = a1
			//	v5: X2 = a2
			//	v6: X2 = a3
			
			if (constIds.size() > typeSize) {
				throw new IllegalArgumentException("Domain size too small to represent constants : "+constIds.keySet());
			}
			else if (constIds.size() < typeSize) {
				// Extend with additional constants to represent the full type size
				int id = 1;
				while (constIds.size() < typeSize) {
					Expression newConstant = Expressions.makeSymbol("a" + id);
					if (!constIds.containsKey(newConstant)) {
						constIds.put(newConstant, constIds.size()+1);
					}
					id++;
				}
			}
			
			conversionListener.start(varIds.size() * typeSize);
	
			//
			// Describe the type
			stopConversion = describeType(conversionListener, varIds.size(), typeSize);
			
			if (!stopConversion) {
				//
				// Describe the formula
				for (Expression fClause : cnfFormula.getArguments()) {
					
					Expression propEquivFormula    = expandHardLiterals(fClause, constIds.keySet(), typeSize, process);
					// Ensure expansion in CNF form so we can just read off the clauses.
					Expression cnfPropEquivFormula = FormulaToCNF.convertToExponentialCNF(propEquivFormula, process);
					
					for (Expression pClause : cnfPropEquivFormula.getArguments()) {
						int[] clause = new int[pClause.numberOfArguments()];
						int current = 0;
						for (Expression literal : pClause.getArguments()) {
							int sign    = Equality.isEquality(literal) ? 1 : -1;
							int varId   = varIds.get(literal.get(0));
							int constId = constIds.get(literal.get(1));					
							
							clause[current] = getPropVarId(varId, constId, typeSize) * sign;
		
							current++;
						}
						if (!conversionListener.processClauseAndContinue(clause)) {
							stopConversion = true;
							break;
						}
					}
					if (stopConversion) {
						break;
					}
				}
			}
			
			conversionListener.end(PropositionalCNFListener.EndState.NEEDS_SOLVING);
		}
	}
	
	//
	// PRIVATE
	//
	private static Map<Expression, Integer> getConstants(Expression formula, final RewritingProcess process) {
		Set<Expression> consts = FormulaUtil.getConstants(formula, process);
		
		Map<Expression, Integer> constIds = new LinkedHashMap<Expression, Integer>();
		int id = 0;
		for (Expression cons : consts) {
			constIds.put(cons, ++id);
		}
		
		return constIds;
	}
	
	private static Map<Expression, Integer> getVariables(Expression formula, final RewritingProcess process) {
		Set<Expression> vars   = new LinkedHashSet<Expression>();
		
		vars.addAll(Expressions.getVariables(formula, process));
		
		Map<Expression, Integer> varIds = new LinkedHashMap<Expression, Integer>();
		int id = 0;
		for (Expression var : vars) {
			varIds.put(var, ++id);
		}
		
		return varIds;
	}
	
	private static boolean describeType(PropositionalCNFListener conversionListener, int numVars, int typeSize) {
		boolean stopConversion = false;
		// The first series of clauses should determine that 
		// "X1 equals to a1 or a2 or a3". Similarly, we have to specify that 
		// "X2 equals to a1 or a2 or a3". The following clauses describe these:
		// v1 or v2 or v3	
		// v4 or v5 or v6
		for (int v = 0; v < numVars && !stopConversion; v++) {
			int[] clause = new int[typeSize];
			for (int i = 0; i < typeSize; i++) {
				clause[i] = (v*typeSize) + i + 1;
			}
			if (!conversionListener.processClauseAndContinue(clause)) {
				stopConversion = true;
			}
		}
		
		// Then we have to enforce that X1 and X2 can be at most one of a1, a2, a3. 
		// So if "X1=a1 => X1!=a2" and "X1=a1 => X1!=a3". We then have the following 
		// clauses:
		// -v1 or -v2
		// -v1 or -v3	
		// -v2 or -v3	
		// -v4 or -v5	
		// -v4 or -v6	
		// -v5 or -v6
		for (int b = 0; b < (numVars*typeSize) && !stopConversion; b += typeSize) {
			for (int d = 0; d < typeSize && !stopConversion; d++) {
				int svidx = b + d + 1;
				for (int i = d+1; i < typeSize && !stopConversion; i++) {
					int[] clause = new int[2];
					clause[0] = 0 - svidx;
					clause[1] = 0 - svidx - (i-d);
					if (!conversionListener.processClauseAndContinue(clause)) {
						stopConversion = true;
					}
				}
			}
		}
		
		return stopConversion;
	}
	
	/**
	 * "Hard Literals" are of the form "X=Y" or "X!=Y" (i.e. no constant) and
	 * need to be expanded. For instance, the literal "X1=X2" need be defined
	 * using the propositional variables above. First, note that "X1=X2" is
	 * equivalent to:
	 * "(X1=a1 and X2=a1) or (X1=a2 and X2=a2) or (X1=a3 and X2=a3)". 
	 * Similarly "X1!=X2" is equivalent to:
	 * "(X1=a1 and X2!=a1) or (X1=a2 and X2!=a2) or (X1=a3 and X2!=a3)".
	 */
	private static Expression expandHardLiterals(Expression clause, Set<Expression> consts, int typeSize, RewritingProcess process) {
		Expression result = clause;
		
		List<Expression> disjuncts = new ArrayList<Expression>();
		for (Expression literal : clause.getArguments()) {
			if (process.isVariable(literal.get(0)) && process.isVariable(literal.get(1))) {
				for (Expression cons : consts) {
					List<Expression> conjuncts = new ArrayList<Expression>();
					
					conjuncts.add(Equality.make(literal.get(0), cons));
					if (Equality.isEquality(literal)) {
						conjuncts.add(Equality.make(literal.get(1), cons));
					}
					else {
						conjuncts.add(Disequality.make(literal.get(1), cons));
					}
					
					disjuncts.add(And.make(conjuncts));
				}
			}
			else {
				// Is a literal that maps to a propositional variable (i.e. has a constant).
				disjuncts.add(literal);
			}
		}
		
		result = Expressions.makeExpressionOnSyntaxTreeWithLabelAndSubTrees(Or.FUNCTOR, disjuncts);
 		
		return result;
	}
	
	private static int getPropVarId(int varId, int constId, int typeSize) {
		return (((varId-1) * typeSize) + constId);
	}
}
