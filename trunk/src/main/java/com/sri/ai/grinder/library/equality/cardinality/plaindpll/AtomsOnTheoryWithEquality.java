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
package com.sri.ai.grinder.library.equality.cardinality.plaindpll;

import static com.sri.ai.expresso.helper.Expressions.FALSE;
import static com.sri.ai.expresso.helper.Expressions.TRUE;
import static com.sri.ai.grinder.helper.GrinderUtil.isBooleanTyped;
import static com.sri.ai.grinder.library.Equality.isEquality;

import java.util.Collection;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.AbstractExpressionWrapper;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.DefaultRewritingProcess;
import com.sri.ai.grinder.library.Equality;
import com.sri.ai.grinder.library.boole.Not;

@Beta
/** 
 * A {@link Theory} adding boolean atoms as splitters to another theory that has equality literals as splitters.
 * It works by converting atom splitters "A" and "not A" to "A = true" or "A = false", respectively.
 */
public class AtomsOnTheoryWithEquality extends AbstractTheory {
	
	// This class deals with two types of theories and splitters;
	// first, its base equality theory and its equality splitters,
	// and itself as a theory with its own class of splitters which
	// is the union of equality splitters and boolean function applications or symbols.
	// We differentiate those two types of theory and splitters by always
	// calling the first type "equality theory" and "equality splitters".
	
	Theory theoryWithEquality;
	
	public AtomsOnTheoryWithEquality(AbstractTheory theoryWithEquality) {
		this.theoryWithEquality = theoryWithEquality;
	}

	/**
	 * Given a signed splitter (s, S), returns a corresponding signed splitter guaranteed to be an equality splitter
	 * (and therefore suitable for passing to the base theory),
	 * by taking returning (s, S) if S is an equality, and (true, S = s) if S is an atom.
	 * @param splitterSign
	 * @param splitter
	 * @return
	 */
	private static SignedSplitter getSignedEqualitySplitter(boolean splitterSign, Expression splitter) {
		Expression equalitySplitter     = Equality.isEquality(splitter)? splitter     : Equality.make(splitter, splitterSign);
		boolean    equalitySplitterSign = Equality.isEquality(splitter)? splitterSign : true;
		SignedSplitter result = new SignedSplitter(equalitySplitterSign, equalitySplitter);
		return result;
	}
	
	@Override
	public boolean isVariableTerm(Expression term, RewritingProcess process) {
		return theoryWithEquality.isVariableTerm(term, process);
	}

	@Override
	public Expression makeSplitterIfPossible(Expression expression, Collection<Expression> indices, RewritingProcess process) {
		Expression result;
		if (theoryWithEquality.isVariableTerm(expression, process) && isBooleanTyped(expression, process)) {
			result = expression;
		}
		else {
			result = theoryWithEquality.makeSplitterIfPossible(expression, indices, process);
		}
		return result;
	}

	@Override
	public boolean applicationOfConstraintOnSplitterAlwaysEitherTrivializesItOrEffectsNoChangeAtAll() {
		boolean result = theoryWithEquality.applicationOfConstraintOnSplitterAlwaysEitherTrivializesItOrEffectsNoChangeAtAll();
		return result;
	}

	@Override
	public Expression simplify(Expression expression, RewritingProcess process) {
		return theoryWithEquality.simplify(expression, process);
	}
	
	protected boolean useDefaultImplementationOfApplySplitterToExpressionByOverriddingGetSplitterApplier() {
		return false; // will instead delegate to theoryWithEquality.applySplitterToExpression
	}

	@Override
	public Expression applySplitterToExpression(boolean splitterSign, Expression splitter, Expression expression, RewritingProcess process) {
		SignedSplitter equalitySignedSplitter = getSignedEqualitySplitter(splitterSign, splitter);
		Expression result = theoryWithEquality.applySplitterToExpression(equalitySignedSplitter, expression, process);
		return result;
	}

	@Override
	public AtomsOnTheoryWithEqualityConstraint makeConstraint(Collection<Expression> indices) {
		AtomsOnTheoryWithEqualityConstraint result = new AtomsOnTheoryWithEqualityConstraint(theoryWithEquality.makeConstraint(indices));
		return result;
	}

	private class AtomsOnTheoryWithEqualityConstraint extends AbstractExpressionWrapper implements ConjunctiveConstraint {

		private static final long serialVersionUID = 1L;
		
		private ConjunctiveConstraint equalityConstraint;
		
		public AtomsOnTheoryWithEqualityConstraint(ConjunctiveConstraint equalityConstraint) {
			this.equalityConstraint = equalityConstraint;
		}
		
		@Override
		public Expression clone() {
			return new AtomsOnTheoryWithEqualityConstraint(equalityConstraint);
		}

		@Override
		public Theory getTheory() {
			return AtomsOnTheoryWithEquality.this;
		}

		@Override
		public Collection<Expression> getSupportedIndices() {
			return equalityConstraint.getSupportedIndices();
		}

		@Override
		public Expression normalizeSplitterGivenConstraint(Expression splitter, RewritingProcess process) {
			Expression equalitySplitter = Equality.isEquality(splitter)? splitter : Equality.make(splitter, TRUE);
			Expression impliedByEqualityConstraint = equalityConstraint.normalizeSplitterGivenConstraint(equalitySplitter, process);
			Expression result;
			if (impliedByEqualityConstraint.equals(TRUE) || impliedByEqualityConstraint.equals(FALSE)) {
				result = impliedByEqualityConstraint;
			}
			else {
				result = splitter;
			}
			return result;
		}

		@Override
		public AtomsOnTheoryWithEqualityConstraint applySplitter(boolean splitterSign, Expression splitter, RewritingProcess process) {
			SignedSplitter equalitySignedSplitter = getSignedEqualitySplitter(splitterSign, splitter);
			ConjunctiveConstraint newEqualityConstraint = equalityConstraint.applySplitter(equalitySignedSplitter, process);
			AtomsOnTheoryWithEqualityConstraint result;
			if (newEqualityConstraint != null) {
				result = new AtomsOnTheoryWithEqualityConstraint(newEqualityConstraint);
			}
			else {
				result = null;
			}
			return result;
		}

		@Override
		public Expression pickSplitter(Collection<Expression> indicesSubSet, RewritingProcess process) {
			Expression equalitySplitter = equalityConstraint.pickSplitter(indicesSubSet, process);
			Expression result;
			if (equalitySplitter != null) {
				result = fromEqualitySplitterToSplitter(equalitySplitter);
			}
			else {
				result = null;
			}
			return result;
		}

		@Override
		public Expression modelCount(Collection<Expression> indicesSubSet, RewritingProcess process) {
			Expression equalityModelCount = equalityConstraint.modelCount(indicesSubSet, process);
			Expression result =
					equalityModelCount.replaceAllOccurrences(
							e -> fromEqualitySplitterToSplitterIfEqualitySplitterInTheFirstPlace(e, process),
							process);
			return result;
		}

		@Override
		public Constraint project(Collection<Expression> indicesSubSet, RewritingProcess process) {
			Solver projector = new SGDPLLT(getTheory(), new Satisfiability());
			Expression resultExpression = projector.solve(this, getSupportedIndices(), process);
			Constraint result = new ExpressionConstraint(getTheory(), getSupportedIndices(), resultExpression);
			return result;
		}

		private Expression fromEqualitySplitterToSplitterIfEqualitySplitterInTheFirstPlace(Expression expression, RewritingProcess process) {
			Expression equalitySplitter = makeSplitterIfPossible(expression, equalityConstraint.getSupportedIndices(), process);
			Expression result;
			if (equalitySplitter == null) {
				result = expression;
			}
			else {
				result = fromEqualitySplitterToSplitter(equalitySplitter);
			}
			return result;
		}

		private Expression fromEqualitySplitterToSplitter(Expression equalitySplitter) {
			Expression result;
			if (equalitySplitter.get(1).equals(TRUE) || equalitySplitter.get(1).equals(FALSE)) {
				// equality splitters of the form "V = true" and "V = false" get translated to splitter "V".
				result = equalitySplitter.get(0);
			}
			else {
				result = equalitySplitter;
			}
			return result;
		}

		@Override
		public Expression normalize(Expression expression, RewritingProcess process) {
			Expression result = equalityConstraint.normalize(expression, process);
			return result;
		}
		
		@Override
		protected Expression computeInnerExpression() {
			Expression result =
					equalityConstraint.replaceAllOccurrences(
							e -> {
								Expression conjunct;
								if (isEquality(e)) {
									if (e.get(1).equals(TRUE)) {
										conjunct = e.get(0);
									}
									else if (e.get(1).equals(FALSE)) {
										conjunct = Not.make(e.get(0));
									}
									else {
										conjunct = e;
									}
								}
								else {
									conjunct = e;
								}
								return conjunct;
							},
							new DefaultRewritingProcess(null));
			return result;
		}
	}
}