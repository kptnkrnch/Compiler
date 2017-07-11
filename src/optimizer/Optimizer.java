package optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;

import asmCodeGenerator.codeStorage.ASMCodeChunk;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMInstruction;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.RunTime;

public class Optimizer {
	private ASMCodeFragment fragment;
	
	public static ASMCodeFragment optimize(ASMCodeFragment fragment) {
		Optimizer optimizer = new Optimizer(fragment);
		return optimizer.optimize();
	}
	public Optimizer(ASMCodeFragment fragment) {
		this.fragment = fragment;
	}
	
	public ASMCodeFragment optimize() {
		LinkedList<ASMInstruction> code = getIntructionListFromFragment(fragment);
		LinkedList<ASMInstruction> dataLoadingCode = getDataLoaderCode(code);
		LinkedList<ASMInstruction> runTimeCode = getRunTimeInstructionsCode(code);
		dataLoadingCode = optimizeDataLoading(dataLoadingCode, runTimeCode);
		runTimeCode = optimizeRunTimeInstructions(runTimeCode);
		runTimeCode = divideIntoBasicBlocks(runTimeCode);
		runTimeCode = optimizeRunTimeInstructions(runTimeCode);
		dataLoadingCode = removeUnusedDataLoadingInstructions(dataLoadingCode, runTimeCode);
		ASMCodeFragment dataLoadingFragment = instructionListToCodeFragment(dataLoadingCode, fragment);
		ASMCodeFragment runTimeFragment = instructionListToCodeFragment(runTimeCode, fragment);
		dataLoadingFragment.append(runTimeFragment);
		return dataLoadingFragment;
	}
	
	private LinkedList<ASMInstruction> getIntructionListFromFragment(ASMCodeFragment frag) {
		LinkedList<ASMInstruction> code = new LinkedList<ASMInstruction>();
		for (ASMCodeChunk chunk : frag.getCodeChunks()) {
			for (ASMInstruction instruction : chunk.getInstructions()) {
				code.add(instruction);
			}
		}
		return code;
	}
	
	private ASMCodeFragment instructionListToCodeFragment(LinkedList<ASMInstruction> newCode, ASMCodeFragment oldFragment) {
		ASMCodeFragment frag = new ASMCodeFragment(oldFragment.getCodeType());
		for (ASMInstruction instruction : newCode) {
			frag.add(instruction);
		}
		return frag;
	}
	
	private LinkedList<ASMInstruction> optimizeDataLoading(LinkedList<ASMInstruction> loadTimeInstructions,
														   LinkedList<ASMInstruction> runTimeInstructions) {
		Hashtable<String, String> stringConstants = new Hashtable<String, String>(); // <string value, DLabel stringName>
		Hashtable<String, String> stringReplacements = new Hashtable<String, String>(); // <DLabel StringName, DLabel replacement>
		for (int i = 0; i < loadTimeInstructions.size(); i++) {
			ASMInstruction instruction = loadTimeInstructions.get(i);
			if (instruction.getOpcode() == ASMOpcode.DLabel && ((String)(instruction.getArgument())).contains("stringConstant")) {
				int k = i + 4;
				String value = "";
				ASMInstruction string = loadTimeInstructions.get(k);
				if (string.getOpcode() == ASMOpcode.DataS) {
					value = (String)string.getArgument();
					if (value != null && value.length() > 0 && value.charAt(0) == '"' && value.length() > 1) {
						value = value.substring(1, value.length());
						string.setArgument(value);
					}
					if (value != null && value.length() > 0 && value.charAt(value.length() - 1) == '"' && value.length() > 1) {
						value = value.substring(0, value.length() - 1);
						string.setArgument(value);
					}
					if (value != null && stringConstants.get(value) != null) {
						stringReplacements.put(((String)(instruction.getArgument())), stringConstants.get(value));
						for (int n = 0; n < k - i + 1; n++) {
							loadTimeInstructions.remove(i);
						}
					} else {
						stringConstants.put(value, ((String)(instruction.getArgument())));
					}
				} else if (string.getOpcode() == ASMOpcode.DataC && ((Integer)(string.getArgument())) != 0) {
					do {
						Integer ival = (Integer)(string.getArgument());
						value += (char)ival.intValue();
						k += 1;
						string = loadTimeInstructions.get(k);
					} while(string.getOpcode() == ASMOpcode.DataC && ((Integer)(string.getArgument())) != 0);
					
					if (stringConstants.get(value) != null) {
						stringReplacements.put(((String)(instruction.getArgument())), stringConstants.get(value));
						for (int n = 0; n < k - i + 1; n++) {
							loadTimeInstructions.remove(i);
						}
					} else {
						stringConstants.put(value, ((String)(instruction.getArgument())));
					}
				}
			}
		}
		for (int i = 0; i < runTimeInstructions.size(); i++) {
			ASMInstruction instruction = runTimeInstructions.get(i);
			if (instruction.getOpcode() == ASMOpcode.PushD) {
				String argument = (String)(instruction.getArgument());
				String replacement = null;
				if ((replacement = stringReplacements.get(argument)) != null) {
					instruction.setArgument(replacement);
				}
			}
		}
		return loadTimeInstructions;
	}
	
	private LinkedList<ASMInstruction> removeUnusedDataLoadingInstructions(LinkedList<ASMInstruction> loadTimeInstructions,
			   LinkedList<ASMInstruction> runTimeInstructions) {
		boolean madeAChange = false;
		do {
			madeAChange = false;
			LinkedList<ASMInstruction> usedInstructions = new LinkedList<ASMInstruction>();
			for (ASMInstruction instruction : runTimeInstructions) {
				if (isPushD(instruction) && instruction.getArgument() != null) {
					for (ASMInstruction loadInstruction : loadTimeInstructions) {
						if (isDLabel(loadInstruction) && 
								loadInstruction.getArgument() != null &&
								loadInstruction.getArgument().equals(instruction.getArgument())) {
							usedInstructions.add(loadInstruction);
						}
					}
				}
			}
			for (int i = 0; i < loadTimeInstructions.size(); i++) {
				ASMInstruction loadInstruction = loadTimeInstructions.get(i);
				if (isDLabel(loadInstruction) && !usedInstructions.contains(loadInstruction)) {
					madeAChange = true;
					loadTimeInstructions.remove(i);
					loadInstruction = loadTimeInstructions.get(i);
					while (isDataInstruction(loadInstruction)) {
						loadTimeInstructions.remove(i);
						loadInstruction = loadTimeInstructions.get(i);
					}
					i--;
				}
			}
		} while (madeAChange);
		return loadTimeInstructions;
	}
	
	private LinkedList<ASMInstruction> optimizeRunTimeInstructions(LinkedList<ASMInstruction> instructions) {
		boolean wasChange = false;
		do {
			wasChange = false;
			for (int i = 0; i < instructions.size(); i++) {
				ASMInstruction instruction = instructions.get(i);
				if (isArithmeticOperation(instruction)) {
					if (i >= 2 && instructionsAreConstants(instructions.get(i-2), instructions.get(i-1))) {
						LinkedList<ASMInstruction> optimal = optimizeArithmeticInstructions(instruction, instructions.get(i-2), instructions.get(i-1), null);
						instructions.remove(i-2);
						instructions.remove(i-2);
						instructions.remove(i-2);
						instructions.addAll(i-2, optimal);
						wasChange = true;
					} else if (i >= 4 && instructionsAreDivisionStatement(instructions.get(i-4), instructions.get(i-3), instructions.get(i-2), instructions.get(i-1))) {
						LinkedList<ASMInstruction> optimal = optimizeArithmeticInstructions(instruction, instructions.get(i-4), instructions.get(i-3), (String)(instructions.get(i-1).getArgument()));
						instructions.remove(i-4);
						instructions.remove(i-4);
						instructions.remove(i-4);
						instructions.remove(i-4);
						instructions.remove(i-4);
						instructions.addAll(i-4, optimal);
						wasChange = true;
					}
				} else if (isCastOperation(instruction)) {
					if (i >= 1 && instructionIsConstant(instructions.get(i-1))) {
						LinkedList<ASMInstruction> optimal = optimizeCastingInstructions(instruction, instructions.get(i-1));
						instructions.remove(i-1);
						instructions.remove(i-1);
						instructions.addAll(i-1, optimal);
						wasChange = true;
					}
				} else if (i >= 1 && isNegateOperation(instruction)) {
					if (instructionIsConstant(instructions.get(i-1))) {
						LinkedList<ASMInstruction> optimal = optimizeNegateInstructions(instruction, instructions.get(i-1));
						instructions.remove(i-1);
						instructions.remove(i-1);
						instructions.addAll(i-1, optimal);
						wasChange = true;
					}
				}
			}
		} while (wasChange);
		return instructions;
	}
	
	private LinkedList<ASMInstruction> getDataLoaderCode(LinkedList<ASMInstruction> instructions) {
		LinkedList<ASMInstruction> code = new LinkedList<ASMInstruction>();
		for (int i = 0; i < instructions.size(); i++) {
			ASMInstruction instruction = instructions.get(i);
			switch(instruction.getOpcode()) {
			case DLabel:
			case DataC:
			case DataI:
			case DataF:
			case DataS:
			case DataZ:
			case DataD:
				code.add(instruction);
				break;
			default:
				break;
			}
		}
		return code;
	}
	
	private LinkedList<ASMInstruction> getRunTimeInstructionsCode(LinkedList<ASMInstruction> instructions) {
		LinkedList<ASMInstruction> code = new LinkedList<ASMInstruction>();
		for (int i = 0; i < instructions.size(); i++) {
			ASMInstruction instruction = instructions.get(i);
			switch(instruction.getOpcode()) {
			case DLabel:
			case DataC:
			case DataI:
			case DataF:
			case DataS:
			case DataZ:
			case DataD:
				break;
			default:
				code.add(instruction);
				break;
			}
		}
		return code;
	}
	
	private LinkedList<ASMInstruction> optimizeArithmeticInstructions(ASMInstruction operator, 
																	  ASMInstruction instruction1, 
																	  ASMInstruction instruction2,
																	  String divideByZeroLocation) {
		LinkedList<ASMInstruction> code = new LinkedList<ASMInstruction>();
		Integer i1, i2, iresult;
		Double f1, f2, fresult;
		
		ASMInstruction nInstruction;
		
		if (isArithmeticOperation(operator)) {
			switch(operator.getOpcode()) {
			case Add:
				i1 = (Integer)(instruction1.getArgument());
				i2 = (Integer)(instruction2.getArgument());
				iresult = i1 + i2;
				nInstruction = new ASMInstruction(ASMOpcode.PushI, iresult);
				code.add(nInstruction);
				break;
			case Subtract:
				i1 = (Integer)(instruction1.getArgument());
				i2 = (Integer)(instruction2.getArgument());
				iresult = i1 - i2;
				nInstruction = new ASMInstruction(ASMOpcode.PushI, iresult);
				code.add(nInstruction);
				break;
			case Multiply:
				i1 = (Integer)(instruction1.getArgument());
				i2 = (Integer)(instruction2.getArgument());
				iresult = i1 * i2;
				nInstruction = new ASMInstruction(ASMOpcode.PushI, iresult);
				code.add(nInstruction);
				break;
			case Divide:
				i1 = (Integer)(instruction1.getArgument());
				i2 = (Integer)(instruction2.getArgument());
				if (i2 != 0) {
					iresult = i1 / i2;
					nInstruction = new ASMInstruction(ASMOpcode.PushI, iresult);
				} else {
					if (divideByZeroLocation == null) {
						nInstruction = new ASMInstruction(ASMOpcode.Jump, RunTime.INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR);
					} else {
						nInstruction = new ASMInstruction(ASMOpcode.Jump, divideByZeroLocation);
					}
				}
				code.add(nInstruction);
				break;
			case FAdd:
				f1 = (Double)(instruction1.getArgument());
				f2 = (Double)(instruction2.getArgument());
				fresult = f1 + f2;
				nInstruction = new ASMInstruction(ASMOpcode.PushF, fresult);
				code.add(nInstruction);
				break;
			case FSubtract:
				f1 = (Double)(instruction1.getArgument());
				f2 = (Double)(instruction2.getArgument());
				fresult = f1 - f2;
				nInstruction = new ASMInstruction(ASMOpcode.PushF, fresult);
				code.add(nInstruction);
				break;
			case FMultiply:
				f1 = (Double)(instruction1.getArgument());
				f2 = (Double)(instruction2.getArgument());
				fresult = f1 * f2;
				nInstruction = new ASMInstruction(ASMOpcode.PushF, fresult);
				code.add(nInstruction);
				break;
			case FDivide:
				f1 = (Double)(instruction1.getArgument());
				f2 = (Double)(instruction2.getArgument());
				if (f2 != 0) {
					fresult = f1 / f2;
					nInstruction = new ASMInstruction(ASMOpcode.PushF, fresult);
				} else {
					if (divideByZeroLocation == null) {
						nInstruction = new ASMInstruction(ASMOpcode.Jump, RunTime.FLOAT_DIVIDE_BY_ZERO_RUNTIME_ERROR);
					} else {
						nInstruction = new ASMInstruction(ASMOpcode.Jump, divideByZeroLocation);
					}
				}
				code.add(nInstruction);
				break;
			default:
				break;
			}
		}
		
		return code;
	}
	
	private LinkedList<ASMInstruction> optimizeCastingInstructions(ASMInstruction operation, ASMInstruction instruction) {
		LinkedList<ASMInstruction> code = new LinkedList<ASMInstruction>();
		Integer iresult;
		Double fresult;
		ASMInstruction nInstruction;
		switch (operation.getOpcode()) {
		case ConvertI:
			iresult = ((Double)(instruction.getArgument())).intValue();
			nInstruction = new ASMInstruction(ASMOpcode.PushI, iresult);
			code.add(nInstruction);
			break;
		case ConvertF:
			fresult = ((Integer)(instruction.getArgument())).doubleValue();
			nInstruction = new ASMInstruction(ASMOpcode.PushF, fresult);
			code.add(nInstruction);
			break;
		default:
			break;
		}
		return code;
	}
	
	private LinkedList<ASMInstruction> optimizeNegateInstructions(ASMInstruction operation, ASMInstruction instruction) {
		LinkedList<ASMInstruction> code = new LinkedList<ASMInstruction>();
		Integer iresult;
		Double fresult;
		ASMInstruction nInstruction;
		switch (operation.getOpcode()) {
		case Negate:
			iresult = (-1)*((Integer)(instruction.getArgument()));
			nInstruction = new ASMInstruction(ASMOpcode.PushI, iresult);
			code.add(nInstruction);
			break;
		case FNegate:
			fresult = (-1)*((Double)(instruction.getArgument()));
			nInstruction = new ASMInstruction(ASMOpcode.PushF, fresult);
			code.add(nInstruction);
			break;
		default:
			break;
		}
		return code;
	}
	
	private boolean instructionsAreConstants(ASMInstruction instruction1, ASMInstruction instruction2) {
		if (instruction1.getOpcode() == ASMOpcode.PushI && instruction2.getOpcode() == ASMOpcode.PushI) {
			return true;
		} else if (instruction1.getOpcode() == ASMOpcode.PushF && instruction2.getOpcode() == ASMOpcode.PushF) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean instructionsAreDivisionStatement(ASMInstruction instruction1, 
													 ASMInstruction instruction2,
													 ASMInstruction instruction3,
													 ASMInstruction instruction4) {
		if (instruction1.getOpcode() == ASMOpcode.PushI && instruction2.getOpcode() == ASMOpcode.PushI
				&& instruction3.getOpcode() == ASMOpcode.Duplicate && instruction4.getOpcode() == ASMOpcode.JumpFalse) {
			return true;
		} else if (instruction1.getOpcode() == ASMOpcode.PushF && instruction2.getOpcode() == ASMOpcode.PushF
				&& instruction3.getOpcode() == ASMOpcode.Duplicate && instruction4.getOpcode() == ASMOpcode.JumpFZero){
			return true;
		} else {
			return false;
		}
	}
	
	private LinkedList<ASMInstruction> divideIntoBasicBlocks(LinkedList<ASMInstruction> instructions) {
		
		ArrayList<BasicBlock> basicBlocks = new ArrayList<BasicBlock>();
		for (int i = 0; i < instructions.size(); i++) {
			ASMInstruction currentInstruction = instructions.get(i);
			//if (isBlockStart(currentInstruction)) {
			int blockStart = i;
			int blockEnd = i;
			for (int n = i; n < instructions.size(); n++) {
				currentInstruction = instructions.get(n);
				if (isJump(currentInstruction) || isBranch(currentInstruction) || isCall(currentInstruction)) {
					int location = findJumpEndPoint(currentInstruction.getArgument(), instructions);
					int end = findBlockEndPoint(location, instructions);
					if (end != -1) {
						BasicBlock block = new BasicBlock(location, end);
						basicBlocks.add(block);
					}
				}
				if (isBlockEnd(currentInstruction)) {
					if (isBranch(currentInstruction)) {
						currentInstruction = instructions.get(n+1);
						if (isJump(currentInstruction)) {
							n = n + 1;
						}
					}
					if (isCall(currentInstruction)) {
						System.out.print("");
					}
					blockEnd = n;
					break;
				}
			}
			i = blockEnd;
			BasicBlock block = new BasicBlock(blockStart, blockEnd);
			basicBlocks.add(block);
		}
		basicBlocks = removeOverlappingBasicBlocks(basicBlocks);
		ControlFlowGraph cfg = convertBasicBlocksToControlFlowGraph(basicBlocks, instructions);
		//markFunctionsByPushDInstructions(cfg);
		//cfg = relabelBlocks(cfg);
		//tempCFG5 = removeOrSimplifyBranches(cfg, instructions);
		//if (tempCFG5 != null) {
			//cfg = tempCFG5;
			//instructions = convertControlFlowGraphToInstructionSet(cfg);
		//} else {
			//tempCFG1 = removeUnreachableBlocksFromControlFlowGraph(cfg, instructions);
			//if (tempCFG1 != null) {
			//	cfg = tempCFG1;
			//	instructions = convertControlFlowGraphToInstructionSet(cfg);
			//}
			//cfg = relabelBlocks(cfg);
			
			/*tempCFG3 = simplifyConditionalBlocks(cfg, instructions);
			if (tempCFG3 != null) {
				cfg = tempCFG3;
			}
			cfg = relabelBlocks(cfg);*/
		//generateDominanceLists(cfg);
		ControlFlowGraph tempCFG1 = null;
		ControlFlowGraph tempCFG2 = null;
		ControlFlowGraph conditionalCFG = null;
		ControlFlowGraph unreachableCFG = null;
		ControlFlowGraph simplifyBranchCFG = null;
		ControlFlowGraph mergeCFG = null;
		ControlFlowGraph removeUnusedBlocks = null;
		do {
			markFunctionsByPushDInstructions(cfg);
			mergeCFG = mergeNeighbourBlocksInControlFlowGraph(cfg, instructions);
			if (mergeCFG != null) {
				cfg = mergeCFG;
			}
			simplifyBranchCFG = removeOrSimplifyBranches(cfg, instructions);
			if (simplifyBranchCFG != null) {
				cfg = simplifyBranchCFG;
			}
			unreachableCFG = removeUnreachableBlocksFromControlFlowGraph(cfg, instructions);
			if (unreachableCFG != null) {
				cfg = unreachableCFG;
			}
			conditionalCFG = simplifyConditionalBlocks(cfg, instructions);
			if (conditionalCFG != null) {
				cfg = conditionalCFG;
			}
			removeUnusedBlocks = removeUnusedInstructions(cfg);
			if (removeUnusedBlocks != null) {
				cfg = removeUnusedBlocks;
			}
			
			cfg = relabelBlocks(cfg);
			//System.out.print(cfg.toString(instructions));
		} while (mergeCFG != null || simplifyBranchCFG != null || unreachableCFG != null || conditionalCFG != null);
		
		cfg = relabelBlocks(cfg);
		findLoopHeaders(cfg);
		
		instructions = convertControlFlowGraphToInstructionSet(cfg);
			//break;
		//System.out.print(cfg.toString());
		//}
		//} while (tempCFG1 != null || tempCFG2 != null || tempCFG3 != null || tempCFG4 != null || tempCFG5 != null);
		
		return instructions;
	}
	
	private ArrayList<BasicBlock> removeOverlappingBasicBlocks(ArrayList<BasicBlock> blocks) {
		Collections.sort(blocks);
		for (int i = 0; i < blocks.size(); i++) {
			BasicBlock current = blocks.get(i);
			BasicBlock next = null;
			if (i + 1 < blocks.size()) {
				next = blocks.get(i + 1);
				if (current.getStartIndex() == next.getStartIndex() && 
						current.getEndIndex() == next.getEndIndex()) {
					blocks.remove(i + 1);
					i -= 1;
				} else if (current.getEndIndex() > next.getStartIndex()) {
					current.setEndIndex(next.getStartIndex() - 1);
					blocks.set(i, current);
				}
			}
		}
		/*for (BasicBlock block : blocks) {
			System.out.println("[" + block.getStartIndex() + ", " + block.getEndIndex() + "]");
		}*/
		return blocks;
	}
	
	private ControlFlowGraph convertBasicBlocksToControlFlowGraph(ArrayList<BasicBlock> blocks, 
																	   LinkedList<ASMInstruction> instructions) {
		ControlFlowGraph graph = new ControlFlowGraph();
		int i = 1;
		for (BasicBlock block : blocks) {
			BasicBlockNode node = new BasicBlockNode(block, "BB" + i);
			for (int n = block.getStartIndex(); n >= 0 && n <= block.getEndIndex(); n++) {
				ASMInstruction instruction = instructions.get(n);
				node.addInstruction(instruction);
			}
			graph.addNode(node);
			i++;
		}
		
		for (BasicBlock block : blocks) {
			int endIndex = block.getEndIndex();
			int startIndex = block.getStartIndex();
			
			if (endIndex >= 0 && endIndex < instructions.size() && startIndex >= 0 && startIndex < instructions.size()) {
				BasicBlockNode currentNode = graph.getBlockNodeByStartIndex(startIndex);
				ASMInstruction endInstruction = instructions.get(endIndex);
				if (isJump(endInstruction)) {
					if (endIndex > 0) {
						ASMInstruction prevInstruction = instructions.get(endIndex - 1);
						if (isBranch(prevInstruction)) {
							int endLocation = findJumpEndPoint(prevInstruction.getArgument(), instructions);
							BasicBlockNode outNode = graph.getBlockNodeByStartIndex(endLocation);
							if (outNode != null) {
								ASMInstruction jumpEndPoint = instructions.get(endLocation);
								jumpEndPoint = outNode.getInstruction(jumpEndPoint);
								prevInstruction = currentNode.getInstruction(prevInstruction);
								BasicBlockNodeConnector connector = new BasicBlockNodeConnector(currentNode, outNode, prevInstruction, jumpEndPoint, instructions);
								outNode.addInNode(currentNode);
								//outNode.removeInstruction(jumpEndPoint);
								currentNode.addOutNode(outNode);
								//currentNode.removeInstruction(endInstruction);
								outNode.addInConnector(connector);
								currentNode.addOutConnector(connector);
							}
						}
					}
					int endLocation = findJumpEndPoint(endInstruction.getArgument(), instructions);
					BasicBlockNode outNode = graph.getBlockNodeByInstructionIndex(endLocation);
					if (outNode != null && endLocation >= 0) {
						ASMInstruction jumpEndPoint = instructions.get(endLocation);
						jumpEndPoint = outNode.getInstruction(jumpEndPoint);
						endInstruction = currentNode.getInstruction(endInstruction);
						BasicBlockNodeConnector connector = new BasicBlockNodeConnector(currentNode, outNode, endInstruction, jumpEndPoint, instructions);
						outNode.addInNode(currentNode);
						//outNode.removeInstruction(jumpEndPoint);
						currentNode.addOutNode(outNode);
						//currentNode.removeInstruction(endInstruction);
						outNode.addInConnector(connector);
						currentNode.addOutConnector(connector);
					}
				} else if (isBranch(endInstruction)) {
					int endLocation = findJumpEndPoint(endInstruction.getArgument(), instructions);
					BasicBlockNode outNode = graph.getBlockNodeByStartIndex(endLocation);
					ASMInstruction jumpEndPoint = instructions.get(endLocation);
					jumpEndPoint = outNode.getInstruction(jumpEndPoint);
					endInstruction = currentNode.getInstruction(endInstruction);
					BasicBlockNodeConnector connector = new BasicBlockNodeConnector(currentNode, outNode, endInstruction, jumpEndPoint, instructions);
					outNode.addInNode(currentNode);
					//outNode.removeInstruction(jumpEndPoint);
					currentNode.addOutNode(outNode);
					//currentNode.removeInstruction(endInstruction);
					outNode.addInConnector(connector);
					currentNode.addOutConnector(connector);
					
					BasicBlockNode nextNode = graph.getBlockNodeByStartIndex(block.getEndIndex() + 1);
					if (nextNode != null) {
						BasicBlockNodeConnector flowIntoConnector = new BasicBlockNodeConnector(currentNode, nextNode, block.getEndIndex(), block.getEndIndex() + 1);
						nextNode.addInNode(currentNode);
						currentNode.addOutNode(nextNode);
						nextNode.addInConnector(flowIntoConnector);
						currentNode.addOutConnector(flowIntoConnector);
					}
				} else if (isCall(endInstruction)) {
					int endLocation = findJumpEndPoint(endInstruction.getArgument(), instructions);
					BasicBlockNode subrtNode = graph.getBlockNodeByStartIndex(endLocation);
					subrtNode.setIsSubroutine(true);
					
					BasicBlockNode nextNode = graph.getBlockNodeByStartIndex(block.getEndIndex() + 1);
					if (nextNode != null) {
						BasicBlockNodeConnector flowIntoConnector = new BasicBlockNodeConnector(currentNode, nextNode, block.getEndIndex(), block.getEndIndex() + 1);
						nextNode.addInNode(currentNode);
						currentNode.addOutNode(nextNode);
						nextNode.addInConnector(flowIntoConnector);
						currentNode.addOutConnector(flowIntoConnector);
					}
				} else if (isReturn(endInstruction) || isPopPC(endInstruction)) {
					System.out.print("");
					/*ArrayList<BasicBlockNode> visitedNodes = new ArrayList<BasicBlockNode>();
					BasicBlockNode parent = getUnmarkedSubroutineStartNodeByReturnNode(currentNode, graph, visitedNodes);
					while (parent != null) {
						parent.setIsSubroutine(true);
						parent = getUnmarkedSubroutineStartNodeByReturnNode(currentNode, graph, visitedNodes);
					}*/
				} else if (!isHalt(endInstruction)) {
					BasicBlockNode nextNode = graph.getBlockNodeByStartIndex(block.getEndIndex() + 1);
					if (nextNode != null) {
						BasicBlockNodeConnector flowIntoConnector = new BasicBlockNodeConnector(currentNode, nextNode, block.getEndIndex(), block.getEndIndex() + 1);
						nextNode.addInNode(currentNode);
						currentNode.addOutNode(nextNode);
						nextNode.addInConnector(flowIntoConnector);
						currentNode.addOutConnector(flowIntoConnector);
					}
				}
			}
		}
		
		return graph;
	}
	
	private BasicBlockNode getUnmarkedSubroutineStartNodeByReturnNode(BasicBlockNode returnNode, ControlFlowGraph graph, ArrayList<BasicBlockNode> visitedNodes) {
		BasicBlockNode startingNode = graph.getStartingNode();
		if (returnNode.getInNodesSize() == 0  && !returnNode.equals(startingNode) && !returnNode.isSubroutine() && !returnNode.isFunction()) {
			return returnNode;
		}
		for (int i = 0; i < returnNode.getInNodesSize(); i++) {
			BasicBlockNode parent = returnNode.getInNode(i);
			if (!visitedNodes.contains(parent)) {
				visitedNodes.add(parent);
				if (parent.getInNodesSize() == 0 && !parent.equals(startingNode) && !parent.isSubroutine() && !returnNode.isFunction()) {
					return parent;
				} else {
					BasicBlockNode temp = getUnmarkedSubroutineStartNodeByReturnNode(parent, graph, visitedNodes);
					if (temp != null) {
						return temp;
					}
				}
			}
		}
		return null;
	}
	
	private void findLoopHeaders(ControlFlowGraph graph) {
		generateDominanceLists(graph);
		int blockHeaderNumber = 1;
		for (int i = 0; i < graph.size(); i++) {
			BasicBlockNode current = graph.get(i);
			ArrayList<ASMInstruction> instructions = current.getInstructions();
			for (ASMInstruction instruction : instructions) {
				if (isBranch(instruction) || isJump(instruction)) {
					BasicBlockNode loopStart = findBlockNodeByLabel(graph, instruction.getArgument());
					if (loopStart != null) {
						boolean isSimpleLoop = evaluateIfSimpleLoop(current, loopStart);
						if (isSimpleLoop) {
							String blockHeaderName = "basicBlockHeader-" + blockHeaderNumber;
							blockHeaderNumber++;
							for (ASMInstruction loopHeaderInstruction : loopStart.getInstructions()) {
								if (isLabel(loopHeaderInstruction) && loopHeaderInstruction.getArgument().equals(instruction.getArgument())) {
									String originalName = instruction.getArgument().toString();
									loopHeaderInstruction.setArgument(blockHeaderName);
									instruction.setArgument(blockHeaderName);
									updateLabelReferences(originalName, blockHeaderName, graph);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private boolean evaluateIfSimpleLoop(BasicBlockNode branch, BasicBlockNode loopStart) {
		ArrayList<BasicBlockNode> openNodes = new ArrayList<BasicBlockNode>();
		openNodes.add(branch);
		boolean foundLoopStart = false;
		while (openNodes.size() > 0) {
			BasicBlockNode currentNode = openNodes.get(0);
			openNodes.remove(0);
			if (currentNode.equals(loopStart)) {
				foundLoopStart = true;
			}
			if (!foundLoopStart) {
				for (BasicBlockNode inNode: currentNode.getInNodes()) {
					if (!inNode.getDominanceList().contains(loopStart)) {
						return false;
					}
					openNodes.add(inNode);
				}
			} else {
				if (!currentNode.getDominanceList().contains(loopStart)) {
					return false;
				}
			}
		}
		return foundLoopStart;
	}
	
	private void generateDominanceLists(ControlFlowGraph graph) {
		BasicBlockNode startingNode = graph.getStartingNode();
		//ArrayList<BasicBlockNode> visitedNodes = new ArrayList<BasicBlockNode>();
		for (BasicBlockNode node : graph) {
			node.resetDominanceList();
			if (node.equals(startingNode) || node.isFunction() || node.isSubroutine()) {
				node.addDominanceNode(node);
			} else {
				for (BasicBlockNode dominanceNode : graph) {
					node.addDominanceNode(dominanceNode);
				}
			}
		}
		boolean changed = false;
		do {
			changed = false;
			for (BasicBlockNode node : graph) {
				boolean wasChange = evaluateDominanceListByPredecessors(node);
				if (wasChange) {
					changed = true;
				}
			}
		} while (changed);
	}
	
	private boolean evaluateDominanceListByPredecessors(BasicBlockNode startingNode) {
		boolean changed = false;
		ArrayList<BasicBlockNode> dominanceList = startingNode.getDominanceList();
		if (startingNode.getInNodesSize() > 0) {
			for (int i = 0; i < startingNode.getInNodesSize(); i++) {
				BasicBlockNode predecessor = startingNode.getInNode(i);
				ArrayList<BasicBlockNode> predDominanceList = predecessor.getDominanceList();
				for (int n = 0; n < dominanceList.size(); n++) {
					BasicBlockNode dominatingNode = dominanceList.get(n);
					if (!predDominanceList.contains(dominatingNode) && !dominatingNode.equals(startingNode)) {
						dominanceList.remove(n);
						n--;
						changed = true;
					}
				}
			}
		}
		return changed;
	}
	
	private void markFunctionsByPushDInstructions(ControlFlowGraph graph) {
		for (BasicBlockNode node: graph) {
			if (node.isFunction()) {
				node.setIsFunction(false);
			}
		}
		for (BasicBlockNode node: graph) {
			ArrayList<ASMInstruction> instructions = node.getInstructions();
			for (ASMInstruction instruction : instructions) {
				if (instruction.getOpcode() == ASMOpcode.PushD && instruction.getArgument() != null) {
					BasicBlockNode function = findBlockNodeByLabel(graph, instruction.getArgument());
					if (function != null) {
						function.setIsFunction(true);
					}
				}
			}
		}
	}
	
	private BasicBlockNode findBlockNodeByLabel(ControlFlowGraph graph, Object label) {
		for (BasicBlockNode node: graph) {
			ArrayList<ASMInstruction> instructions = node.getInstructions();
			for (ASMInstruction instruction : instructions) {
				if (instruction.getOpcode() == ASMOpcode.Label && instruction.getArgument() != null && instruction.getArgument().equals(label)) {
					return node;
				}
			}
		}
		return null;
	}
	
	private ControlFlowGraph removeUnreachableBlocksFromControlFlowGraph(ControlFlowGraph graph, 
			   LinkedList<ASMInstruction> instructions) {
		boolean madeAChange = false;
		BasicBlockNode start = graph.getStartingNode();
		ArrayList<BasicBlockNode> visitedNodes = new ArrayList<BasicBlockNode>();
		if (start != null) {
			visitNeighbours(start, visitedNodes);
		}
		for (BasicBlockNode node : graph) {
			if (node.isSubroutine() || node.isFunction()) {
				visitNeighbours(node, visitedNodes);
			}
		}
		for (int i = 0; i < graph.size(); i++) {
			BasicBlockNode node = graph.get(i);
			if (!visitedNodes.contains(node) && !node.isSubroutine() && !node.isFunction()) {
				graph.removeNode(node);
				i--;
				madeAChange = true;
			}
		}
		if (madeAChange) {
			return graph;
		} else {
			return null;
		}
	}
	/*private void visitNeighbours(BasicBlockNode current, ArrayList<BasicBlockNode> visitedNodes) {
		// Mark the current node as visited and print it
		visitedNodes.add(current);
		
		// Recur for all the vertices adjacent to this vertex
		for (BasicBlockNode node: current.getOutNodes()) {
			if (!visitedNodes.contains(node)) {
				visitNeighbours(node, visitedNodes);
			}
		}
    }*/
	private void visitNeighbours(BasicBlockNode current, ArrayList<BasicBlockNode> visitedNodes) {
		// Mark the current node as visited and print it
		visitedNodes.add(current);
		
		// Recur for all the vertices adjacent to this vertex
		for (BasicBlockNodeConnector connector: current.getOutConnectors()) {
			BasicBlockNode node = connector.getGoesToNode();
			if (!visitedNodes.contains(node)) {
				visitNeighbours(node, visitedNodes);
			}
		}
    }
	
	private ControlFlowGraph mergeNeighbourBlocksInControlFlowGraph(ControlFlowGraph graph, 
			   LinkedList<ASMInstruction> instructions) {
		boolean madeAMerge = false;
		for (int i = 0; i < graph.size(); i++) {
			BasicBlockNode current = graph.get(i);
			//BasicBlockNode next = graph.get(i + 1);
			BasicBlockNode next = null;
			if (current.getOutNodesSize() == 1) {
				next = current.getOutNode(0);
			}
			if (next != null 
					&& current.getOutNodes().contains(next) 
					&& next.getInNodes().contains(current)
					&& current.getOutNodesSize() == 1
					&& next.getInNodesSize() == 1
					&& !isCall(current.getLastInstruction())) {
					//&& next.getOutNodesSize() >= 1) {
				for (BasicBlockNode outNode : next.getOutNodes()) {
					current.addOutNode(outNode);
					outNode.addInNode(current);
				}
				for (BasicBlockNodeConnector outConnector : next.getOutConnectors()) {
					outConnector.setGoesFromNode(current);
					current.addOutConnector(outConnector);
				}
				while (isJump(current.getLastInstruction())) {
					current.removeInstruction(current.getLastInstruction());
				}
				while (isLabel(next.getInstruction(0))) {
					next.removeInstruction(0);
				}
				for (ASMInstruction instruction : next.getInstructions()) {
					BasicBlockNodeConnector connector = next.findConnectorByInstruction(instruction);
					
					ASMInstruction newInstruction = current.addInstruction(instruction);
					if (connector != null) {
						if (connector.goesTo().equals(instruction)) {
							connector.setGoesTo(newInstruction);
						} else {
							connector.setGoesFrom(newInstruction);
						}
					}
				}
				BasicBlock currentBlock = current.getBlock();
				BasicBlock nextBlock = next.getBlock();
				/*if (current.getInstructionListSize() - 1 >= 0 
						&& (isJump(current.getInstruction(current.getInstructionListSize() - 1)) 
						|| isBranch(current.getInstruction(current.getInstructionListSize() - 1)))) {
					current.removeInstruction(current.getInstructionListSize() - 1);
					if (current.getInstructionListSize() - 1 >= 0 
							&& isBranch(current.getInstruction(current.getInstructionListSize() - 1))) {
						current.removeInstruction(current.getInstructionListSize() - 1);
					}
				}*/
				currentBlock.setEndIndex(nextBlock.getEndIndex());
				current.updateBasicBlock(currentBlock);
				
				graph.removeNode(next);
				i--;
				madeAMerge = true;
			}
		}
		if (madeAMerge) {
			return graph;
		} else {
			return null;
		}
	}
	
	private ControlFlowGraph removeOrSimplifyBranches(ControlFlowGraph graph, 
			   LinkedList<ASMInstruction> instructions) {
		boolean madeAChange = false;
		for (int i = 0; i < graph.size(); i++) {
			BasicBlockNode node = graph.get(i);
			if (isJump(node.getLastInstruction())) {
				if (isBranch(node.getSecondLastInstruction())) {
					//ASMInstruction prior = node.getThirdLastInstruction();
					ASMInstruction prior = node.getInstructionPriorToBranch();
					ASMInstruction branch = node.getSecondLastInstruction();
					ArrayList<ASMInstruction> optimizedInstructions = peepHoleOptimization(prior, branch);
					if (optimizedInstructions != null) {
						if (optimizedInstructions.size() == 0) {
							node.removeInstruction(prior);
							node.removeInstruction(branch);
							BasicBlockNodeConnector connector = node.findConnectorByInstruction(branch);
							if (connector != null) {
								if (node.getOutConnectors().contains(connector)) {
									BasicBlockNode to = connector.getGoesToNode();
									to.removeFromInConnectors(node);
									to.removeFromInNodes(node);
									node.removeFromOutConnectors(to);
									node.removeFromOutNodes(to);
								}
							}
						} else {
							node.removeInstruction(node.getLastInstruction());
							node.removeInstruction(prior);
							node.removeInstruction(branch);
							for (ASMInstruction instruction : optimizedInstructions) {
								instruction = node.addInstruction(instruction);
								BasicBlockNodeConnector connector = node.findConnectorByInstruction(branch);
								if (connector != null) {
									ASMInstruction goesFrom = connector.goesFrom();
									ASMInstruction goesTo = connector.goesTo();
									if (goesFrom != null && 
											goesFrom.getOpcode().equals(branch.getOpcode()) &&
											goesFrom.getArgument().equals(branch.getArgument()) &&
											goesFrom.getComment().equals(branch.getComment())) {
										connector.setGoesFrom(instruction);
									} else if (goesTo != null && 
											goesTo.getOpcode().equals(branch.getOpcode()) &&
											goesTo.getArgument().equals(branch.getArgument()) &&
											goesTo.getComment().equals(branch.getComment())) {
										connector.setGoesTo(instruction);
									}
									for (int n = 0; n < node.getOutConnectorsSize(); n++) {
										BasicBlockNodeConnector unreachable = node.getOutConnector(n);
										if (!connector.equals(unreachable)) {
											BasicBlockNode goesToNode = unreachable.getGoesToNode();
											node.removeFromOutConnectors(goesToNode);
											node.removeFromOutNodes(goesToNode);
											goesToNode.removeFromInConnectors(node);
											goesToNode.removeFromInNodes(node);
											n--;
										}
									}
								}
							}
						}
						madeAChange = true;
					}
				}
			} else if (isBranch(node.getLastInstruction())) {
				//ASMInstruction prior = node.getSecondLastInstruction();
				ASMInstruction prior = node.getInstructionPriorToBranch();
				ASMInstruction branch = node.getLastInstruction();
				ArrayList<ASMInstruction> optimizedInstructions = peepHoleOptimization(prior, branch);
				if (optimizedInstructions != null) {
					if (optimizedInstructions.size() == 0) {
						node.removeInstruction(prior);
						node.removeInstruction(branch);
						BasicBlockNodeConnector connector = node.findConnectorByInstruction(branch);
						if (connector != null) {
							if (node.getOutConnectors().contains(connector)) {
								BasicBlockNode to = connector.getGoesToNode();
								to.removeFromInConnectors(node);
								to.removeFromInNodes(node);
								node.removeFromOutConnectors(to);
								node.removeFromOutNodes(to);
							}
						}
					} else {
						node.removeInstruction(prior);
						node.removeInstruction(branch);
						for (ASMInstruction instruction : optimizedInstructions) {
							instruction = node.addInstruction(instruction);
							BasicBlockNodeConnector connector = node.findConnectorByInstruction(branch);
							if (connector != null) {
								ASMInstruction goesFrom = connector.goesFrom();
								ASMInstruction goesTo = connector.goesTo();
								if (goesFrom != null && 
										goesFrom.getOpcode().equals(branch.getOpcode()) &&
										goesFrom.getArgument().equals(branch.getArgument()) &&
										goesFrom.getComment().equals(branch.getComment())) {
									connector.setGoesFrom(instruction);
								} else if (goesTo != null && 
										goesTo.getOpcode().equals(branch.getOpcode()) &&
										goesTo.getArgument().equals(branch.getArgument()) &&
										goesTo.getComment().equals(branch.getComment())) {
									connector.setGoesTo(instruction);
								}
								for (int n = 0; n < node.getOutConnectorsSize(); n++) {
									BasicBlockNodeConnector unreachable = node.getOutConnector(n);
									if (!connector.equals(unreachable)) {
										BasicBlockNode goesToNode = unreachable.getGoesToNode();
										node.removeFromOutConnectors(goesToNode);
										node.removeFromOutNodes(goesToNode);
										goesToNode.removeFromInConnectors(node);
										goesToNode.removeFromInNodes(node);
										n--;
									}
								}
							}
						}
					}
					madeAChange = true;
				}
			}
		}
		if (madeAChange) {
			return graph;
		} else {
			return null;
		}
	}
	private ArrayList<ASMInstruction> peepHoleOptimization(ASMInstruction prior, ASMInstruction branch) {
		ArrayList<ASMInstruction> newInstructions = new ArrayList<ASMInstruction>();
		Integer tempInt;
		Double tempFloat;
		if (prior != null && prior.getArgument() != null) {
			switch(branch.getOpcode()) {
			case JumpFalse:
				if (prior.getArgument() instanceof Integer) {
					tempInt = (Integer)(prior.getArgument());
					if (tempInt == 0) {
						ASMInstruction instruction = new ASMInstruction(ASMOpcode.Jump, (String)(branch.getArgument()));
						newInstructions.add(instruction);
					}
				} else {
					return null;
				}
				break;
			case JumpFNeg:
				if (prior.getArgument() instanceof Double) {
					tempFloat = (Double)(prior.getArgument());
					if (tempFloat < 0) {
						ASMInstruction instruction = new ASMInstruction(ASMOpcode.Jump, (String)(branch.getArgument()));
						newInstructions.add(instruction);
						return newInstructions;
					}
				} else {
					return null;
				}
				break;
			case JumpFPos:
				if (prior.getArgument() instanceof Double) {
					tempFloat = (Double)(prior.getArgument());
					if (tempFloat > 0) {
						ASMInstruction instruction = new ASMInstruction(ASMOpcode.Jump, (String)(branch.getArgument()));
						newInstructions.add(instruction);
						return newInstructions;
					}
				} else {
					return null;
				}
				break;
			case JumpFZero:
				if (prior.getArgument() instanceof Double) {
					tempFloat = (Double)(prior.getArgument());
					if (tempFloat == 0) {
						ASMInstruction instruction = new ASMInstruction(ASMOpcode.Jump, (String)(branch.getArgument()));
						newInstructions.add(instruction);
						return newInstructions;
					}
				} else {
					return null;
				}
				break;
			case JumpNeg:
				if (prior.getArgument() instanceof Integer) {
					tempInt = (Integer)(prior.getArgument());
					if (tempInt < 0) {
						ASMInstruction instruction = new ASMInstruction(ASMOpcode.Jump, (String)(branch.getArgument()));
						newInstructions.add(instruction);
						return newInstructions;
					}
				} else {
					return null;
				}
				break;
			case JumpPos:
				if (prior.getArgument() instanceof Integer) {
					tempInt = (Integer)(prior.getArgument());
					if (tempInt > 0) {
						ASMInstruction instruction = new ASMInstruction(ASMOpcode.Jump, (String)(branch.getArgument()));
						newInstructions.add(instruction);
					}
				} else {
					return null;
				}
				break;
			case JumpTrue:
				if (prior.getArgument() instanceof Integer) {
					tempInt = (Integer)(prior.getArgument());
					if (tempInt != 0) {
						ASMInstruction instruction = new ASMInstruction(ASMOpcode.Jump, (String)(branch.getArgument()));
						newInstructions.add(instruction);
					}
				} else {
					return null;
				}
				break;
			default:
			}
		} else {
			return null;
		}
		return newInstructions;
	}
	
	private ControlFlowGraph removeUnusedInstructions(ControlFlowGraph graph) {
		boolean madeAChange = false;
		BasicBlockNode start = graph.getStartingNode();
		ArrayList<BasicBlockNode> visitedNodes = new ArrayList<BasicBlockNode>();
		if (start != null) {
			visitNeighbours(start, visitedNodes);
		}
		boolean foundANewSubroutine = false;
		do {
			foundANewSubroutine = false;
			ArrayList<BasicBlockNode> subroutinesToVisit = new ArrayList<BasicBlockNode>();
			for (BasicBlockNode node : graph) {
				if ((node.isSubroutine() || node.isFunction()) && !visitedNodes.contains(node)) {
					//visitNeighbours(node, visitedNodes);
					for (BasicBlockNode visitedNode : visitedNodes) {
						for (ASMInstruction instruction : visitedNode.getInstructions()) {
							if (isPushD(instruction) || isBranch(instruction) || isJump(instruction) || isCall(instruction)) {
								for (ASMInstruction subrtInstruction : node.getInstructions()) {
									if (isLabel(subrtInstruction) && 
											instruction.getArgument() != null && 
											subrtInstruction.getArgument() != null && instruction.getArgument().equals(subrtInstruction.getArgument())) {
										subroutinesToVisit.add(node);
										foundANewSubroutine = true;
									}
								}
							}
						}
					}
				}
			} 
			for (BasicBlockNode node : subroutinesToVisit) {
				visitNeighbours(node, visitedNodes);
			}
		} while(foundANewSubroutine);
		for (int i = 0; i < graph.size(); i++) {
			BasicBlockNode node = graph.get(i);
			if (!visitedNodes.contains(node)) {
				graph.removeNode(node);
				i--;
				madeAChange = true;
			}
		}
		if (madeAChange) {
			return graph;
		} else {
			return null;
		}
	}
	
	private ControlFlowGraph relabelBlocks(ControlFlowGraph graph) {
		int blockNumber = 1;
		resetAllReferences(graph);
		//ArrayList<Object> referencesToUpdate = new ArrayList<>;
		for (BasicBlockNode node: graph) {
			String blockName = "basicBlock-" + blockNumber;
			blockNumber++;
			ASMInstruction label = node.getInstruction(0);
			if (isLabel(label)) {
				String originalName = label.getArgument().toString();
				label.setArgument(blockName);
				updateLabelReferences(originalName, blockName, graph);
			} else {
				node.insertInstruction(0, new ASMInstruction(ASMOpcode.Label, blockName));
			}
		}
		return graph;
	}
	
	private void resetAllReferences(ControlFlowGraph graph) {
		for (BasicBlockNode node: graph) {
			ASMInstruction label = node.getInstruction(0);
			if (isLabel(label)) {
				String originalName = label.getArgument().toString();
				String newName = originalName + "-reset";
				for (BasicBlockNode temp: graph) {
					ArrayList<ASMInstruction> instructions = temp.getInstructions();
					for (ASMInstruction instruction : instructions) {
						if (instruction.getArgument() != null && instruction.getArgument().equals(originalName)) {
							instruction.setArgument(newName);
						}
					}
				}
			}
		}
	}
	
	private void updateLabelReferences(String oldLabel, String newLabel, ControlFlowGraph graph) {
		for (BasicBlockNode node: graph) {
			/*for (BasicBlockNodeConnector connector : node.getInConnectors()) {
				if (connector.goesFrom() != null && connector.goesFrom().getArgument() != null && connector.goesFrom().getArgument().equals(oldLabel)) {
					connector.goesFrom().setArgument(newLabel);
				}
				if (connector.goesTo() != null && connector.goesTo().getArgument() != null && connector.goesTo().getArgument().equals(oldLabel)) {
					connector.goesTo().setArgument(newLabel);
				}
			}
			for (BasicBlockNodeConnector connector : node.getOutConnectors()) {
				if (connector.goesFrom() != null && connector.goesFrom().getArgument() != null && connector.goesFrom().getArgument().equals(oldLabel)) {
					connector.goesFrom().setArgument(newLabel);
				}
				if (connector.goesTo() != null && connector.goesTo().getArgument() != null && connector.goesTo().getArgument().equals(oldLabel)) {
					connector.goesTo().setArgument(newLabel);
				}
			}*/
			ArrayList<ASMInstruction> instructions = node.getInstructions();
			for (ASMInstruction instruction : instructions) {
				if (instruction.getOpcode() != ASMOpcode.Label
						&& instruction.getArgument() != null && instruction.getArgument().equals(oldLabel)) {
					instruction.setArgument(newLabel);
				}
			}
		}
	}
	
	/*private LinkedList<ASMInstruction> convertControlFlowGraphToInstructionSet(ControlFlowGraph graph) {
		LinkedList<ASMInstruction> optimizedBlockInstructions = new LinkedList<ASMInstruction>();
		for (BasicBlockNode node : graph) {
			//for (BasicBlockNodeConnector connector: node.getInConnectors()) {
			//	ASMInstruction start = connector.goesTo();
			//	if (start != null && !optimizedBlockInstructions.contains(start)) {
			//		optimizedBlockInstructions.add(start);
			//	}
			//}
			ArrayList<ASMInstruction> nodeInstructions = node.getInstructions();
			for (ASMInstruction nodeInstruction : nodeInstructions) {
				optimizedBlockInstructions.add(nodeInstruction);
			}
			//for (BasicBlockNodeConnector connector: node.getOutConnectors()) {
			//	ASMInstruction end = connector.goesFrom();
			//	if (end != null) {
			//		optimizedBlockInstructions.add(end);
			//	}
			//}
		}
		return optimizedBlockInstructions;
	}*/
	
	private LinkedList<ASMInstruction> convertControlFlowGraphToInstructionSet(ControlFlowGraph graph) {
		LinkedList<ASMInstruction> optimizedBlockInstructions = new LinkedList<ASMInstruction>();
		boolean noChange = true;
		do {
			noChange = true;
			for (int i = 0; i < graph.size(); i++) {
				BasicBlockNode current = graph.get(i);
				if (i +1 < graph.size()) {
					BasicBlockNode next = graph.get(i + 1);
					for (BasicBlockNodeConnector connector : current.getOutConnectors()) {
						if (connector.goesTo() == null && connector.getGoesToNode() != null && !connector.goesToNode(next)) {
							int neighbourIndex = graph.indexOf(connector.getGoesToNode());
							BasicBlockNode neighbour = graph.get(neighbourIndex);
							graph.remove(neighbourIndex);
							graph.add(i + 1, neighbour);
							noChange = false;
						}
					}
				}
			}
		} while (!noChange);
		for (BasicBlockNode node : graph) {
			ArrayList<ASMInstruction> nodeInstructions = node.getInstructions();
			for (ASMInstruction nodeInstruction : nodeInstructions) {
				if (nodeInstruction.getOpcode() != ASMOpcode.Nop) {
					optimizedBlockInstructions.add(nodeInstruction);
				}
			}
		}
		return optimizedBlockInstructions;
	}
	
	private ControlFlowGraph simplifyConditionalBlocks(ControlFlowGraph graph, LinkedList<ASMInstruction> instructions) {
		boolean simplifiedABlock = false;
		for (int i = 0; i < graph.size(); i++) {
			BasicBlockNode node = graph.get(i);
			boolean isConditionalBlock = checkIfConditionalBlock(node);
			if (isConditionalBlock) {
				int copiesMade = 0;
				for (int n = 0; n < node.getInNodesSize(); n++) {
					BasicBlockNode inNode = node.getInNode(n);
					BasicBlockNodeConnector inConnector = node.getInConnector(n);
					
					BasicBlockNode copy = copyBlockNode(node, n);
					inNode.removeFromOutNodes(node);
					inNode.addOutNode(copy);
					copy.addInNode(inNode);
					BasicBlockNodeConnector newInConnection = new BasicBlockNodeConnector(inConnector);
					ASMInstruction temp = inNode.getOutConnector(0).goesFrom();
					if (inConnector.goesTo() != null && inConnector.goesTo().getArgument() != null) {
						temp.setArgument(newInConnection.goesTo().getArgument() + "-" + n);
						newInConnection.goesTo().setArgument(newInConnection.goesTo().getArgument() + "-" + n);
						newInConnection.setGoesToNode(copy);
					}
					inNode.removeFromOutConnectors(node);
					inNode.addOutConnector(newInConnection);
					copy.addInConnector(newInConnection);
					/*for (BasicBlockNodeConnector connector : copy.getOutConnectors()) {
						if (connector.goesFrom() == null) {
							BasicBlockNode tempNode = connector.getGoesToNode();
							if (tempNode != null) {
								ASMInstruction labelInstruction = tempNode.getInstruction(0);
								if (isLabel(labelInstruction) && labelInstruction.getArgument() != null) {
									ASMInstruction newJumpInstruction = new ASMInstruction(ASMOpcode.Jump, labelInstruction.getArgument().toString());
									connector.setGoesFrom(newJumpInstruction, instructions);
									connector.setGoesTo(labelInstruction, instructions);
									copy.addInstruction(newJumpInstruction);
								}
							}
						}
					}*/
					newInConnection.setGoesTo(null, instructions);
					newInConnection.setGoesFrom(null, instructions);
					if (isJump(inNode.getInstruction(inNode.getInstructionListSize() - 1))) {
						inNode.removeInstruction(inNode.getInstructionListSize() - 1);
					}
					int index = graph.indexOf(inNode);
					graph.add(index + 1, copy);
					//graph.add(i + n, copy);
					copiesMade = n;
				}
				i += copiesMade;
				graph.removeNode(node);
				simplifiedABlock = true;
			}
		}
		if (simplifiedABlock) {
			return graph;
		} else {
			return null;
		}
	}
	
	private BasicBlockNode copyBlockNode(BasicBlockNode source, int copyNumber) {
		BasicBlockNode copy = new BasicBlockNode(source, copyNumber);
		for (BasicBlockNodeConnector connector : copy.getOutConnectors()) {
			for (ASMInstruction instruction : copy.getInstructions()) {
				ASMInstruction goesFrom = connector.goesFrom();
				ASMInstruction goesTo = connector.goesTo();
				if (goesFrom != null && 
						goesFrom.getOpcode().equals(instruction.getOpcode()) &&
						goesFrom.getArgument().equals(instruction.getArgument()) &&
						goesFrom.getComment().equals(instruction.getComment())) {
					connector.setGoesFrom(instruction);
				} else if (goesTo != null && 
						goesTo.getOpcode().equals(instruction.getOpcode()) &&
						goesTo.getArgument().equals(instruction.getArgument()) &&
						goesTo.getComment().equals(instruction.getComment())) {
					connector.setGoesTo(instruction);
				}
			}
		}
		return copy;
	}
	
	private boolean checkIfConditionalBlock(BasicBlockNode node) {
		ArrayList<ASMInstruction> trimmedBlock = node.getTrimmedInstructionList();
		if (trimmedBlock != null && trimmedBlock.size() == 0 && node.getOutConnectorsSize() == 2 && node.getInNodesSize() >= 2) {
			for (BasicBlockNode inNeighbour : node.getInNodes()) {
				if (inNeighbour.getOutNodesSize() != 1) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	private boolean isBlockStart(ASMInstruction instruction) {
		if (isLabel(instruction) || isBranch(instruction) || isJump(instruction)) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isBranch(ASMInstruction instruction) {
		if (instruction != null && (instruction.getOpcode() == ASMOpcode.JumpFalse ||
				instruction.getOpcode() == ASMOpcode.JumpFNeg ||
				instruction.getOpcode() == ASMOpcode.JumpFPos ||
				instruction.getOpcode() == ASMOpcode.JumpFZero ||
				instruction.getOpcode() == ASMOpcode.JumpNeg ||
				instruction.getOpcode() == ASMOpcode.JumpPos ||
				instruction.getOpcode() == ASMOpcode.JumpTrue)) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isJump(ASMInstruction instruction) {
		if (instruction != null && instruction.getOpcode() == ASMOpcode.Jump) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isCall(ASMInstruction instruction) {
		if (instruction != null && (instruction.getOpcode() == ASMOpcode.Call ||
				instruction.getOpcode() == ASMOpcode.CallV)) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isReturn(ASMInstruction instruction) {
		if (instruction != null && instruction.getOpcode() == ASMOpcode.Return) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isPopPC(ASMInstruction instruction) {
		if (instruction != null && instruction.getOpcode() == ASMOpcode.PopPC) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isHalt(ASMInstruction instruction) {
		if (instruction != null && instruction.getOpcode() == ASMOpcode.Halt) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isLabel(ASMInstruction instruction) {
		if (instruction != null && instruction.getOpcode() == ASMOpcode.Label) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isDLabel(ASMInstruction instruction) {
		if (instruction != null && instruction.getOpcode() == ASMOpcode.DLabel) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isDataInstruction(ASMInstruction instruction) {
		if (instruction != null && (instruction.getOpcode() == ASMOpcode.DataC ||
				instruction.getOpcode() == ASMOpcode.DataD ||
				instruction.getOpcode() == ASMOpcode.DataF ||
				instruction.getOpcode() == ASMOpcode.DataI ||
				instruction.getOpcode() == ASMOpcode.DataS ||
				instruction.getOpcode() == ASMOpcode.DataZ)) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isPushD(ASMInstruction instruction) {
		if (instruction != null && instruction.getOpcode() == ASMOpcode.PushD) {
			return true;
		} else {
			return false;
		}
	}
	
	private int findJumpEndPoint(Object name, LinkedList<ASMInstruction> instructions) {
		for (int i = 0; i < instructions.size(); i++) {
			ASMInstruction current = instructions.get(i);
			if (isLabel(current) && current.getArgument().equals(name)) {
				return i;
			}
		}
		return -1;
	}
	
	private int findBlockEndPoint(int start, LinkedList<ASMInstruction> instructions) {
		int blockEnd = instructions.size();
		for (int i = start; i >= 0 && i < instructions.size(); i++) {
			ASMInstruction current = instructions.get(i);
			if (isBlockEnd(current)) {
				if (isBranch(current)) {
					current = instructions.get(i+1);
					if (isJump(current)) {
						i = i + 1;
					}
				}
				blockEnd = i;
				break;
			}
		}
		if (blockEnd == -1) {
			System.out.print("");
		}
		return blockEnd;
	}
	
	private boolean isBlockEnd(ASMInstruction instruction) {
		return isBranch(instruction) || isJump(instruction) || isHalt(instruction) || isCall(instruction) || isReturn(instruction);
	}
	
	private boolean isArithmeticOperation(ASMInstruction instruction) {
		switch(instruction.getOpcode()) {
		case Add:
			return true;
		case Subtract:
			return true;
		case Multiply:
			return true;
		case Divide:
			return true;
		case FAdd:
			return true;
		case FSubtract:
			return true;
		case FMultiply:
			return true;
		case FDivide:
			return true;
		default:
			return false;
		}
	}
	
	private boolean isCastOperation(ASMInstruction instruction) {
		switch(instruction.getOpcode()) {
		case ConvertI:
			return true;
		case ConvertF:
			return true;
		default:
			return false;
		}
	}
	
	private boolean isNegateOperation(ASMInstruction instruction) {
		switch(instruction.getOpcode()) {
		case Negate:
			return true;
		case FNegate:
			return true;
		default:
			return false;
		}
	}
	
	private boolean isIntegerOperation(ASMInstruction operator) {
		switch(operator.getOpcode()) {
		case Add:
		case Subtract:
		case Multiply:
		case Divide:
			return true;
		default:
			return false;
		}
	}
	
	private boolean isFloatingOperation(ASMInstruction operator) {
		switch(operator.getOpcode()) {
		case Add:
		case Subtract:
		case Multiply:
		case Divide:
			return true;
		default:
			return false;
		}
	}
	
	private boolean instructionIsConstant(ASMInstruction instruction) {
		if (instruction.getOpcode() == ASMOpcode.PushI) {
			return true;
		} else if (instruction.getOpcode() == ASMOpcode.PushF) {
			return true;
		} else {
			return false;
		}
	}
}
