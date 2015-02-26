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

import static com.sri.ai.expresso.helper.Expressions.ONE;
import static com.sri.ai.expresso.helper.Expressions.ZERO;
import static com.sri.ai.expresso.helper.Expressions.parse;
import static com.sri.ai.util.Util.list;
import static com.sri.ai.util.Util.setDifference;

import java.util.Collection;
import java.util.Map;

import com.google.common.base.Predicate;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.core.DefaultRewritingProcess;
import com.sri.ai.grinder.core.PrologConstantPredicate;
import com.sri.ai.grinder.library.controlflow.IfThenElse;
import com.sri.ai.grinder.library.number.Division;
import com.sri.ai.grinder.library.number.Times;
import com.sri.ai.util.Util;

/**
 * An example on how to use SGDPLL(T) to convert table representations to decision tree representations.
 * @author braz
 *
 */
public class ProbabilisticInference {

	/**
	 * Solves a factor graph (Markov network or Bayesian network) provided as a product of potential functions.
	 * @param factorGraph an Expression representing the product of potential functions
	 * @param isBayesianNetwork indicates the factor graph is a bayesian network (each potential function in normalized for one of its variables, forms a DAG).
	 * @param resultFromPreviousQueryIfKnown the {@link Result} object from a previous query for the same model and evidence, if available, or null.
	 * @param queryExpression an Expression representing the query
	 * @param evidence an Expression representing the evidence
	 * @param mapFromTypeNameToSizeStringParam a map from type name strings to their size strings
	 * @param mapFromRandomVariableNameToTypeNameParam a map from random variable name strings to their type name strings
	 * @return
	 */
	public static Expression solveFactorGraph(
			Expression factorGraph, boolean isBayesianNetwork, Result resultFromPreviousQueryIfKnown,
			Expression queryExpression, Expression evidence, Map<String, String> mapFromTypeNameToSizeStringParam, Map<String, String> mapFromRandomVariableNameToTypeNameParam) {
		
		Result result = solveFactorGraphAndReturnIntermediateInformation(factorGraph, isBayesianNetwork, resultFromPreviousQueryIfKnown, queryExpression, evidence, mapFromTypeNameToSizeStringParam, mapFromRandomVariableNameToTypeNameParam);

		return result.getQueryMarginal();
	}

	/**
	 * same as {@link #solveFactorGraph(Expression, boolean, Result, Expression, Expression, Map, Map),
	 * but returns an object with all intermediate information that can be reused in a new invocation.
	 * @param factorGraph
	 * @param isBayesianNetwork
	 * @param resultFromPreviousQueryIfKnown
	 * @param queryExpression
	 * @param evidence
	 * @param mapFromTypeNameToSizeStringParam
	 * @param mapFromRandomVariableNameToTypeNameParam
	 * @return
	 */
	public static Result solveFactorGraphAndReturnIntermediateInformation(Expression factorGraph, boolean isBayesianNetwork, Result resultFromPreviousQueryIfKnown, Expression queryExpression, Expression evidence, Map<String, String> mapFromTypeNameToSizeStringParam, Map<String, String> mapFromRandomVariableNameToTypeNameParam) {
		Expression evidenceProbability;
		Map<String, String> mapFromTypeNameToSizeString;
		Map<String, String> mapFromRandomVariableNameToTypeName;
		Expression queryVariable;
		Collection<Expression> allVariables;
		Predicate<Expression> isUniquelyNamedConstantPredicate;
		Theory theory;
		ProblemType problemType;
		DPLLGeneralizedAndSymbolic solver;

		if (resultFromPreviousQueryIfKnown == null) {
			mapFromTypeNameToSizeString = mapFromTypeNameToSizeStringParam;
			mapFromRandomVariableNameToTypeName = mapFromRandomVariableNameToTypeNameParam;

			mapFromRandomVariableNameToTypeName.put("query", "Boolean");
			queryVariable = parse("query");

			allVariables = Util.mapIntoList(mapFromRandomVariableNameToTypeName.keySet(), Expressions::parse);

			// We use the Prolog convention of small-letter initials for constants, but we need an exception for the random variables.
			Predicate<Expression> isPrologConstant = new PrologConstantPredicate();
			isUniquelyNamedConstantPredicate = e -> isPrologConstant.apply(e) && ! allVariables.contains(e);

			// The theory of atoms plus equality on function (relational) terms.
			theory = new AtomsOnTheoryWithEquality(new EqualityTheory(new SymbolTermTheory()));
			problemType = new Sum(); // for marginalization
			// The solver for the parameters above.
			solver = new DPLLGeneralizedAndSymbolic(theory, problemType);

			evidenceProbability = null;
		}
		else {
			mapFromTypeNameToSizeString = resultFromPreviousQueryIfKnown.getMapFromTypeNameToSizeString();
			mapFromRandomVariableNameToTypeName = resultFromPreviousQueryIfKnown.getMapFromRandomVariableNameToTypeName();
			queryVariable = resultFromPreviousQueryIfKnown.getQueryVariable();
			allVariables = resultFromPreviousQueryIfKnown.getAllVariables();
			isUniquelyNamedConstantPredicate = resultFromPreviousQueryIfKnown.getIsUniquelyNamedConstantPredicate();
			theory = resultFromPreviousQueryIfKnown.getTheory();
			problemType = resultFromPreviousQueryIfKnown.getProblemType();
			solver = resultFromPreviousQueryIfKnown.getSolver();
			evidenceProbability = resultFromPreviousQueryIfKnown.getEvidenceProbability();
		}

		// Add a query variable equivalent to query expression; this introduces no cycles and the model remains a Bayesian network
		factorGraph = Times.make(list(factorGraph, parse("if query <=> " + queryExpression + " then 1 else 0")));
		
		if (evidence != null) {
			// add evidence factor
			factorGraph = Times.make(list(factorGraph, IfThenElse.make(evidence, ONE, ZERO)));
		}
		
		// We sum out all variables but the query
		Collection<Expression> indices = setDifference(allVariables, list(queryVariable)); 
	
		// Solve the problem.
		Expression unnormalizedMarginal = solver.solve(factorGraph, indices, mapFromRandomVariableNameToTypeName, mapFromTypeNameToSizeString, isUniquelyNamedConstantPredicate);
		
		Expression marginal;
		if (evidence == null && isBayesianNetwork) {
			marginal = unnormalizedMarginal; // model was a Bayesian network with no evidence, so marginal is equal to unnormalized marginal.
		}
		else {
			// We now marginalize on all variables. Since unnormalizedMarginal is the marginal on all variables but the query, we simply take that and marginalize on the query alone.
			if (evidenceProbability == null) {
				evidenceProbability = solver.solve(unnormalizedMarginal, list(queryVariable), mapFromRandomVariableNameToTypeName, mapFromTypeNameToSizeString, isUniquelyNamedConstantPredicate);
				System.out.println("Normalization constant (same as evidence probability P(" + evidence + ") ) is " + evidenceProbability);
			}

			marginal = Division.make(unnormalizedMarginal, evidenceProbability); // Bayes theorem: P(Q | E) = P(Q and E)/P(E)
			// now we use the algorithm again for simplifying the above division; this is a lazy way of doing this, as it performs search on the query variable again -- we could instead write an ad hoc function to divide all numerical constants by the normalization constant, but the code would be uglier and the gain very small, since this is a search on a single variable anyway.
			marginal = solver.solve(marginal, list(), mapFromRandomVariableNameToTypeName, mapFromTypeNameToSizeString, isUniquelyNamedConstantPredicate);
		}

		// replace the query variable with the query expression
		marginal = marginal.replaceAllOccurrences(queryVariable, queryExpression, new DefaultRewritingProcess(null));
		
		// Saves all intermediary information that can be re-used for new queries
		Result result = new Result(marginal, evidenceProbability, mapFromTypeNameToSizeString, mapFromRandomVariableNameToTypeName, queryVariable, allVariables, isUniquelyNamedConstantPredicate, theory, problemType, solver);
		return result;
	}

	public static class Result {
		private Expression queryMarginal;
		private Expression evidenceProbability;
		private Map<String, String> mapFromTypeNameToSizeString;
		private Map<String, String> mapFromRandomVariableNameToTypeName;
		private Expression queryVariable;
		private Collection<Expression> allVariables;
		private Predicate<Expression> isUniquelyNamedConstantPredicate;
		private Theory theory;
		private ProblemType problemType;
		private DPLLGeneralizedAndSymbolic solver;
	
		public Result(Expression queryMarginal, Expression evidenceProbability, Map<String, String> mapFromTypeNameToSizeString, Map<String, String> mapFromRandomVariableNameToTypeName, Expression queryVariable, Collection<Expression> allVariables, Predicate<Expression> isUniquelyNamedConstantPredicate, Theory theory, ProblemType problemType, DPLLGeneralizedAndSymbolic solver) {
			super();
			this.queryMarginal = queryMarginal;
			this.evidenceProbability = evidenceProbability;
			this.mapFromTypeNameToSizeString = mapFromTypeNameToSizeString;
			this.mapFromRandomVariableNameToTypeName = mapFromRandomVariableNameToTypeName;
			this.queryVariable = queryVariable;
			this.allVariables = allVariables;
			this.isUniquelyNamedConstantPredicate = isUniquelyNamedConstantPredicate;
			this.theory = theory;
			this.problemType = problemType;
			this.solver = solver;
		}

		public Expression getQueryMarginal() {
			return queryMarginal;
		}
	
		public Expression getEvidenceProbability() {
			return evidenceProbability;
		}

		public Map<String, String> getMapFromTypeNameToSizeString() {
			return mapFromTypeNameToSizeString;
		}

		public Map<String, String> getMapFromRandomVariableNameToTypeName() {
			return mapFromRandomVariableNameToTypeName;
		}

		public Expression getQueryVariable() {
			return queryVariable;
		}

		public Collection<Expression> getAllVariables() {
			return allVariables;
		}

		public Predicate<Expression> getIsUniquelyNamedConstantPredicate() {
			return isUniquelyNamedConstantPredicate;
		}

		public Theory getTheory() {
			return theory;
		}

		public ProblemType getProblemType() {
			return problemType;
		}

		public DPLLGeneralizedAndSymbolic getSolver() {
			return solver;
		}
	}

	/**
	 * 
	 */
	public static void burglarExample() {
		// The definitions of types
		Map<String, String> mapFromTypeNameToSizeString = Util.map(
				"Folks", "10",
				"Boolean", "2");

		// The definitions of variables, types, and type sizes
		Map<String, String> mapFromVariableNameToTypeName = Util.map(
				"earthquake", "Boolean",
				"burglar",    "Folks", // a multi-value random variable
				"alarm",      "Boolean"
				);

		// a variant of the earthquake/burglary model in which some burglars are more active than others.
		Expression bayesianNetwork = parse("" + 
				"(if earthquake then 0.01 else 0.99) * " +
				"(if burglar = none then 0.7 else if burglar = tom then 0.1 else 0.2 / (|Folks| - 2)) * " +
				// note the division above of the potential by number of remaining values, as the probabilities must sum up to 1
				"(if burglar != none or earthquake "
				+    "then if alarm then 0.9 else 0.1 "
				+    "else if alarm then 0.05 else 0.95) " +
				"");

//      Expression evidence = null; // null indicates no evidence
		Expression evidence = parse("alarm");
//		Expression evidence = parse("alarm and burglar = none");
//		Expression evidence = parse("not alarm"); // can be any boolean expression
//		Expression evidence = parse("(alarm or not alarm) and (burglar = tom or burglar != tom)"); // tautology has same effect as no evidence

//		Expression queryExpression = parse("earthquake");
//		Expression queryExpression = parse("burglar = none");
		Expression queryExpression = parse("earthquake");
//		Expression queryExpression = parse("earthquake and burglar = bob");

		Expression marginal = solveFactorGraph(bayesianNetwork, true, null, queryExpression, evidence, mapFromTypeNameToSizeString, mapFromVariableNameToTypeName);

		if (evidence == null) {
			System.out.println("Query marginal probability P(" + queryExpression + ") is: " + marginal);
		}
		else {
			System.out.println("Query posterior probability P(" + queryExpression + " | " + evidence + ") is: " + marginal);
		}
	}

	/**
	 * An example.
	 * @param args
	 */
	public static void main(String[] args) {
		burglarExample();
	}
}
