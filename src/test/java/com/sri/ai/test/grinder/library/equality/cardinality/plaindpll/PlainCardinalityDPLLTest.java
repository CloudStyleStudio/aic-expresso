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
package com.sri.ai.test.grinder.library.equality.cardinality.plaindpll;

import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.expresso.helper.Expressions.parse;
import static com.sri.ai.util.Util.list;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.core.DefaultIntensionalMultiSet;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.helper.GrinderUtil;
import com.sri.ai.grinder.library.DirectCardinalityComputationFactory;
import com.sri.ai.grinder.library.FunctorConstants;
import com.sri.ai.grinder.library.equality.cardinality.direct.core.Normalize;
import com.sri.ai.grinder.library.equality.cardinality.plaindpll.PlainCardinalityDPLL;

@Beta
public class PlainCardinalityDPLLTest extends AbstractPlainDPLLTest {
	
	protected void runSymbolicAndNonSymbolicTests(Expression expression, Collection<String> indicesStrings, Expression expected) {
		
		super.runTest(expression, indicesStrings, expected, true /* no type size */);
		
		RewritingProcess process = DirectCardinalityComputationFactory.newCardinalityProcess();
		
		process.putGlobalObject(everythingCardinality, DEFAULT_EVERYTHING_CARDINALITY_VALUE);
		
		Normalize normalizer = new Normalize();
		
		Map<Expression, Expression> freeSymbolsAndTypes = new HashMap<Expression, Expression>();
		for (Expression freeSymbol : Expressions.freeSymbols(expected, process)) {
			freeSymbolsAndTypes.put(freeSymbol, everythingType);
		}
		process = GrinderUtil.extendContextualSymbols(freeSymbolsAndTypes, process);
				
		Expression expectedWithTypeSize = normalizer.rewrite(expected, process);
	
		super.runTest(expression, indicesStrings, expectedWithTypeSize, false /* use type size */);
	}

	@Override
	protected Expression makeProblem(Expression expression, List<Expression> indexExpressions) {
		Expression set = new DefaultIntensionalMultiSet(indexExpressions, Expressions.ONE, expression);
		Expression problem = apply(FunctorConstants.CARDINALITY, set);
		return problem;
	}

	@Override
	protected PlainCardinalityDPLL makeRewriter() {
		return new PlainCardinalityDPLL();
	}

	@Test
	public void test() {
		
		Expression expression;
		Expression expected;
		Collection<String> indices;
		
		GrinderUtil.setMinimumOutputForProfiling();
		
		expression = parse("true");
		indices    = null; // means all variables
		expected   = parse("1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);

		expression = parse("false");
		indices    = null; // means all variables
		expected   = parse("0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);

		// tests answer completeness
		expression  = parse("(Y = a and X = T) or (Y != a and X = T1 and T = T1)");
		indices     = list("Y");
		// original algorithm provided this incomplete solution due to incomplete condition-applying-on-solution algorithm used in externalization
		// expected = parse("if X = T then if T = T1 then if T = T1 then 10 else 1 else 1 else (if X = T1 then if T = T1 then 9 else 0 else 0)");
		expected    = parse("if X = T then if T = T1 then | Everything | else 1 else 0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);

		
		
		expression = parse("X != Y");
		indices    = list("X");
		expected   = parse("| Everything | - 1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != Y and X != a");
		indices    = list("X");
		expected   = parse("if Y = a then | Everything | - 1 else | Everything | - 2");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != Y and X != Z and X != a");
		indices    = list("X");
		expected   = parse("if Y = Z then if Z = a then | Everything | - 1 else | Everything | - 2 else (if Y = a then | Everything | - 2 else (if Z = a then | Everything | - 2 else | Everything | - 3))");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("Y = a and X != Y and X != a");
		indices    = list("X");
		expected   = parse("if Y = a then | Everything | - 1 else 0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		

		expression = parse("X1 != X2 and (X2 = X3 or X2 = X4) and X3 = X1 and X4 = X1");
		indices    = null; // means all variables
		expected   = parse("0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X1 != X2 and X2 != X0 and X1 != X0");
		indices    = null; // means all variables
		expected   = parse("(| Everything | - 1) * | Everything | * (| Everything | - 2)");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("true");
		indices    = null; // means all variables
		expected   = parse("1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("true");
		indices    = list("X", "Y");
		expected   = parse("| Everything | * | Everything |");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("false");
		indices    = null; // means all variables
		expected   = parse("0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("false");
		indices    = list("X", "Y");
		expected   = parse("0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		
		expression = parse("X = a");
		indices    = null; // means all variables
		expected   = parse("1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a");
		indices    = null; // means all variables
		expected   = parse("| Everything | - 1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X = a");
		indices    = list("X", "Y");
		expected   = parse("| Everything |");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a");
		indices    = list("X", "Y");
		expected   = parse("(| Everything | - 1)*| Everything |");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X = a and Y != b");
		indices    = list("X", "Y");
		expected   = parse("| Everything | - 1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a and Y != b");
		indices    = list("X", "Y");
		expected   = parse("(| Everything | - 1)*(| Everything | - 1)");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a or Y != b");
		indices    = list("X", "Y");
		expected   = parse("| Everything | + -1 + (| Everything | - 1) * | Everything |");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a and X != Y and Y != a");
		indices    = null;
		expected   = parse("(| Everything | - 2) * (| Everything | - 1)");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
}
}
