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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LogChecker {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		List<Path> logFilesList = LogChecker.getLogFileList(scanner);
		String pattern = LogChecker.getPattern(scanner);
		scanner.close();
		String outputFolderString = Paths.get(".").toAbsolutePath().toString();
		Path outputFile = Paths.get(outputFolderString.substring(0, outputFolderString.length() -1) + "output.txt");
		try {
			Files.newBufferedWriter(outputFile);
		} catch (IOException e) {
			System.out.println("Error on creating output file");
			e.printStackTrace();
		}
		LogChecker.check(logFilesList, outputFile, pattern);
		System.out.println("Checking done! Open output.txt to view result");
	}
	
	private static void check(List<Path> logFilesList, Path outputFile, String pattern) {
		for (Path logFile : logFilesList) {
			try {
				LogChecker.checkFile(logFile, pattern, outputFile);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			
		}
		
		
	}
	
	private static void checkFile(Path fileToCheck, String pattern, Path outputFile) throws IOException {
		System.out.println("Checking file " + fileToCheck.toString() + System.lineSeparator());
		// we will be using explicitly charset ISO_8859_1 to avoid java.nio.charset.MalformedInputException 'cause ISO_8859_1 is an all-inclusive charset
		try(BufferedReader bufferReader = Files.newBufferedReader(fileToCheck, StandardCharsets.ISO_8859_1); BufferedWriter bufferedWriter = Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
			String line = null;
			int lineNumber = 0;
			while ((line = bufferReader.readLine()) != null) {
				++lineNumber;
				if (line.toUpperCase().contains(pattern.toUpperCase())) {
					bufferedWriter.write(fileToCheck.toString() + ", line: " + lineNumber + System.lineSeparator());
				}
			}
			
		}
	}
	
	
	private static List<Path> getLogFileList(Scanner scanner) {
		System.out.println("Please enter full path to log folder, or just leave it empy to use current folder:" + System.lineSeparator());
		String userInputLogFolderPath = scanner.nextLine();
		Path workingDirectory = Paths.get(".").toAbsolutePath();
		//scanner.close();
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
	
	private static String getPattern(Scanner scanner) {
		System.out.println("Enter string to check:");
		String pattern = scanner.nextLine();
		return pattern;
	}
}
