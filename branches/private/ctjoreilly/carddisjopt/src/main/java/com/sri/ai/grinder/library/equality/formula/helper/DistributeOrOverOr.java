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
package com.sri.ai.grinder.library.equality.formula.helper;

import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.AbstractRewriter;
import com.sri.ai.grinder.library.boole.Or;

/**
 * Distributes ors over ors in a formula:
 *
 * F0 or (F1 or ... or Fn) -> (F0 or F1 or ... or Fn)
 * (F1 or ... or Fn) or F0 -> (F1 or ... or Fn or F0)
 */
@Beta
public class DistributeOrOverOr extends AbstractRewriter {
	
	@Override
	public Expression rewriteAfterBookkeeping(Expression expression,
			RewritingProcess process) {
		Expression result = expression;
		
		if (Or.isDisjunction(expression) && expression.numberOfArguments() > 0) {
			// F0 or (F1 or ... or Fn) -> (F0 or F1 or ... or Fn)
			// (F1 or ... or Fn) or F0 -> (F1 or ... or Fn or F0)
			boolean nestedDisjuncts = false;
			List<Expression> disjuncts = new ArrayList<Expression>();
			for (Expression disjunct : expression.getArguments()) {
				if (Or.isDisjunction(disjunct)) {
					nestedDisjuncts = true;
					disjuncts.addAll(disjunct.getArguments());
				}
				else {
					disjuncts.add(disjunct);
				}
			}
			if (nestedDisjuncts) {
				result = Or.make(disjuncts);
			}
		}
		
		return result;
	}
}
