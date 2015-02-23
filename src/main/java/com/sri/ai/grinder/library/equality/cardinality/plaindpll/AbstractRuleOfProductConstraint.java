package com.sri.ai.grinder.library.equality.cardinality.plaindpll;

import static com.sri.ai.expresso.helper.Expressions.ZERO;
import static com.sri.ai.util.Util.throwSafeguardError;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Predicate;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.library.controlflow.IfThenElse;
import com.sri.ai.grinder.library.equality.cardinality.plaindpll.AbstractTheory.Contradiction;
import com.sri.ai.grinder.library.equality.cardinality.plaindpll.Theory.Constraint;
import com.sri.ai.grinder.library.number.Times;
import com.sri.ai.util.Util;

/**
 * An abstract {@link AbstractEqualityConstraint} implementation that lays the groundwork for
 * computing model counts based on the rule of product, that is,
 * iterating over each index, computing the number of possible values for it
 * given the previous choices, and multiplying them all.
 *  
 * @author braz
 *
 */
public abstract class AbstractRuleOfProductConstraint implements Theory.Constraint {

	static final private Times timesRewriter = new Times();

	protected Collection<Expression> indices;

	public AbstractRuleOfProductConstraint(Collection<Expression> indices) {
		this.indices = indices;
	}
	
	public abstract AbstractRuleOfProductConstraint clone();
	
	@Override
	public Collection<Expression> getIndices() {
		return indices;
	}

	/**
	 * Given an index x, return one splitter needed for us to be able to
	 * compute this index's number of values, or null if none is needed.
	 * Only required if using default implementation of {@link #pickSplitter(RewritingProcess)} (that is, not overriding it).
	 */
	protected Expression provideSplitterRequiredForComputingNumberOfValuesFor(Expression x, RewritingProcess process) {
		throwSafeguardError(
				getClass().getSimpleName(),
				"provideSplitterRequiredForComputingNumberOfValuesFor", // thisClassName
				"AbstractTheory.AbstractConstraint", // superClassName
				"pickSplitter"); // namesOfMethodsWhoseDefaultImplementationUsesThisMethod
		return null; // never used, as safeguardCheck throws an error no matter what.
	}

	@Override
	public Expression pickSplitter(RewritingProcess process) {
		for (Expression x : getIndices()) {
			Expression splitter = provideSplitterRequiredForComputingNumberOfValuesFor(x, process);
			if (splitter != null) {
				return splitter;
			}
		}
		return null;
	}

	/**
	 * Modify this constraint's inner representation to include this splitter.
	 */
	abstract protected void applyNormalizedSplitterDestructively(boolean splitterSign, Expression splitter, RewritingProcess process);

	@Override
	public Constraint applySplitter(boolean splitterSign, Expression splitter, RewritingProcess process) {
		Constraint result;

		Expression normalizedSplitterGivenConstraint = normalizeSplitterGivenConstraint(splitter, process);
		
		if (normalizedSplitterGivenConstraint.equals(splitterSign)) {
			result = this; // splitter is redundant given constraint
		}
		else if (normalizedSplitterGivenConstraint.equals( ! splitterSign)) {
			result = null; // splitter is contradictory given constraint
		}
		else {
			try {
				result = applyNormalizedSplitter(splitterSign, normalizedSplitterGivenConstraint, process);
			}
			catch (Contradiction e) {
				result = null;
			}
		}

		return result;
	}

	private Constraint applyNormalizedSplitter(boolean splitterSign, Expression splitter, RewritingProcess process) {
		AbstractRuleOfProductConstraint newConstraint = clone();
		newConstraint.applyNormalizedSplitterDestructively(splitterSign, splitter, process);
		return newConstraint;
	}

	protected Collection<Expression> getSplittersToBeSatisfied(RewritingProcess process) {
		throwSafeguardError(
				getClass().getSimpleName(),
				"getSplittersToBeSatisfied", // thisClassName
				"AbstractTheory.AbstractConstraint", // superClassName
				"modelCount"); // namesOfMethodsWhoseDefaultImplementationUsesThisMethod
		return null; // never used, as safeguardCheck throws an error no matter what.
	}

	protected Collection<Expression> getSplittersToBeNotSatisfied(RewritingProcess process) {
		throwSafeguardError(
				getClass().getSimpleName(),
				"getSplittersToBeNotSatisfied", // thisClassName
				"AbstractTheory.AbstractConstraint", // superClassName
				"modelCount"); // namesOfMethodsWhoseDefaultImplementationUsesThisMethod
		return null; // never used, as safeguardCheck throws an error no matter what.
	}

	@Override
	public Expression modelCount(RewritingProcess process) {
		Expression unconditionalCount = computeModelCountGivenConditionsOnFreeVariables(process);
		Expression result =
				makeModelCountConditionedOnFreeVariableSplittersNotAlreadyImpliedByContextualConstraint(
						unconditionalCount,
						getSplittersToBeSatisfied(process), getSplittersToBeNotSatisfied(process),
						process);
		return result;
	}
	
	/**
	 * Returns an expression (in the free variables) for the number of possible values for the given index,
	 * assuming that {@link #provideSplitterRequiredForComputingNumberOfValuesFor(Expression, RewritingProcess)}
	 * currently returns <code>null</code>,
	 * that is, we do not need anything splitters to be either imposed or negated in order to compute that.
	 * Only required if using default implementation of {@link #computeModelCountGivenConditionsOnFreeVariables(Expression index, RewritingProcess)} (that is, not overriding it).
	 */
	protected Expression computeNumberOfPossibleValuesFor(Expression index, RewritingProcess process) {
		throwSafeguardError(
				getClass().getSimpleName(),
				"computeNumberOfPossibleValuesFor", // thisClassName
				"AbstractTheory.AbstractConstraint", // superClassName
				"computeModelCountGivenConditionsOnFreeVariables"); // namesOfMethodsWhoseDefaultImplementationUsesThisMethod
		return null; // never used, as safeguardCheck throws an error no matter what.
	}

	protected Expression computeModelCountGivenConditionsOnFreeVariables(RewritingProcess process) {
		List<Expression> numberOfPossibleValuesForIndicesSoFar = new LinkedList<Expression>();
		
		for (Expression index : indices) {
			Expression numberOfPossibleValuesForIndex = computeNumberOfPossibleValuesFor(index, process);
			numberOfPossibleValuesForIndicesSoFar.add(numberOfPossibleValuesForIndex);
		}
		
		Expression result = Times.make(numberOfPossibleValuesForIndicesSoFar);
		Expression unconditionalCount = AbstractRuleOfProductConstraint.timesRewriter.rewrite(result, process);
		return unconditionalCount;
	}

	/**
	 * Receives the model count for the case in which a certain set of splitter is satisfied, and another is unsatisfied,
	 * and returns conditional model count including the cases in which those conditions are not true
	 * (which entail model count 0),
	 * taking into account the contextual constraint.
	 */
	private Expression makeModelCountConditionedOnFreeVariableSplittersNotAlreadyImpliedByContextualConstraint(
			Expression modelCountGivenUndeterminedSplitters,
			Collection<Expression> splittersToBeSatisfied,
			Collection<Expression> splittersToBeUnsatisfied,
			RewritingProcess process) {
		
		Predicate<Expression> keepUnsatisfiedSplitters         = s -> splitterIsNotSatisfiedFromContextualConstraintAlready(true,  s, process);
		Predicate<Expression> keepUnsatisfiedSplitterNegations = s -> splitterIsNotSatisfiedFromContextualConstraintAlready(false, s, process);
	
		Collection<Expression> undeterminedSplittersThatNeedToBeTrue  = Util.filter(splittersToBeSatisfied,   keepUnsatisfiedSplitters);
		Collection<Expression> undeterminedSplittersThatNeedToBeFalse = Util.filter(splittersToBeUnsatisfied, keepUnsatisfiedSplitterNegations);
		
		Expression result = conditionExpressionOnGivenSplitters(
				modelCountGivenUndeterminedSplitters, undeterminedSplittersThatNeedToBeTrue, undeterminedSplittersThatNeedToBeFalse);
		return result;
	}

	private boolean splitterIsNotSatisfiedFromContextualConstraintAlready(boolean splitterSign, Expression splitter, RewritingProcess process) {
		boolean result;
		Expression splitterNormalizedByContextualConstraint = process.getDPLLContextualConstraint().normalizeSplitterGivenConstraint(splitter, process);
		assert ! splitterNormalizedByContextualConstraint.equals( ! splitterSign); // required splitter must be satisfiable under contextual constraint, otherwise there is a bug somewhere
		result = ! splitterNormalizedByContextualConstraint.equals(splitterSign); // if splitter is implied TRUE by contextual constraint, it is superfluous
		return result;
	}

	/**
	 * Receives an expression and conditions it on a set of splitters required to be true,
	 * and another set of splitters required to be false.
	 */
	private Expression conditionExpressionOnGivenSplitters(
			Expression expression,
			Collection<Expression> splittersThatNeedToBeTrue,
			Collection<Expression> splittersThatNeedToBeFalse) {
		
		Expression result = expression;
		for (Expression splitterToBeSatisfied : splittersThatNeedToBeTrue) {
			result = IfThenElse.make(splitterToBeSatisfied, result, ZERO, false);
		}
		for (Expression splitterToBeNotSatisfied : splittersThatNeedToBeFalse) {
			result = IfThenElse.make(splitterToBeNotSatisfied, ZERO, result, false);
		}
		return result;
	}
}