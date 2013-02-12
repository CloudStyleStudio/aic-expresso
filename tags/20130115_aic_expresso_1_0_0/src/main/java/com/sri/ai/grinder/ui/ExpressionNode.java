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
package com.sri.ai.grinder.ui;

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.tree.TreeNode;

import com.google.common.annotations.Beta;
import com.sri.ai.brewer.api.BasicParsingExpression;
import com.sri.ai.brewer.api.ParsingExpression;
import com.sri.ai.brewer.api.Writer;
import com.sri.ai.brewer.parsingexpression.core.Sequence;
import com.sri.ai.brewer.parsingexpression.helper.AssociativeSequence;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.CompoundSyntaxTree;
import com.sri.ai.expresso.api.Symbol;
import com.sri.ai.expresso.api.SyntaxTree;
import com.sri.ai.expresso.helper.Expressions;

/**
 * 
 * @author braz
 *
 */
@Beta
public class ExpressionNode implements TreeNode {
	public static enum NodeType {TERMINAL, ARGUMENT, FUNCTOR, SYMBOL, INARGUMENT, INFUNCTOR, STRING};
	
	protected Object userObject;
	protected Vector<ExpressionNode> children;
	protected String toolTipText;
	protected NodeType type;
	protected boolean unprocessed = true;
	protected ExpressionNode parent;
	protected Writer writer;
	
	public ExpressionNode(Expression exp, NodeType t, ExpressionNode par) {
		this(exp);
		parent = par;
		if ( exp instanceof Symbol ) {
			if ( t == NodeType.INFUNCTOR ) {
				type = NodeType.TERMINAL;
			} 
			else {
				type = NodeType.SYMBOL;
			}
		} 
		else {
			type = t;
		}
	}
	
	public ExpressionNode(Expression exp) {
		writer = TreeUtil.getWriter();
		toolTipText = exp == null ? "": writer.toString(exp);
		userObject = exp;
		type = null;
		parent = null;
	}
	
	public ExpressionNode(Object obj, ExpressionNode par) {
		writer = TreeUtil.getWriter();
		type = (obj instanceof String)? NodeType.STRING: null;
		userObject = obj;
		parent = par;
		toolTipText = obj == null? "" : obj.toString();
	}	
	
	public Object getUserObject() {
		return userObject;
	}

	public NodeType getType() {
		return type;
	}
	
	public void setType(NodeType t) {
		type = t;
	}
	
	public ExpressionNode() {
		super();
		this.toolTipText = null;
	}

	public String getToolTipText() {
		return toolTipText;
	}

	public void setToolTipText(String tooltip) {
		toolTipText = tooltip;
	}

	public void process() {
		if ( unprocessed ) {
			children = getChildren();
			unprocessed = false;
		}
	}
	
	@Override
	public Enumeration<ExpressionNode> children() {
		process();
		return children.elements();
	}

	public Vector<ExpressionNode> childrenAsVector() {
		process();
		return children;
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public TreeNode getChildAt(int arg0) {
		process();
		return children.get(arg0);
	}

	@Override
	public int getChildCount() {
		process();
		return children.size();
	}

	@Override
	public int getIndex(TreeNode arg0) {
		process();
		return children.indexOf(arg0);
	}

	@Override
	public TreeNode getParent() {
		process();
		return parent;
	}

	@Override
	public boolean isLeaf() {
		if ( userObject instanceof Symbol ) { 
			return true;
		} 
		else if ( userObject instanceof SyntaxTree ) {
			return false;
		} 
		else {
			process();
			return children.isEmpty();
		}
	}
	
	protected Vector<ExpressionNode> getChildren() {
		Vector<ExpressionNode> result = new Vector<ExpressionNode>();
		if ( !(userObject instanceof SyntaxTree) ) return result;
		SyntaxTree expression = (SyntaxTree)userObject;
		BasicParsingExpression parsingExpression = writer.getGrammar().getBasicParsingExpressionFor(expression);
		if (parsingExpression == null) {
			result = simpleExpressionToTree(expression);
		} 
		else if ( parsingExpression instanceof AssociativeSequence ) {
			result = associateSequenceToTree((AssociativeSequence)parsingExpression, expression);
		} 
		else if ( parsingExpression instanceof Sequence ) {
			result = sequenceToTree((Sequence)parsingExpression, expression);
		}
		return result;
	}
	
	protected Vector<ExpressionNode> simpleExpressionToTree(SyntaxTree expression) {
		Vector<ExpressionNode> result = new Vector<ExpressionNode>();
		if (expression == null) {
			return null;
		}
		if (expression instanceof CompoundSyntaxTree ) {
//			if (expression instanceof CompoundSyntaxTree) {
			return functionApplicationToTree(expression);
		}
		// SyntaxTree is just a symbol:
		result.add(new ExpressionNode(expression, NodeType.SYMBOL, this));
		return result;
	}
	
	protected Vector<ExpressionNode> functionApplicationToTree(SyntaxTree expression) {
		if (expression == null) {
			return null;
		}
		Vector<ExpressionNode> result = new Vector<ExpressionNode>();
		SyntaxTree functor = expression.getRootTree();
		result.add(new ExpressionNode(functor, NodeType.FUNCTOR, this));		
		for (SyntaxTree subTree: expression.getSyntaxTree().getImmediateSubTrees()) { // does need to be sub tree
			result.add(new ExpressionNode(subTree, NodeType.ARGUMENT, this));
		}
		return result;
	}
	

	protected Vector<ExpressionNode> sequenceToTree(Sequence parsingExpression, SyntaxTree expression) {
		Vector<ExpressionNode> result = new Vector<ExpressionNode>();
		int argumentIndex = 0;
		try {
		for (SyntaxTree subParsingExpressionAsExpression : parsingExpression.getImmediateSubTrees()) {
			ParsingExpression subParsingExpression = (ParsingExpression) subParsingExpressionAsExpression;

			if (subParsingExpression.hasFunctor("terminal")) {
				result.add(new ExpressionNode(subParsingExpression.get(0), NodeType.TERMINAL, this));
			}
			else if (subParsingExpression.hasFunctor("kleene")) {
				SyntaxTree kleeneList = expression.getImmediateSubTrees().get(argumentIndex);
				
				List<Expression> aaa = Expressions.ensureListFromKleeneList(kleeneList);
				for (Expression a: aaa) {
					result.add(new ExpressionNode(a, NodeType.ARGUMENT, this));
				}
			}
			else if (!(subParsingExpression.hasFunctor("optional") 
					   && expression.getImmediateSubTrees().get(argumentIndex) == null)) {
				
				SyntaxTree subExpression = expression.getImmediateSubTrees().get(argumentIndex);
				// boolean isIt = neighborsAreTerminals(expression, subParsingExpressionIndex);
				result.add(new ExpressionNode(subExpression, NodeType.ARGUMENT, this));
			}


			// everything is an argument, but terminals.
			if ( ! subParsingExpression.hasFunctor("terminal")) {
				argumentIndex++;
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	protected Vector<ExpressionNode> associateSequenceToTree(AssociativeSequence parsingExpression, final SyntaxTree expression) {
		Vector<ExpressionNode> result = new Vector<ExpressionNode>();
		SyntaxTree functor = expression.getRootTree();
		List<SyntaxTree> arguments = expression.getSyntaxTree().getImmediateSubTrees(); // does need to be sub tree
		
		if ( !arguments.isEmpty() ) {
			result.add(new ExpressionNode(arguments.get(0), NodeType.INARGUMENT, this));
		}
		
		for (int i=1; i<arguments.size(); i++) {
			result.add(new ExpressionNode(functor, NodeType.INFUNCTOR, this));
			result.add(new ExpressionNode(arguments.get(i), NodeType.INARGUMENT, this));			
		}

		return result;
	}

	public void setUserObject(Object obj) {
		userObject = obj;
	}

	public void add(Object object) {

		process();
		if ( object instanceof SyntaxTree ) {
			children.add(new ExpressionNode((SyntaxTree)object, null, this));
		} 
		else if ( object instanceof Expression ) {
			children.add(new ExpressionNode(((Expression)object).getSyntaxTree(), this));
		} 
		else {
			children.add(new ExpressionNode(object, this));
		}
	}
	
	public String toString() {
		return toolTipText;
	}
}