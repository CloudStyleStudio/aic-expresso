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
package com.sri.ai.brewer.parsingexpression.core;

import java.util.Stack;

import com.google.common.annotations.Beta;
import com.sri.ai.brewer.api.ParsingExpression;
import com.sri.ai.brewer.api.ParsingProcess;

/**
 * A parsing expression for a non-terminal symbol in a grammar stored in the
 * current process. The expression is considered equivalent to the parsing
 * expression its name maps to in the grammar.
 * 
 * @author braz
 */
@Beta
public class NonTerminal extends AbstractParsingExpression {
	private static final long serialVersionUID = 1L;
	
	public NonTerminal(String name) {
		super("non-terminal", name);
	}

	@Override
	public ParsingExpression equivalentSimplerParsingExpression(ParsingProcess process) {
		String name = getName();
		ParsingExpression referenced = process.getGrammar().get(name);
		if (referenced == null) {
			throw new RuntimeException("Non-terminal " + get(0) + " not found in current grammar.");
		}
		return referenced;
	}

	private String getName() {
		Object result = get(0).getValue();
		return result.toString();
		// we get value first instead of using toString on the symbol straight because the latter includes quotes when there are spaces.
	}

	@Override
	public int computeLengthLowerBoundAfterBookkeeping(Stack<ParsingExpression> beingComputed, ParsingProcess process) {
		ParsingExpression equivalentSimplerParsingExpression = equivalentSimplerParsingExpression(process);
		int result = equivalentSimplerParsingExpression.computeLengthLowerBound(beingComputed, process);
		return result;
	}
}
