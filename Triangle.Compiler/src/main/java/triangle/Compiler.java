/*
 * @(#)Compiler.java                       
 * 
 * Revisions and updates (c) 2022-2023 Sandy Brownlee. alexander.brownlee@stir.ac.uk
 * 
 * Original release:
 *
 * Copyright (C) 1999, 2003 D.A. Watt and D.F. Brown
 * Dept. of Computing Science, University of Glasgow, Glasgow G12 8QQ Scotland
 * and School of Computer and Math Sciences, The Robert Gordon University,
 * St. Andrew Street, Aberdeen AB25 1HG, Scotland.
 * All rights reserved.
 *
 * This software is provided free for educational use only. It may
 * not be used for commercial purposes without the prior written permission
 * of the authors.
 */

package triangle;

import triangle.abstractSyntaxTrees.Program;
import triangle.codeGenerator.Emitter;
import triangle.codeGenerator.Encoder;
import triangle.contextualAnalyzer.Checker;
import triangle.optimiser.ConstantFolder;
import triangle.syntacticAnalyzer.Parser;
import triangle.syntacticAnalyzer.Scanner;
import triangle.syntacticAnalyzer.SourceFile;
import triangle.treeDrawer.Drawer;

// Task 2
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

/**
 * The main driver class for the Triangle compiler.
 *
 * @version 2.1 7 Oct 2003
 * @author Deryck F. Brown
 */
public class Compiler {

	/** The filename for the object program, normally obj.tam. */
	//static String objectNIame = "obj.tam";
	//static boolean showTree = false;
	//static boolean folding = false; //Better implementation below

	private static Scanner scanner;
	private static Parser parser;
	private static Checker checker;
	private static Encoder encoder;
	private static Emitter emitter;
	private static ErrorReporter reporter;
	private static Drawer drawer;

	/** The AST representing the source program. */
	private static Program theAST;
	
	//Task 2
	
	@Argument(alias = "file", description="Name of file you want to compile", required = true)
	static String sourceName = "";
	
	@Argument(alias = "obj", description="Object file name", required = true)
	static String objectName = "obj.tam"; //default
		
	@Argument(alias = "tree", description="Enable AST")
	static boolean showTree = false;
		
	@Argument(alias = "fold", description="Enable folding")
	static boolean folding = false;
		
	@Argument(alias = "tfold", description="Enable AST after folding")
	static boolean showTreeAfterFolding = false;
	
	//Task 5b
	@Argument(alias = "stats", description="Enable stats (Char and Int Expr count)")
	static boolean showStats = false;

	/**
	 * Compile the source program to TAM machine code.
	 *
	 * @param sourceName   		the name of the file containing the source program.
	 * @param objectName   		the name of the file containing the object program.
	 * @param showingAST   		true iff the AST is to be displayed after contextual
	 *                     		analysis
	 * @param showingTable 		true iff the object description details are to be
	 *                     		displayed during code generation (not currently
	 *                     		implemented).
	 * @param showAfterFolding  show the AST after folding is complete
	 * @param showStats 		show stats, these only (so far) inlcude char and int expressions
	 * @return true iff the source program is free of compile-time errors, otherwise
	 *         false.
	 */
	static boolean compileProgram(String sourceName, String objectName, boolean showingAST, boolean showingTable, boolean showAfterFolding, boolean showStats) {

		System.out.println("********** " + "Triangle Compiler (Java Version 2.1)" + " **********");

		System.out.println("Syntactic Analysis ...");
		SourceFile source = SourceFile.ofPath(sourceName);

		if (source == null) {
			System.out.println("Can't access source file " + sourceName);
			System.exit(1);
		}

		scanner = new Scanner(source);
		reporter = new ErrorReporter(false);
		parser = new Parser(scanner, reporter);
		checker = new Checker(reporter);
		emitter = new Emitter(reporter);
		encoder = new Encoder(emitter, reporter);
		drawer = new Drawer();
		
		//Task 5b
		stats = new SummaryStats();

		theAST = parser.parseProgram(); // 1st pass
		if (reporter.getNumErrors() == 0) {
			System.out.println("Contextual Analysis ...");
			checker.check(theAST); // 2nd pass
			
			if (showingAST && !showAfterFolding) {
				drawer.draw(theAST);
			}
			if (folding) {
				theAST.visit(new ConstantFolder());
				if (showingAST && showAfterFolding) {
					drawer.draw(theAST); //if folding then also show tree
				}
			}

			
			if (reporter.getNumErrors() == 0) {
				System.out.println("Code Generation ...");
				encoder.encodeRun(theAST, showingTable); // 3rd pass
			}
		}

		boolean successful = (reporter.getNumErrors() == 0);
		if (successful) {
			emitter.saveObjectProgram(objectName);
			System.out.println("Compilation was successful.");
			//Task 5b
			System.out.println("[STATS] CharExpr: " + stats.getCharExprCount() + "!");
			System.out.println("[STATS] IntExpr: " + stats.getIntExprCount() + "!");
		} else {
			System.out.println("Compilation was unsuccessful.");
		}
		return successful;
	}

	/**
	 * Triangle compiler main program.
	 *
	 * @param args the only command-line argument to the program specifies the
	 *             source filename.
	 */
	public static void main(String[] args) {

		//Task 2
		Args.parseOrExit(Compiler.class, args);
		var compiledOK = compileProgram(Compiler.sourceName, Compiler.objectName, Compiler.showTree, false, Compiler.showTreeAfterFolding);

		if (!showTree) {
			System.exit(compiledOK ? 0 : 1);
		}
	}
	
	/* No longer needed */
	private static void parseArgs(String[] args) {
		for (String s : args) {
			var sl = s.toLowerCase();
			if (sl.equals("tree")) {
				showTree = true;
			} else if (sl.startsWith("-o=")) {
				objectName = s.substring(3);
			} else if (sl.equals("folding")) {
				folding = true;
			}
		}
	}
}
