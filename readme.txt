Running:
	Note: for sample pika code files, see input/tests/*
	
	in cmd or terminal, use:
		java -jar compiler.jar <filename.pika> <optional: output folder for compiled programs>
	to compile a pika programs
	
	to run the pika program, call:
		AbstractStackEmulator.exe <filename.asm>
	where the filename.asm is the output of the compiler
	
Compiling:
	1. import the project into eclipse as a new project
	2. /src/applications/PikaCompiler.java is the main runnable source file for compiling pika code (use this for the run configuration)
	3. run PikaCompiler.java as a Java application (right click + run as...)