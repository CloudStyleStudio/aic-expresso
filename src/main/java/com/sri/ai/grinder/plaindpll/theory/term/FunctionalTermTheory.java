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
package com.sri.ai.grinder.plaindpll.theory.term;

import java.util.Iterator;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.library.FunctorConstants;
import com.sri.ai.grinder.library.equality.formula.FormulaUtil;
import com.sri.ai.grinder.plaindpll.api.TermTheory;

@Beta
/** 
 * A {@link TermTheory} for functional terms (including symbol terms).
 */
public class FunctionalTermTheory implements TermTheory {


	@Override
	public boolean isTerm(Expression expression, RewritingProcess process) {
		boolean result = process.isUniquelyNamedConstant(expression) || isVariableOrFunctionApplication(expression, process);
		return result;
	}

	private boolean isVariableOrFunctionApplication(Expression expression, RewritingProcess process) {
		boolean result =
				process.isVariable(expression)
		||
		(
				expression.getSyntacticFormType().equals("Function application")
				&&
				! FormulaUtil.functorIsAnEqualityLogicalConnectiveIncludingConditionals(expression)
				&&
				! expression.hasFunctor(FunctorConstants.CARDINALITY) // TODO: make this less hard-coded
				&&
				! expression.hasFunctor(FunctorConstants.PLUS)
				&&
				! expression.hasFunctor(FunctorConstants.MINUS)
				&&
				! expression.hasFunctor(FunctorConstants.TIMES)
				&&
				! expression.hasFunctor(FunctorConstants.DIVISION)
				&&
				! expression.hasFunctor(FunctorConstants.MINUS)
				);
		return result;
	}

	@Override
	public boolean isVariableTerm(Expression expression, RewritingProcess process) {
		boolean result =
				isTerm(expression, process)
				&&
				! process.isUniquelyNamedConstant(expression);
		return result;
	}

	@Override
	public boolean equalityBetweenTermsImpliesFurtherFacts() {
		return true; // yes, X = Y => p(X) = p(Y), for example
	}

	@Override
	public boolean disequalityBetweenTermsImpliesFurtherFacts() {
		return true; // yes, p(X) != p(Y) => X != Y, for example
	}

	@Override
	public Expression getSplitterTowardDisunifyingDistinctTerms(Expression term, Expression anotherTerm, RewritingProcess process) {
		Expression result = null;
		if (term.getSyntacticFormType().equals("Function application") &&
				anotherTerm.getSyntacticFormType().equals("Function application") &&
				term.numberOfArguments() == anotherTerm.numberOfArguments() &&
				term.getFunctor().equals(anotherTerm.getFunctor())) {
			
			Iterator<Expression> argument1Iterator =        term.getArguments().iterator();
			Iterator<Expression> argument2Iterator = anotherTerm.getArguments().iterator();
			result = null;
			while (result == null && argument1Iterator.hasNext()) {
				Expression argument1 = argument1Iterator.next();
				Expression argument2 = argument2Iterator.next();
				result = getSplitterTowardDisunifyingDistinctTerms(argument1, argument2, process);
			}
		}
		// now it is either null, or a splitter for disunifying arguments
		return result;
	}

	@Override
	public Expression normalizeTermModuloRepresentatives(Expression term, Function<Expression, Expression> getRepresentative, RewritingProcess process) {
		Expression result = Expressions.replaceImmediateSubexpressions(term, getRepresentative);
		return result;
	}

	@Override
	public boolean termsHaveNoArguments() {
		return false;
	}
}