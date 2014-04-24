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
package com.sri.ai.expresso.helper;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.SyntaxTree;
import com.sri.ai.grinder.core.FunctionApplicationProvider;
import com.sri.ai.util.base.Pair;
import com.sri.ai.util.base.SingletonListMaker;
import com.sri.ai.util.collect.DepthFirstIterator;
import com.sri.ai.util.collect.FunctionIterator;
import com.sri.ai.util.collect.IntegerIterator;
import com.sri.ai.util.collect.ZipIterator;

/**
 * An iterator that, given an expression, ranges over all pairs of its
 * sub-symbol tress and their respective paths.
 * 
 * @author braz
 */
@Beta
public class SubSyntaxTreeAndPathDepthFirstIterator extends DepthFirstIterator<Pair<Expression, List<Integer>>> {

	private static final SingletonListMaker<Integer> INTEGER_SINGLETON_LIST_MAKER = new SingletonListMaker<Integer>();
	
	public SubSyntaxTreeAndPathDepthFirstIterator(Pair<Expression, List<Integer>> expressionAndPath) {
		super(expressionAndPath);
	}

	public SubSyntaxTreeAndPathDepthFirstIterator(Expression expression) {
		super(new Pair<Expression, List<Integer>>(expression, new LinkedList<Integer>()));
	}
	
	@Override
	public DepthFirstIterator<Pair<Expression, List<Integer>>> newInstance(Pair<Expression, List<Integer>> object) {
		return new SubSyntaxTreeAndPathDepthFirstIterator(object);
	}

	@Override
	public Iterator<Pair<Expression, List<Integer>>> getChildrenIterator(Pair<Expression, List<Integer>> expressionAndPath) {
		SyntaxTree syntaxTree = (SyntaxTree) expressionAndPath.first;
		List<Integer> basePath = expressionAndPath.second;
		Iterator<Pair<Expression, List<Integer>>> result =
			new FunctionIterator<List<Object>, Pair<Expression, List<Integer>>>(
					new ZipIterator(
							FunctionApplicationProvider.getImmediateSubTreesIncludingFunctorOne(syntaxTree),
							makeSingleIndexPathsIterator(syntaxTree)),
					new ExpressionAndPathMaker(basePath));
	
		return result;
	}

	private static class ExpressionAndPathMaker implements Function<List<Object>, Pair<Expression, List<Integer>>> {
		private Collection<? extends Integer> basePath;

		public ExpressionAndPathMaker(Collection<? extends Integer> basePath) {
			this.basePath = basePath;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Pair<Expression, List<Integer>> apply(List<Object> binaryList) {
			Expression expression = (Expression)    binaryList.get(0);
			List<Integer> path    = (List<Integer>) binaryList.get(1);

			List<Integer> newPath = new LinkedList<Integer>(basePath);
			newPath.addAll(path);
			
			Pair<Expression, List<Integer>> result =
				new Pair<Expression, List<Integer>>(expression, newPath);
			return result;
		}
	}
	
	private static FunctionIterator<Integer, List<Integer>> makeSingleIndexPathsIterator(SyntaxTree syntaxTree) {
		return new FunctionIterator<Integer, List<Integer>>(
				new IntegerIterator(
						FunctionApplicationProvider.INDEX_OF_FUNCTOR_IN_FUNCTION_APPLICATIONS,
						FunctionApplicationProvider.getNumberOfImmediateSubTreesIncludingFunctorOne(syntaxTree)),
				INTEGER_SINGLETON_LIST_MAKER);
	}
}
