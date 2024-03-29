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
package com.sri.ai.grinder.plaindpll.group;

import static com.sri.ai.expresso.helper.Expressions.INFINITY;
import static com.sri.ai.expresso.helper.Expressions.ONE;
import static com.sri.ai.expresso.helper.Expressions.ZERO;
import static com.sri.ai.util.Util.arrayList;
import static com.sri.ai.util.Util.list;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.Rewriter;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.TotalRewriter;
import com.sri.ai.grinder.library.number.FlattenMinusInPlus;
import com.sri.ai.grinder.library.number.Minus;
import com.sri.ai.grinder.library.number.Plus;
import com.sri.ai.grinder.library.number.Times;
import com.sri.ai.grinder.library.number.UnaryMinus;
import com.sri.ai.util.math.Rational;

/**
 * Object representing a group on symbolic numbers with addition.
 * 
 * @author braz
 *
 */
@Beta
public class SymbolicPlusGroup extends AbstractSymbolicNumbersGroup {
	
	@Override
	public Expression additiveIdentityElement() {
		return ZERO;
	}

	@Override
	public boolean isAdditiveAbsorbingElement(Expression value) {
		boolean result = value.equals(INFINITY);
		return result;
	}

	@Override
	public Expression add(Expression value1, Expression value2, RewritingProcess process) {
		Expression result;
		if (value1.getValue() instanceof Number && value2.getValue() instanceof Number) { // not necessary, as else clause is generic enough to deal with this case as well, but hopefully this saves time.
			result = Expressions.makeSymbol(value1.rationalValue().add(value2.rationalValue()));
		}
		else {
			Expression sum = Plus.make(arrayList(value1, value2));
			result = plusAndMinusRewriter.rewrite(sum, process);
		}
		return result;
	}

	private static Rewriter plusAndMinusRewriter = new TotalRewriter(new Plus(), new Minus(), new UnaryMinus(), new FlattenMinusInPlus());
	
	@Override
	protected Expression addNTimesWithUnconditionalValueAndNAndNDistinctFromZero(Expression valueToBeAdded, Expression n) {
		Expression result;
		if (valueToBeAdded.equals(ZERO)) { // optimization
			result = ZERO;
		}
		else if (valueToBeAdded.equals(ONE)) { // optimization
			result = n;
		}
		else {
			Rational valueToBeAddedRationalValue = valueToBeAdded.rationalValue();
			Rational nRationalValue = n.rationalValue();
			if (valueToBeAddedRationalValue != null && nRationalValue != null) {
				result = Expressions.makeSymbol(valueToBeAddedRationalValue.multiply(nRationalValue));
			}
			else {
				result = Times.make(list(valueToBeAdded, n));
			}
		}
		return result;
	}
}