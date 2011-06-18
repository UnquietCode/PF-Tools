/*
	Copyright 2011 Vodori Inc.

	The following code is provided "as is" and without warranty of any kind.
 */

package com.vodori.blf.pftools;


import com.sun.istack.internal.NotNull;
import static com.vodori.blf.pftools.PFUtils.*;

import java.io.*;
import java.lang.Exception;
import java.lang.String;
import java.util.*;

/**
 * @author	Ben Fagin
 * @version	5/30/2011
 *
 *
 * The remap method takes in two files. The first is the 'template' and the second is the 'edit' file to be modified.
 * All of the files should be provided as String paths to the current or future file.
 * A map is passed in which remaps codes in the edit file from one code to another. The message is preserved.
 * Any code not remapped and not present in the template file WILL BE DELETED.
 *
 * A third file is the 'output' file where the modified edit file will be written.
 *
 * Due to the nature of the remapping, comments in the edit file will not be preserved.
 * Any relevant comments should be retained separately.
 *
 * Comments in the template file of the form "#comment" will be copied to the edit file.
 * To prevent a comment from being copied, use the "!comment" notation, which is also available to property files.
 * Comments of this form will be skipped when rendering the output file.
 */
public class PFRemapper {
	static final String NL = System.getProperty("line.separator");
	static final String BOM = "\uFEFF";

	private static List<String> templateFile;
	private static Map<String, String> editFile;
	private static List<String> outputFile;
	private static Map<String, Set<String>> remappings;


	/*
		Initializer for each run.
		Obviously not thread safe.
	 */
	private static void initialize() {
		templateFile = new ArrayList<String>();
		editFile = new HashMap<String, String>();
		outputFile = new ArrayList<String>();
	}

	/*
		Cleanup to occur on exiting the remap method.
	 */
	private static void cleanup() {
		templateFile = null;
		editFile = null;
		outputFile = null;
		remappings = null;
	}

	/*
		Main method.
	 */
	public static void remap(String templateFile, String editFile, String outputFile, Map<String, Set<String>> remappings) {
		Scanner scanner = null;

		try {
			// setup
			if (templateFile == null || editFile == null || outputFile == null) {
				throw new PFRemapperException("Input file paths cannot be null.");
			}

			if (remappings == null) {
				PFRemapper.remappings = new HashMap<String, Set<String>>();
			} else {
				PFRemapper.remappings = remappings;
			}

			initialize();

			// open the template file
			try {
				scanner = getReader(templateFile);
			} catch (Exception ex) {
				throw new PFRemapperException("Could not open template file for reading.", ex);
			}

			// read in the template file
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				// Have to consume the leading BOM if present. Thanks Java!
				if (line.startsWith(BOM)) { line = line.substring(1); }

				if (isMultiLine(line)) {
					// consume additional connected lines
					while (true) {
						if (!scanner.hasNextLine()) {
							throw new PFRemapperException("Unexpected end of template file.");
						}

						String nextLine = scanner.nextLine();
						line += nextLine;

						if (isMultiLine(nextLine)) {
							continue;
						} else {
							break;
						}
					}
				}

				PFRemapper.templateFile.add(line);
			}
			scanner.close();

			// open the edit file
			try {
				scanner = getReader(editFile);
			} catch (Exception ex) {
				throw new PFRemapperException("Could not open edit file for reading.", ex);
			}

			// read in the edit file
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith(BOM)) { line = line.substring(1); }

				if (isComment(line) || line.trim().isEmpty()) {
					continue;
				} else if (isMultiLine(line)) {
					// consume additional connected lines
					while (true) {
						if (!scanner.hasNextLine()) {
							throw new PFRemapperException("Unexpected end of edit file.");
						}

						String nextLine = scanner.nextLine();
						line += nextLine;

						if (isMultiLine(nextLine)) {
							continue;
						} else {
							break;
						}
					}
				}

				String code = getCode(line);
				String message = getMessage(line);
				PFRemapper.editFile.put(code, message);
			}
			scanner.close();

			// construct the output file
			buildOutputFile();

			// write the output file
			try {
				Writer out = new BufferedWriter(
								new OutputStreamWriter(
									new FileOutputStream(
										new File(outputFile)), "UTF-8"));

				// append each line of the output file
				for (String line : PFRemapper.outputFile) {
					// recreate line breaks for multiline strings
					line = line.replaceAll("\\\\", "\\\\"+NL);
					out.append(line).append(NL);
				}

				out.flush();
				out.close();
			} catch (Exception ex) {
				throw new PFRemapperException("Error writing output file.", ex);
			}
		} catch (RuntimeException ex) {
			throw ex;
		} finally {
			if (scanner != null) {
				scanner.close();
			}

			cleanup();
		}
	}

	private ArrayList<String> readLines(String fileName, boolean ignoreComments) {
		Scanner scanner;
		ArrayList<String> lines = new ArrayList<String>();

		// open the file
		try {
			scanner = getReader(fileName);
		} catch (Exception ex) {
			throw new PFRemapperException("Could not open file for reading.", ex);
		}

		// read in the file
		boolean first = true;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			
			// Have to consume the leading BOM if present. Thanks Java!
			if (first) {
				if (line.startsWith(BOM)) {	line = line.substring(1); }
				first = false;
			}

			if ((ignoreComments && isComment(line)) || line.trim().isEmpty()) {
				continue;
			} else if (isMultiLine(line)) {

				// consume additional connected lines
				while (true) {
					if (!scanner.hasNextLine()) {
						throw new PFRemapperException("Unexpected end of file.");
					}

					String nextLine = scanner.nextLine();
					line += nextLine;

					if (isMultiLine(nextLine)) {
						continue;
					} else {
						break;
					}
				}
			}

			lines.add(line);
		}

		return lines;
	}

	/*
	  Convenience method for opening a remapping file and using its contents
	*/
	public static void remap(String templateFile, String editFile, String outputFile, String remappings) {
		try {
			Scanner scanner = getReader(remappings);
			StringBuilder sb = new StringBuilder();

			while (scanner.hasNext()) {
				sb.append(scanner.next());
			}

			remap(templateFile, editFile, outputFile, createMap(sb.toString()));
		} catch (Exception ex) {
			throw new PFRemapperException("Error opening remappings  file.", ex);
		}
	}

	private static void buildOutputFile() {
		for (String tLine : templateFile) {

			// only copy #comments
			if (isComment(tLine) || tLine.trim().isEmpty()) {
				if (!tLine.trim().startsWith("!")) {
					outputFile.add(tLine);
				}
			} else {
				String code = getCode(tLine);
				String lookupCode = null;

				// check first if a remapping exists
				if (remappings.containsKey(code)) {
					for (String alt : remappings.get(code)) {
						if (editFile.containsKey(alt)) {
							lookupCode = alt;
							break;
						}
					}
				} else {
					lookupCode = code;
				}

				// should not occur, but...
				if (lookupCode == null) {
					throw new PFRemapperException("Fatal error: Lookup code is null.");
				}

				String message = editFile.get(lookupCode);

				// if no message, this is more of a warning, but not fatal
				if (message == null) {
					System.err.println("No message found for code '"+ lookupCode +"'");
					continue;
				}

				// at this point the code and message are set (use the actual code, not the lookup code)
				outputFile.add(code+ "=" +message);
			}
		}
	}

	private static class PFRemapperException extends RuntimeException {
		PFRemapperException(String message) {
			super(message);
		}

		PFRemapperException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static @NotNull Map<String, Set<String>> createMap(String input) {
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();

		if (input == null) {
			return map;
		}

		input = input.trim();
		if (!input.startsWith("{") || !input.endsWith("}")) {
			throw new PFRemapperException("Parse error. Check your input string for consistency.");
		}

		String pairs[] = input.substring(1,input.length()-1).split(",");
		
		for (String pair : pairs) {
			String kv[] = pair.split(":");

			if (kv.length != 2) {
				throw new PFRemapperException("Parse error. Check your input string for consistency.");
			}

			addToSet(kv[0].trim(), kv[1].trim(), map);
		}

		return map;
	}


	/*
		If running from this file directly.

		Usage is:
			java PFRemapper templateFile editFile outputFile "{new1:old1, new2:old2}"

		where the remappings are optional.
	 */
	public static void main(String args[]) {
		if (args.length < 3) {
			System.err.println("Proper usage is:\n\tteplateFile editFile outputFile {optional:mappings}");
		}

		Map<String, Set<String>> remappings = null;
		if (args.length > 3) {
			remappings = createMap(args[3]);
		}

		remap(args[0], args[1], args[2], remappings);
	}
}