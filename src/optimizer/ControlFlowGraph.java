package optimizer;

import java.util.ArrayList;
import java.util.LinkedList;

import asmCodeGenerator.codeStorage.ASMInstruction;

public class ControlFlowGraph extends ArrayList<BasicBlockNode>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 762332868261358454L;
	
	private BasicBlockNode startingBlock = null;
	
	public BasicBlockNode getStartingNode() {
		return this.startingBlock;
	}
	
	public void addNode(BasicBlockNode node) {
		if (startingBlock == null && node.getBlockStart() == 0) {
			startingBlock = node;
		}
		this.add(node);
	}
	
	public void removeNode(BasicBlockNode node) {
		for (BasicBlockNode temp: this) {
			if (temp.contains(node)) {
				temp.removeFromInNodes(node);
				temp.removeFromOutNodes(node);
			}
			if (temp.containsNodeInConnectors(node)) {
				ASMInstruction remove;
				remove = temp.removeFromInConnectors(node);
				if (remove != null) {
					//temp.removeInstruction(remove);
				}
				remove = temp.removeFromOutConnectors(node);
				if (remove != null) {
					//temp.removeInstruction(remove);
				}
			}
		}
		this.remove(node);
	}
	
	public BasicBlockNode getBlockNodeByStartIndex(int startIndex) {
		for (BasicBlockNode node : this) {
			if (node.getBlock().getStartIndex() == startIndex) {
				return node;
			}
		}
		return null;
	}
	
	public BasicBlockNode getBlockNodeByInstructionIndex(int index) {
		for (BasicBlockNode node : this) {
			if (node.getBlock().instructionIndexInBlock(index)) {
				return node;
			}
		}
		return null;
	}
	
	public String toString() {
		String graph = "";
		for (BasicBlockNode node: this) {
			graph += node.toString() + "\n";
		}
		return graph;
	}
	
	public String toString(LinkedList<ASMInstruction> instructions) {
		String graph = "";
		for (BasicBlockNode node: this) {
			graph += node.toString(instructions) + "\n";
		}
		return graph;
	}
}
