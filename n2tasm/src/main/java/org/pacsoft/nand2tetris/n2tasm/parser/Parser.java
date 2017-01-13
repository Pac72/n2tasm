package org.pacsoft.nand2tetris.n2tasm.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pacsoft.nand2tetris.n2tasm.AssemblerException;
import org.pacsoft.nand2tetris.n2tasm.SymbolTable;
import org.pacsoft.nand2tetris.n2tasm.instruction.Instruction;

public class Parser {
	private final static String TOK_COMMENT = "//";
	private final static Pattern A_INST_PATTERN = Pattern.compile("^\\s*@([0-9]+|[a-zA-Z_.$:][a-zA-Z_.$:0-9]*)\\s*$");
	private final static Pattern C_INST_PATTERN = Pattern.compile("^\\s*(([ADM]{1,3})\\s*=)?\\s*([-+ADM!01|&]{1,3})\\s*(;\\s*([EGJLMNPQT]{3}))?$");
	private final static Pattern L_INST_PATTERN = Pattern.compile("^\\s*\\(([a-zA-Z_.$:][a-zA-Z_.$:0-9]*)\\)\\s*$");

	private final SymbolTable symbolTable;
	private final List<Instruction> instructions;
	private final List<ParserException> errors;
	private int currLineNo;

	public Parser(SymbolTable symbolTable) {
		this.symbolTable = symbolTable;
		currLineNo = 0;
		instructions = new ArrayList<Instruction>();
		errors = new ArrayList<ParserException>();
	}

	public void parse(BufferedReader reader) throws AssemblerException, IOException {
		String line = reader.readLine();
		while (line != null) {
			currLineNo++;
			parseLine(line);
			line = reader.readLine();
		}
	}

	public boolean isSuccesful() {
		return errors.size() < 1;
	}

	public List<Instruction> getInstructions() {
		return instructions;
	}

	public List<ParserException> getErrors() {
		return errors;
	}

	private static String eraseComments(String rawline) {
		String result = rawline;
		int ii = rawline.indexOf(TOK_COMMENT);
		if (ii >= 0) {
			result = rawline.substring(0, ii);
		}
		return result.trim();
	}

	private void parseLine(String rawline) {
		String line;

		try {
			line = eraseComments(rawline);
	
			if (line.length() < 1) {
				return;
			}

			Matcher mm;
			
			mm = C_INST_PATTERN.matcher(line);
	
			if (mm.matches()) {
				Instruction instr = CInstructionParser.parse(mm.group(2), mm.group(3), mm.group(5));
				instructions.add(instr);
				return;
			}
	
			mm = A_INST_PATTERN.matcher(line);

			if (mm.matches()) {
				Instruction instr = AInstructionParser.parse(currLineNo, mm.group(1), symbolTable);
				instr.setLine(currLineNo);
				instructions.add(instr);
				return;
			}

			mm = L_INST_PATTERN.matcher(line);
			if (mm.matches()) {
				int address = instructions.size();
				symbolTable.defineLabel(currLineNo, mm.group(1), address);
				return;
			}

			throw new SyntaxErrorException("Invalid syntax \"" + rawline + "\"");

		} catch (ParserException pe) {
			pe.setLine(currLineNo);
			errors.add(pe);
		}
	}
}
