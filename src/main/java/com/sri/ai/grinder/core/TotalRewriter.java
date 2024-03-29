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
package com.sri.ai.grinder.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.ExpressionAndContext;
import com.sri.ai.expresso.api.ReplacementFunctionWithContextuallyUpdatedProcess;
import com.sri.ai.expresso.core.AbstractReplacementFunctionWithContextuallyUpdatedProcess;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.GrinderConfiguration;
import com.sri.ai.grinder.api.NoOpRewriter;
import com.sri.ai.grinder.api.Rewriter;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.expression.ExpressionCache;
import com.sri.ai.grinder.helper.Justification;
import com.sri.ai.grinder.helper.Trace;
import com.sri.ai.grinder.library.boole.ForAll;
import com.sri.ai.grinder.library.boole.ThereExists;
import com.sri.ai.util.Util;
import com.sri.ai.util.base.Pair;
import com.sri.ai.util.base.TernaryProcedure;
import com.sri.ai.util.cache.CacheMap;

/**
 * A Rewriter that exhaustively applies a list of rewriters until no changes
 * occur any more.
 * 
 * @author oreilly
 * @author braz
 */
@Beta
public class TotalRewriter extends AbstractRewriter {
	// NOTE: This is a temporary option (so will not use ExpressoConfiguration) directly, see:
	// https://code.google.com/p/aic-expresso/issues/detail?id=40
	// for more details on the optimization this refers to.
	private static boolean _useExperimentalOptimization = Boolean.getBoolean("expresso.use.totalrewriter.experimental.optimization");
	//
	private List<Rewriter>  childRewriters          = null;
	private List<Rewriter>  activeRewriters         = new ArrayList<Rewriter>();
	private int             totalNumberOfSelections = 0;
	private int             rewritingCount          = 0; 
	private boolean         outerTraceEnabled       = true;
	//
	private ExpressionCache deadEndsCache = new ExpressionCache(
			GrinderConfiguration.getRewriteDeadEndsCacheMaximumSize(),
			null,
			CacheMap.NO_GARBAGE_COLLECTION);
	private PruningPredicate deadEndPruner = new PruningPredicate() {
		@Override
		public boolean apply(Expression expression, Function<Expression, Expression> replacementFunction, RewritingProcess process) {
			boolean result = deadEndsCache.containsKeyFor(expression, process);
			return result;
		}
	};
	private TernaryProcedure<Expression, Expression, RewritingProcess> deadEndListener = new TernaryProcedure<Expression, Expression, RewritingProcess>() {
		@Override
		public void apply(Expression o1, Expression o2, RewritingProcess process) {
			// the listener is invoked after the whole expression, including its sub-expressions, are checked for changes.
			// If there are none, the expression is a dead end.
			if (o1 == o2) {
				deadEndsCache.putUnderKeyFor(o1, o2, process);
			}
//			registerEquivalency(o1, o2, process);
		}
	};
	//
	private CallRewriterDecisionTree callRewriterDecisionTree = null;
	
	
	/**
	 * Constructor
	 * @param name
	 *        Must be a name that uniquely identifies the list of rewriters
	 *        passed to the constructor. Works in conjunction with the
	 *        Rewriting Processes rewrite cache mechanism.
	 * @param rewriters
	 *        The list of rewriters to be exhaustively called until no 
	 *        more rewriting can occur.
	 */
	public TotalRewriter(String name, List<Rewriter> rewriters) {
		super();
		setName(name);
		this.childRewriters = rewriters;
		// Filter out the NoOpRewriters up front
		for (Rewriter rewriter : childRewriters) {
			if (!(rewriter instanceof NoOpRewriter)) {
				activeRewriters.add(rewriter);
			}
		}
		
		callRewriterDecisionTree = new CallRewriterDecisionTree(activeRewriters);
	}
	
	/**
	 * Constructor
	 * @param name
	 *        Must be a name that uniquely identifies the list of rewriters
	 *        passed to the constructor. Works in conjunction with the
	 *        Rewriting Processes rewrite cache mechanism.
	 * @param rewriters
	 *        The list of rewriters to be exhaustively called until no 
	 *        more rewriting can occur.
	 */
	public TotalRewriter(String name, Rewriter... rewriters) {
		this(name, Arrays.asList(rewriters));
	}
	
	/**
	 * Constructor that makes up a random name for the TotalRewriter.
	 * The idea is the have a nameless TotalRewriter, but because using a standard fixed name
	 * could lead the system to confuse two distinct TotalRewriters, this constructor generates a random name for it.
	 * @param rewriters
	 *        The list of rewriters to be exhaustively called until no 
	 *        more rewriting can occur.
	 */
	public TotalRewriter(Rewriter... rewriters) {
		this("TotalRewriter with random name " + Math.random(), Arrays.asList(rewriters));
	}
	
	public boolean isOuterTraceEnabled() {
		return outerTraceEnabled;
	}
	
	public void setOuterTraceEnabled(boolean enabled) {
		this.outerTraceEnabled = enabled;
	}
	
	//
	// START-Rewriter

	/**
	 * Returns an iterator ranging over the base rewriters.
	 */
	@Override
	public Iterator<Rewriter> getChildrenIterator() {
		return childRewriters.iterator();
	}
	
	@Override
	public void rewritingProcessInitiated(RewritingProcess process) {
		totalNumberOfSelections = 0;
	}
	
	@Override
	public Expression rewriteAfterBookkeeping(final Expression topExpression, RewritingProcess process) {
		Expression current  = topExpression;
		Expression previous = null;
		final Expression[] currentTopExpression = new Expression[1];
		// Note: make the rewriter function local so that it can be multi-threaded correctly with respect 
		// to tracking the topExpression for trace output. This is where the guts of the logic occurs.
		final Expression[] currentTopExpressionForDebugging = new Expression[1];
		final boolean       traceEnabled         = Trace.isEnabled() && isOuterTraceEnabled();
		final boolean       justificationEnabled = Justification.isEnabled();

		final AtomicInteger numberOfSelections   = new AtomicInteger(0);
		ReplacementFunctionWithContextuallyUpdatedProcess rewriteCurrentExpressionExhaustivelyFunction = new AbstractReplacementFunctionWithContextuallyUpdatedProcess() {
			@Override
			public Expression apply(Expression expression, RewritingProcess process) {
				Expression result      = expression;
				Expression priorResult = expression;
				Rewriter   rewriter    = null;
		
//				Expression cached = getFinalEquivalent(expression, process);
//				if (cached != null) {
//					return cached;
//				}

//				cached = getFinalEquivalent(expression, process);
				
				// Exhaustively apply each rewriter in turn.
				long startTime = 0L;
				do {
					priorResult = result;

					if (traceEnabled) {
						startTime = System.currentTimeMillis();
					}

					if (traceEnabled) {
						Trace.setTraceLevel(Trace.getTraceLevel() + 1);
					}
					
					Pair<Rewriter, Expression> rewriterWrote = callRewriterDecisionTree.rewrite(priorResult, process);
					rewriter = rewriterWrote.first;
					result   = rewriterWrote.second;

					if (traceEnabled) {
						Trace.setTraceLevel(Trace.getTraceLevel() - 1);
					}

					// Track Selections
					numberOfSelections.addAndGet(1);
					totalNumberOfSelections += 1;

					// Output trace and justification information if a change occurred
					if (result != priorResult) {
						if (traceEnabled) {
							long relativeTime = System.currentTimeMillis() - startTime;

							boolean isWholeExpressionRewrite = priorResult == currentTopExpression[0];
							if (isWholeExpressionRewrite) {
								Trace.log("Rewriting whole expression:");
								Trace.log("{}", priorResult);
								currentTopExpression[0]             = result;
								currentTopExpressionForDebugging[0] = result;
							} else {
								Trace.log("Rewriting sub-expression:");
								Trace.log("{}", priorResult);
							}
							Trace.log("   ----> (" + rewriter.getName() + ",  "
									+ relativeTime + " ms, #"
									+ (++rewritingCount) + ", "
									+ numberOfSelections
									+ " rewriter selections ("
									+ totalNumberOfSelections
									+ " since start))");
							Trace.log("{}", result);
						}

						if (justificationEnabled) {
							Justification.log(expression);
							Justification.beginEqualityStep(rewriter.getName());
							Justification.endEqualityStep(result);
						}
					}
				} while (result != priorResult);

//				if (cached != null && ! cached.equals(result)) {
//					System.out.println("Equivalency cache used in non-trivial way.");
//			 		System.out.println("expression  : " + expression);
//			    	System.out.println("rewritten to: " + result);
//				    System.out.println("cached      : " + cached);
//					System.out.println("context     : " + process.getContextualConstraint());
//			    }
				
				return result;
			}
		};
		
		// Keep rewriting until no changes occur.
		if (_useExperimentalOptimization) {
			while (current != previous) {
				previous                            = current;
				currentTopExpression[0]             = current;
				currentTopExpressionForDebugging[0] = current;
				
				current = rewriteCurrentExpressionExhaustivelyFunction.apply(current, process);
				
				RecurseImmediateSubExpressionsFunction f = new RecurseImmediateSubExpressionsFunction(
						current, rewriteCurrentExpressionExhaustivelyFunction,
						isExhaustiveRewriteWanted(current, process),
						process);
				
				current = current.replace(f, // replacementFunction
						null, // ReplacementFunctionMaker
						deadEndPruner, // prunePredicate
						f, // PruningPredicateMaker
						false, // onlyTheFirstOne
						true, // ignoreTopExpression,
						false, // replaceOnChildrenBeforeTopExpression,
						deadEndListener, process);
			}
		}
		else {
			// Keep rewriting until no changes occur.
			while (current != previous) {
				previous = current;
				currentTopExpression[0] = current;
				currentTopExpressionForDebugging[0] = current;
				current = previous.replaceAllOccurrences(rewriteCurrentExpressionExhaustivelyFunction, deadEndPruner, deadEndListener, process);
			}
		}
		
		Expression result = current;
		return result;
	}
	
	// NOTE: always returning true causes the experimental logic to rewrite expressions differently
	// due to calls to R_quantifier_elimination occurring differently, see:
	// https://code.google.com/p/aic-expresso/issues/detail?id=40
	// for a discussion.
	private boolean isExhaustiveRewriteWanted(Expression expression, RewritingProcess process) {
		// For R_quantifier_elimination we want to go top down (i.e. not exhaustive at a level) 
		// as opposed to bottom up (exhaustively at each level).
		boolean result = !(ForAll.isForAll(expression) || ThereExists.isThereExists(expression));
		
		return result;
	}

	class RecurseImmediateSubExpressionsFunction extends
			AbstractReplacementFunctionWithContextuallyUpdatedProcess implements
			PruningPredicateMaker {
		private ReplacementFunctionWithContextuallyUpdatedProcess rewriteFunction       = null;
		private boolean                                           exhaustive            = true;
		private ChildPruningPredicate                             childPruningPredicate = new ChildPruningPredicate();

		public RecurseImmediateSubExpressionsFunction(
				Expression topExpression,
				ReplacementFunctionWithContextuallyUpdatedProcess rewriteFunction,
				boolean exhaustive,
				RewritingProcess process) {
			this.rewriteFunction = rewriteFunction;
			this.exhaustive      = exhaustive;
		}

		@Override
		public Expression apply(Expression expression, RewritingProcess process) {
			Expression result   = expression;
			Expression previous = null;
			while (previous != result) {
				previous = result;
				result   = rewriteFunction.apply(result, process);
				
				if (!exhaustive && previous != result) {
					return result;
				}

				RecurseImmediateSubExpressionsFunction f = new RecurseImmediateSubExpressionsFunction(result, rewriteFunction, isExhaustiveRewriteWanted(result, process), process);
				
				result = result.replace(f, // replacementFunction
						null, // ReplacementFunctionMaker
						deadEndPruner, // prunePredicate
						f, // PruningPredicateMaker
						false, // onlyTheFirstOne
						true, // ignoreTopExpression,
						false, // replaceOnChildrenBeforeTopExpression,
						deadEndListener, process);
				
				if (!exhaustive && previous != result) {
					return result;
				}
			}

			return result;
		}

		@Override
		public PruningPredicate apply(Expression expression,
				PruningPredicate pruningPredicate,
				ExpressionAndContext subExpressionAndContext) {
			if (pruningPredicate == childPruningPredicate) {
				return Expressions.TRUE_PRUNING_PREDICATE;
			}

			return childPruningPredicate;
		}
	}
	
	class ChildPruningPredicate implements PruningPredicate {		
		@Override
		public boolean apply(Expression expression, Function<Expression, Expression> replacementFunction, RewritingProcess process) {
			return deadEndPruner.apply(expression, replacementFunction, process);
		}		
	}
	
	// BEGIN -- EQUIVALENCY CACHE
	
	// The equivalency cache remembers what expressions were rewritten to, ultimately.
	// This is not limited to single rewritings; when looking for what an expression was rewritten to,
	// we follow the equivalencies until no others are found.
	// If an expression is not in the cache yet, we don't know yet what it is equivalent to.
	// However, if it maps to itself, then we know that no rewriter rewrites it (so it's a dead end).
	
	private static final String EQUIVALENCY_CACHE_GLOBAL_OBJECTS_KEY = "equivalency cache";

	@SuppressWarnings("unused")
	private static void registerEquivalency(Expression originalExpression, Expression rewrittenExpression, RewritingProcess process) {
		ExpressionCache equivalencyCache = getEquivalencyCache(process);
		equivalencyCache.putUnderKeyFor(originalExpression, rewrittenExpression, process);
//		System.out.println("Stored " + originalExpression + " -> " + rewrittenExpression + " under " + process.getContextualConstraint());
//		System.out.println("Equivalency cache: " + equivalencyCache);
	}
	
	/**
	 * Takes an expression and, if it is not mapped as equivalent to any other, returns null.
	 * If it is mapped as equivalent to another expression, follows the equivalence chain
	 * until a <i>final equivalent</i> expression is found, and returns it.
	 * An expression is final equivalent if it is either not mapped (in the equivalency cache)
	 * to any other expression, or if it is mapped to itself by it.
	 * Note that, as a consequence of the above definition,
	 * if E is cached as equivalent to itself, it is itself returned.
	 */
	@SuppressWarnings("unused")
	private static Expression getFinalEquivalent(Expression expression, RewritingProcess process) {
		ExpressionCache equivalencyCache = getEquivalencyCache(process);

//		System.out.println("Getting final equivalent to " + expression);

		Expression next = equivalencyCache.getUnderKeyFor(expression, process);
		if (next == null) {
//			System.out.println("There is no final equivalent for " + expression);
			return null;
		}
		
		if (next == expression) {
			// we could have let the loop do this, but because in this particular case there is no need to insert anything in the cache, as in (*),
			// we do it here for a slight efficiency gain.
//			System.out.println("Final equivalent for " + expression + " is itself");
			return expression;
		}

		// At this point, we know that expression is equivalent to something else, so we follow the chain of equivalencies until we reach a final equivalent expression.
		
		Expression current = null;
		do {
			current = next;
			next = equivalencyCache.get(current);
			if (isFinalEquivalent(current, next)) {
				equivalencyCache.putUnderKeyFor(expression, current, process); // (*) remember more direct route for next time.
//				System.out.println("Final equivalent for " + expression + " is " + current);
				return current;
			}
		} while (true);
	}

	private static boolean isFinalEquivalent(Expression current, Expression next) {
		return next == null || next == current;
	}
	
	@SuppressWarnings("unchecked")
	private static ExpressionCache getEquivalencyCache(RewritingProcess process) {
		ExpressionCache result =
				Util.getValuePossiblyCreatingIt( (Map) process.getGlobalObjects(), EQUIVALENCY_CACHE_GLOBAL_OBJECTS_KEY, EQUIVALENCY_CACHE_MAKER);
		return result;
	}
	
	private static Function<String, ExpressionCache> EQUIVALENCY_CACHE_MAKER = new Function<String, ExpressionCache>() {

		@Override
		public ExpressionCache apply(String key) {
			ExpressionCache result =
					new ExpressionCache(
							100000,
							null,
							CacheMap.NO_GARBAGE_COLLECTION);
			return result;
		}
		
	};

	// END -- EQUIVALENCY CACHE

}
