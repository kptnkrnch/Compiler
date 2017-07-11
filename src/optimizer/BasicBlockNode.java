package optimizer;

import java.util.ArrayList;
import java.util.LinkedList;

import asmCodeGenerator.codeStorage.ASMInstruction;
import asmCodeGenerator.codeStorage.ASMOpcode;

public class BasicBlockNode {
	private BasicBlock block;
	private String name;
	private ArrayList<BasicBlockNode> inNodes;
	private ArrayList<BasicBlockNode> outNodes;
	private ArrayList<BasicBlockNodeConnector> inConnectors;
	private ArrayList<BasicBlockNodeConnector> outConnectors;
	private ArrayList<BasicBlockNode> dominatedBy;
	private boolean subrtStatus;
	private boolean functionStatus;
	private boolean simpleLoopStatus;
	private ArrayList<ASMInstruction> codeChunk;
	
	public BasicBlockNode(BasicBlock block, String name) {
		this.block = block;
		this.name = name;
		this.inNodes = new ArrayList<BasicBlockNode>();
		this.outNodes = new ArrayList<BasicBlockNode>();
		this.inConnectors = new ArrayList<BasicBlockNodeConnector>();
		this.outConnectors = new ArrayList<BasicBlockNodeConnector>();
		this.dominatedBy = new ArrayList<BasicBlockNode>();
		this.subrtStatus = false;
		this.functionStatus = false;
		this.simpleLoopStatus = false;
		this.codeChunk = new ArrayList<ASMInstruction>();
	}
	
	public BasicBlockNode(BasicBlockNode node, String name) {
		this.block = node.block;
		this.name = name;
		this.inNodes = new ArrayList<BasicBlockNode>();
		for (BasicBlockNode inNode : node.getInNodes()) {
			this.inNodes.add(inNode);
		}
		this.outNodes = new ArrayList<BasicBlockNode>();
		for (BasicBlockNode outNode : node.getOutNodes()) {
			this.outNodes.add(outNode);
		}
		this.inConnectors = new ArrayList<BasicBlockNodeConnector>();
		for (BasicBlockNodeConnector inConnector : node.getInConnectors()) {
			this.inConnectors.add(inConnector);
		}
		this.outConnectors = new ArrayList<BasicBlockNodeConnector>();
		for (BasicBlockNodeConnector outConnector : node.getOutConnectors()) {
			this.outConnectors.add(outConnector);
		}
		this.dominatedBy = new ArrayList<BasicBlockNode>();
		for (BasicBlockNode dominatedBy : node.dominatedBy) {
			this.dominatedBy.add(dominatedBy);
		}
		this.subrtStatus = node.subrtStatus;
		this.functionStatus = node.functionStatus;
		this.simpleLoopStatus = node.simpleLoopStatus;
		this.codeChunk = new ArrayList<ASMInstruction>();
		for (ASMInstruction instruction : node.getInstructions()) {
			this.codeChunk.add(instruction);
		}
	}
	
	public BasicBlockNode(BasicBlockNode node, int copyNumber) {
		this.block = node.block;
		this.name = node.name + "-" + copyNumber;
		this.inNodes = new ArrayList<BasicBlockNode>();
		this.outNodes = new ArrayList<BasicBlockNode>();
		for (BasicBlockNode outNode : node.getOutNodes()) {
			outNode.addInNode(this);
			this.outNodes.add(outNode);
		}
		this.inConnectors = new ArrayList<BasicBlockNodeConnector>();
		this.outConnectors = new ArrayList<BasicBlockNodeConnector>();
		for (BasicBlockNodeConnector outConnector : node.getOutConnectors()) {
			BasicBlockNodeConnector newOutConnector = new BasicBlockNodeConnector(outConnector);
			newOutConnector.setGoesFromNode(this);
			BasicBlockNode outNode = newOutConnector.getGoesToNode();
			outNode.addInConnector(newOutConnector);
			this.outConnectors.add(newOutConnector);
		}
		this.subrtStatus = node.subrtStatus;
		this.functionStatus = node.functionStatus;
		this.simpleLoopStatus = node.simpleLoopStatus;
		this.codeChunk = new ArrayList<ASMInstruction>();
		for (ASMInstruction instruction : node.getInstructions()) {
			ASMInstruction newInstruction = new ASMInstruction(instruction);
			if (newInstruction.getOpcode() == ASMOpcode.Label) {
				newInstruction.setArgument(newInstruction.getArgument() + "-" + copyNumber);
			}
			this.codeChunk.add(newInstruction);
		}
	}
	
	public String getName() {
		return this.name;
	}
	
	public ArrayList<ASMInstruction> getTrimmedInstructionList() {
		ArrayList<ASMInstruction> trimmedList = new ArrayList<ASMInstruction>();
		for (ASMInstruction instruction : codeChunk) {
			trimmedList.add(instruction);
		}
		for (BasicBlockNodeConnector connector : inConnectors) {
			if (connector.goesTo() != null) {
				ASMInstruction goesTo = connector.goesTo();
				trimmedList.remove(goesTo);
			}
		}
		for (BasicBlockNodeConnector connector : outConnectors) {
			if (connector.goesFrom() != null) {
				ASMInstruction goesFrom = connector.goesFrom();
				trimmedList.remove(goesFrom);
			}
		}
		return trimmedList;
	}
	
	public ASMInstruction addInstruction(ASMInstruction instruction) {
		ASMInstruction newInstruction = new ASMInstruction(instruction);
		this.codeChunk.add(newInstruction);
		return newInstruction;
	}
	
	public ASMInstruction insertInstruction(int index, ASMInstruction instruction) {
		ASMInstruction newInstruction = new ASMInstruction(instruction);
		this.codeChunk.add(index, newInstruction);
		return newInstruction;
	}
	
	public ASMInstruction getInstruction(int index) {
		if (index >= 0 && index < this.codeChunk.size()) {
			return this.codeChunk.get(index);
		} else {
			return null;
		}
	}
	
	public ASMInstruction getLastInstruction() {
		if (this.codeChunk.size() >= 1) {
			return this.codeChunk.get(this.codeChunk.size() - 1);
		} else {
			return null;
		}
	}
	
	public ASMInstruction getSecondLastInstruction() {
		if (this.codeChunk.size() >= 2) {
			return this.codeChunk.get(this.codeChunk.size() - 2);
		} else {
			return null;
		}
	}
	
	public ASMInstruction getThirdLastInstruction() {
		if (this.codeChunk.size() >= 3) {
			return this.codeChunk.get(this.codeChunk.size() - 3);
		} else {
			return null;
		}
	}
	
	public ASMInstruction getInstructionPriorToBranch() {
		ASMInstruction last = this.getLastInstruction();
		ASMInstruction secondLast = this.getSecondLastInstruction();
		
		if (isBranch(last) && this.codeChunk.size() >= 2) {
			ASMInstruction temp = null;
			for (int i = this.codeChunk.size() - 2; i >= 0; i--) {
				temp = this.codeChunk.get(i);
				if (!isLabel(temp)) {
					return temp;
				}
			}
			return null;
		} else if (isBranch(secondLast) && this.codeChunk.size() >= 3) {
			ASMInstruction temp = null;
			for (int i = this.codeChunk.size() - 3; i >= 0; i--) {
				temp = this.codeChunk.get(i);
				if (!isLabel(temp)) {
					return temp;
				}
			}
			return null;
		} else {
			return null;
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
	
	private boolean isLabel(ASMInstruction instruction) {
		if (instruction != null && instruction.getOpcode() == ASMOpcode.Label) {
			return true;
		} else {
			return false;
		}
	}
	
	public ASMInstruction getInstruction(ASMInstruction instruction) {
		for (ASMInstruction nodeInstruction : this.codeChunk) {
			if (nodeInstruction.getOpcode().equals(instruction.getOpcode()) &&
					nodeInstruction.getArgument().equals(instruction.getArgument()) &&
					nodeInstruction.getComment().equals(instruction.getComment())) {
				return nodeInstruction;
			}
		}
		return null;
	}
	
	public BasicBlockNodeConnector findConnectorByInstruction(ASMInstruction instruction) {
		for (BasicBlockNodeConnector connector : this.inConnectors) {
			if ((connector.goesFrom() != null && connector.goesFrom().equals(instruction)) 
					|| (connector.goesTo() != null && connector.goesTo().equals(instruction))) {
				return connector;
			}
		}
		for (BasicBlockNodeConnector connector : this.outConnectors) {
			if ((connector.goesFrom() != null && connector.goesFrom().equals(instruction)) 
					|| (connector.goesTo() != null && connector.goesTo().equals(instruction))) {
				return connector;
			}
		}
		return null;
	}
	
	public void removeInstruction(int index) {
		this.codeChunk.remove(index);
	}
	
	public void removeInstruction(ASMInstruction instruction) {
		if (instruction != null) {
			this.codeChunk.remove(instruction);
		}
	}
	
	public int getInstructionListSize() {
		return this.codeChunk.size();
	}
	
	public ArrayList<ASMInstruction> getInstructions() {
		return this.codeChunk;
	}
	
	public int getBlockStart() {
		return this.block.getStartIndex();
	}
	
	public int getBlockEnd() {
		return this.block.getEndIndex();
	}
	
	public BasicBlock getBlock() {
		return this.block;
	}
	
	public void addInNode(BasicBlockNode node) {
		this.inNodes.add(node);
	}
	
	public void addOutNode(BasicBlockNode node) {
		this.outNodes.add(node);
	}
	
	public BasicBlockNode getInNode(int index) {
		return this.inNodes.get(index);
	}
	
	public BasicBlockNode getOutNode(int index) {
		return this.outNodes.get(index);
	}
	
	public void setInNode(int index, BasicBlockNode in) {
		this.inNodes.set(index, in);
	}
	
	public void setOutNode(int index, BasicBlockNode out) {
		this.outNodes.set(index, out);
	}
	
	public int getInNodesSize() {
		return this.inNodes.size();
	}
	
	public int getOutNodesSize() {
		return this.outNodes.size();
	}
	
	public ArrayList<BasicBlockNode> getInNodes() {
		return this.inNodes;
	}
	
	public ArrayList<BasicBlockNode> getOutNodes() {
		return this.outNodes;
	}
	
	public ArrayList<BasicBlockNode> getDominanceList() {
		return this.dominatedBy;
	}
	
	public void setDominanceList(ArrayList<BasicBlockNode> dominatedBy) {
		this.dominatedBy = dominatedBy;
	}
	
	public void addDominanceNode(BasicBlockNode node) {
		this.dominatedBy.add(node);
	}
	
	public void removeDominanceNode(BasicBlockNode node) {
		this.dominatedBy.remove(node);
	}
	
	public void removeDominanceNode(int index) {
		this.dominatedBy.remove(index);
	}
	
	public BasicBlockNode getDominanceNode(int index) {
		return this.dominatedBy.get(index);
	}
	
	public void resetDominanceList() {
		while (this.dominatedBy.size() > 0) {
			this.dominatedBy.remove(0);
		}
	}
	
	public boolean contains(BasicBlockNode node) {
		if (inNodes.contains(node) || outNodes.contains(node)) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean removeFromInNodes(BasicBlockNode node) {
		if (inNodes.contains(node)) {
			return inNodes.remove(node);
		} else {
			return false;
		}
	}
	
	public boolean removeFromOutNodes(BasicBlockNode node) {
		if (outNodes.contains(node)) {
			return outNodes.remove(node);
		} else {
			return false;
		}
	}
	
	public void addInConnector(BasicBlockNodeConnector node) {
		this.inConnectors.add(node);
	}
	
	public void addOutConnector(BasicBlockNodeConnector node) {
		this.outConnectors.add(node);
	}
	
	public BasicBlockNodeConnector getInConnector(int index) {
		if (index < this.getInConnectorsSize()) {
			return this.inConnectors.get(index);
		} else {
			return null;
		}
	}
	
	public BasicBlockNodeConnector getOutConnector(int index) {
		if (index < this.getOutConnectorsSize()) {
			return this.outConnectors.get(index);
		} else {
			return null;
		}
	}
	
	public void setInConnector(int index, BasicBlockNodeConnector in) {
		this.inConnectors.set(index, in);
	}
	
	public void setOutConnector(int index, BasicBlockNodeConnector out) {
		this.outConnectors.set(index, out);
	}
	
	public int getInConnectorsSize() {
		return this.inConnectors.size();
	}
	
	public int getOutConnectorsSize() {
		return this.outConnectors.size();
	}
	
	public ArrayList<BasicBlockNodeConnector> getInConnectors() {
		return this.inConnectors;
	}
	
	public ArrayList<BasicBlockNodeConnector> getOutConnectors() {
		return this.outConnectors;
	}
	
	public boolean containsConnector(BasicBlockNodeConnector node) {
		if (inConnectors.contains(node) || outConnectors.contains(node)) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean containsNodeInConnectors(BasicBlockNode node) {
		for (BasicBlockNodeConnector connector: this.outConnectors) {
			if (connector.goesToNode(node) || connector.goesFromNode(node)) {
				return true;
			}
		}
		for (BasicBlockNodeConnector connector: this.inConnectors) {
			if (connector.goesToNode(node) || connector.goesFromNode(node)) {
				return true;
			}
		}
		return false;
	}
	
	public ASMInstruction removeFromInConnectors(BasicBlockNode node) {
		ASMInstruction removed = null;
		for (int i = 0; i < this.inConnectors.size(); i++) {
			BasicBlockNodeConnector temp = this.inConnectors.get(i);
			if (temp.goesFromNode(node) || temp.goesToNode(node)) {
				removed = temp.goesFrom();
				this.inConnectors.remove(temp);
				i--;
			}
		}
		return removed;
	}
	
	public ASMInstruction removeFromOutConnectors(BasicBlockNode node) {
		ASMInstruction removed = null;
		
		for (int i = 0; i < this.outConnectors.size(); i++) {
			BasicBlockNodeConnector temp = this.outConnectors.get(i);
			if (temp.goesFromNode(node) || temp.goesToNode(node)) {
				removed = temp.goesTo();
				this.outConnectors.remove(temp);
				i--;
			}
		}
		return removed;
	}
	
	public void updateBasicBlock(BasicBlock block) {
		this.block = block;
	}
	
	public void setIsSubroutine(boolean subrtStatus) {
		this.subrtStatus = subrtStatus;
	}
	
	public boolean isSubroutine() {
		return this.subrtStatus;
	}
	
	public void setIsFunction(boolean functionStatus) {
		this.functionStatus = functionStatus;
	}
	
	public boolean isFunction() {
		return this.functionStatus;
	}
	
	public void setIsSimpleLoop(boolean simpleLoopStatus) {
		this.simpleLoopStatus = simpleLoopStatus;
	}
	
	public boolean isSimpleLoop() {
		return this.simpleLoopStatus;
	}
	
	public String toString() {
		String inNodeString = "";
		for (int i = 0; i < inNodes.size(); i++) {
			BasicBlockNode node = inNodes.get(i);
			inNodeString += node.getName();
			if (i < inNodes.size() - 1) {
				inNodeString += ", ";
			}
		}
		
		String outNodeString = "";
		for (int i = 0; i < outNodes.size(); i++) {
			BasicBlockNode node = outNodes.get(i);
			outNodeString += node.getName();
			if (i < outNodes.size() - 1) {
				outNodeString += ", ";
			}
		}
		
		String output = this.name + " " + block.toString() + ":     InNodes {" + inNodeString + "}     OutNodes {" + outNodeString + "}";
		
		return output;
	}
	
	public String toString(LinkedList<ASMInstruction> instructions) {
		String inNodeString = "";
		for (int i = 0; i < inNodes.size(); i++) {
			BasicBlockNode node = inNodes.get(i);
			inNodeString += node.getBlock().toString();
			if (i < inNodes.size() - 1) {
				inNodeString += ", ";
			}
		}
		
		String outNodeString = "";
		for (int i = 0; i < outNodes.size(); i++) {
			BasicBlockNode node = outNodes.get(i);
			outNodeString += node.getBlock().toString();
			if (i < outNodes.size() - 1) {
				outNodeString += ", ";
			}
		}
		
		String startInstruction = "";
		String endInstruction = "";
		//Object start = instructions.get(getBlockStart()).getArgument();
		//Object end = instructions.get(getBlockEnd()).getArgument();
		Object start = this.getInstruction(0).getArgument();
		Object end = this.getInstruction(this.getInstructionListSize() - 1).getArgument();
		if (start != null) {
			startInstruction = start.toString();
		}
		if (end != null) {
			endInstruction = end.toString();
		}
		
		String output = this.name + " " + block.toString() + ":     InNodes {" + inNodeString + "}     OutNodes {" + outNodeString + "} - " + startInstruction + " " + endInstruction;
		
		return output;
	}
}
