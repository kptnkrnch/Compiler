package asmCodeGenerator;

public class IdentifierFactory {
	private static int labelSequenceNumber = 0;

	private int labelNumber;

	public IdentifierFactory(String userPrefix) {
		labelSequenceNumber++;
		labelNumber = labelSequenceNumber;
	}

	public static String makeIdentifier(String identifier) {
		identifier = "!" + identifier + "-" + labelSequenceNumber;
		labelSequenceNumber++;
		return identifier;
	}
}
