import os
import pdb

ASM_COMPILER_SRC_DIRECTORY = "D:\\Java\\workspace\\pika4\\src\\"
ASM_COMPILER_CMD = "java -cp D:\\Java\\workspace\\pika4\\bin applications.PikaCompiler"
PIKA_FILE_DIRECTORY = "D:\\Java\\workspace\\pika4\\input\\tests\\"
ASM_EMULATOR = "D:\\Java\\workspace\\pika4\\ASM_EMULATOR\\ASMEmu.exe"
TEST_DESCRIPTION_FILE = "D:\\Java\workspace\\pika4\\ASMTests\\testdictionary.dict"
ASM_FILE_DIRECTORY = "D:\\Java\\workspace\\pika4\\output\\tests\\"
OUTPUT_DIRECTORY = "D:\\Java\workspace\\pika4\\ASMTests\\Output\\"
EXPECTED_OUTPUT_DIRECTORY = "D:\\Java\\workspace\\pika4\\ASMTests\\ExpectedOutput\\"

class ASMTest:
	def __init__(self, testname, input, expectedOutput, expectFail):
		self.testname = testname
		self.input = input
		self.expectedOutput = expectedOutput
		self.expectFail = expectFail
		
	def toString(self):
		output = self.expectedOutput
		if self.expectFail:
			output = "Expected Fail"
		return self.testname + ": " + self.input + " " + output

def readTestDictionary():
	print("Reading Tests...")
	tests = []
	with open(TEST_DESCRIPTION_FILE, "r") as f:
		lines = f.readlines()	
		foundTest = False
		foundInput = False
		inFile = None
		outFile = None
		name = None
		expectFail = False
		for line in lines:
			if not foundTest:
				tokens = line.strip().split(" ")
				for token in tokens:
					if token is not None and token == "test:":
						foundTest = True
					elif token is not None and foundTest and token == "expectFail":
						expectFail = True
					elif token is not None and foundTest:
						name = token.strip()
			elif line is not None and not foundInput:
				inFile = line.strip()
				foundInput = True
			elif line is not None:
				outFile = line.strip()
				foundTest = False
				foundInput = False
				if inFile is not None and outFile is not None and name is not None:
					print("Read test: " + name)
					test = ASMTest(name, inFile, outFile, expectFail)
					tests.append(test)
				inFile = None
				outFile = None
				name = None
				expectFail = False
	print()
	return tests

def deleteOldTestResults():
	print("Deleting old test output...")
	filesToDelete = os.listdir(OUTPUT_DIRECTORY)
	
	for file in filesToDelete:
		if os.path.isfile(OUTPUT_DIRECTORY + file):
			print("Deleted: " + file)
			os.remove(OUTPUT_DIRECTORY + file)
	print()
	
def deleteOldASMPrograms():
	print("Deleting old ASM programs...")
	programsToDelete = os.listdir(ASM_FILE_DIRECTORY)
	
	for file in programsToDelete:
		if os.path.isfile(ASM_FILE_DIRECTORY + file):
			print("Deleted: " + file)
			os.remove(ASM_FILE_DIRECTORY + file)
	print()
	
def compileTests(tests):
	print("Compiling tests...")
	os.chdir(ASM_COMPILER_SRC_DIRECTORY)
	for test in tests:
		command = ASM_COMPILER_CMD + " " + PIKA_FILE_DIRECTORY + test.input + ".pika " + ASM_FILE_DIRECTORY
		print("Compiled: " + test.input + ".pika")
		if test.expectFail:
			os.system(command + " > nul 2>&1")
		else:
			os.system(command)
	print()

def runTests():
	tests = readTestDictionary()
	deleteOldTestResults()
	
	deleteOldASMPrograms()
	compileTests(tests)
	
	tests_failed = 0
	tests_passed = 0
	testsRan = 0
	
	failedTests = {}
	print("Starting tests...")
	for test in tests:
		print("Running test " + test.toString())
		command = ASM_EMULATOR + " " + ASM_FILE_DIRECTORY + test.input + ".asm > " + OUTPUT_DIRECTORY + test.testname + ".out"
		os.system(command)
		with open(EXPECTED_OUTPUT_DIRECTORY + test.expectedOutput, "r") as expected, open(OUTPUT_DIRECTORY + test.testname + ".out", "r") as actual:
			expected_lines = expected.readlines()
			actual_lines = actual.readlines()
			if len(actual_lines) != len(expected_lines):
				if test.expectFail:
					print("Pass")
					tests_passed += 1
				else:
					print("Fail")
					tests_failed += 1
					failedTests[test.testname] = "Expected %d lines; Actual: %d" % (len(expected_lines), len(actual_lines))
			else:
				passed = True
				for index in range(len(expected_lines)):
					if expected_lines[index] != actual_lines[index]:
						print("Fail")
						passed = False
						tests_failed += 1
						failedTests[test.testname] = "Error on Line %d:  Actual: \"%s\"; Expected: \"%s\"" % (index+1, actual_lines[index].strip(), expected_lines[index].strip())
						break
				if passed:
					print("Pass")
					tests_passed += 1
			testsRan += 1
			print()
	print("Number of Tests: %d   Passed: %d   Failed: %d" % (testsRan, tests_passed, tests_failed))
	if tests_failed > 0:
		print("Failed Tests:")
		for test, reason in failedTests.items():
			print("\t" + test + ":")
			print("\t\t" + reason)
	
runTests()