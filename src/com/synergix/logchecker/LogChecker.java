package com.synergix.logchecker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class LogChecker {
	static final List<String> sqlCommonPattern = Arrays.asList("UPDATE", "SELECT", "DELETE FROM");
	public static final String QUERY_PARAMS_LIST_ANNOTATION = "params";

	public static void main(String[] args) {
		
		Scanner scanner = new Scanner(System.in);
		List<Path> logFilesList = LogChecker.getLogFileList(scanner);
		List<String> patternList = LogChecker.getPattern(scanner);
		//List<String> exceptPattern = LogChecker.getExceptPattern(scanner);
		scanner.close();
		String outputFolderString = Paths.get(".").toAbsolutePath().toString();
		String outputFileName = LogChecker.getCurrentDateTimeString() + "-output.txt";
		Path outputFile = Paths.get(outputFolderString.substring(0, outputFolderString.length() - 1) + outputFileName);
		try {
			Files.newBufferedWriter(outputFile);
		} catch (IOException e) {
			System.out.println("Error on creating output file");
			e.printStackTrace();
		}
		LogChecker.check(logFilesList, outputFile, patternList);
		System.out.println("Checking done! Open " + outputFileName + " to view result");
	}

	private static void check(List<Path> logFilesList, Path outputFile, List<String> patternList) {
		for (Path logFile : logFilesList) {
			try {
				LogChecker.checkFile(logFile, patternList, outputFile);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

		}

	}

	private static void checkFile(Path fileToCheck, List<String> patternList, Path outputFile) throws IOException {
		System.out.println("Checking file " + fileToCheck.toString() + System.lineSeparator());
		// we will be using explicitly charset ISO_8859_1 to avoid
		// java.nio.charset.MalformedInputException 'cause ISO_8859_1 is an
		// all-inclusive charset
		try (BufferedReader bufferReader = Files.newBufferedReader(fileToCheck, StandardCharsets.ISO_8859_1);
				BufferedWriter bufferedWriter = Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE,
						StandardOpenOption.APPEND)) {
			String line = null;
			int lineNumber = 0;
			boolean firstLineInFileFound = true;
			while ((line = bufferReader.readLine()) != null) {
				++lineNumber;
				boolean lineContainsAllPattern = true;
				for (String pattern : patternList) {
					if (!line.toUpperCase().contains(pattern.toUpperCase())) {
						lineContainsAllPattern = false;
						break;
					}
				}
				if (lineContainsAllPattern) {
					if (firstLineInFileFound) {
						firstLineInFileFound = false;
						bufferedWriter.write(System.lineSeparator() + System.lineSeparator() + fileToCheck.toString());
					}
					bufferedWriter.write(formatOutputLine(line, lineNumber));
				}
			}

		}
	}

	private static List<Path> getLogFileList(Scanner scanner) {
		System.out.println("Please enter full path to log folder, or just leave it empy to use current folder:"
				+ System.lineSeparator());
		String userInputLogFolderPath = scanner.nextLine();
		Path workingDirectory = Paths.get(".").toAbsolutePath();
		// scanner.close();
		if (userInputLogFolderPath != null && !userInputLogFolderPath.trim().isEmpty()) {
			workingDirectory = Paths.get(userInputLogFolderPath);
		}
		List<Path> listOfLogFiles = new ArrayList<>();
		try {
			Files.walkFileTree(workingDirectory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
					if (Files.isRegularFile(file)) {
						listOfLogFiles.add(file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}
		return listOfLogFiles;
	}

	private static List<String> getPattern(Scanner scanner) {
		System.out.println("Enter string to check (multiple key will be separated by comma):");
		String pattern = scanner.nextLine();
		List<String> patternList = new ArrayList<> (Arrays.asList(pattern.split(",")));
		patternList.removeIf(item -> item.trim().isEmpty());
		return patternList;
	}

	private static String getCurrentDateTimeString() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM_HH-mm-ss");
		return dtf.format(LocalDateTime.now());
	}

	/*
	private static List<String> getExceptPattern(Scanner scanner) {
		System.out.println("Enter exception string to check (multiple key will be separated by comma):");
		String pattern = scanner.nextLine();
		List<String> patternList = new ArrayList<> (Arrays.asList(pattern.split(",")));
		patternList.removeIf(item -> item.trim().isEmpty());
		return patternList;
	}
	*/
	
	private static boolean doesLineSeemToBeASQLQuery(String line) {
		for (String dmlPattern : sqlCommonPattern) {
			if (line.contains(dmlPattern) && line.contains(QUERY_PARAMS_LIST_ANNOTATION)) {
				return true;
			}
		}
		return false;
	}
	//System.lineSeparator() + "line: "  + System.lineSeparator() + LogChecker.formatSqlLogLine(line)
	private static String formatOutputLine(String queryLog, int lineNumber) {
		String outputLine = System.lineSeparator() + "line: " + lineNumber;
		if (doesLineSeemToBeASQLQuery(queryLog)) {
			String[] splitedDateTimeAndQuery = splitDatetimeAndQuery(queryLog);
			outputLine += " " + splitedDateTimeAndQuery[0] + System.lineSeparator() + splitedDateTimeAndQuery[1];
		} else {
			outputLine = outputLine + System.lineSeparator() + queryLog;
		}
		int indexOfWhere = outputLine.indexOf("WHERE");
		outputLine = outputLine.substring(0, indexOfWhere) + System.lineSeparator() + outputLine.substring(indexOfWhere);
		return outputLine;
	}
	
	private static String[] splitDatetimeAndQuery(String queryLog) {
		String dateTime = null, query = null;
		for (String sqlKeyWord : sqlCommonPattern) {
			if (queryLog.contains(sqlKeyWord)) {
				dateTime = queryLog.substring(0, queryLog.indexOf("]") +1);
				String rawQuery = queryLog.substring(queryLog.indexOf(sqlKeyWord));
				query = putParametersInCorrectPosition(rawQuery);
				break;
			}
		}
		String[] result = {dateTime, query};
		return result;
	}
	
	private static String putParametersInCorrectPosition(String rawQuery) {
		String queryPart = rawQuery.substring(0, rawQuery.indexOf("["));
		String paramsPart = rawQuery.substring(rawQuery.indexOf("params=") + 7, rawQuery.length() -1);
		String[] paramList = paramsPart.split(", ");
		// now for each question mark in queryPart, replace with parameter
		for (int i = 0; i < paramList.length; i++) {
			queryPart = queryPart.replaceFirst(Pattern.quote("?"), paramList[i]);
		}
		return queryPart;
	}
}
