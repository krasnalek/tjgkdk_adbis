package pl.polsl.adbis2017;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.polsl.adbis2017.model.Queries;
import pl.polsl.adbis2017.services.QueryGenerator;
import pl.polsl.adbis2017.services.TimeCounter;
import pl.polsl.adbis2017.services.VisDicParser;

public class Main {

	public static void createStatisticalData(String file, Integer count) {
		String line, item;
		List<String> lines;
		Matcher matcher1, matcher2;
		Random random = new Random();
		Set<String> synsets = new TreeSet<>();
		Set<String> literals = new TreeSet<>();
		Set<String> literalSenses = new TreeSet<>();
		Pattern pattern1 = Pattern.compile("<ID>[^<]+</ID>");
		Pattern pattern2 = Pattern
				.compile("<LITERAL>[^<]+<SENSE>[^<]+</SENSE></LITERAL>");

		try (BufferedWriter bwS = Files.newBufferedWriter(
				Paths.get("synsets" + count + ".txt"),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				BufferedWriter bwL = Files.newBufferedWriter(
						Paths.get("literals" + count + ".txt"),
						StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				BufferedWriter bwLS = Files.newBufferedWriter(
						Paths.get("literal_senses" + count + ".txt"),
						StandardOpenOption.CREATE, StandardOpenOption.WRITE);) {
			lines = Files.readAllLines(Paths.get(file));
			while (synsets.size() < count) {
				line = lines.get(random.nextInt(lines.size()));
				matcher1 = pattern1.matcher(line);
				while (matcher1.find()) {
					item = matcher1.group().replaceAll("</?ID>", "");
					if (!item.contains("pwn")) {
						synsets.add(item);
					}
				}
			}
			for (String synset : synsets) {
				bwS.write(synset);
				bwS.newLine();
			}
			while (literals.size() < count) {
				line = lines.get(random.nextInt(lines.size()));
				matcher1 = pattern1.matcher(line);
				while (matcher1.find()) {
					item = matcher1.group().replaceAll("</?ID>", "");
					if (!item.contains("pwn")) {
						matcher2 = pattern2.matcher(line);
						while (matcher2.find()) {
							literals.add(matcher2.group()
									.replaceAll("</?LITERAL>", "")
									.replaceAll("<SENSE>[^<]+</SENSE>", ""));
							break;
						}
					}

				}
			}
			for (String literal : literals) {
				bwL.write(literal);
				bwL.newLine();
			}
			while (literalSenses.size() < count) {
				line = lines.get(random.nextInt(lines.size()));
				matcher1 = pattern1.matcher(line);
				while (matcher1.find()) {
					item = matcher1.group().replaceAll("</?ID>", "");
					if (!item.contains("pwn")) {
						matcher2 = pattern2.matcher(line);
						while (matcher2.find()) {
							literalSenses.add(matcher2.group()
									.replaceAll("</?LITERAL>", "")
									.replace("<SENSE>", ";")
									.replace("</SENSE>", ""));
							break;
						}
					}
				}
			}
			for (String literalSense : literalSenses) {
				bwLS.write(literalSense);
				bwLS.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void executeComplexQueries(boolean basic, String synset,
			String literal, String sense) {
		QueryGenerator queryGenerator = new QueryGenerator();

		for (int i = 0; i < Queries.COMPLEX_QUERIES.length; i++) {
			queryGenerator.sendComplexSQLQuery(Queries.COMPLEX_QUERIES[i],
					basic, synset, literal, sense);
			System.out.println(String.format(
					"Time (ms): %d, Time (s): %.2f, query no. %d",
					queryGenerator.getTimeCounter().getTimeDiff(),
					queryGenerator.getTimeCounter().getTimeDiffSec(), i));
		}
	}

	public static void executeComplexRegexQueries(String file, String synset,
			String literal, String sense) {
		QueryGenerator queryGenerator = new QueryGenerator();

		for (int i = 0; i < Queries.COMPLEX_QUERIES.length; i++) {
			queryGenerator.sendComplexRegexQuery(Queries.COMPLEX_QUERIES[i],
					file, synset, literal, sense);
			System.out.println(String.format(
					"Time (ms): %d, Time (s): %.2f, query no. %d",
					queryGenerator.getTimeCounter().getTimeDiff(),
					queryGenerator.getTimeCounter().getTimeDiffSec(), i));
		}
	}

	public static void executeSimpleSQLQueries(boolean basic) {
		QueryGenerator queryGenerator = new QueryGenerator();

		for (int i = 0; i < Queries.SIMPLE_QUERIES.length; i++) {
			queryGenerator.sendSimpleSQLQuery(Queries.SIMPLE_QUERIES[i], basic);
			System.out.println(String.format(
					"Time (ms): %d, Time (s): %.2f, query no. %d",
					queryGenerator.getTimeCounter().getTimeDiff(),
					queryGenerator.getTimeCounter().getTimeDiffSec(), i));
		}
	}

	public static void executeSimpleXPathQueries(String file) {
		QueryGenerator queryGenerator = new QueryGenerator();

		for (int i = 0; i < Queries.SIMPLE_XPATH_QUERIES.length; i++) {
			queryGenerator.sendSimpleXPathQuery(
					Queries.SIMPLE_XPATH_QUERIES[i], file);
			System.out.println(String.format(
					"Time (ms): %d, Time (s): %.2f, query no. %d",
					queryGenerator.getTimeCounter().getTimeDiff(),
					queryGenerator.getTimeCounter().getTimeDiffSec(), i));
		}
	}

	public static void executeStatisticalQueries(String file,
			String synsetFile, String literalFile, String literalSenseFile) {
		List<String> synsets, literals, literalSenses;
		QueryGenerator queryGenerator = new QueryGenerator();

		try {
			synsets = Files.readAllLines(Paths.get(synsetFile));
			literals = Files.readAllLines(Paths.get(literalFile));
			literalSenses = Files.readAllLines(Paths.get(literalSenseFile));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		for (int i = 0; i < Queries.STATS_QUERIES_S.length; i++) {
			for (String synset : synsets) {
				queryGenerator.sendComplexSQLQuery(Queries.STATS_QUERIES_S[i],
						true, synset);
				System.out.println(String.format(
						"Time (ms): %d, Time (s): %.2f, query no. %d",
						queryGenerator.getTimeCounter().getTimeDiff(),
						queryGenerator.getTimeCounter().getTimeDiffSec(), i));
				queryGenerator.sendComplexSQLQuery(Queries.STATS_QUERIES_S[i],
						false, synset);
				System.out.println(String.format(
						"Time (ms): %d, Time (s): %.2f, query no. %d",
						queryGenerator.getTimeCounter().getTimeDiff(),
						queryGenerator.getTimeCounter().getTimeDiffSec(), i));
				queryGenerator.sendComplexRegexQuery(
						Queries.STATS_QUERIES_S[i], file, synset);
				System.out.println(String.format(
						"Time (ms): %d, Time (s): %.2f, query no. %d",
						queryGenerator.getTimeCounter().getTimeDiff(),
						queryGenerator.getTimeCounter().getTimeDiffSec(), i));
			}
			System.out.println("\r\n=========================\r\n");
		}
		System.out.println("\r\n*************************\r\n");
		for (int i = 0; i < Queries.STATS_QUERIES_L.length; i++) {
			for (String literal : literals) {
				queryGenerator.sendComplexSQLQuery(Queries.STATS_QUERIES_L[i],
						true, null, literal);
				System.out.println(String.format(
						"Time (ms): %d, Time (s): %.2f, query no. %d",
						queryGenerator.getTimeCounter().getTimeDiff(),
						queryGenerator.getTimeCounter().getTimeDiffSec(), i));
				queryGenerator.sendComplexSQLQuery(Queries.STATS_QUERIES_L[i],
						false, null, literal);
				System.out.println(String.format(
						"Time (ms): %d, Time (s): %.2f, query no. %d",
						queryGenerator.getTimeCounter().getTimeDiff(),
						queryGenerator.getTimeCounter().getTimeDiffSec(), i));
				queryGenerator.sendComplexRegexQuery(
						Queries.STATS_QUERIES_L[i], file, null, literal);
				System.out.println(String.format(
						"Time (ms): %d, Time (s): %.2f, query no. %d",
						queryGenerator.getTimeCounter().getTimeDiff(),
						queryGenerator.getTimeCounter().getTimeDiffSec(), i));
			}
			System.out.println("\r\n=========================\r\n");
		}
		System.out.println("\r\n*************************\r\n");
		for (int i = 0; i < Queries.STATS_QUERIES_LS.length; i++) {
			for (String literalSense : literalSenses) {
				queryGenerator.sendComplexSQLQuery(Queries.STATS_QUERIES_LS[i],
						true, null, literalSense.split(";")[0],
						literalSense.split(";")[1]);
				System.out.println(String.format(
						"Time (ms): %d, Time (s): %.2f, query no. %d",
						queryGenerator.getTimeCounter().getTimeDiff(),
						queryGenerator.getTimeCounter().getTimeDiffSec(), i));
				queryGenerator.sendComplexSQLQuery(Queries.STATS_QUERIES_LS[i],
						false, null, literalSense.split(";")[0],
						literalSense.split(";")[1]);
				System.out.println(String.format(
						"Time (ms): %d, Time (s): %.2f, query no. %d",
						queryGenerator.getTimeCounter().getTimeDiff(),
						queryGenerator.getTimeCounter().getTimeDiffSec(), i));
				queryGenerator.sendComplexRegexQuery(
						Queries.STATS_QUERIES_LS[i], file, null,
						literalSense.split(";")[0], literalSense.split(";")[1]);
				System.out.println(String.format(
						"Time (ms): %d, Time (s): %.2f, query no. %d",
						queryGenerator.getTimeCounter().getTimeDiff(),
						queryGenerator.getTimeCounter().getTimeDiffSec(), i));
			}
			System.out.println("\r\n=========================\r\n");
		}
		System.out.println("\r\n*************************\r\n");
	}

	public static void loadDatabases(String[] args) {
		VisDicParser parser = new VisDicParser();
		TimeCounter timeCounter = new TimeCounter();
		QueryGenerator queryGenerator = new QueryGenerator();

		try {
			timeCounter.saveSTime();
			parser.parseVisDic(args[1]);
			timeCounter.saveETime();
			System.out.println(String.format(
					"Time (ms): %d, Time (s): %.2f, load",
					timeCounter.getTimeDiff(), timeCounter.getTimeDiffSec()));
			queryGenerator.loadDataIntoSQLBasic(parser.getSynsetNodes());
			System.out.println(String.format(
					"Time (ms): %d, Time (s): %.2f, basic", queryGenerator
							.getTimeCounter().getTimeDiff(), queryGenerator
							.getTimeCounter().getTimeDiffSec()));
			queryGenerator.loadDataIntoSQLMod(parser.getSynsetNodes());
			System.out.println(String.format(
					"Time (ms): %d, Time (s): %.2f, mod", queryGenerator
							.getTimeCounter().getTimeDiff(), queryGenerator
							.getTimeCounter().getTimeDiffSec()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			switch (args[0]) {
			case "-ld":
				loadDatabases(args);
				break;
			case "-sq":
				executeSimpleSQLQueries(Boolean.parseBoolean(args[1]));
				break;
			case "-cq":
				executeComplexQueries(Boolean.parseBoolean(args[1]), args[2],
						args[3], args[4]);
				break;
			case "-sx":
				executeSimpleXPathQueries(args[1]);
				break;
			case "-cx":
				executeComplexRegexQueries(args[1], args[2], args[3], args[4]);
				break;
			case "-sv":
				simplifyVisdic(args);
				break;
			case "-st":
				executeStatisticalQueries(args[1], args[2], args[3], args[4]);
				break;
			case "-cs":
				createStatisticalData(args[1], Integer.parseInt(args[2]));
				break;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Missing program arguments. See manual");
		}
	}

	public static void simplifyVisdic(String[] args) {
		VisDicParser parser = new VisDicParser();

		try {
			parser.parseVisDic(args[1]);
			parser.writeNewVisDic(args[2]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
