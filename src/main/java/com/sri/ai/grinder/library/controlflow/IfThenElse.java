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
package com.sri.ai.grinder.library.controlflow;

import java.util.Arrays;
import java.util.Collections;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.SubExpressionAddress;
import com.sri.ai.expresso.core.SyntaxTreeBasedSubExpressionAddress;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.AbstractRewriter;
import com.sri.ai.grinder.core.HasKind;
import com.sri.ai.grinder.library.Equality;
import com.sri.ai.grinder.library.FunctorConstants;
import com.sri.ai.grinder.library.boole.And;
import com.sri.ai.grinder.library.boole.Not;
import com.sri.ai.grinder.library.boole.Or;
import com.sri.ai.util.base.Pair;

/**
 * An atomic rewriter for conditional expressions. Returns the then or else
 * branches directly if condition is trivial. Includes related helper methods.
 * 
 * @author braz
 * 
 */
@Beta
public class IfThenElse extends AbstractRewriter {

	private static final Expression NOT_FUNCTOR = Expressions.makeSymbol(FunctorConstants.NOT);
	//
	private static final SubExpressionAddress _pathToFunctor   = SyntaxTreeBasedSubExpressionAddress.get(Collections.unmodifiableList(Arrays.asList(-1)));
	private static final SubExpressionAddress _pathToCondition = SyntaxTreeBasedSubExpressionAddress.get(Collections.unmodifiableList(Arrays.asList(0)));
	private static final SubExpressionAddress _pathToThen      = SyntaxTreeBasedSubExpressionAddress.get(Collections.unmodifiableList(Arrays.asList(1)));
	private static final SubExpressionAddress _pathToElse      = SyntaxTreeBasedSubExpressionAddress.get(Collections.unmodifiableList(Arrays.asList(2)));

	public IfThenElse() {
		this.setReifiedTests(new HasKind(FunctorConstants.IF_THEN_ELSE));
	}
	
	@Override
	public Expression rewriteAfterBookkeeping(Expression expression, RewritingProcess process) {
		
		if (expression.get(0).equals(true)) {
			return expression.get(1);
		}
		if (expression.get(0).equals(false)) {
			return expression.get(2);
		}
		
		return expression;
	}
	
	/**
	 * Same as {@link #makeIfDistinctFrom(Expression, Expression, Expression, Expression, boolean)}
	 * with last argument equal to to true.
	 */
	public static Expression makeIfDistinctFrom(Expression original, Expression newCondition, Expression newThenBranch, Expression newElseBranch) {
		return makeIfDistinctFrom(original, newCondition, newThenBranch, newElseBranch, true);
	}
	
	/**
	 * Given an original if then else expression and a condition, then-branch and else-branch,
	 * makes and returns a new if then else expression with these components only if at least one of them is a different instance from
	 * the arguments of the original one, or returns the original otherwise.
	 * This is useful if the arguments of the original expression have been through some potential transformation
	 * and we want to return a new if then else that is nonetheless guaranteed to be the same instance if no transformation actually took place.
	 */
	public static Expression makeIfDistinctFrom(Expression original, Expression newCondition, Expression newThenBranch, Expression newElseBranch, boolean simplifyToConditionIfPossible) {
		Expression result;
		if (newCondition != condition(original) || newThenBranch != thenBranch(original) || newElseBranch != elseBranch(original)) {
			result = make(newCondition, newThenBranch, newElseBranch, simplifyToConditionIfPossible);
		}
		else {
			result = original;
		}
		return result;
	}

	public static Expression flipBranchesWithThisCondition(Expression ifThenElse, Expression newCondition) {
		Expression thenBranch = thenBranch(ifThenElse);
		Expression elseBranch = elseBranch(ifThenElse);
		ifThenElse = make(newCondition, elseBranch, thenBranch);
		return ifThenElse;
	}

	public static Expression makeBooleanFormulaEquivalentToIfThenElse(Expression ifThenElse) {
		Expression condition    = condition(ifThenElse);
		Expression thenBranch   = thenBranch(ifThenElse);
		Expression elseBranch   = elseBranch(ifThenElse);
		Expression notCondition = Not.make(condition);
		Expression equivalent   = Or.make(And.make(condition, thenBranch), And.make(notCondition, elseBranch));
		return equivalent;
	}

	public static SubExpressionAddress getPathToFunctor() {
		return _pathToFunctor;
	}
	
	public static SubExpressionAddress getPathToCondition() {
		return _pathToCondition;
	}
	
	public static SubExpressionAddress getPathToThen() {
		return _pathToThen;
	}
	
	public static SubExpressionAddress getPathToElse() {
		return _pathToElse;
	}

	/**
	 * Make an if then else expression, returning the then or else branches directly if condition is trivial,
	 * or the condition (or its negation) if the branches are trivial.
	 */
	public static Expression make(Expression condition, Expression thenBranch, Expression elseBranch) {
		if (condition.equals(true)) {
			return thenBranch;
		}
		if (condition.equals(false)) {
			return elseBranch;
		}
		if (thenBranch.equals(true) && elseBranch.equals(false)) {
			return condition;
		}
//		if (thenBranch.equals(elseBranch)) { // breaking some code even though it should not; we want to have it eventually
//			return thenBranch;
//		}
//		if (thenBranch.equals(false) && elseBranch.equals(true)) { // this may violate normalization routines in which 'not' is moved in
//			return Not.make(condition);
//		}
		Expression result = Expressions.makeExpressionOnSyntaxTreeWithLabelAndSubTrees(FunctorConstants.IF_THEN_ELSE, condition, thenBranch, elseBranch);
		return result;
	}

	/**
	 * Make an if then else expression, returning the then or else branches directly if condition is trivial.
	 * Flag 'simplifyToConditionIfPossible' authorizes simplifications "if C then true else false -> C" to occur.
	 */
	public static Expression make(Expression condition, Expression thenBranch, Expression elseBranch, boolean simplifyToConditionIfPossible) {
		if (condition.equals(true)) {
			return thenBranch;
		}
		if (condition.equals(false)) {
			return elseBranch;
		}
		if (simplifyToConditionIfPossible) {
			if (thenBranch.equals(true) && elseBranch.equals(false)) {
				return condition;
			}
			//		if (thenBranch.equals(false) && elseBranch.equals(true)) { // this may violate normalization routines in which 'not' is moved in
			//			return Not.make(condition);
			//		}
		}
		if (thenBranch.equals(elseBranch)) {
			return thenBranch;
		}
		Expression result = Expressions.makeExpressionOnSyntaxTreeWithLabelAndSubTrees(FunctorConstants.IF_THEN_ELSE, condition, thenBranch, elseBranch);
		return result;
	}

	/**
	 * Makes an if then else expression by receiving the branches but, instead of relying on the argument order to decide which branch is the then branch and which one is the else branch,
	 * receives their respective indices (they must be 1 and 2, or 2 and 1 -- index 0 is the index of the condition itself).
	 */
	public static Expression make(Expression condition, int indexOfFirstBranch, Expression firstBranch, int indexOfSecondBranch, Expression secondBranch) {
		if (indexOfFirstBranch == 1) {
			return IfThenElse.make(condition, firstBranch, secondBranch);
		}
		return IfThenElse.make(condition, secondBranch, firstBranch);
	}

	public static Expression makeWithEqualityOrDisequalityConditionAndInvertedToEqualityIfNeeded(Expression equalityLiteral, Expression thenBranch, Expression elseBranch) {
		Expression result;
		if (equalityLiteral.hasFunctor(FunctorConstants.DISEQUALITY)) {
			Expression equalityCondition = Equality.make(equalityLiteral.getArguments());
			result = make(equalityCondition, elseBranch, thenBranch); // invert
		}
		else {
			result = make(equalityLiteral, thenBranch, elseBranch);
		}
		return result;
	}

	public static Expression makeWithEqualityOrDisequalityConditionAndInvertedToEqualityIfNeeded(Expression equalityLiteral, Expression thenBranch, Expression elseBranch, boolean simplifyToConditionIfPossible) {
		Expression result;
		if (equalityLiteral.hasFunctor(FunctorConstants.DISEQUALITY)) {
			Expression equalityCondition = Equality.make(equalityLiteral.getArguments());
			result = make(equalityCondition, elseBranch, thenBranch, simplifyToConditionIfPossible); // invert
		}
		else {
			result = make(equalityLiteral, thenBranch, elseBranch, simplifyToConditionIfPossible);
		}
		return result;
	}

	public static boolean isIfThenElse(Expression expression) {
		boolean result = expression.hasFunctor(FunctorConstants.IF_THEN_ELSE);
		return result;
	}
	
	/** Returns the condition of an if then else expression. */
	public static Expression condition(Expression expression) {
		Expression result = expression.get(0);
		return result;
	}
	
	/** Returns the then branch of an if then else expression. */
	public static Expression thenBranch(Expression expression) {
		Expression result = expression.get(1);
		return result;
	}
	
	/** Returns the else branch of an if then else expression. */
	public static Expression elseBranch(Expression expression) {
		Expression result = expression.get(2);
		return result;
	}

	/**
	 * Make a copy of a given if then else condition, but for a replaced condition,
	 * possibly simplifying it if new condition is true or false.
	 */
	public static Expression copyWithReplacedCondition(Expression ifThenElse, Expression newCondition) {
		if (newCondition.equals(Expressions.TRUE)) {
			return thenBranch(ifThenElse);
		}
		if (newCondition.equals(Expressions.FALSE)) {
			return elseBranch(ifThenElse);
		}
		Expression result = IfThenElse.make(newCondition, thenBranch(ifThenElse), elseBranch(ifThenElse));
		return result;
	}

	/**
	 * Given an index of one of the branches of an if then else expression,
	 * returns the index of the other branch.
	 * @see #make(Expression, int, Expression, int, Expression)
	 */
	public static int oppositeBranchIndex(int branchIndex) {
		return branchIndex == 1 ? 2 : 1;
	}

	public static Expression flipIfThenElseWithNegatedCondition(Expression ifThenElseWithNegatedCondition) {
		Expression result =
				make(
						condition(ifThenElseWithNegatedCondition).get(0),
						elseBranch(ifThenElseWithNegatedCondition),
						thenBranch(ifThenElseWithNegatedCondition));
		return result;
	}
	
	/** 
	 * Receives an expression and, if it is an if then else, returns an equivalent one without top {@link Not} applications in the condition.
	 */
	public static Expression equivalentWithNonNegatedCondition(Expression expression) {
		if (isIfThenElse(expression)) {
			Pair<Integer, Expression> numberOfNotApplicationsAndArgument =
					Expressions.getNumberOfConsecutiveApplicationsOfUnaryFunctorAndUnderlyingArgument(condition(expression), NOT_FUNCTOR);
			if (numberOfNotApplicationsAndArgument.first != 0) {
				if (numberOfNotApplicationsAndArgument.first % 2 == 0) {
					expression = make(numberOfNotApplicationsAndArgument.second, thenBranch(expression), elseBranch(expression));
				}
				else {
					expression = make(numberOfNotApplicationsAndArgument.second, elseBranch(expression), thenBranch(expression));
				}
			}
		}
		return expression;
	}

	public static Expression simplify(Expression ifThenElse) {
		Expression condition  = condition(ifThenElse);
		Expression thenBranch = thenBranch(ifThenElse);
		Expression elseBranch = elseBranch(ifThenElse);
		Expression result = IfThenElse.make(condition, thenBranch, elseBranch);
		if (ifThenElse.equals(result)) {
			result = ifThenElse; // make sure to return same instance if there were no changes 
		}
		return result;
	}
}
