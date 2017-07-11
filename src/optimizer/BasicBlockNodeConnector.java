package optimizer;

import java.util.LinkedList;

import asmCodeGenerator.codeStorage.ASMInstruction;

public class BasicBlockNodeConnector {
	private BasicBlockNode goesFromNode;
	private BasicBlockNode goesToNode;
	private ASMInstruction goesFromInstruction;
	private int goesFromIndex;
	private ASMInstruction goesToInstruction;
	private int goesToIndex;
	
	public BasicBlockNodeConnector(BasicBlockNode goesFromNode, 
			BasicBlockNode goesToNode, 
			ASMInstruction goesFrom, 
			ASMInstruction goesTo, 
			LinkedList<ASMInstruction> instructions) {
		this.goesFromNode = goesFromNode;
		this.goesToNode = goesToNode;
		this.goesFromInstruction = goesFrom;
		this.goesToInstruction = goesTo;
		if (goesFrom != null) {
			this.goesFromIndex = instructions.indexOf(goesFrom);
		}
		if (goesTo != null) {
			this.goesToIndex = instructions.indexOf(goesTo);
		}
	}
	
	public BasicBlockNodeConnector(BasicBlockNode goesFromNode, 
			BasicBlockNode goesToNode, 
			int goesFromIndex, 
			int goesToIndex) {
		this.goesFromNode = goesFromNode;
		this.goesToNode = goesToNode;
		this.goesFromInstruction = null;
		this.goesToInstruction = null;
		this.goesFromIndex = goesFromIndex;
		this.goesToIndex = goesToIndex;
	}
	
	public BasicBlockNodeConnector(BasicBlockNodeConnector source) {
		this.goesFromNode = source.goesFromNode;
		this.goesToNode = source.goesToNode;
		this.goesFromInstruction = source.goesFromInstruction;
		this.goesToInstruction = source.goesToInstruction;
		//if (source.goesFromInstruction != null) {
		//	this.goesFromInstruction = new ASMInstruction(source.goesFromInstruction);
		//} else {
		//	this.goesFromInstruction = null;
		//}
		//if (source.goesToInstruction != null) {
		//	this.goesToInstruction = new ASMInstruction(source.goesToInstruction);
		//} else { 
		//	this.goesToInstruction = null;
		//}
		this.goesFromIndex = source.goesFromIndex;
		this.goesToIndex = source.goesToIndex;
	}
	
	public boolean goesToNode(BasicBlockNode node) {
		return this.goesToNode.equals(node);
	}
	
	public boolean goesFromNode(BasicBlockNode node) {
		return this.goesFromNode.equals(node);
	}
	
	public BasicBlockNode getGoesFromNode() {
		return this.goesFromNode;
	}
	
	public BasicBlockNode getGoesToNode() {
		return this.goesToNode;
	}
	
	public void setGoesFromNode(BasicBlockNode goesFromNode) {
		this.goesFromNode = goesFromNode;
	}
	
	public void setGoesToNode(BasicBlockNode goesToNode) {
		this.goesToNode = goesToNode;
	}
	
	public int indexOfGoesToInstruction() {
		return this.goesToIndex;
	}
	
	public int indexOfGoesFromInstruction() {
		return this.goesFromIndex;
	}
	
	public ASMInstruction goesTo() {
		return this.goesToInstruction;
	}
	
	public ASMInstruction goesFrom() {
		return this.goesFromInstruction;
	}
	
	public void setGoesTo(ASMInstruction goesTo) {
		this.goesToInstruction = goesTo;
		if (goesTo != null) {
			this.goesToIndex = -1;
		}
	}
	
	public void setGoesFrom(ASMInstruction goesFrom) {
		this.goesFromInstruction = goesFrom;
		if (goesFrom != null) {
			this.goesFromIndex = -1;
		}
	}
	
	public void setGoesTo(ASMInstruction goesTo, LinkedList<ASMInstruction> instructions) {
		this.goesToInstruction = goesTo;
		if (goesTo != null) {
			this.goesToIndex = instructions.indexOf(goesTo);
		}
	}
	
	public void setGoesFrom(ASMInstruction goesFrom, LinkedList<ASMInstruction> instructions) {
		this.goesFromInstruction = goesFrom;
		if (goesFrom != null) {
			this.goesFromIndex = instructions.indexOf(goesFrom);
		}
	}
}
