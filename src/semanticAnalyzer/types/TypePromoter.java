package semanticAnalyzer.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.nodeTypes.CastOperatorNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import tokens.CastToken;

public class TypePromoter {
	public static List<Type> findPromotions(Object key, List<Type> types) {
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(key);
		ArrayList<Type> tempTypes = new ArrayList<Type>(types.size());
		for (Type t : types) {
			tempTypes.add(t);
		}
		ArrayList<Type> tempTypes2 = new ArrayList<Type>(types.size());
		for (Type t : types) {
			tempTypes2.add(t);
		}
		int i = 0;
		if (key instanceof Punctuator && key == Punctuator.ASSIGN) {
			i += 1;
		}
		for (; i < tempTypes.size(); i++) {
			Collections.copy(tempTypes, types);
			List<Type> promotions = getImplicitPromotions(tempTypes.get(i));
			List<Type> signatureTypes = signatures.getArgumentNumberTypes(i);
			ArrayList<Type> validPromotions = new ArrayList<Type>();
			for (Type temp: promotions) {
				if (signatureTypes.contains(temp)) {
					validPromotions.add(temp);
				}
			}
			//ArrayList<Type> finalPromotions = new ArrayList<Type>();
			/*for (Type temp: validPromotions) {
				List<Type> tempPromotions = getImplicitPromotions(temp);
				if (tempPromotions != null && !tempPromotions.isEmpty()) {
					finalPromotions.add(temp);
					break;
				} else {
					finalPromotions.add(temp);
				}
			}*/
			int matchingSignatures = 0;
			Type matchedType = null;
			ArrayList<List<Type>> signatureTypes1 = new ArrayList<List<Type>>();
			for (Type promotion: validPromotions) {
				ArrayList<Type> tempSignatureTypes = new ArrayList<Type>();
				tempTypes.set(i, promotion);
				FunctionSignature testSignature = signatures.acceptingSignature(tempTypes);
				if (testSignature != null && testSignature.accepts(tempTypes)) {
					for (Type t : tempTypes) {
						tempSignatureTypes.add(t);
					}
					Collections.copy(tempSignatureTypes, tempTypes);
					signatureTypes1.add(tempSignatureTypes);
					matchedType = promotion;
					matchingSignatures++;
				}
			}
			if (signatureTypes1.size() == 1) {
				return signatureTypes1.get(0);
			} else if (signatureTypes1.size() > 1) {
				ArrayList<Integer> numPredecessors = new ArrayList<Integer>();
				for (int x = 0; x < signatureTypes1.size(); x++) {
					numPredecessors.add(0);
				}
				for (int x = 0; x < signatureTypes1.size(); x++) {
					for (int y = 0; y < signatureTypes1.size(); y++) {
						if (x != y) {
							List<Type> tempSigType1 = signatureTypes1.get(x);
							List<Type> tempSigType2 = signatureTypes1.get(y);
							if (isPredecessor(tempSigType1, tempSigType2)) {
								numPredecessors.set(x, numPredecessors.get(x) + 1);
							}
						}
					}
				}
				for (int signatureNumber = 0; signatureNumber < numPredecessors.size(); signatureNumber++) {
					int predecessor = numPredecessors.get(signatureNumber);
					if (predecessor == (numPredecessors.size() - 1)) {
						return signatureTypes1.get(signatureNumber);
					}
				}
				return null;
			}
			/*if (finalPromotions.size() > 1) {
				return null;
			} else if (finalPromotions.size() == 1) {
				tempTypes.set(i, finalPromotions.get(0));
				FunctionSignature testSignature = signatures.acceptingSignature(tempTypes);
				if (testSignature != null && testSignature.accepts(tempTypes)) {
					return tempTypes;
				}
			}*/
		}
		i = 0;
		if (key instanceof Punctuator && key == Punctuator.ASSIGN) {
			i += 1;
		}
		for (; i < tempTypes.size(); i++) {
			Collections.copy(tempTypes, types);
			List<Type> promotions1 = getImplicitPromotions(tempTypes.get(i));
			List<Type> signatureTypes1 = signatures.getArgumentNumberTypes(i);
			ArrayList<Type> validPromotions1 = new ArrayList<Type>();
			for (Type temp: promotions1) {
				if (signatureTypes1.contains(temp)) {
					validPromotions1.add(temp);
				}
			}
			/*ArrayList<Type> finalPromotions1 = new ArrayList<Type>();
			for (Type temp: validPromotions1) {
				List<Type> tempPromotions = getImplicitPromotions(temp);
				if (tempPromotions != null && !tempPromotions.isEmpty()) {
					finalPromotions1.add(temp);
					break;
				} else {
					finalPromotions1.add(temp);
				}
			}*/
			for (int n = i + 1; n < tempTypes.size(); n++) {
				Collections.copy(tempTypes2, tempTypes);
				List<Type> promotions2 = getImplicitPromotions(tempTypes2.get(i));
				List<Type> signatureTypes2 = signatures.getArgumentNumberTypes(i);
				ArrayList<Type> validPromotions2 = new ArrayList<Type>();
				for (Type temp: promotions2) {
					if (signatureTypes2.contains(temp)) {
						validPromotions2.add(temp);
					}
				}
				/*ArrayList<Type> finalPromotions2 = new ArrayList<Type>();
				for (Type temp: validPromotions2) {
					List<Type> tempPromotions = getImplicitPromotions(temp);
					if (tempPromotions != null && !tempPromotions.isEmpty()) {
						finalPromotions2.add(temp);
						break;
					} else {
						finalPromotions2.add(temp);
					}
				}*/
				/*if (finalPromotions1.size() > 1 || finalPromotions2.size() > 1) {
					return null;
				} else if (finalPromotions1.size() == 1 && finalPromotions2.size() == 1) {
					tempTypes2.set(i, finalPromotions1.get(0));
					tempTypes2.set(n, finalPromotions2.get(0));
					FunctionSignature testSignature = signatures.acceptingSignature(tempTypes2);
					if (testSignature != null && testSignature.accepts(tempTypes2)) {
						return tempTypes2;
					}
				}*/
				int matchingSignatures = 0;
				Type matchedType1 = null;
				Type matchedType2 = null;
				ArrayList<List<Type>> signatureTypes = new ArrayList<List<Type>>();
				for (Type promotion1: validPromotions1) {
					for (Type promotion2: validPromotions2) {
						ArrayList<Type> tempSignatureTypes = new ArrayList<Type>();
						tempTypes2.set(i, promotion1);
						tempTypes2.set(n, promotion2);
						FunctionSignature testSignature = signatures.acceptingSignature(tempTypes2);
						if (testSignature != null && testSignature.accepts(tempTypes2)) {
							for (Type t : tempTypes2) {
								tempSignatureTypes.add(t);
							}
							Collections.copy(tempSignatureTypes, tempTypes2);
							signatureTypes.add(tempSignatureTypes);
							matchingSignatures++;
						}
					}
				}
				if (signatureTypes.size() == 1) {
					tempTypes2.set(i, matchedType1);
					tempTypes2.set(n, matchedType2);
					return signatureTypes.get(0);
				} else if (signatureTypes.size() > 1) {
					ArrayList<Integer> numPredecessors = new ArrayList<Integer>();
					for (int x = 0; x < signatureTypes.size(); x++) {
						numPredecessors.add(0);
					}
					for (int x = 0; x < signatureTypes.size(); x++) {
						for (int y = 0; y < signatureTypes.size(); y++) {
							if (x != y) {
								List<Type> tempSigType1 = signatureTypes.get(x);
								List<Type> tempSigType2 = signatureTypes.get(y);
								if (isPredecessor(tempSigType1, tempSigType2)) {
									numPredecessors.set(x, numPredecessors.get(x) + 1);
								}
							}
						}
					}
					for (int signatureNumber = 0; signatureNumber < numPredecessors.size(); signatureNumber++) {
						int predecessor = numPredecessors.get(signatureNumber);
						if (predecessor == (numPredecessors.size() - 1)) {
							return signatureTypes.get(signatureNumber);
						}
					}
					return null;
				}
			}
		}
		return null;
	}
	
	public static List<Type> findArrayExpressionPromotions(List<Type> types) {
		
		Type type1 = types.get(0);
		boolean changed = true;
		boolean valid = true;
		ArrayList<Type> tempTypes = new ArrayList<Type>(types.size());
		for (Type t : types) {
			tempTypes.add(t);
		}
		while (changed) {
			changed = false;
			for (int i = 0; i < tempTypes.size(); i++) {
				List<Type> promotions = getImplicitPromotions(tempTypes.get(i));
				if (promotions != null && promotions.contains(type1)) {
					tempTypes.set(i, type1);
					changed = true;
				}
			}
		}
		for (Type t: tempTypes) {
			if (!(t.equals(type1))) {
				valid = false;
			}
		}
		if (!valid) {
			Type testType = PrimitiveType.INTEGER;
			while (testType != null) {
				Collections.copy(tempTypes, types);
				changed = true;
				valid = true;
				while (changed) {
					changed = false;
					for (int i = 0; i < tempTypes.size(); i++) {
						List<Type> promotions = getImplicitPromotions(tempTypes.get(i));
						if (promotions != null && promotions.contains(testType)) {
							tempTypes.set(i, testType);
							changed = true;
						}
					}
				}
				for (Type t: tempTypes) {
					if (!(t.equals(testType))) {
						valid = false;
					}
				}
				if (!valid) {
					if (testType.equals(PrimitiveType.INTEGER)) {
						testType = PrimitiveType.FLOAT;
					} else if (testType.equals(PrimitiveType.FLOAT)) {
						testType = PrimitiveType.RATIONAL;
					} else {
						testType = null;
					}
				} else {
					return tempTypes;
				}
			}
		}
		
		return null;
	}
	
	public static List<Type> getImplicitPromotions(Type type) {
		List<Type> types = Arrays.asList();
		if (type instanceof PrimitiveType) {
			PrimitiveType ptype = (PrimitiveType)type;
			if (ptype.equals(PrimitiveType.CHARACTER)) {
				types = Arrays.asList(PrimitiveType.INTEGER, PrimitiveType.FLOAT, PrimitiveType.RATIONAL);
			} else if (ptype.equals(PrimitiveType.INTEGER)) {
				types = Arrays.asList(PrimitiveType.FLOAT, PrimitiveType.RATIONAL);
			}
		}
		return types;
	}
	
	public static boolean isPredecessor(List<Type> predecessor, List<Type> successor) {
		if (predecessor.size() != successor.size()) {
			return false;
		}
		for (int i = 0; i < predecessor.size(); i++) {
			Type preType = predecessor.get(i);
			Type sucType = successor.get(i);
			List<Type> promotions = getImplicitPromotions(preType);
			if (!promotions.contains(sucType) && sucType != preType) {
				return false;
			}
		}
		return true;
	}
	
	public static ParseNode promoteType(ParseNode node, Type type) {
		CastOperatorNode castNode = CastOperatorNode.withChildren(CastToken.make(node.getToken()), node, type);
		return castNode;
	}
}
