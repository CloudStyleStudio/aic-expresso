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
package com.sri.ai.test.grinder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.OrderNormalize;
import com.sri.ai.grinder.library.DirectCardinalityComputationFactory;

public class OrderNormalizeTest {

	@Test
	public void testComparator() {
		@SuppressWarnings("unused")
		RewritingProcess process = DirectCardinalityComputationFactory.newCardinalityProcess();
		// we create this process to force Expressions to use the cardinality one which contains the SymmetryModule.
		
		Expression a;
		Expression b;
		OrderNormalize comparator = new OrderNormalize();
		
		a = Expressions.parse("p and q and r");
		b = Expressions.parse("p and q and r");
		assertEquals(0, comparator.compare(a, b));
		
		a = Expressions.parse("p and q and r");
		b = Expressions.parse("r and p and q");
		assertEquals(0, comparator.compare(a, b));
		
		a = Expressions.parse("p and q and r");
		b = Expressions.parse("r and p and q");
		assertTrue(comparator.equals(a, b));
		
		a = Expressions.parse("f(p,q,r)");
		b = Expressions.parse("f(p,q,r)");
		assertEquals(0, comparator.compare(a, b));
		
		a = Expressions.parse("f(p,q,r)");
		b = Expressions.parse("f(r,p,q)");
		assertTrue(comparator.compare(a, b) < 0);
		
		a = Expressions.parse("p and q and r");
		b = Expressions.parse("r and p and q and r");
		assertTrue(comparator.compare(a, b) < 0);
		// normalized a and normalized b have the same "prefix" but normalized a is shorter
	}
	
}
