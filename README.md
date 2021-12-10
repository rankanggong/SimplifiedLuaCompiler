# Basic Lua Compiler By Java

## Intro:
Input string content as command. Scanner would read single letter and extract list of tokens. Parse tokens and convert to Abstract Syntax Tree. Semantic validation and initiate Java function.

## Scanner
- Read single letters, and compare content in buffer with key words stored in a hash map, or can be recognized as string, operator, name and parameter names.
- Extract tokens and store them in list.

## Parser
- We have several patterns here. Like binary operation, loops, goto expression, or function expression etc.
- Set up the AST with priorities.
- Sample of a AST:

			binary op
		/              |              \
               1             +       binary op
	  (Num::1) (Op::+)  /     |     \
                                      3     -     ….(other defined structures)
			    (Num::3)(Op::-)

## Interpreter
- Traverse AST, and apply static analysis.
- Semantic validation.
- Assign the value, do some computation, and execute java functions.	
