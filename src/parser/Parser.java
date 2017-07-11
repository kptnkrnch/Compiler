package parser;

import java.util.ArrayList;
import java.util.Arrays;

import asmCodeGenerator.IdentifierFactory;
import logging.PikaLogger;
import parseTree.*;
import parseTree.nodeTypes.ArrayExpressionNode;
import parseTree.nodeTypes.AssignmentNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BlockStatementNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BreakStatementNode;
import parseTree.nodeTypes.CallStatementNode;
import parseTree.nodeTypes.CastOperatorNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.CloneStatementNode;
import parseTree.nodeTypes.ContinueStatementNode;
import parseTree.nodeTypes.MainBlockNode;
import parseTree.nodeTypes.MapStatementNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ElseStatementNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.ExpressionListNode;
import parseTree.nodeTypes.FloatingConstantNode;
import parseTree.nodeTypes.FoldStatementNode;
import parseTree.nodeTypes.ForStatementIncrementNode;
import parseTree.nodeTypes.ForStatementNode;
import parseTree.nodeTypes.FunctionDefinitionNode;
import parseTree.nodeTypes.FunctionInvocationNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IndexOperatorNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.LambdaNode;
import parseTree.nodeTypes.LambdaParamTypeNode;
import parseTree.nodeTypes.LengthStatementNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.NotOperatorNode;
import parseTree.nodeTypes.ParameterListNode;
import parseTree.nodeTypes.ParameterSpecificationNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReduceStatementNode;
import parseTree.nodeTypes.ReleaseStatementNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.ReverseStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.SubstringStatementNode;
import parseTree.nodeTypes.TabNode;
import parseTree.nodeTypes.TypeListNode;
import parseTree.nodeTypes.TypeLiteralNode;
import parseTree.nodeTypes.UnaryOperatorNode;
import parseTree.nodeTypes.WhileStatementNode;
import parseTree.nodeTypes.ZipStatementNode;
import semanticAnalyzer.types.PrimitiveType;
import symbolTable.Binding;
import tokens.*;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import lexicalAnalyzer.Scanner;


public class Parser {
	private Scanner scanner;
	private Token nowReading;
	private Token previouslyRead;
	
	public static ParseNode parse(Scanner scanner) {
		Parser parser = new Parser(scanner);
		return parser.parse();
	}
	public Parser(Scanner scanner) {
		super();
		this.scanner = scanner;
	}
	
	public ParseNode parse() {
		readToken();
		return parseProgram();
	}

	////////////////////////////////////////////////////////////
	// "program" is the start symbol S
	// S -> globalDefinition* EXEC mainBlock
	
	private ParseNode parseProgram() {
		//if(!startsProgram(nowReading)) {
		//	return syntaxErrorNode("program");
		//}
		ParseNode program = new ProgramNode(nowReading);
		
		while(startsGlobalDefinition(nowReading)) {
			ParseNode globalDefinition = parseGlobalDefinition();
			program.appendChild(globalDefinition);
		}
		
		expect(Keyword.EXEC);
		ParseNode mainBlock = parseBlockStatement();
		program.appendChild(mainBlock);
		
		if(!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}
		
		return program;
	}
	private boolean startsProgram(Token token) {
		return token.isLextant(Keyword.EXEC);
	}
	
	///////////////////////////////////////////////////////////
	// global Definitions (Functions)
	// globalDefinition -> functionDefinition
	private ParseNode parseGlobalDefinition() {
		if(!startsGlobalDefinition(nowReading)) {
			return syntaxErrorNode("globalDefinition");
		}
		ParseNode node = null;
		if (startsFunctionDefinition(nowReading)) {
			node = parseFunctionDefinition();
		} else if (startsDeclaration(nowReading)) {
			node = parseDeclaration();
		}
		return node;
	}
	private boolean startsGlobalDefinition(Token token) {
		return startsFunctionDefinition(token) ||
				startsDeclaration(token);
	}
	
	///////////////////////////////////////////////////////////
	// functionDefinition
	// functionDefinition -> FUNC identifier lambda
	private ParseNode parseFunctionDefinition() {
		if(!startsFunctionDefinition(nowReading)) {
			return syntaxErrorNode("functionDefinition");
		}
		Token funcDefToken = nowReading;
		expect(Keyword.FUNC);
		ParseNode identifier = parseIdentifier();
		ParseNode lambda = parseLambda();
		ParseNode functionDefinition = FunctionDefinitionNode.make(funcDefToken, identifier, lambda);
		return functionDefinition;
	}
	private boolean startsFunctionDefinition(Token token) {
		return token.isLextant(Keyword.FUNC);
	}
	
	///////////////////////////////////////////////////////////
	// lambda
	// lambda -> lambdaParamType blockStatement
	private ParseNode parseLambda() {
		if(!startsLambda(nowReading)) {
			return syntaxErrorNode("lambda");
		}
		ParseNode lambdaParamType = parseLambdaParamType();
		ParseNode blockStatement = parseBlockStatement();
		ParseNode lambda = LambdaNode.make(nowReading, lambdaParamType, blockStatement);
		return lambda;
	}
	private boolean startsLambda(Token token) {
		return startsLambdaParamType(token);
	}
	
	///////////////////////////////////////////////////////////
	// lambdaParamType
	// lambdaParamType -> < parameterList > -> type
	private ParseNode parseLambdaParamType() {
		if(!startsLambdaParamType(nowReading)) {
			return syntaxErrorNode("lambdaParamType");
		}
		Token lambdaParamTypeToken = nowReading;
		ParseNode parameterList = new ParameterListNode(nowReading);
		expect(Punctuator.LESSER);
		if (startsParameterList(nowReading)) {
			parameterList = parseParameterList();
		}
		expect(Punctuator.GREATER);
		expect(Punctuator.RIGHT_ARROW);
		ParseNode typeLiteral = parseTypeLiteral();
		//ParseNode typeLiteral = null;// = parseTypeLiteral();
		//if (startsExpression(nowReading)) {
			//typeLiteral = parseExpression();
		//} else {
		//	return syntaxErrorNode("lambdaParamType - missing return type");
		//}
		ParseNode lambdaParamType = LambdaParamTypeNode.make(lambdaParamTypeToken, parameterList, typeLiteral);
		return lambdaParamType;
	}
	private boolean startsLambdaParamType(Token token) {
		return token.isLextant(Punctuator.LESSER);
	}
	
	///////////////////////////////////////////////////////////
	// parameterList
	// parameterList -> parameterSpecification,
	private ParseNode parseParameterList() {
		if(!startsParameterList(nowReading)) {
			return syntaxErrorNode("parameterList");
		}
		ParseNode parameterList = new ParameterListNode(nowReading);
		boolean foundSeparator = true;
		while(startsParameterSpecification(nowReading)) {
			if (!foundSeparator) {
				return syntaxErrorNode("parameterList - missing separator");
			}
			ParseNode parameterSpecification = parseParameterSpecification();
			parameterList.appendChild(parameterSpecification);
			if (nowReading.isLextant(Punctuator.SEPARATOR)) {
				readToken();
			} else {
				foundSeparator = false;
			}
		}
		return parameterList;
	}
	private boolean startsParameterList(Token token) {
		return startsParameterSpecification(token);
	}
	
	private ParseNode parseTypeList() {
		if(!startsTypeList(nowReading)) {
			return syntaxErrorNode("typeList");
		}
		ParseNode typeList = new TypeListNode(nowReading);
		boolean foundSeparator = true;
		while(startsParameterSpecification(nowReading)) {
			if (!foundSeparator) {
				return syntaxErrorNode("typeList - missing separator");
			}
			ParseNode typeLiteral = parseTypeLiteral();
			//ParseNode typeLiteral = parseExpression();
			typeList.appendChild(typeLiteral);
			if (nowReading.isLextant(Punctuator.SEPARATOR)) {
				readToken();
			} else {
				foundSeparator = false;
			}
		}
		return typeList;
	}
	private boolean startsTypeList(Token token) {
		return startsTypeLiteral(token);
	}
		
	///////////////////////////////////////////////////////////
	// parameterSpecification
	// parameterSpecification -> type identifier
	private ParseNode parseParameterSpecification() {
		if(!startsParameterSpecification(nowReading)) {
			return syntaxErrorNode("parameterSpecification");
		}
		Token parameterSpecificationToken = nowReading;
		ParseNode typeLiteralExpression = parseTypeLiteral();
		//ParseNode typeLiteralExpression = parseExpression();
		ParseNode identfier = parseIdentifier();
		ParseNode node = ParameterSpecificationNode.make(parameterSpecificationToken, typeLiteralExpression, identfier);
		return node;
	}
	private boolean startsParameterSpecification(Token token) {
		return startsTypeLiteral(token);
	}
	
	///////////////////////////////////////////////////////////
	// mainBlock 
	//   -deprecated. replaced by generic blockStatement
	// mainBlock -> { statement* }
	private ParseNode parseMainBlock() {
		if(!startsMainBlock(nowReading)) {
			return syntaxErrorNode("mainBlock");
		}
		ParseNode mainBlock = new MainBlockNode(nowReading);
		expect(Punctuator.OPEN_BRACE);
		
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			mainBlock.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return mainBlock;
	}
	private boolean startsMainBlock(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}
	
	///////////////////////////////////////////////////////////
	// blockStatements
	
	// blockStatement -> { statement* }
	private ParseNode parseBlockStatement() {
		if(!startsBlockStatement(nowReading)) {
			return syntaxErrorNode("blockStatement");
		}
		ParseNode block = new BlockStatementNode(nowReading);
		expect(Punctuator.OPEN_BRACE);
		
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			block.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return block;
	}
	private boolean startsBlockStatement(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}
	
	///////////////////////////////////////////////////////////
	// statements
	
	// statement-> declaration | printStmt | assignment
	private ParseNode parseStatement() {
		if(!startsStatement(nowReading)) {
			return syntaxErrorNode("statement");
		}
		if(startsDeclaration(nowReading)) {
			return parseDeclaration();
		}
		if(startsPrintStatement(nowReading)) {
			return parsePrintStatement();
		}
		//if(startsIdentifierExpression(nowReading)) {
		//	return parseIdentifierExpression();
		//}
		if (startsTargetStatement(nowReading)) {
			return parseTargetStatement();
		}
		if(startsBlockStatement(nowReading)) {
			return parseBlockStatement();
		}
		if(startsIfStatement(nowReading)) {
			return parseIfStatement();
		}
		if(startsWhileStatement(nowReading)) {
			return parseWhileStatement();
		}
		if(startsForStatement(nowReading)) {
			return parseForStatement();
		}
		if(startsReleaseStatement(nowReading)) {
			return parseReleaseStatement();
		}
		if(startsReturnStatement(nowReading)) {
			return parseReturnStatement();
		}
		if(startsCallStatement(nowReading)) {
			return parseCallStatement();
		}
		if(startsBreakStatement(nowReading)) {
			return parseBreakStatement();
		}
		if(startsContinueStatement(nowReading)) {
			return parseContinueStatement();
		}
		
		return syntaxErrorNode("statement");
	}
	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) ||
			   startsDeclaration(token) ||
			   startsTargetStatement(token) ||
			   //startsIdentifierExpression(token) ||
			   startsBlockStatement(token) ||
			   startsIfStatement(token) ||
			   startsWhileStatement(token) ||
			   startsForStatement(token) ||
			   startsReleaseStatement(token) ||
			   startsReturnStatement(token) ||
			   startsCallStatement(token) ||
			   startsBreakStatement(token) ||
			   startsContinueStatement(token);
	}
	
	// printStmt -> PRINT printExpressionList .
	private ParseNode parsePrintStatement() {
		if(!startsPrintStatement(nowReading)) {
			return syntaxErrorNode("print statement");
		}
		PrintStatementNode result = new PrintStatementNode(nowReading);
		
		readToken();
		result = parsePrintExpressionList(result);
		
		expect(Punctuator.TERMINATOR);
		return result;
	}
	private boolean startsPrintStatement(Token token) {
		return token.isLextant(Keyword.PRINT);
	}	

	// This adds the printExpressions it parses to the children of the given parent
	// printExpressionList -> printExpression* bowtie (,|;)  (note that this is nullable)

	private PrintStatementNode parsePrintExpressionList(PrintStatementNode parent) {
		while(startsPrintExpression(nowReading) || startsPrintSeparator(nowReading)) {
			parsePrintExpression(parent);
			parsePrintSeparator(parent);
		}
		return parent;
	}
	

	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> (expr | nl)?     (nullable)
	
	private void parsePrintExpression(PrintStatementNode parent) {
		if(startsExpression(nowReading)) {
			ParseNode child = parseExpression();
			parent.appendChild(child);
		}
		else if(nowReading.isLextant(Keyword.NEWLINE)) {
			readToken();
			ParseNode child = new NewlineNode(previouslyRead);
			parent.appendChild(child);
		} else if (nowReading.isLextant(Keyword.TAB)) {
			readToken();
			ParseNode child = new TabNode(previouslyRead);
			parent.appendChild(child);
		}
		// else we interpret the printExpression as epsilon, and do nothing
	}
	private boolean startsPrintExpression(Token token) {
		return startsExpression(token) || token.isLextant(Keyword.NEWLINE) || token.isLextant(Keyword.TAB);
	}
	
	
	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> expr? ,? nl? 
	
	private void parsePrintSeparator(PrintStatementNode parent) {
		if(!startsPrintSeparator(nowReading) && !nowReading.isLextant(Punctuator.TERMINATOR)) {
			ParseNode child = syntaxErrorNode("print separator");
			parent.appendChild(child);
			return;
		}
		
		if(nowReading.isLextant(Punctuator.SPACE)) {
			readToken();
			ParseNode child = new SpaceNode(previouslyRead);
			parent.appendChild(child);
		}
		else if(nowReading.isLextant(Punctuator.SEPARATOR)) {
			readToken();
		}		
		else if(nowReading.isLextant(Punctuator.TERMINATOR)) {
			// we're at the end of the bowtie and this printSeparator is not required.
			// do nothing.  Terminator is handled in a higher-level nonterminal.
		}
	}
	private boolean startsPrintSeparator(Token token) {
		return token.isLextant(Punctuator.SEPARATOR, Punctuator.SPACE) ;
	}
	
	
	// declaration -> STATIC? CONST/VAR identifier := expression .
	private ParseNode parseDeclaration() {
		if(!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		boolean isStatic = false;
		boolean isMutable = false;
		Token declarationToken = nowReading;
		
		if (nowReading.isLextant(Keyword.STATIC)) {
			expect(Keyword.STATIC);
			isStatic = true;
		}
		
		if (nowReading.isLextant(Keyword.CONST)) {
			expect(Keyword.CONST);
			isMutable = false;
		} else {
			expect(Keyword.VAR);
			isMutable = true;
		}
		
		ParseNode identifier = parseIdentifier();
		if (identifier instanceof IdentifierNode) {
			((IdentifierNode) identifier).setStatic(isStatic);
			((IdentifierNode) identifier).setMutable(isMutable);
		}
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		//identifier.setType(initializer.getType());
		
		DeclarationNode node = DeclarationNode.withChildren(declarationToken, identifier, initializer);
		//node.setType(initializer.getType());
		return node;
	}
	private boolean startsDeclaration(Token token) {
		return token.isLextant(Keyword.CONST) || 
				token.isLextant(Keyword.VAR) || 
				token.isLextant(Keyword.STATIC);
	}
	
	// assignment -> identifier := expression.
	private ParseNode parseAssignment(ParseNode identifier) {
		if(!startsAssignment(nowReading)) {
			return syntaxErrorNode("assignment");
		}
		Token assignmentToken = nowReading;
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		//identifier.setType(initializer.getType());
		AssignmentNode node = AssignmentNode.withChildren(assignmentToken, identifier, initializer);
		//node.setType(initializer.getType());
		return node;
	}
	private boolean startsAssignment(Token token) {
		return token.isLextant(Punctuator.ASSIGN);
	}
	
	private ParseNode parseAssignmentIndexingOperation(ParseNode identifier) {
		if(!startsAssignmentIndexingOperation(nowReading)) {
			return syntaxErrorNode("assignmentIndexingOperation");
		}
		IndexOperatorToken indexToken = IndexOperatorToken.make(nowReading);
		ParseNode indexOperation = new IndexOperatorNode(indexToken);
		indexOperation.appendChild(identifier);
		while (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
			expect(Punctuator.OPEN_BRACKET);
			ParseNode index = parseExpression();
			indexOperation.appendChild(index);
			expect(Punctuator.CLOSE_BRACKET);
		}
		identifier = indexOperation;
		return identifier;
	}
	private boolean startsAssignmentIndexingOperation(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACKET);
	}
	
	private ParseNode parseIdentifierExpression() {
		if(!startsIdentifierExpression(nowReading)) {
			return syntaxErrorNode("identifierExpression");
		}
		ParseNode identifier = parseIdentifier();
		if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
			ParseNode expression = parseAssignmentIndexingOperation(identifier);
			expression = parseAssignment(expression);
			return expression;
		} else if (nowReading.isLextant(Punctuator.ASSIGN)) { 
			ParseNode expression = parseAssignment(identifier);
			return expression;
		} /*else if (nowReading.isLextant(Punctuator.OPEN_PARENTHESES)) {
			ParseNode expression = parseFunctionCall(identifier);
			expect(Punctuator.TERMINATOR);
			return expression;
		}*/ else {
			return syntaxErrorNode("identifierExpression");
		}
	}
	private boolean startsIdentifierExpression(Token token) {
		return token instanceof IdentifierToken;
	}
	
	private ParseNode parseTargetStatement() {
		if(!startsTargetStatement(nowReading)) {
			return syntaxErrorNode("targetStatement");
		}
		ParseNode expression = parseExpression();
		if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
			ParseNode subExpression = parseAssignmentIndexingOperation(expression);
			subExpression = parseAssignment(subExpression);
			return subExpression;
		} else if (nowReading.isLextant(Punctuator.ASSIGN)) { 
			ParseNode subExpression = parseAssignment(expression);
			return subExpression;
		} else {
			return syntaxErrorNode("targetStatement");
		}
	}
	private boolean startsTargetStatement(Token token) {
		return startsExpression(token);
	}
	
	private ParseNode parseIfStatement() {
		if(!startsIfStatement(nowReading)) {
			return syntaxErrorNode("ifStatement");
		}
		ParseNode ifStatement = new IfStatementNode(nowReading);
		readToken();
		expect(Punctuator.OPEN_PARENTHESES);
		if (startsExpression(nowReading)) {
			ParseNode expression = parseExpression();
			ifStatement.appendChild(expression);
		} else {
			return syntaxErrorNode("ifStatement - missing conditional statement");
		}
		expect(Punctuator.CLOSE_PARENTHESES);
		if (startsBlockStatement(nowReading)) {
			ifStatement.appendChild(parseBlockStatement());
		} else {
			return syntaxErrorNode("ifStatement - missing block statement");
		}
		if (nowReading.isLextant(Keyword.ELSE)) {
			ParseNode elseStatement = new ElseStatementNode(nowReading);
			readToken();
			if (startsBlockStatement(nowReading)) {
				elseStatement.appendChild(parseBlockStatement());
				ifStatement.appendChild(elseStatement);
			} else {
				return syntaxErrorNode("elseStatement - missing block statement");
			}
		}
		return ifStatement;
	}
	private boolean startsIfStatement(Token token) {
		return token.isLextant(Keyword.IF);
	}
	
	private ParseNode parseWhileStatement() {
		if(!startsWhileStatement(nowReading)) {
			return syntaxErrorNode("whileStatement");
		}
		ParseNode whileStatement = new WhileStatementNode(nowReading);
		readToken();
		expect(Punctuator.OPEN_PARENTHESES);
		if (startsExpression(nowReading)) {
			ParseNode expression = parseExpression();
			whileStatement.appendChild(expression);
		} else {
			return syntaxErrorNode("whileStatement - missing conditional statement");
		}
		expect(Punctuator.CLOSE_PARENTHESES);
		if (startsBlockStatement(nowReading)) {
			whileStatement.appendChild(parseBlockStatement());
		} else {
			return syntaxErrorNode("whileStatement - missing block statement");
		}
		
		return whileStatement;
	}
	private boolean startsWhileStatement(Token token) {
		return token.isLextant(Keyword.WHILE);
	}
	
	private ParseNode parseForStatement() {
		if(!startsForStatement(nowReading)) {
			return syntaxErrorNode("forStatement");
		}
		ForStatementNode forStatement = new ForStatementNode(nowReading);
		ForStatementIncrementNode header = new ForStatementIncrementNode(nowReading);
		IdentifierNode indexer = new IdentifierNode(IdentifierToken.make(nowReading.getLocation(), IdentifierFactory.makeIdentifier("index")));
		forStatement.appendChild(header);
		
		expect(Keyword.FOR);
		
		if (nowReading.isLextant(Keyword.INDEX)) {
			expect(Keyword.INDEX);
			forStatement.setIndexLoop(true);
			header.setIndexLoop(true);
		} else {
			expect(Keyword.ELEM);
			forStatement.setElemLoop(true);
			header.setElemLoop(true);
		}
		
		if (startsIdentifier(nowReading)) {
			ParseNode identifier = parseIdentifier();
			header.appendChild(identifier);
		} else {
			return syntaxErrorNode("forStatement - missing identifier");
		}
		
		expect(Keyword.OF);
		
		if (startsExpression(nowReading)) {
			ParseNode expression = parseExpression();
			header.appendChild(expression);
		} else {
			return syntaxErrorNode("forStatement - missing expression");
		}
		
		header.appendChild(indexer);
		
		if (startsBlockStatement(nowReading)) {
			forStatement.appendChild(parseBlockStatement());
		} else {
			return syntaxErrorNode("forStatement - missing block statement");
		}
		
		return forStatement;
	}
	private boolean startsForStatement(Token token) {
		return token.isLextant(Keyword.FOR);
	}
	
	private ParseNode parseReleaseStatement() {
		if(!startsReleaseStatement(nowReading)) {
			return syntaxErrorNode("release");
		}
		Token releaseToken = nowReading;
		expect(Keyword.RELEASE);
		ParseNode expression = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		//identifier.setType(initializer.getType());
		ReleaseStatementNode node = ReleaseStatementNode.make(releaseToken, expression);
		//node.setType(initializer.getType());
		return node;
	}
	private boolean startsReleaseStatement(Token token) {
		return token.isLextant(Keyword.RELEASE);
	}
	
	private ParseNode parseReturnStatement() {
		if(!startsReturnStatement(nowReading)) {
			return syntaxErrorNode("return");
		}
		Token returnToken = nowReading;
		ParseNode expression = null;
		expect(Keyword.RETURN);
		if (startsExpression(nowReading)) {
			expression = parseExpression();
		}
		expect(Punctuator.TERMINATOR);
		
		ReturnStatementNode node = ReturnStatementNode.make(returnToken, expression);
		return node;
	}
	private boolean startsReturnStatement(Token token) {
		return token.isLextant(Keyword.RETURN);
	}
	
	private ParseNode parseCallStatement() {
		if(!startsCallStatement(nowReading)) {
			return syntaxErrorNode("call");
		}
		Token callToken = nowReading;
		ParseNode expression = null;
		expect(Keyword.CALL);
		if (startsExpression(nowReading)) {
			expression = parseFunctionInvocationExpression();
		}
		expect(Punctuator.TERMINATOR);
		
		CallStatementNode node = CallStatementNode.make(callToken, expression);
		return node;
	}
	private boolean startsCallStatement(Token token) {
		return token.isLextant(Keyword.CALL);
	}
	
	private ParseNode parseBreakStatement() {
		if(!startsBreakStatement(nowReading)) {
			return syntaxErrorNode("break");
		}
		Token breakToken = nowReading;
		expect(Keyword.BREAK);
		expect(Punctuator.TERMINATOR);
		
		BreakStatementNode node = new BreakStatementNode(breakToken);
		return node;
	}
	private boolean startsBreakStatement(Token token) {
		return token.isLextant(Keyword.BREAK);
	}
	
	private ParseNode parseContinueStatement() {
		if(!startsContinueStatement(nowReading)) {
			return syntaxErrorNode("continue");
		}
		Token continueToken = nowReading;
		expect(Keyword.CONTINUE);
		expect(Punctuator.TERMINATOR);
		
		ContinueStatementNode node = new ContinueStatementNode(continueToken);
		return node;
	}
	private boolean startsContinueStatement(Token token) {
		return token.isLextant(Keyword.CONTINUE);
	}

	private ParseNode parseBracketStatement() {
		if (!startsBracketStatement(nowReading)) {
			return syntaxErrorNode("bracket expression");
		}
		ParseNode node = null;
		if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
			Token startingLocation = nowReading;
			expect(Punctuator.OPEN_BRACKET);
			
			ParseNode child;
			if (startsExpression(nowReading)) {
				child = parseExpression();
				if (nowReading.isLextant(Punctuator.VERTICAL_LINE)) {
					node = parseCastingExpression(startingLocation, child);
				} else {
					node = parseArrayExpressionList(startingLocation, child);
				}
			} /*else if (startsTypeLiteral(nowReading) && (child = parseTypeLiteral()) != null) {
				
			}*/ else {
				return syntaxErrorNode("bracket invalid expression");
			}
		} else {
			node = parseLambdaExpression();
		}
		
		return node;
	}
	private boolean startsBracketStatement(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACKET) || 
				startsLambdaExpression(token);
	}
	
	
	
	///////////////////////////////////////////////////////////
	// expressions
	// expr                     -> comparisonExpression
	// comparisonExpression     -> additiveExpression [> additiveExpression]?
	// additiveExpression       -> subtractiveExpression [+ subtractiveExpression]*  (left-assoc)
	// subtractiveExpression    -> multiplicativeExpression [- multiplicativeExpression]*  (left-assoc)
	// multiplicativeExpression -> divisionalExpression [MULT divisionalExpression]*  (left-assoc)
	// divisionalExpression     -> atomicExpression [DIV atomicExpression]*  (left-assoc)
	// atomicExpression         -> literal
	// literal                  -> intNumber | identifier | booleanConstant

	// expr  -> comparisonExpression
	private ParseNode parseExpression() {		
		if(!startsExpression(nowReading)) {
			return syntaxErrorNode("expression");
		}
 		return parseOrOperatorExpression();
	}
	private boolean startsExpression(Token token) {
		return startsOrOperatorExpression(token);
	}
	
	
	
	private ParseNode parseLambdaExpression() {
		if (!startsLambdaExpression(nowReading)) {
			return syntaxErrorNode("lambda expression");
		}
		if (startsLambda(nowReading)) {
			ParseNode lambdaExpression = parseLambda();
			return lambdaExpression;
		} else {
			return parseNewArrayExpression();
		}
	}
	private boolean startsLambdaExpression(Token token) {
		return startsLambda(token) || startsNewArrayExpression(token);
	}
	
	/*private ParseNode parseFunctionInvocationExpression() {
		if (!startsFunctionInvocationExpression(nowReading)) {
			return syntaxErrorNode("function invocation expression");
		}
		if (startsIdentifier(nowReading)) {
			ParseNode identifier = parseIdentifier();
			if (startsIndexBracketOperator(nowReading)) {
				identifier = parseIndexingOperation(identifier);
			}
			if (startsFunctionCall(nowReading)) {
				ParseNode functionInvocation = parseFunctionCall(identifier);
				return functionInvocation;
			} else {
				return identifier;
			}
		} else {
			return parseNewArrayExpression();
		}
	}
	private boolean startsFunctionInvocationExpression(Token token) {
		return startsNewArrayExpression(token) || startsIdentifier(token);
	}*/
	private ParseNode parseCastingExpression(Token start, ParseNode child) {
		if (!startsCastingExpression(nowReading) && !startsCastingExpression(start)) {
			return syntaxErrorNode("casting expression");
		}
		ParseNode node = null;
		if (start.isLextant(Punctuator.OPEN_BRACKET)) {
			Token castingToken = CastToken.make(start);
			
			expect(Punctuator.VERTICAL_LINE);
			ParseNode typeLiteralExpression = parseTypeLiteral();
			//ParseNode typeLiteralExpression = parseExpression();
			node = CastOperatorNode.withChildren(castingToken, child, typeLiteralExpression);
			/*if (nowReading.isLextant(Keyword.FLOAT)) {
				node = CastOperatorNode.withChildren(castingToken, child, PrimitiveType.FLOAT);
			} else if (nowReading.isLextant(Keyword.INT)) {
				node = CastOperatorNode.withChildren(castingToken, child, PrimitiveType.INTEGER);
			} else if (nowReading.isLextant(Keyword.BOOL)) {
				node = CastOperatorNode.withChildren(castingToken, child, PrimitiveType.BOOLEAN);
			} else if (nowReading.isLextant(Keyword.STRING)) {
				node = CastOperatorNode.withChildren(castingToken, child, PrimitiveType.STRING);
			} else if (nowReading.isLextant(Keyword.CHAR)) {
				node = CastOperatorNode.withChildren(castingToken, child, PrimitiveType.CHARACTER);
			} else {
				return syntaxErrorNode("valid casting type");
			}*/
			expect(Punctuator.CLOSE_BRACKET);
		} else {
			node = parseLambdaExpression();
		}
		return node;
	}
	private boolean startsCastingExpression(Token token) {
		return startsFunctionInvocationExpression(token) || token.isLextant(Punctuator.OPEN_BRACKET);
	}
	private ParseNode parseParenthesesExpression() {
		if (!startsParenthesesExpression(nowReading)) {
			return syntaxErrorNode("parentheses expression");
		}
		ParseNode node;
		if (nowReading.isLextant(Punctuator.OPEN_PARENTHESES)) {
			expect(Punctuator.OPEN_PARENTHESES);
			node = parseExpression();
			expect(Punctuator.CLOSE_PARENTHESES);
		} else {
			node = parseAtomicExpression();
		}
		return node;
	}
	private boolean startsParenthesesExpression(Token token) {
		return startsAtomicExpression(token) || token.isLextant(Punctuator.OPEN_PARENTHESES);
	}
	
	private ParseNode parseOrOperatorExpression() {
		if(!startsComparisonExpression(nowReading)) {
			return syntaxErrorNode("comparison expression");
		}
		
		ParseNode left = parseAndOperatorExpression();
		while(nowReading.isLextant(Punctuator.OR)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseAndOperatorExpression();
			
			left = BinaryOperatorNode.withChildren(compareToken, left, right);
		}
		return left;

	}
	private boolean startsOrOperatorExpression(Token token) {
		return startsAndOperatorExpression(token);
	}
	
	private ParseNode parseAndOperatorExpression() {
		if(!startsComparisonExpression(nowReading)) {
			return syntaxErrorNode("comparison expression");
		}
		
		ParseNode left = parseComparisonExpression();
		while(nowReading.isLextant(Punctuator.AND)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseComparisonExpression();
			
			left = BinaryOperatorNode.withChildren(compareToken, left, right);
		}
		return left;

	}
	private boolean startsAndOperatorExpression(Token token) {
		return startsComparisonExpression(token);
	}
	// comparisonExpression -> additiveExpression [> additiveExpression]?
	private ParseNode parseComparisonExpression() {
		if(!startsComparisonExpression(nowReading)) {
			return syntaxErrorNode("comparison expression");
		}
		
		//ParseNode left = parseAdditiveExpression();
		ParseNode left = parseAdditiveLevelExpression();
		while(nowReading.isLextant(Punctuator.GREATER, 
								Punctuator.GREATER_EQUAL,
								Punctuator.LESSER,
								Punctuator.LESSER_EQUAL,
								Punctuator.EQUAL,
								Punctuator.NOT_EQUAL)) {
			Token compareToken = nowReading;
			readToken();
			//ParseNode right = parseAdditiveExpression();
			ParseNode right = parseAdditiveLevelExpression();
			
			left = BinaryOperatorNode.withChildren(compareToken, left, right);
		}
		return left;

	}
	private boolean startsComparisonExpression(Token token) {
		//return startsAdditiveExpression(token);
		return startsAdditiveLevelExpression(token);
	}
	
	private ParseNode parseAdditiveLevelExpression() {
		if(!startsAdditiveExpression(nowReading)) {
			return syntaxErrorNode("additiveExpression");
		}
		
 		ParseNode left = parseMultiplicativeLevelExpression();
		while(nowReading.isLextant(Punctuator.ADD) || nowReading.isLextant(Punctuator.SUBTRACT)) {
			Token additiveToken = nowReading;
			readToken();
			ParseNode right = parseMultiplicativeLevelExpression();
			
			left = BinaryOperatorNode.withChildren(additiveToken, left, right);
		}
		return left;
	}
	private boolean startsAdditiveLevelExpression(Token token) {
		return startsMultiplicativeLevelExpression(token);
	}

	// additiveExpression -> multiplicativeExpression [+ multiplicativeExpression]*  (left-assoc)
	private ParseNode parseAdditiveExpression() {
		if(!startsAdditiveExpression(nowReading)) {
			return syntaxErrorNode("additiveExpression");
		}
		
		ParseNode left = parseSubtractiveExpression();
		while(nowReading.isLextant(Punctuator.ADD)) {
			Token additiveToken = nowReading;
			readToken();
			ParseNode right = parseSubtractiveExpression();
			
			left = BinaryOperatorNode.withChildren(additiveToken, left, right);
		}
		return left;
	}
	private boolean startsAdditiveExpression(Token token) {
		return startsSubtractiveExpression(token);
	}
	
	
	private ParseNode parseSubtractiveExpression() {
		if(!startsSubtractiveExpression(nowReading)) {
			return syntaxErrorNode("additiveExpression");
		}
		
		//ParseNode left = parseMultiplicativeExpression();
		ParseNode left = parseMultiplicativeOrDivisionalExpression();
		while(nowReading.isLextant(Punctuator.SUBTRACT)) {
			Token subtractiveToken = nowReading;
			readToken();
			//ParseNode right = parseMultiplicativeExpression();
			ParseNode right = parseMultiplicativeOrDivisionalExpression();
			
			left = BinaryOperatorNode.withChildren(subtractiveToken, left, right);
		}
		return left;
	}
	private boolean startsSubtractiveExpression(Token token) {
		//return startsMultiplicativeExpression(token);
		return startsMultiplicativeOrDivisionalExpression(token);
	}

	private ParseNode parseMultiplicativeOrDivisionalExpression() {
		if(!startsMultiplicativeOrDivisionalExpression(nowReading)) {
			return syntaxErrorNode("multiplicativeExpression");
		}
		
		ParseNode left = parseOverExpression();
		while(nowReading.isLextant(Punctuator.MULTIPLY) || nowReading.isLextant(Punctuator.DIVIDE)) {
			Token multiplicativeOrDivisionalToken = nowReading;
			readToken();
			ParseNode right = parseOverExpression();
			
			left = BinaryOperatorNode.withChildren(multiplicativeOrDivisionalToken, left, right);
		}
		return left;
	}
	private boolean startsMultiplicativeOrDivisionalExpression(Token token) {
		return startsOverExpression(token);
	}
	
	private ParseNode parseMultiplicativeLevelExpression() {
		if(!startsMultiplicativeOrDivisionalExpression(nowReading)) {
			return syntaxErrorNode("multiplicativeExpression");
		}
		
		ParseNode left = parseFoldStatement();
		while(nowReading.isLextant(Punctuator.MULTIPLY) 
				|| nowReading.isLextant(Punctuator.DIVIDE)
				|| nowReading.isLextant(Punctuator.OVER)
				|| nowReading.isLextant(Punctuator.EXPRESS_OVER)
				|| nowReading.isLextant(Punctuator.RATIONALIZE)) {
			Token multiplicativeOrDivisionalToken = nowReading;
			readToken();
			ParseNode right = parseFoldStatement();
			
			left = BinaryOperatorNode.withChildren(multiplicativeOrDivisionalToken, left, right);
		}
		return left;
	}
	private boolean startsMultiplicativeLevelExpression(Token token) {
		return startsFoldStatement(token);
	}
	
	// multiplicativeExpression -> atomicExpression [MULT atomicExpression]*  (left-assoc)
	private ParseNode parseMultiplicativeExpression() {
		if(!startsMultiplicativeExpression(nowReading)) {
			return syntaxErrorNode("multiplicativeExpression");
		}
		
		ParseNode left = parseDivisionalExpression();
		while(nowReading.isLextant(Punctuator.MULTIPLY)) {
			Token multiplicativeToken = nowReading;
			readToken();
			ParseNode right = parseDivisionalExpression();
			
			left = BinaryOperatorNode.withChildren(multiplicativeToken, left, right);
		}
		return left;
	}
	private boolean startsMultiplicativeExpression(Token token) {
		return startsDivisionalExpression(token);
	}
	
	
	private ParseNode parseDivisionalExpression() {
		if(!startsDivisionalExpression(nowReading)) {
			return syntaxErrorNode("multiplicativeExpression");
		}
		ParseNode left = parseOverExpression();
		
		while(nowReading.isLextant(Punctuator.DIVIDE)) {
			Token divisionalToken = nowReading;
			readToken();
			ParseNode right = parseOverExpression();
			
			left = BinaryOperatorNode.withChildren(divisionalToken, left, right);
		}
		return left;
	}
	private boolean startsDivisionalExpression(Token token) {
		return startsOverExpression(token);
	}
	
	private ParseNode parseOverExpression() {
		if(!startsOverExpression(nowReading)) {
			return syntaxErrorNode("overExpression");
		}
		ParseNode left = parseExpressOverExpression();
		
		while(nowReading.isLextant(Punctuator.OVER)) {
			Token overToken = nowReading;
			readToken();
			ParseNode right = parseExpressOverExpression();
			
			left = BinaryOperatorNode.withChildren(overToken, left, right);
		}
		return left;
	}
	private boolean startsOverExpression(Token token) {
		return startsExpressOverExpression(token);
	}
	
	private ParseNode parseExpressOverExpression() {
		if(!startsExpressOverExpression(nowReading)) {
			return syntaxErrorNode("expressOverExpression");
		}
		ParseNode left = parseRationalizeExpression();
		
		while(nowReading.isLextant(Punctuator.EXPRESS_OVER)) {
			Token expressOverToken = nowReading;
			readToken();
			ParseNode right = parseRationalizeExpression();
			
			left = BinaryOperatorNode.withChildren(expressOverToken, left, right);
		}
		return left;
	}
	private boolean startsExpressOverExpression(Token token) {
		return startsRationalizeExpression(token);
	}
	
	private ParseNode parseRationalizeExpression() {
		if(!startsRationalizeExpression(nowReading)) {
			return syntaxErrorNode("rationalizeExpression");
		}
		ParseNode left = parseFoldStatement();
		
		while(nowReading.isLextant(Punctuator.RATIONALIZE)) {
			Token rationalizeToken = nowReading;
			readToken();
			ParseNode right = parseFoldStatement();
			
			left = BinaryOperatorNode.withChildren(rationalizeToken, left, right);
		}
		return left;
	}
	private boolean startsRationalizeExpression(Token token) {
		return startsFoldStatement(token);
	}
	
	private ParseNode parseFoldStatement() {
		if (!startsFoldStatement(nowReading)) {
			return syntaxErrorNode("foldStatement");
		}
		//ParseNode left = parseReduceStatement();
		ParseNode left = parseMapReduceLevelStatement();
		
		if (nowReading.isLextant(Keyword.FOLD)) {
			Token foldToken = nowReading;
			readToken();
			ParseNode foldStatement = new FoldStatementNode(foldToken);
			foldStatement.appendChild(left);
			if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
				expect(Punctuator.OPEN_BRACKET);
				//ParseNode baseValue = parseReduceStatement();
				ParseNode baseValue = parseMapReduceLevelStatement();
				foldStatement.appendChild(baseValue);
				expect(Punctuator.CLOSE_BRACKET);
			}
			//ParseNode right = parseReduceStatement();
			ParseNode right = parseMapReduceLevelStatement();
			foldStatement.appendChild(right);
			left = foldStatement;
		}
		return left;
	}
	private boolean startsFoldStatement(Token token) {
		//return startsReduceStatement(token);
		return startsMapReduceLevelStatement(token);
	}
	
	private ParseNode parseMapReduceLevelStatement() {
		if (!startsMapReduceLevelStatement(nowReading)) {
			return syntaxErrorNode("MapReduceLevelStatement");
		}
		//ParseNode left = parseNotOperatorExpression();
		ParseNode left = parseNotCloneReverseLengthZipLevelOperators();
		while (nowReading.isLextant(Keyword.REDUCE) || nowReading.isLextant(Keyword.MAP)) {
			if (nowReading.isLextant(Keyword.REDUCE)) {
				Token reduceToken = nowReading;
				readToken();
				//ParseNode right = parseNotOperatorExpression();
				ParseNode right = parseNotCloneReverseLengthZipLevelOperators();
				ParseNode reduce = new ReduceStatementNode(reduceToken);
				reduce.appendChild(left);
				reduce.appendChild(right);
				left = reduce;
			} else if (nowReading.isLextant(Keyword.MAP)) {
				Token mapToken = nowReading;
				readToken();
				//ParseNode right = parseNotOperatorExpression();
				ParseNode right = parseNotCloneReverseLengthZipLevelOperators();
				ParseNode map = new MapStatementNode(mapToken);
				map.appendChild(left);
				map.appendChild(right);
				left = map;
			}
		}
		return left;
	}
	private boolean startsMapReduceLevelStatement(Token token) {
		return startsNotCloneReverseLengthZipLevelOperators(token);
	}
	
	private ParseNode parseReduceStatement() {
		if (!startsReduceStatement(nowReading)) {
			return syntaxErrorNode("reverseStatement");
		}
		ParseNode left = parseMapStatement();
		
		while(nowReading.isLextant(Keyword.REDUCE)) {
			Token reduceToken = nowReading;
			readToken();
			ParseNode right = parseMapStatement();
			ParseNode reduce = new ReduceStatementNode(reduceToken);
			reduce.appendChild(left);
			reduce.appendChild(right);
			left = reduce;
		}
		return left;
	}
	private boolean startsReduceStatement(Token token) {
		return startsMapStatement(token);
	}
	
	private ParseNode parseMapStatement() {
		if (!startsMapStatement(nowReading)) {
			return syntaxErrorNode("mapStatement");
		}
		//ParseNode left = parseNotOperatorExpression();
		ParseNode left = parseNotCloneReverseLengthZipLevelOperators();
		
		while(nowReading.isLextant(Keyword.MAP)) {
			Token mapToken = nowReading;
			readToken();
			//ParseNode right = parseNotOperatorExpression();
			ParseNode right = parseNotCloneReverseLengthZipLevelOperators();
			ParseNode map = new MapStatementNode(mapToken);
			map.appendChild(left);
			map.appendChild(right);
			left = map;
		}
		return left;
	}
	private boolean startsMapStatement(Token token) {
		//return startsNotOperatorExpression(token);
		return startsNotCloneReverseLengthZipLevelOperators(token);
	}
	
	private ParseNode parseNotCloneReverseLengthZipLevelOperators() {
		if(!startsNotCloneReverseLengthZipLevelOperators(nowReading)) {
			return syntaxErrorNode("Not-Clone-Reverse-Length-Zip-Level-Operator expression");
		}
		ParseNode node = null;
		if (nowReading.isLextant(Punctuator.NOT) ||
				nowReading.isLextant(Keyword.CLONE) ||
				nowReading.isLextant(Keyword.LENGTH) ||
				nowReading.isLextant(Keyword.REVERSE) ||
				nowReading.isLextant(Keyword.ZIP)) {
			while (nowReading.isLextant(Punctuator.NOT) ||
					nowReading.isLextant(Keyword.CLONE) ||
					nowReading.isLextant(Keyword.LENGTH) ||
					nowReading.isLextant(Keyword.REVERSE) ||
					nowReading.isLextant(Keyword.ZIP)) {
				if (nowReading.isLextant(Punctuator.NOT)) {
					Token notToken = nowReading;
					readToken();
					ParseNode child;
					//child = parseExpression();
					child = parseFunctionInvocationExpression();
					
					node = NotOperatorNode.withChildren(notToken, child);
				} else if (nowReading.isLextant(Keyword.CLONE)) {
					node = new CloneStatementNode(nowReading);
					expect(Keyword.CLONE);
					if (startsExpression(nowReading)) {
						ParseNode expression = parseExpression();
						//ParseNode expression = parseFunctionInvocationExpression();
						node.appendChild(expression);
					} else {
						return syntaxErrorNode("lengthStatement - missing expression");
					}
				} else if (nowReading.isLextant(Keyword.LENGTH)) {
					node = new LengthStatementNode(nowReading);
					readToken();
					if (startsExpression(nowReading)) {
						ParseNode expression = parseExpression();
						//ParseNode expression = parseFunctionInvocationExpression();
						node.appendChild(expression);
					} else {
						return syntaxErrorNode("lengthStatement - missing expression");
					}
				} else if (nowReading.isLextant(Keyword.REVERSE)) {
					node = new ReverseStatementNode(nowReading);
					readToken();
					if (startsExpression(nowReading)) {
						ParseNode expression = parseExpression();
						//ParseNode expression = parseFunctionInvocationExpression();
						node.appendChild(expression);
					} else {
						return syntaxErrorNode("reverseStatement - missing expression");
					}
				} else if (nowReading.isLextant(Keyword.ZIP)) {
					node = new ZipStatementNode(nowReading);
					readToken();
					if (startsExpression(nowReading)) {
						ParseNode expression1 = parseExpression();
						//ParseNode expression1 = parseFunctionInvocationExpression();
						node.appendChild(expression1);
						expect(Punctuator.SEPARATOR);
					} else {
						return syntaxErrorNode("reverseStatement - missing expression 1");
					}
					if (startsExpression(nowReading)) {
						ParseNode expression2 = parseExpression();
						//ParseNode expression2 = parseFunctionInvocationExpression();
						node.appendChild(expression2);
						expect(Punctuator.SEPARATOR);
					} else {
						return syntaxErrorNode("reverseStatement - missing expression 2");
					}
					if (startsExpression(nowReading)) {
						//ParseNode expression3 = parseExpression();
						ParseNode expression3 = parseFunctionInvocationExpression();
						node.appendChild(expression3);
					} else {
						return syntaxErrorNode("reverseStatement - missing expression 3");
					}
				} else {
					node = parseFunctionInvocationExpression();
				}
			}
		} else {
			node = parseFunctionInvocationExpression();
		}
		return node;
	}
	
	private boolean startsNotCloneReverseLengthZipLevelOperators(Token token) {
		return startsNotOperatorExpression(token);
	}
	
	private ParseNode parseNotOperatorExpression() {
		if(!startsNotOperatorExpression(nowReading)) {
			return syntaxErrorNode("comparison expression");
		}
		ParseNode node;
		
		if (nowReading.isLextant(Punctuator.NOT)) {
			Token notToken = nowReading;
			readToken();
			ParseNode child;
			child = parseCloneStatement();
			
			node = NotOperatorNode.withChildren(notToken, child);
		} else {
			node = parseCloneStatement();
		}
		
		return node;
	}
	private boolean startsNotOperatorExpression(Token token) {
		return startsCloneStatement(token) || token.isLextant(Punctuator.NOT);
	}
	
	private ParseNode parseCloneStatement() {
		if(!startsCloneStatement(nowReading)) {
			return syntaxErrorNode("lengthStatement");
		}
		ParseNode node;
		if (nowReading.isLextant(Keyword.CLONE)) {
			node = new CloneStatementNode(nowReading);
			expect(Keyword.CLONE);
			if (startsExpression(nowReading)) {
				ParseNode expression = parseExpression();
				node.appendChild(expression);
			} else {
				return syntaxErrorNode("lengthStatement - missing expression");
			}
		} else {
			node = parseLengthStatement();
		}

		return node;
	}
	private boolean startsCloneStatement(Token token) {
		return startsLengthStatement(token) ||token.isLextant(Keyword.CLONE);
	}
	
	private ParseNode parseLengthStatement() {
		if(!startsLengthStatement(nowReading)) {
			return syntaxErrorNode("lengthStatement");
		}
		ParseNode node;
		if (nowReading.isLextant(Keyword.LENGTH)) {
			node = new LengthStatementNode(nowReading);
			readToken();
			if (startsExpression(nowReading)) {
				ParseNode expression = parseExpression();
				node.appendChild(expression);
			} else {
				return syntaxErrorNode("lengthStatement - missing expression");
			}
		} else {
			node = parseReverseStatement();
		}

		return node;
	}
	private boolean startsLengthStatement(Token token) {
		return token.isLextant(Keyword.LENGTH) || startsReverseStatement(token);
	}
	
	private ParseNode parseReverseStatement() {
		if (!startsReverseStatement(nowReading)) {
			return syntaxErrorNode("reverseStatement");
		}
		ParseNode node;
		if (nowReading.isLextant(Keyword.REVERSE)) {
			node = new ReverseStatementNode(nowReading);
			readToken();
			if (startsExpression(nowReading)) {
				ParseNode expression = parseExpression();
				node.appendChild(expression);
			} else {
				return syntaxErrorNode("reverseStatement - missing expression");
			}
		} else {
			node = parseZipStatement();
		}

		return node;
	}
	private boolean startsReverseStatement(Token token) {
		return token.isLextant(Keyword.REVERSE) || startsZipStatement(token);
	}
	
	private ParseNode parseZipStatement() {
		if (!startsZipStatement(nowReading)) {
			return syntaxErrorNode("zipStatement");
		}
		ParseNode node;
		if (nowReading.isLextant(Keyword.ZIP)) {
			node = new ZipStatementNode(nowReading);
			readToken();
			if (startsExpression(nowReading)) {
				//ParseNode expression1 = parseExpression();
				ParseNode expression1 = parseFunctionInvocationExpression();
				node.appendChild(expression1);
				expect(Punctuator.SEPARATOR);
			} else {
				return syntaxErrorNode("reverseStatement - missing expression 1");
			}
			if (startsExpression(nowReading)) {
				//ParseNode expression2 = parseExpression();
				ParseNode expression2 = parseFunctionInvocationExpression();
				node.appendChild(expression2);
				expect(Punctuator.SEPARATOR);
			} else {
				return syntaxErrorNode("reverseStatement - missing expression 2");
			}
			if (startsExpression(nowReading)) {
				//ParseNode expression3 = parseExpression();
				ParseNode expression3 = parseFunctionInvocationExpression();
				node.appendChild(expression3);
			} else {
				return syntaxErrorNode("reverseStatement - missing expression 3");
			}
		} else {
			node = parseFunctionInvocationExpression();
		}

		return node;
	}
	private boolean startsZipStatement(Token token) {
		return token.isLextant(Keyword.ZIP) || startsFunctionInvocationExpression(token);
	}
	private ParseNode parseFunctionInvocationExpression() {
		if (!startsFunctionInvocationExpression(nowReading)) {
			return syntaxErrorNode("function invocation expression");
		}
		ParseNode expression = parseIndexingOperation();
		while(startsFunctionCall(nowReading)) {
			ParseNode functionInvocation = parseFunctionCall(expression);
			expression = functionInvocation;
		}
		return expression;
		
		/*if (startsFunctionCall(nowReading)) {
			ParseNode functionInvocation = parseFunctionCall(expression);
			return functionInvocation;
		} else {
			return expression;
		}*/
	}
	private boolean startsFunctionInvocationExpression (Token token) {
		return startsIndexingOperation(token);
	}
	
	private ParseNode parseIndexingOperation() {
		if(!startsLengthStatement(nowReading)) {
			return syntaxErrorNode("indexOperator");
		}
		ParseNode node = null;
		Token indexingToken = IndexOperatorToken.make(nowReading);
		Token substringToken = SubstringOperatorToken.make(nowReading);
		ParseNode expression = parseBracketStatement();
		if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
			node = new IndexOperatorNode(indexingToken);
			ParseNode previous = null;
			while (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
				if (previous != null) {
					node = new IndexOperatorNode(indexingToken);
					node.appendChild(previous);
				} else {
					node.appendChild(expression);
				}
				expect(Punctuator.OPEN_BRACKET);
				if (startsExpression(nowReading)) {
					ParseNode indexExpression = parseExpression();
					node.appendChild(indexExpression);
					if (nowReading.isLextant(Punctuator.SEPARATOR) && previous == null) {
						expect(Punctuator.SEPARATOR);
						ParseNode endIndexExpression = parseExpression();
						node = new SubstringStatementNode(substringToken);
						node.appendChild(expression);
						node.appendChild(indexExpression);
						node.appendChild(endIndexExpression);
						expect(Punctuator.CLOSE_BRACKET);
						return node;
					}
				} else {
					return syntaxErrorNode("indexOperator - missing index");
				}
				expect(Punctuator.CLOSE_BRACKET);
				previous = node;
			}
		} else {
			node = expression;
		}

		return node;
	}
	private boolean startsIndexingOperation(Token token) {
		return startsBracketStatement(token) || token instanceof IdentifierToken;
	}
	
	/*private ParseNode parseIndexingOperation(ParseNode identifier) {
		if(!startsIndexBracketOperator(nowReading)) {
			return syntaxErrorNode("indexOperator");
		}
		ParseNode node = null;
		Token indexingToken = IndexOperatorToken.make(nowReading);
		ParseNode expression = identifier;
		if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
			node = new IndexOperatorNode(indexingToken);
			ParseNode previous = null;
			while (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
				if (previous != null) {
					node = new IndexOperatorNode(indexingToken);
					node.appendChild(previous);
				} else {
					node.appendChild(expression);
				}
				expect(Punctuator.OPEN_BRACKET);
				if (startsExpression(nowReading)) {
					ParseNode index_expression = parseExpression();
					node.appendChild(index_expression);
				} else {
					return syntaxErrorNode("indexOperator - missing index");
				}
				expect(Punctuator.CLOSE_BRACKET);
				previous = node;
			}
		} else {
			node = expression;
		}

		return node;
	}
	private boolean startsIndexBracketOperator(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACKET);
	}*/
	
	private ParseNode parseFunctionCall(ParseNode expression) {
		if (!startsFunctionCall(nowReading)) {
			return syntaxErrorNode("functionInvocation");
		}
		Token functionToken = nowReading;
		ParseNode parameters = parseExpressionList();
		
		ParseNode functionCall = FunctionInvocationNode.make(functionToken, expression, parameters);
		
		return functionCall;
	}
	private boolean startsFunctionCall(Token token) {
		return token.isLextant(Punctuator.OPEN_PARENTHESES);
	}
	
	private ParseNode parseExpressionList() {
		ParseNode expressionListNode = new ExpressionListNode(nowReading);
		if (nowReading.isLextant(Punctuator.OPEN_PARENTHESES)) {
			readToken();
			while(!nowReading.isLextant(Punctuator.CLOSE_PARENTHESES)) {
				ParseNode expression = parseExpression();
				expressionListNode.appendChild(expression);
				if (!nowReading.isLextant(Punctuator.SEPARATOR) && !nowReading.isLextant(Punctuator.CLOSE_PARENTHESES)) {
					return syntaxErrorNode("expressionList");
				}
				if (!nowReading.isLextant(Punctuator.CLOSE_PARENTHESES)) {
					readToken();
				}
			}
			expect(Punctuator.CLOSE_PARENTHESES);
		}
		return expressionListNode;
	}
	private boolean startsExpressionList(Token token) {
		return startsExpression(token);
	}
	
	// atomicExpression -> literal
	private ParseNode parseAtomicExpression() {
		if(!startsAtomicExpression(nowReading)) {
			return syntaxErrorNode("atomic expression");
		}
		return parseLiteral();
	}
	private boolean startsAtomicExpression(Token token) {
		return startsLiteral(token);
	}
	
	// literal -> number | float |identifier | booleanConstant | string | character
	private ParseNode parseLiteral() {
		if(!startsLiteral(nowReading)) {
			return syntaxErrorNode("literal");
		}
		
		if(startsIntNumber(nowReading)) {
			return parseIntNumber();
		}
		if(startsFloatNumber(nowReading)) {
			return parseFloatNumber();
		}
		if(startsIdentifier(nowReading)) {
			return parseIdentifier();
		}
		if(startsBooleanConstant(nowReading)) {
			return parseBooleanConstant();
		}
		if(startsStringConstant(nowReading)) {
			return parseStringConstant();
		}
		if(startsCharacterConstant(nowReading)) {
			return parseCharacterConstant();
		}

		return syntaxErrorNode("literal");
	}
	private boolean startsLiteral(Token token) {
		return startsIntNumber(token) || 
				startsFloatNumber(token) || 
				startsIdentifier(token) || 
				startsBooleanConstant(token) ||
				startsStringConstant(token) ||
				startsCharacterConstant(token) ||
				startsTypeLiteral(token);
	}
	
	private ParseNode parseTypeLiteral() {		
		if(!startsTypeLiteral(nowReading)) {
			return syntaxErrorNode("Type Literal expression");
		}
		ParseNode node = null;
		if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
			node = new TypeLiteralNode(nowReading);
			readToken();
			ParseNode subtypeLiteral = parseTypeLiteral();
			((TypeLiteralNode)node).setSubNode((TypeLiteralNode)subtypeLiteral);
			expect(Punctuator.CLOSE_BRACKET);
		} else if (nowReading.isLextant(Keyword.BOOL) || 
				   nowReading.isLextant(Keyword.CHAR) || 
				   nowReading.isLextant(Keyword.FLOAT) || 
				   nowReading.isLextant(Keyword.INT) || 
				   nowReading.isLextant(Keyword.STRING) ||
				   nowReading.isLextant(Keyword.RAT) ||
				   nowReading.isLextant(Keyword.VOID)) {
			node = new TypeLiteralNode(nowReading);
			readToken();
		} else if (nowReading.isLextant(Punctuator.LESSER)) {
			node = new TypeLiteralNode(nowReading);
			expect(Punctuator.LESSER);
			ParseNode parameterTypes = null;
			if (startsTypeList(nowReading)) {
				parameterTypes = parseTypeList();
			} else {
				parameterTypes = new TypeListNode(nowReading);
			}
			expect(Punctuator.GREATER);
			expect(Punctuator.RIGHT_ARROW);
			ParseNode returnType = parseTypeLiteral();
			//ParseNode returnType = parseExpression();
			node.appendChild(parameterTypes);
			node.appendChild(returnType);
		} else {
			return syntaxErrorNode("Type Literal expression");
		}
		return node;
	}
	private boolean startsTypeLiteral(Token token) {
		if (token.isLextant(Keyword.BOOL) || 
				token.isLextant(Keyword.CHAR) || 
				token.isLextant(Keyword.FLOAT) || 
				token.isLextant(Keyword.INT) || 
				token.isLextant(Keyword.STRING) ||
				token.isLextant(Keyword.RAT) ||
				token.isLextant(Keyword.VOID) ||
				token.isLextant(Punctuator.OPEN_BRACKET) ||
				token.isLextant(Punctuator.LESSER)) {
			return true;
		} else {
			return false;
		}
	}
	
	
	private ParseNode parseArrayExpressionList(Token start, ParseNode firstElement) {
		if((!startsArrayExpressionList(nowReading) && start == null) || !startsArrayExpressionList(nowReading) && !startsArrayExpressionList(start)) {
			return syntaxErrorNode("array expression");
		}
		ParseNode arrayExpressionNode = new ArrayExpressionNode(start);
		if (start != null && start.isLextant(Punctuator.OPEN_BRACKET)) {
			arrayExpressionNode.appendChild(firstElement);
			if (!nowReading.isLextant(Punctuator.CLOSE_BRACKET)) {
				expect(Punctuator.SEPARATOR);
			}
			while(!nowReading.isLextant(Punctuator.CLOSE_BRACKET)) {
				ParseNode expression = parseExpression();
				arrayExpressionNode.appendChild(expression);
				if (!nowReading.isLextant(Punctuator.SEPARATOR) && !nowReading.isLextant(Punctuator.CLOSE_BRACKET)) {
					return syntaxErrorNode("array expression");
				}
				if (!nowReading.isLextant(Punctuator.CLOSE_BRACKET)) {
					readToken();
				}
			}
			expect(Punctuator.CLOSE_BRACKET);
		}
		
		return arrayExpressionNode;
	}
	private boolean startsArrayExpressionList(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACKET);
	}
	
	
	private ParseNode parseNewArrayExpression() {
		if(!startsNewArrayExpression(nowReading)) {
			return syntaxErrorNode("new array expression");
		}
		
		if (nowReading.isLextant(Keyword.NEW)) {
			ParseNode arrayExpressionNode = new ArrayExpressionNode(nowReading);
			expect(Keyword.NEW);
			ParseNode arrayType = null;
			ParseNode arrayLength = null;
			if (startsTypeLiteral(nowReading)) {
				arrayType = parseTypeLiteral();
			} else {
				return syntaxErrorNode("new array expression - missing type literal");
			}
			expect(Punctuator.OPEN_PARENTHESES);
			if (startsExpression(nowReading)) {
				arrayLength = parseExpression();
			} else {
				return syntaxErrorNode("new array expression - missing length expression");
			}
			expect(Punctuator.CLOSE_PARENTHESES);
			arrayExpressionNode.appendChild(arrayType);
			arrayExpressionNode.appendChild(arrayLength);
			return arrayExpressionNode;
		} else {
			return parseParenthesesExpression();
		}
	}
	private boolean startsNewArrayExpression(Token token) {
		return startsParenthesesExpression(token) || token.isLextant(Keyword.NEW);
	}

	// number (terminal)
	private ParseNode parseIntNumber() {
		if(!startsIntNumber(nowReading)) {
			return syntaxErrorNode("integer constant");
		}
		readToken();
		return new IntegerConstantNode(previouslyRead);
	}
	private boolean startsIntNumber(Token token) {
		return token instanceof NumberToken;
	}
	
	// float (terminal)
	private ParseNode parseFloatNumber() {
		if(!startsFloatNumber(nowReading)) {
			return syntaxErrorNode("floating constant");
		}
		readToken();
		return new FloatingConstantNode(previouslyRead);
	}
	private boolean startsFloatNumber(Token token) {
		return token instanceof FloatingToken;
	}

	// identifier (terminal)
	private ParseNode parseIdentifier() {
		if(!startsIdentifier(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		readToken();
		ParseNode identifier = new IdentifierNode(previouslyRead);
		return identifier;
	}
	private boolean startsIdentifier(Token token) {
		return token instanceof IdentifierToken;
	}

	// boolean constant (terminal)
	private ParseNode parseBooleanConstant() {
		if(!startsBooleanConstant(nowReading)) {
			return syntaxErrorNode("boolean constant");
		}
		readToken();
		return new BooleanConstantNode(previouslyRead);
	}
	private boolean startsBooleanConstant(Token token) {
		return token.isLextant(Keyword.TRUE, Keyword.FALSE);
	}
	
	private ParseNode parseStringConstant() {
		if(!startsStringConstant(nowReading)) {
			return syntaxErrorNode("string constant");
		}
		readToken();
		return new StringConstantNode(previouslyRead);
	}
	private boolean startsStringConstant(Token token) {
		return token instanceof StringToken;
	}
	
	private ParseNode parseCharacterConstant() {
		if(!startsCharacterConstant(nowReading)) {
			return syntaxErrorNode("character constant");
		}
		readToken();
		return new CharacterConstantNode(previouslyRead);
	}
	private boolean startsCharacterConstant(Token token) {
		return token instanceof CharacterToken;
	}

	private void readToken() {
		previouslyRead = nowReading;
		nowReading = scanner.next();
	}	
	
	// if the current token is one of the given lextants, read the next token.
	// otherwise, give a syntax error and read next token (to avoid endless looping).
	private void expect(Lextant ...lextants ) {
		if(!nowReading.isLextant(lextants)) {
			syntaxError(nowReading, "expecting " + Arrays.toString(lextants));
		}
		readToken();
	}	
	private ErrorNode syntaxErrorNode(String expectedSymbol) {
		syntaxError(nowReading, "expecting " + expectedSymbol);
		ErrorNode errorNode = new ErrorNode(nowReading);
		readToken();
		return errorNode;
	}
	private void syntaxError(Token token, String errorDescription) {
		String message = "" + token.getLocation() + " " + errorDescription;
		error(message);
	}
	private void error(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.Parser");
		log.severe("syntax error: " + message);
	}	
}

