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
package com.sri.ai.grinder.library.function;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.core.AbstractModuleNoOpRewriter;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.Module;
import com.sri.ai.grinder.api.RewritingProcess;


/**
 * This module concentrates the functionality for registering and using pieces
 * of knowledge that can tell whether a function is symmetric
 * (meaning that all its arguments are symmetric, that is, their order does not matter).
 * 
 * @author braz
 */
@Beta
public class SymmetricModule extends AbstractModuleNoOpRewriter {

	/**
	 * An interface for objects that know how to determine
	 * whether the a function is symmetric.
	 * Providers must notify {@link SymmetricModule} of their existence
	 * with the method {@link SymmetricModule#register(Provider)} so it can invoke them.
	 * as necessary.
	 * {@link SymmetricModule#register(Provider, RewritingProcess)} is provided as a convenience for finding the module in the rewriting process.
	 */
	public static interface Provider extends Module.Provider {
		boolean isSymmetric(Expression function, RewritingProcess process);
		
		/** 
		 * If all the provider does is to compare the given function to a fixed functor,
		 * it should return that functor with this method.
		 * This allows the module to cache these values and be faster.
		 * Return null to indicate this is not possible.
		 */
		Object getFunctor();
	}

	/**
	 * Registers a {@link Provider} in the {@link SymmetricModule} module of the given process,
	 * or throw an error if there is not one.
	 */
	public static void register(Provider provider, RewritingProcess process) throws Error {
		register(SymmetricModule.class, provider, process);
	}

	private Set<Expression> cachedFunctors = new LinkedHashSet<Expression>();
	private boolean notAllProvidersHaveFixedFunctor = false;
	
	@Override
	public void register(Module.Provider moduleProvider) {
		Provider provider = (Provider) moduleProvider;
		Expression functor = Expressions.wrap(provider.getFunctor());
		if (functor != null) {
			cachedFunctors.add(functor);
			// note that in this case we do not register the provider, as it will not be used anymore.
		}
		else {
			notAllProvidersHaveFixedFunctor = true;
			super.register(provider);
		}
	}

	public boolean isSymmetric(Expression function, RewritingProcess process) {
		if ( ! cachedFunctors.isEmpty()) {
			boolean isInCachedFunctors = cachedFunctors.contains(function);
			if (isInCachedFunctors) {
				return true;
			}
			if ( ! notAllProvidersHaveFixedFunctor) {
				// cachedFunctors are exhaustive and it was not found,
				// so it will not be found by asking providers
				return false;
			}
		}
		
		// At this point, either we do not have cached functors, or function is not there and the cache is not exhaustive,
		// so we ask each provider.
		for (Module.Provider moduleProvider : providers.keySet()) {
			Provider provider = (Provider) moduleProvider;
			boolean result = provider.isSymmetric(function, process);
			if (result) {
				return true;
			}
		}
		return false;
	}
}
