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
package com.sri.ai.grinder.library.equality.cardinality.direct.core;

import java.util.List;

import com.google.common.annotations.Beta;
import com.sri.ai.grinder.api.Rewriter;
import com.sri.ai.grinder.helper.GrinderUtil;
import com.sri.ai.grinder.library.controlflow.IfThenElseConditionIsTrueInThenBranchAndFalseInElseBranch;
import com.sri.ai.grinder.library.controlflow.IfThenElseSubExpressionsAndImposedConditionsProvider;
import com.sri.ai.grinder.library.equality.cardinality.direct.CardinalityRewriter;
import com.sri.ai.util.base.Pair;

/**
 * Complete implementation of R_complete_simplify(E), including complete checking of implied certainties.
 * 
 * @author oreilly
 *
 */
@Beta
public class CompleteSimplify extends Simplify implements CardinalityRewriter {
	
	@Override
	public String getName() {
		return CardinalityRewriter.R_complete_simplify;
	}
	
	//
	// PROTECTED METHODS
	//
	@SuppressWarnings("unchecked")
	@Override
	protected List<Rewriter> getAtomicRewriters() {
		List<Rewriter> atomicRewriters = super.getAtomicRewriters();
		
		atomicRewriters = GrinderUtil.addRewritersBefore(atomicRewriters,
				//
				// Support for: full satisfiability testing
				new Pair<Class<?>, Rewriter>(
						IfThenElseSubExpressionsAndImposedConditionsProvider.class,
						new TopImpliedCertainty()),
				//
				// Support for: full satisfiability testing
				new Pair<Class<?>, Rewriter>(
						IfThenElseConditionIsTrueInThenBranchAndFalseInElseBranch.class,
						new ConjunctsHoldTrueForEachOther(true)) // the 'true' argument indicates the use of a complete normalizer inside
				);
		
		// One might think that the complete version of rewriters should replace their incomplete versions instead of being
		// added to the list of rewriters, but the incomplete versions are still useful for being faster in many cases.
		// Also, IncompleteTopImpliedCertainty does purely syntactic simplifications such as
		// if pretty(X) then if not pretty(X) then 1 else 2 else 3
		// ---->
		// if pretty(X) then 2 else 3
		// which TopImpliedCertainty does not do (it should eventually).
		
		return atomicRewriters;
	}
}
