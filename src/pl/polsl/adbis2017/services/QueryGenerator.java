package pl.polsl.adbis2017.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pl.polsl.adbis2017.model.Literal;
import pl.polsl.adbis2017.model.Relation;
import pl.polsl.adbis2017.model.SynsetNode;

public class QueryGenerator {

	private class Counter {
		private int count = 0;

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

	}

	private static final String[] HYPONYMS = { "hiponimia" };
	private static final String[] HYPERNYMS = { "hypernym" };
	private static final String CONNECTION_URL_MOD = "jdbc:mysql://localhost:3306/plwn_mod";
	private static final String CONNECTION_URL_BASIC = "jdbc:mysql://localhost:3306/plwn_basic";
	private static final String INSERT_PATH_QUERY = "INSERT INTO paths(id, synset_path) values (?, ?)";
	private static final String INSERT_SYNSET_QUERY = "INSERT INTO synsets(id, synset_id) values (?, ?)";
	private static final String INSERT_LITERAL_QUERY = "INSERT INTO literals(id, literal, sense_id) values (?, ?, ?)";
	private static final String INSERT_SYNSET_LITERAL_QUERY = "INSERT INTO synset_literals(id_synset, id_literal) values (?, ?)";
	private static final String INSERT_RELATION_QUERY = "INSERT INTO relations(id, synset_id_p, synset_id_c, relation_type) values (?, ?, ?, ?)";
	private boolean withLog = false;

	private final TimeCounter timeCounter = new TimeCounter();

	private void closeConnection(Connection connection) throws SQLException {
		connection.close();
	}

	private boolean countPaths(PreparedStatement pstmt, Integer id, int pos,
			Counter counter) throws SQLException {
		ResultSet rs;
		boolean empty = true;
		List<Integer> dbIds = new ArrayList<>();

		pstmt.setInt(pos, id);
		rs = pstmt.executeQuery();
		while (rs.next()) {
			dbIds.add(rs.getInt(1));
		}
		for (int dbId : dbIds) {
			empty = false;
			if (countPaths(pstmt, dbId, pos, counter)) {
				counter.setCount(counter.getCount() + 1);
			}
		}
		dbIds.clear();
		return empty;
	}

	private boolean countPathsRegex(Map<String, String> doc, Pattern pattern,
			String id, Counter counter) {
		Matcher matcher;
		boolean empty = true;
		List<String> ids = new ArrayList<>();

		matcher = pattern.matcher(doc.get(id));
		while (matcher.find()) {
			ids.add(matcher.group().replaceAll("</?ILR>", "")
					.replaceAll("<TYPE>[^<]+</TYPE>", ""));
		}
		for (String dbId : ids) {
			empty = false;
			if (countPathsRegex(doc, pattern, dbId, counter)) {
				counter.setCount(counter.getCount() + 1);
			}
		}
		return empty;
	}

	private Connection establishConnection(String url)
			throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");

		return DriverManager.getConnection(url, "plwn", "plwn");
	}

	private Map<String, String> establishRegexConnection(String file)
			throws IOException {
		Matcher matcher;
		List<String> lines;
		Map<String, String> map = new TreeMap<>();
		Pattern pattern = Pattern.compile("<ID>[^<]+</ID>");

		lines = Files.readAllLines(Paths.get(file));
		for (String line : lines) {
			matcher = pattern.matcher(line);
			matcher.find();
			map.put(matcher.group().replaceAll("</?ID>", ""), line);
		}
		return map;
	}

	private Document establishSimpleXPathConnection(String file)
			throws FileNotFoundException, SAXException, IOException,
			ParserConfigurationException {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new FileInputStream(file));
	}

	private void followPathAndCountLength(PreparedStatement pstmt, Integer id,
			int pos, int count, Counter counter) throws SQLException {
		ResultSet rs;
		List<Integer> dbIds = new ArrayList<>();

		pstmt.setInt(pos, id);
		rs = pstmt.executeQuery();
		while (rs.next()) {
			dbIds.add(rs.getInt(1));
		}
		for (int dbId : dbIds) {
			count++;
			followPathAndCountLength(pstmt, dbId, pos, count, counter);
			if (count > counter.getCount()) {
				counter.setCount(count);
			}
			count--;
		}
		dbIds.clear();
	}

	private void followPathAndCountLengthRegex(Map<String, String> doc,
			Pattern pattern, String id, int count, Counter counter) {
		Matcher matcher;
		List<String> ids = new ArrayList<>();

		matcher = pattern.matcher(doc.get(id));
		while (matcher.find()) {
			ids.add(matcher.group().replaceAll("</?ILR>", "")
					.replaceAll("<TYPE>[^<]+</TYPE>", ""));
		}
		for (String newId : ids) {
			count++;
			followPathAndCountLengthRegex(doc, pattern, newId, count, counter);
			if (count > counter.getCount()) {
				counter.setCount(count);
			}
			count--;
		}
		ids.clear();
	}

	private boolean followPathAndPrint(PreparedStatement pstmt, Integer id,
			int pos, List<String> last) throws SQLException {
		ResultSet rs;
		boolean empty = true;
		List<Integer> dbIds = new ArrayList<>();

		pstmt.setInt(pos, id);
		rs = pstmt.executeQuery();
		while (rs.next()) {
			dbIds.add(rs.getInt(1));
		}
		for (int dbId : dbIds) {
			empty = false;
			if (followPathAndPrint(pstmt, dbId, pos, last)) {
				last.add(Integer.toString(dbId));
			}
		}
		dbIds.clear();
		return empty;
	}

	private boolean followPathAndPrintRegex(Map<String, String> doc,
			Pattern pattern, String id, List<String> last) {
		Matcher matcher;
		boolean empty = true;
		List<String> ids = new ArrayList<>();

		matcher = pattern.matcher(doc.get(id));
		while (matcher.find()) {
			ids.add(matcher.group().replaceAll("</?ILR>", "")
					.replaceAll("<TYPE>[^<]+</TYPE>", ""));
		}
		for (String dbId : ids) {
			empty = false;
			if (followPathAndPrintRegex(doc, pattern, dbId, last)) {
				last.add(dbId);
			}
		}
		return empty;
	}

	public TimeCounter getTimeCounter() {
		return timeCounter;
	}

	public void loadDataIntoSQLBasic(List<SynsetNode> synsetNodes) {
		int count;
		Connection con;
		Set<Literal> uniqueLiterals = new HashSet<>();
		Map<String, Integer> literalToDbId = new HashMap<>();
		Map<String, SynsetNode> synsetIdToDbId = new HashMap<>();
		PreparedStatement pstmtSynset, pstmtLiteral, pstmtRelation, pstmtSynsetLiteral;

		try {
			con = establishConnection(CONNECTION_URL_BASIC);
			pstmtRelation = con.prepareStatement(INSERT_RELATION_QUERY);
			pstmtSynsetLiteral = con
					.prepareStatement(INSERT_SYNSET_LITERAL_QUERY);
			pstmtSynset = con.prepareStatement(INSERT_SYNSET_QUERY);
			pstmtLiteral = con.prepareStatement(INSERT_LITERAL_QUERY);

			timeCounter.saveSTime();
			count = 0;
			for (SynsetNode synsetNode : synsetNodes) {
				count++;
				synsetNode.setDbId(count);
				pstmtSynset.setInt(1, count);
				pstmtSynset.setString(2, synsetNode.getId());
				uniqueLiterals.addAll(synsetNode.getLiterals());
				synsetIdToDbId.put(synsetNode.getId(), synsetNode);
				pstmtSynset.addBatch();
				if (count % 500 == 0) {
					pstmtSynset.executeBatch();
					pstmtSynset.clearBatch();
				}
			}
			pstmtSynset.executeBatch();
			count = 0;
			for (Literal literal : uniqueLiterals) {
				count++;
				literal.setDbId(count);
				pstmtLiteral.setInt(1, count);
				pstmtLiteral.setString(2, literal.getWord());
				pstmtLiteral.setInt(3, literal.getSenseId());
				literalToDbId.put(literal.getWord() + literal.getSenseId(),
						count);
				pstmtLiteral.addBatch();
				if (count % 500 == 0) {
					pstmtLiteral.executeBatch();
					pstmtLiteral.clearBatch();
				}
			}
			pstmtLiteral.executeBatch();
			count = 0;
			for (SynsetNode synsetNode : synsetNodes) {
				pstmtSynsetLiteral.setInt(1, synsetNode.getDbId());
				for (Literal literal : synsetNode.getLiterals()) {
					pstmtSynsetLiteral.setInt(
							2,
							literalToDbId.get(literal.getWord()
									+ literal.getSenseId()));
					pstmtSynsetLiteral.addBatch();
				}
				pstmtRelation.setInt(2, synsetNode.getDbId());
				for (Relation relation : synsetNode.getRelations()) {
					count++;
					pstmtRelation.setInt(1, count);
					pstmtRelation.setInt(3,
							synsetIdToDbId.get(relation.getSynsetId())
									.getDbId());
					pstmtRelation.setString(4, relation.getType());
					pstmtRelation.addBatch();
				}
				if (count % 500 == 0) {
					pstmtSynsetLiteral.executeBatch();
					pstmtSynsetLiteral.clearBatch();
					pstmtRelation.executeBatch();
					pstmtRelation.clearBatch();
				}
			}
			pstmtRelation.executeBatch();
			pstmtSynsetLiteral.executeBatch();
			timeCounter.saveETime();
			closeConnection(con);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	public void loadDataIntoSQLMod(List<SynsetNode> synsetNodes) {
		int count;
		Connection con;
		boolean noHypernym, withHyponym;
		StringBuilder sb = new StringBuilder();
		Set<Literal> uniqueLiterals = new HashSet<>();
		Map<String, Integer> literalToDbId = new HashMap<>();
		Map<SynsetNode, List<String>> paths = new HashMap<>();
		Map<String, SynsetNode> synsetIdToDbId = new HashMap<>();
		PreparedStatement pstmtPath, pstmtSynset, pstmtLiteral, pstmtRelation, pstmtSynsetLiteral;

		try {
			con = establishConnection(CONNECTION_URL_MOD);
			pstmtPath = con.prepareStatement(INSERT_PATH_QUERY);
			pstmtRelation = con.prepareStatement(INSERT_RELATION_QUERY);
			pstmtSynsetLiteral = con
					.prepareStatement(INSERT_SYNSET_LITERAL_QUERY);
			pstmtSynset = con.prepareStatement(INSERT_SYNSET_QUERY);
			pstmtLiteral = con.prepareStatement(INSERT_LITERAL_QUERY);

			timeCounter.saveSTime();
			count = 0;
			for (SynsetNode synsetNode : synsetNodes) {
				count++;
				synsetNode.setDbId(count);
				pstmtSynset.setInt(1, count);
				pstmtSynset.setString(2, synsetNode.getId());
				uniqueLiterals.addAll(synsetNode.getLiterals());
				synsetIdToDbId.put(synsetNode.getId(), synsetNode);
				pstmtSynset.addBatch();
				if (count % 500 == 0) {
					pstmtSynset.executeBatch();
					pstmtSynset.clearBatch();
				}
			}
			pstmtSynset.executeBatch();
			count = 0;
			for (Literal literal : uniqueLiterals) {
				count++;
				literal.setDbId(count);
				pstmtLiteral.setInt(1, count);
				pstmtLiteral.setString(2, literal.getWord());
				pstmtLiteral.setInt(3, literal.getSenseId());
				literalToDbId.put(literal.getWord() + literal.getSenseId(),
						count);
				pstmtLiteral.addBatch();
				if (count % 500 == 0) {
					pstmtLiteral.executeBatch();
					pstmtLiteral.clearBatch();
				}
			}
			pstmtLiteral.executeBatch();

			count = 0;
			for (SynsetNode synsetNode : synsetNodes) {
				noHypernym = true;
				withHyponym = false;
				sb.delete(0, sb.length());
				pstmtSynsetLiteral.setInt(1, synsetNode.getDbId());
				for (Literal literal : synsetNode.getLiterals()) {
					pstmtSynsetLiteral.setInt(
							2,
							literalToDbId.get(literal.getWord()
									+ literal.getSenseId()));
					pstmtSynsetLiteral.addBatch();
				}
				pstmtRelation.setInt(2, synsetNode.getDbId());
				for (Relation relation : synsetNode.getRelations()) {
					count++;
					pstmtRelation.setInt(1, count);
					pstmtRelation.setInt(3,
							synsetIdToDbId.get(relation.getSynsetId())
									.getDbId());
					pstmtRelation.setString(4, relation.getType());
					pstmtRelation.addBatch();
					for (String hypernym : HYPERNYMS) {
						if (hypernym.equals(relation.getType())) {
							noHypernym = false;
							break;
						}
					}
					for (String hyponym : HYPONYMS) {
						if (hyponym.equals(relation.getType())) {
							withHyponym = true;
							break;
						}
					}
				}
				if (noHypernym && withHyponym) {
					paths.put(synsetNode, new ArrayList<>());
					sb.append(";").append(synsetNode.getDbId()).append(";");
					for (Relation relation : synsetNode.getRelations()) {
						for (String hyponym : HYPONYMS) {
							if (hyponym.equals(relation.getType())) {
								trackPaths(synsetNode,
										synsetIdToDbId.get(relation
												.getSynsetId()), sb,
										synsetIdToDbId, paths);
								break;
							}
						}
					}
				}
				if (count % 500 == 0) {
					pstmtSynsetLiteral.executeBatch();
					pstmtSynsetLiteral.clearBatch();
					pstmtRelation.executeBatch();
					pstmtRelation.clearBatch();
				}
			}
			pstmtRelation.executeBatch();
			pstmtSynsetLiteral.executeBatch();
			count = 0;
			for (List<String> path : paths.values()) {
				for (String onePath : path) {
					count++;
					pstmtPath.setInt(1, count);
					pstmtPath.setString(2, onePath);
					pstmtPath.addBatch();
					if (count % 500 == 0) {
						pstmtPath.executeBatch();
						pstmtPath.clearBatch();
					}
				}
			}
			pstmtPath.executeBatch();
			timeCounter.saveETime();
			closeConnection(con);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	private String selectGivenRelationsRegex(boolean hypernyms) {
		StringBuilder sb = new StringBuilder();

		sb.append("<ILR>[^<]+<TYPE>(");
		if (hypernyms) {
			for (int i = 0; i < HYPERNYMS.length; i++) {
				sb.append(HYPERNYMS[i]);
				if (i < HYPERNYMS.length - 1) {
					sb.append("|");
				}
			}
		} else {
			for (int i = 0; i < HYPONYMS.length; i++) {
				sb.append(HYPONYMS[i]);
				if (i < HYPONYMS.length - 1) {
					sb.append("|");
				}
			}
		}
		sb.append(")</TYPE></ILR>");
		return sb.toString();
	}

	private PreparedStatement selectPathWithGivenRelations(Connection con,
			boolean hypernyms) throws SQLException {
		PreparedStatement pstmt;
		StringBuilder sb = new StringBuilder();

		sb.append("SELECT DISTINCT synset_id_c FROM relations WHERE relation_type IN (");
		if (hypernyms) {
			for (int i = 0; i < HYPERNYMS.length; i++) {
				sb.append("?");
				if (i < HYPERNYMS.length - 1) {
					sb.append(", ");
				}
			}
		} else {
			for (int i = 0; i < HYPONYMS.length; i++) {
				sb.append("?");
				if (i < HYPONYMS.length - 1) {
					sb.append(", ");
				}
			}
		}
		sb.append(") AND synset_id_p = ?");
		pstmt = con.prepareStatement(sb.toString());
		if (hypernyms) {
			for (int i = 0; i < HYPERNYMS.length; i++) {
				pstmt.setString(i + 1, HYPERNYMS[i]);
			}
		} else {
			for (int i = 0; i < HYPONYMS.length; i++) {
				pstmt.setString(i + 1, HYPONYMS[i]);
			}
		}
		return pstmt;
	}

	private ResultSet selectSynsetForId(Connection con, String synsetId)
			throws SQLException {
		PreparedStatement pstmt;

		pstmt = con
				.prepareStatement("SELECT id FROM synsets WHERE synset_id = ?");
		pstmt.setString(1, synsetId);
		return pstmt.executeQuery();
	}

	private ResultSet selectSynsetForLiteral(Connection con, String literal)
			throws SQLException {
		PreparedStatement pstmt;

		pstmt = con
				.prepareStatement("SELECT DISTINCT s.id FROM synsets s INNER JOIN synset_literals sl ON s.id = sl.id_synset INNER JOIN literals l on sl.id_literal = l.id WHERE literal = ?");
		pstmt.setString(1, literal);
		return pstmt.executeQuery();
	}

	private ResultSet selectSynsetForLiteralAndSense(Connection con,
			String literal, String sense) throws SQLException {
		PreparedStatement pstmt;

		pstmt = con
				.prepareStatement("SELECT DISTINCT s.id FROM synsets s INNER JOIN synset_literals sl ON s.id = sl.id_synset INNER JOIN literals l on sl.id_literal = l.id WHERE literal = ? AND sense_id = ?");
		pstmt.setString(1, literal);
		pstmt.setInt(2, Integer.parseInt(sense));
		return pstmt.executeQuery();
	}

	private List<String> selectSynsetForLiteralAndSenseRegex(
			Map<String, String> doc, String literal, String sense) {
		List<String> result = new ArrayList<>();
		String query = ".*<LITERAL>" + literal + "<SENSE>" + sense
				+ "</SENSE></LITERAL>.*";

		for (Map.Entry<String, String> entry : doc.entrySet()) {
			if (entry.getValue().matches(query)) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	private List<String> selectSynsetForLiteralRegex(Map<String, String> doc,
			String literal) {
		List<String> result = new ArrayList<>();
		String query = ".*<LITERAL>" + literal
				+ "<SENSE>[^<]+</SENSE></LITERAL>.*";

		for (Map.Entry<String, String> entry : doc.entrySet()) {
			if (entry.getValue().matches(query)) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	private ResultSet selectSynsetsWithGivenRelations(Connection con,
			boolean hypernymsBeforeHyponyms) throws SQLException {
		PreparedStatement pstmt;
		StringBuilder sb = new StringBuilder();

		sb.append("SELECT DISTINCT synset_id_p FROM relations WHERE relation_type IN (");
		if (hypernymsBeforeHyponyms) {
			for (int i = 0; i < HYPERNYMS.length; i++) {
				sb.append("?");
				if (i < HYPERNYMS.length - 1) {
					sb.append(", ");
				}
			}
		} else {
			for (int i = 0; i < HYPONYMS.length; i++) {
				sb.append("?");
				if (i < HYPONYMS.length - 1) {
					sb.append(", ");
				}
			}
		}
		sb.append(") AND synset_id_p NOT IN (SELECT synset_id_p FROM relations WHERE relation_type IN (");
		if (hypernymsBeforeHyponyms) {
			for (int i = 0; i < HYPONYMS.length; i++) {
				sb.append("?");
				if (i < HYPONYMS.length - 1) {
					sb.append(", ");
				}
			}
		} else {
			for (int i = 0; i < HYPERNYMS.length; i++) {
				sb.append("?");
				if (i < HYPERNYMS.length - 1) {
					sb.append(", ");
				}
			}
		}
		sb.append("))");
		pstmt = con.prepareStatement(sb.toString());
		if (hypernymsBeforeHyponyms) {
			for (int i = 0; i < HYPERNYMS.length; i++) {
				pstmt.setString(i + 1, HYPERNYMS[i]);
			}
			for (int i = 0; i < HYPONYMS.length; i++) {
				pstmt.setString(HYPERNYMS.length + i + 1, HYPONYMS[i]);
			}
		} else {
			for (int i = 0; i < HYPONYMS.length; i++) {
				pstmt.setString(i + 1, HYPONYMS[i]);
			}
			for (int i = 0; i < HYPERNYMS.length; i++) {
				pstmt.setString(HYPONYMS.length + i + 1, HYPERNYMS[i]);
			}
		}
		return pstmt.executeQuery();
	}

	private List<String> selectSynsetsWithGivenRelationsRegex(
			Map<String, String> doc, boolean hypernymsBeforeHyponyms) {
		String query1, query2;
		List<String> resultList;
		StringBuilder sb = new StringBuilder();

		sb.append("<SYNSET>.+<ILR>[^<]+<TYPE>(");
		if (hypernymsBeforeHyponyms) {
			for (int i = 0; i < HYPERNYMS.length; i++) {
				sb.append(HYPERNYMS[i]);
				if (i < HYPERNYMS.length - 1) {
					sb.append("|");
				}
			}
		} else {
			for (int i = 0; i < HYPONYMS.length; i++) {
				sb.append(HYPONYMS[i]);
				if (i < HYPONYMS.length - 1) {
					sb.append("|");
				}
			}
		}
		sb.append(")</TYPE></ILR>.*</SYNSET>");
		query1 = sb.toString();
		sb.delete(0, sb.length());
		sb.append("<SYNSET>.+<ILR>[^<]+<TYPE>(");
		if (hypernymsBeforeHyponyms) {
			for (int i = 0; i < HYPONYMS.length; i++) {
				sb.append(HYPONYMS[i]);
				if (i < HYPONYMS.length - 1) {
					sb.append("|");
				}
			}
		} else {
			for (int i = 0; i < HYPERNYMS.length; i++) {
				sb.append(HYPERNYMS[i]);
				if (i < HYPERNYMS.length - 1) {
					sb.append("|");
				}
			}
		}
		sb.append(")</TYPE></ILR>.*</SYNSET>");
		query2 = sb.toString();
		sb.delete(0, sb.length());
		resultList = new ArrayList<>(doc.size());
		for (Map.Entry<String, String> entry : doc.entrySet()) {
			if (entry.getValue().matches(query1)
					&& !entry.getValue().matches(query2)) {
				resultList.add(entry.getKey());
			}
		}
		return new ArrayList<>(resultList);
	}

	public void sendComplexRegexQuery(String query, String file, String... args) {
		Pattern pattern;
		Map<String, String> document;
		Counter counter = new Counter();
		List<String> ids = new ArrayList<>();
		List<String> last = new ArrayList<>();
		int min = Integer.MAX_VALUE, max = 0, count = 0;
		Map<Integer, List<String>> countToSynset = new HashMap<>();

		try {
			document = establishRegexConnection(file);
			timeCounter.saveSTime();
			switch (query) {
			case "SELECT MAX(HYPERNYM PATH LENGTH)": {
				// Select synsets with hypernymy relation, but without
				// hyponymy relation (bottommost synsets)
				ids = selectSynsetsWithGivenRelationsRegex(document, true);
				// Select next hypernym
				pattern = Pattern.compile(selectGivenRelationsRegex(true));
				for (String id : ids) {
					counter.setCount(1);
					followPathAndCountLengthRegex(document, pattern, id, 1,
							counter);
					if (counter.getCount() > max) {
						max = counter.getCount();
					}
				}
				ids.clear();
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %d",
							query, max));
				}
				break;
			}
			case "SELECT TOPMOST HYPERNYM FOR SYNSET X": {
				// Select next hypernym
				followPathAndPrintRegex(document,
						Pattern.compile(selectGivenRelationsRegex(true)),
						args[0], last);
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, last.toString()));
				}
				last.clear();
				break;
			}
			case "SELECT SYNSET WITH MIN(HYPONYM PATHS)": {
				// Select synsets with hyponymy relation, but without
				// hyperymy relation (topmost synsets)
				ids = selectSynsetsWithGivenRelationsRegex(document, false);
				// Select next hyponym
				pattern = Pattern.compile(selectGivenRelationsRegex(false));
				for (String id : ids) {
					counter.setCount(0);
					countPathsRegex(document, pattern, id, counter);
					if (counter.getCount() <= min) {
						min = counter.getCount();
						if (!countToSynset.containsKey(min)) {
							countToSynset.put(min, new ArrayList<>());
						}
						countToSynset.get(min).add(id);
					}
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, countToSynset.get(min).toString()));
				}
				ids.clear();
				countToSynset.clear();
				break;
			}
			case "SELECT SYNSET WITH MAX(HYPONYM PATHS)": {
				// Select synsets with hyponymy relation, but without
				// hyperymy relation (topmost synsets)
				ids = selectSynsetsWithGivenRelationsRegex(document, false);
				// Select next hyponym
				pattern = Pattern.compile(selectGivenRelationsRegex(false));
				for (String id : ids) {
					counter.setCount(0);
					countPathsRegex(document, pattern, id, counter);
					if (counter.getCount() >= max) {
						max = counter.getCount();
						if (!countToSynset.containsKey(max)) {
							countToSynset.put(max, new ArrayList<>());
						}
						countToSynset.get(max).add(id);
					}
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, countToSynset.get(max).toString()));
				}
				ids.clear();
				countToSynset.clear();
				break;
			}
			case "SELECT TOPMOST HYPERNYM FOR LITERAL X": {
				// Select synsets for given literal
				ids = selectSynsetForLiteralRegex(document, args[1]);
				// Select next hypernym
				pattern = Pattern.compile(selectGivenRelationsRegex(true));
				for (String id : ids) {
					followPathAndPrintRegex(document, pattern, id, last);
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, last.toString()));
				}
				ids.clear();
				last.clear();
				break;
			}
			case "SELECT BOTTOMMOST HYPONYM FOR SYNSET X": {
				// Select next hyponym
				followPathAndPrintRegex(document,
						Pattern.compile(selectGivenRelationsRegex(false)),
						args[0], last);
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, last.toString()));
				}
				last.clear();
				break;
			}
			case "SELECT SYNSET WITH MIN(HYPERNYM PATHS)": {
				// Select synsets with hyponymy relation, but without
				// hyperymy relation (topmost synsets)
				ids = selectSynsetsWithGivenRelationsRegex(document, true);
				// Select next hyponym
				pattern = Pattern.compile(selectGivenRelationsRegex(true));
				for (String id : ids) {
					counter.setCount(0);
					countPathsRegex(document, pattern, id, counter);
					if (counter.getCount() <= min) {
						min = counter.getCount();
						if (!countToSynset.containsKey(min)) {
							countToSynset.put(min, new ArrayList<>());
						}
						countToSynset.get(min).add(id);
					}
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, countToSynset.get(min).toString()));
				}
				ids.clear();
				countToSynset.clear();
				break;
			}
			case "SELECT SYNSET WITH MAX(HYPERNYM PATHS)": {
				// Select synsets with hyperymy relation, but without
				// hyponymy relation (bottommost synsets)
				ids = selectSynsetsWithGivenRelationsRegex(document, true);
				// Select next hyponym
				pattern = Pattern.compile(selectGivenRelationsRegex(true));
				for (String id : ids) {
					counter.setCount(0);
					countPathsRegex(document, pattern, id, counter);
					if (counter.getCount() >= max) {
						max = counter.getCount();
						if (!countToSynset.containsKey(max)) {
							countToSynset.put(max, new ArrayList<>());
						}
						countToSynset.get(max).add(id);
					}
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, countToSynset.get(max).toString()));
				}
				ids.clear();
				countToSynset.clear();
				break;
			}
			case "SELECT BOTTOMMOST HYPONYM FOR LITERAL X": {
				// Select synsets for given literal
				ids = selectSynsetForLiteralRegex(document, args[1]);
				// Select next hypernym
				pattern = Pattern.compile(selectGivenRelationsRegex(false));
				for (String id : ids) {
					followPathAndPrintRegex(document, pattern, id, last);
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, last.toString()));
				}
				ids.clear();
				last.clear();
				break;
			}
			case "SELECT COUNT(HYPONYM PATHS) FOR SYNSET X": {
				// Select next hyponym
				counter.setCount(0);
				countPathsRegex(document,
						Pattern.compile(selectGivenRelationsRegex(false)),
						args[0], counter);
				count += counter.getCount();
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %d",
							query, count));
				}
				break;
			}
			case "SELECT COUNT(HYPERNYM PATHS) FOR SYNSET X": {
				// Select next hypernym
				counter.setCount(0);
				countPathsRegex(document,
						Pattern.compile(selectGivenRelationsRegex(true)),
						args[0], counter);
				count += counter.getCount();
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %d",
							query, count));
				}
				break;
			}
			case "SELECT COUNT(HYPONYM PATHS) FOR LITERAL X": {
				// Select synsets for given literal
				ids = selectSynsetForLiteralRegex(document, args[1]);
				// Select next hyponym
				pattern = Pattern.compile(selectGivenRelationsRegex(false));
				for (String id : ids) {
					counter.setCount(0);
					countPathsRegex(document, pattern, id, counter);
					count += counter.getCount();
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %d",
							query, count));
				}
				ids.clear();
				break;
			}
			case "SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X": {
				// Select synsets for given literal
				ids = selectSynsetForLiteralRegex(document, args[1]);
				// Select next hypernym
				pattern = Pattern.compile(selectGivenRelationsRegex(true));
				for (String id : ids) {
					counter.setCount(0);
					countPathsRegex(document, pattern, id, counter);
					count += counter.getCount();
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %d",
							query, count));
				}
				ids.clear();
				break;
			}
			case "SELECT TOPMOST HYPERNYM FOR LITERAL X AND SENSE Y": {
				// Select synsets for given literal
				ids = selectSynsetForLiteralAndSenseRegex(document, args[1],
						args[2]);
				// Select next hypernym
				pattern = Pattern.compile(selectGivenRelationsRegex(true));
				for (String id : ids) {
					followPathAndPrintRegex(document, pattern, id, last);
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, last.toString()));
				}
				ids.clear();
				last.clear();
				break;
			}
			case "SELECT BOTTOMMOST HYPONYM FOR LITERAL X AND SENSE Y": {
				// Select synsets for given literal
				ids = selectSynsetForLiteralAndSenseRegex(document, args[1],
						args[2]);
				// Select next hyponym
				pattern = Pattern.compile(selectGivenRelationsRegex(false));
				for (String id : ids) {
					followPathAndPrintRegex(document, pattern, id, last);
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %s",
							query, last.toString()));
				}
				ids.clear();
				last.clear();
				break;
			}
			case "SELECT COUNT(HYPONYM PATHS) FOR LITERAL X AND SENSE Y": {
				// Select synsets for given literal
				ids = selectSynsetForLiteralAndSenseRegex(document, args[1],
						args[2]);
				// // Select next hyponym
				pattern = Pattern.compile(selectGivenRelationsRegex(false));
				for (String id : ids) {
					counter.setCount(0);
					countPathsRegex(document, pattern, id, counter);
					count += counter.getCount();
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %d",
							query, count));
				}
				ids.clear();
				break;
			}
			case "SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X AND SENSE Y": {
				// Select synsets for given literal
				ids = selectSynsetForLiteralAndSenseRegex(document, args[1],
						args[2]);
				// Select next hyponym
				pattern = Pattern.compile(selectGivenRelationsRegex(true));
				for (String id : ids) {
					counter.setCount(0);
					countPathsRegex(document, pattern, id, counter);
					count += counter.getCount();
				}
				if (withLog) {
					System.out.println(String.format("Query: %s, Answer: %d",
							query, count));
				}
				ids.clear();
				break;
			}
			}
			timeCounter.saveETime();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void sendComplexSQLQuery(String query, boolean basic, String... args) {
		ResultSet rs;
		Connection con;
		PreparedStatement pstmt;
		Counter counter = new Counter();
		List<String> last = new ArrayList<>();
		List<Integer> dbIds = new ArrayList<>();
		int min = Integer.MAX_VALUE, max = 0, count = 0;
		Map<Integer, List<Integer>> countToSynset = new HashMap<>();

		try {
			con = establishConnection(basic ? CONNECTION_URL_BASIC
					: CONNECTION_URL_MOD);
			timeCounter.saveSTime();
			if (basic) {
				switch (query) {
				case "SELECT MAX(HYPERNYM PATH LENGTH)": {
					// Select synsets with hypernymy relation, but without
					// hyponymy relation (bottommost synsets)
					rs = selectSynsetsWithGivenRelations(con, true);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, true);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						followPathAndCountLength(pstmt, dbId,
								HYPERNYMS.length + 1, 1, counter);
						if (counter.getCount() > max) {
							max = counter.getCount();
						}
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, max));
					}
					break;
				}
				case "SELECT TOPMOST HYPERNYM FOR SYNSET X": {
					// Select id for given synset
					rs = selectSynsetForId(con, args[0]);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, true);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						followPathAndPrint(pstmt, dbId, HYPERNYMS.length + 1,
								last);
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT SYNSET WITH MIN(HYPONYM PATHS)": {
					// Select synsets with hyponymy relation, but without
					// hyperymy relation (topmost synsets)
					rs = selectSynsetsWithGivenRelations(con, false);
					// Select next hyponym
					pstmt = selectPathWithGivenRelations(con, false);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPONYMS.length + 1, counter);
						if (counter.getCount() <= min) {
							min = counter.getCount();
							if (!countToSynset.containsKey(min)) {
								countToSynset.put(min, new ArrayList<>());
							}
							countToSynset.get(min).add(dbId);
						}
					}
					if (withLog) {
						Collections.sort(countToSynset.get(min));
						System.out.println(String.format(
								"Query: %s, Answer: %s", query, countToSynset
										.get(min).toString()));
					}
					countToSynset.clear();
					break;
				}
				case "SELECT SYNSET WITH MAX(HYPONYM PATHS)": {
					// Select synsets with hyponymy relation, but without
					// hyperymy relation (topmost synsets)
					rs = selectSynsetsWithGivenRelations(con, false);
					// Select next hyponym
					pstmt = selectPathWithGivenRelations(con, false);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPONYMS.length + 1, counter);
						if (counter.getCount() >= max) {
							max = counter.getCount();
							if (!countToSynset.containsKey(max)) {
								countToSynset.put(max, new ArrayList<>());
							}
							countToSynset.get(max).add(dbId);
						}
					}
					if (withLog) {
						Collections.sort(countToSynset.get(max));
						System.out.println(String.format(
								"Query: %s, Answer: %s", query, countToSynset
										.get(max).toString()));
					}
					countToSynset.clear();
					break;
				}
				case "SELECT TOPMOST HYPERNYM FOR LITERAL X": {
					// Select synsets for given literal
					rs = selectSynsetForLiteral(con, args[1]);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, true);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						followPathAndPrint(pstmt, dbId, HYPERNYMS.length + 1,
								last);
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT BOTTOMMOST HYPONYM FOR SYNSET X": {
					// Select id for given synset
					rs = selectSynsetForId(con, args[0]);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, false);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						followPathAndPrint(pstmt, dbId, HYPONYMS.length + 1,
								last);
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT SYNSET WITH MIN(HYPERNYM PATHS)": {
					// Select synsets with hypernymy relation, but without
					// hyponymy relation (bottommost synsets)
					rs = selectSynsetsWithGivenRelations(con, true);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, true);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPERNYMS.length + 1, counter);
						if (counter.getCount() <= min) {
							min = counter.getCount();
							if (!countToSynset.containsKey(min)) {
								countToSynset.put(min, new ArrayList<>());
							}
							countToSynset.get(min).add(dbId);
						}
					}
					if (withLog) {
						Collections.sort(countToSynset.get(min));
						System.out.println(String.format(
								"Query: %s, Answer: %s", query, countToSynset
										.get(min).toString()));
					}
					countToSynset.clear();
					break;
				}
				case "SELECT SYNSET WITH MAX(HYPERNYM PATHS)": {
					// Select synsets with hyponymy relation, but without
					// hyperymy relation (topmost synsets)
					rs = selectSynsetsWithGivenRelations(con, true);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, true);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPERNYMS.length + 1, counter);
						if (counter.getCount() >= max) {
							max = counter.getCount();
							if (!countToSynset.containsKey(max)) {
								countToSynset.put(max, new ArrayList<>());
							}
							countToSynset.get(max).add(dbId);
						}
					}
					if (withLog) {
						Collections.sort(countToSynset.get(max));
						System.out.println(String.format(
								"Query: %s, Answer: %s", query, countToSynset
										.get(max).toString()));
					}
					countToSynset.clear();
					break;
				}
				case "SELECT BOTTOMMOST HYPONYM FOR LITERAL X": {
					// Select synsets for given literal
					rs = selectSynsetForLiteral(con, args[1]);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, false);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						followPathAndPrint(pstmt, dbId, HYPONYMS.length + 1,
								last);
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT COUNT(HYPONYM PATHS) FOR SYNSET X": {
					// Select id for given synset
					rs = selectSynsetForId(con, args[0]);
					// Select next hyponym
					pstmt = selectPathWithGivenRelations(con, false);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPONYMS.length + 1, counter);
						count += counter.getCount();
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT COUNT(HYPERNYM PATHS) FOR SYNSET X": {
					// Select id for given synset
					rs = selectSynsetForId(con, args[0]);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, true);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPERNYMS.length + 1, counter);
						count += counter.getCount();
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT COUNT(HYPONYM PATHS) FOR LITERAL X": {
					// Select synsets for given literal
					rs = selectSynsetForLiteral(con, args[1]);
					// Select next hyponym
					pstmt = selectPathWithGivenRelations(con, false);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPONYMS.length + 1, counter);
						count += counter.getCount();
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X": {
					// Select synsets for given literal
					rs = selectSynsetForLiteral(con, args[1]);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, true);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPERNYMS.length + 1, counter);
						count += counter.getCount();
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT TOPMOST HYPERNYM FOR LITERAL X AND SENSE Y": {
					// Select synsets for given literal
					rs = selectSynsetForLiteralAndSense(con, args[1], args[2]);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, true);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						followPathAndPrint(pstmt, dbId, HYPERNYMS.length + 1,
								last);
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT BOTTOMMOST HYPONYM FOR LITERAL X AND SENSE Y": {
					// Select synsets for given literal
					rs = selectSynsetForLiteralAndSense(con, args[1], args[2]);
					// Select next hypernym
					pstmt = selectPathWithGivenRelations(con, false);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						followPathAndPrint(pstmt, dbId, HYPONYMS.length + 1,
								last);
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT COUNT(HYPONYM PATHS) FOR LITERAL X AND SENSE Y": {
					// Select synsets for given literal
					rs = selectSynsetForLiteralAndSense(con, args[1], args[2]);
					// Select next hyponym
					pstmt = selectPathWithGivenRelations(con, false);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPONYMS.length + 1, counter);
						count += counter.getCount();
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X AND SENSE Y": {
					// Select synsets for given literal
					rs = selectSynsetForLiteralAndSense(con, args[1], args[2]);
					// Select next hyponym
					pstmt = selectPathWithGivenRelations(con, true);
					while (rs.next()) {
						dbIds.add(rs.getInt(1));
					}
					for (int dbId : dbIds) {
						counter.setCount(0);
						countPaths(pstmt, dbId, HYPERNYMS.length + 1, counter);
						count += counter.getCount();
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				}
			} else {
				switch (query) {
				case "SELECT MAX(HYPERNYM PATH LENGTH)": {
					pstmt = con
							.prepareStatement("SELECT MAX(ROUND((LENGTH(synset_path) - LENGTH(REPLACE(synset_path, ';', ''))) / 2, 0)) FROM paths");
					rs = pstmt.executeQuery();
					while (rs.next()) {
						if (withLog) {
							System.out.println(String.format(
									"Query: %s, Answer: %d", query,
									rs.getInt(1)));
						}
					}
					break;
				}
				case "SELECT TOPMOST HYPERNYM FOR SYNSET X": {
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', 2) FROM paths WHERE synset_path LIKE (SELECT CONCAT('%;', id,';%') FROM synsets WHERE synset_id = ?) AND synset_path NOT LIKE (SELECT CONCAT(';', id,';%') FROM synsets WHERE synset_id = ?)");
					pstmt.setString(1, args[0]);
					pstmt.setString(2, args[0]);
					rs = pstmt.executeQuery();
					while (rs.next()) {
						last.add(rs.getString(1).replace(";", ""));
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT TOPMOST HYPERNYM FOR LITERAL X": {
					ResultSet rsLocal;
					// Select synsets for given literal
					rs = selectSynsetForLiteral(con, args[1]);
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', 2) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT(';', ?, ';%')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							last.add(rsLocal.getString(1).replace(";", ""));
						}
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT SYNSET WITH MIN(HYPONYM PATHS)": {
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', 2) FROM paths GROUP BY SUBSTRING_INDEX(synset_path, ';', 2) HAVING COUNT(*) = (SELECT MIN(cnt) FROM (SELECT COUNT(*) AS cnt FROM paths GROUP BY SUBSTRING_INDEX(synset_path, ';', 2)) AS T)");
					rs = pstmt.executeQuery();
					while (rs.next()) {
						last.add(rs.getString(1).replace(";", ""));
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT SYNSET WITH MAX(HYPONYM PATHS)": {
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', 2) FROM paths GROUP BY SUBSTRING_INDEX(synset_path, ';', 2) HAVING COUNT(*) = (SELECT MAX(cnt) FROM (SELECT COUNT(*) AS cnt FROM paths GROUP BY SUBSTRING_INDEX(synset_path, ';', 2)) AS T)");
					rs = pstmt.executeQuery();
					while (rs.next()) {
						last.add(rs.getString(1).replace(";", ""));
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT BOTTOMMOST HYPONYM FOR SYNSET X": {
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', -2) FROM paths WHERE synset_path LIKE (SELECT CONCAT('%;', id, ';%') FROM synsets WHERE synset_id = ?) AND synset_path NOT LIKE (SELECT CONCAT('%;', id, ';') FROM synsets WHERE synset_id = ?)");
					pstmt.setString(1, args[0]);
					pstmt.setString(2, args[0]);
					rs = pstmt.executeQuery();
					while (rs.next()) {
						last.add(rs.getString(1).replace(";", ""));
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT SYNSET WITH MIN(HYPERNYM PATHS)": {
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', -2) FROM paths GROUP BY SUBSTRING_INDEX(synset_path, ';', -2) HAVING COUNT(*) = (SELECT MIN(cnt) FROM (SELECT COUNT(*) AS cnt FROM paths GROUP BY SUBSTRING_INDEX(synset_path, ';', -2)) AS T)");
					rs = pstmt.executeQuery();
					while (rs.next()) {
						last.add(rs.getString(1).replace(";", ""));
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT SYNSET WITH MAX(HYPERNYM PATHS)": {
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', -2) FROM paths GROUP BY SUBSTRING_INDEX(synset_path, ';', -2) HAVING COUNT(*) = (SELECT MAX(cnt) FROM (SELECT COUNT(*) AS cnt FROM paths GROUP BY SUBSTRING_INDEX(synset_path, ';', -2)) AS T)");
					rs = pstmt.executeQuery();
					while (rs.next()) {
						last.add(rs.getString(1).replace(";", ""));
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT BOTTOMMOST HYPONYM FOR LITERAL X": {
					ResultSet rsLocal;

					// Select synsets for given literal
					rs = selectSynsetForLiteral(con, args[1]);
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', -2) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT('%;', ?, ';')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							last.add(rsLocal.getString(1).replace(";", ""));
						}
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT COUNT(HYPONYM PATHS) FOR SYNSET X": {
					ResultSet rsLocal;

					rs = selectSynsetForId(con, args[0]);
					pstmt = con
							.prepareStatement("SELECT COUNT(DISTINCT SUBSTRING_INDEX(synset_path, CONCAT(';', ?, ';'), -1)) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT('%;', ?, ';')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						pstmt.setInt(3, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							count += rsLocal.getInt(1);
						}
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT COUNT(HYPERNYM PATHS) FOR SYNSET X": {
					ResultSet rsLocal;

					rs = selectSynsetForId(con, args[0]);
					pstmt = con
							.prepareStatement("SELECT COUNT(DISTINCT SUBSTRING_INDEX(synset_path, CONCAT(';', ?, ';'), 1)) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT(';', ?, ';%')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						pstmt.setInt(3, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							count += rsLocal.getInt(1);
						}
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT COUNT(HYPONYM PATHS) FOR LITERAL X": {
					ResultSet rsLocal;

					// Select synsets for given literal
					rs = selectSynsetForLiteral(con, args[1]);
					pstmt = con
							.prepareStatement("SELECT COUNT(DISTINCT SUBSTRING_INDEX(synset_path, CONCAT(';', ?, ';'), -1)) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT('%;', ?, ';')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						pstmt.setInt(3, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							count += rsLocal.getInt(1);
						}
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X": {
					ResultSet rsLocal;

					// Select synsets for given literal
					rs = selectSynsetForLiteral(con, args[1]);
					pstmt = con
							.prepareStatement("SELECT COUNT(DISTINCT SUBSTRING_INDEX(synset_path, CONCAT(';', ?, ';'), 1)) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT(';', ?, ';%')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						pstmt.setInt(3, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							count += rsLocal.getInt(1);
						}
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT TOPMOST HYPERNYM FOR LITERAL X AND SENSE Y": {
					ResultSet rsLocal;

					// Select synsets for given literal
					rs = selectSynsetForLiteralAndSense(con, args[1], args[2]);
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', 2) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT(';', ?, ';%')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							last.add(rsLocal.getString(1).replace(";", ""));
						}
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT BOTTOMMOST HYPONYM FOR LITERAL X AND SENSE Y": {
					ResultSet rsLocal;

					// Select synsets for given literal
					rs = selectSynsetForLiteralAndSense(con, args[1], args[2]);
					pstmt = con
							.prepareStatement("SELECT DISTINCT SUBSTRING_INDEX(synset_path, ';', -2) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT('%;', ?, ';')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							last.add(rsLocal.getString(1).replace(";", ""));
						}
					}
					if (withLog) {
						Collections.sort(last);
						System.out
								.println(String.format("Query: %s, Answer: %s",
										query, last.toString()));
					}
					last.clear();
					break;
				}
				case "SELECT COUNT(HYPONYM PATHS) FOR LITERAL X AND SENSE Y": {
					ResultSet rsLocal;

					rs = selectSynsetForLiteralAndSense(con, args[1], args[2]);
					pstmt = con
							.prepareStatement("SELECT COUNT(DISTINCT SUBSTRING_INDEX(synset_path, CONCAT(';', ?, ';'), -1)) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT('%;', ?, ';')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						pstmt.setInt(3, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							count += rsLocal.getInt(1);
						}
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				case "SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X AND SENSE Y": {
					ResultSet rsLocal;

					rs = selectSynsetForLiteralAndSense(con, args[1], args[2]);
					pstmt = con
							.prepareStatement("SELECT COUNT(DISTINCT SUBSTRING_INDEX(synset_path, CONCAT(';', ?, ';'), 1)) FROM paths WHERE synset_path LIKE CONCAT('%;', ?, ';%') AND synset_path NOT LIKE CONCAT(';', ?, ';%')");
					while (rs.next()) {
						pstmt.setInt(1, rs.getInt(1));
						pstmt.setInt(2, rs.getInt(1));
						pstmt.setInt(3, rs.getInt(1));
						rsLocal = pstmt.executeQuery();
						while (rsLocal.next()) {
							count += rsLocal.getInt(1);
						}
					}
					if (withLog) {
						System.out.println(String.format(
								"Query: %s, Answer: %d", query, count));
					}
					break;
				}
				}
			}
			timeCounter.saveETime();
			closeConnection(con);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	public void sendSimpleSQLQuery(String query, boolean basic) {
		Connection con;
		Statement stmt;

		try {
			con = establishConnection(basic ? CONNECTION_URL_BASIC
					: CONNECTION_URL_MOD);
			stmt = con.createStatement();
			timeCounter.saveSTime();
			stmt.executeQuery(query);
			timeCounter.saveETime();
			closeConnection(con);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	public void sendSimpleXPathQuery(String query, String file) {
		Document document;
		XPath xpath = XPathFactory.newInstance().newXPath();

		try {
			document = establishSimpleXPathConnection(file);
			timeCounter.saveSTime();
			switch (query) {
			case "/ROOT/SYNSET/LITERAL[SENSE > 1]/text()": {
				String value;
				Map<String, Integer> counts = new HashMap<>();
				NodeList nodeList = (NodeList) xpath.evaluate(query, document,
						XPathConstants.NODESET);

				for (int i = 0; i < nodeList.getLength(); i++) {
					value = nodeList.item(i).getNodeValue();
					if (counts.containsKey(value)) {
						counts.put(value, counts.get(value) + 1);
					} else {
						counts.put(value, 2);
					}
				}
				break;
			}
			case "/ROOT/SYNSET/LITERAL/text()":
			case "/ROOT/SYNSET/ILR/TYPE/text()": {
				Set<String> set = new HashSet<>();
				NodeList nodeList = (NodeList) xpath.evaluate(query, document,
						XPathConstants.NODESET);

				for (int i = 0; i < nodeList.getLength(); i++) {
					set.add(nodeList.item(i).getNodeValue());
				}
				break;
			}
			case "/ROOT/SYNSET[count(ILR) > 1]": {
				Node value;
				NodeList children;
				Map<Node, Integer> counts = new HashMap<>();
				NodeList nodeList = (NodeList) xpath.evaluate(query, document,
						XPathConstants.NODESET);

				for (int i = 0; i < nodeList.getLength(); i++) {
					value = nodeList.item(i);
					children = value.getChildNodes();
					counts.put(value, 0);
					for (int j = 0; j < children.getLength(); j++) {
						if (children.item(j).getNodeName().equals("ILR")) {
							counts.put(value, counts.get(value) + 1);
						}
					}
				}
				break;
			}
			case "/ROOT/SYNSET[count(LITERAL) > 1]": {
				Node value;
				NodeList children;
				Map<Node, Integer> counts = new HashMap<>();
				NodeList nodeList = (NodeList) xpath.evaluate(query, document,
						XPathConstants.NODESET);

				for (int i = 0; i < nodeList.getLength(); i++) {
					value = nodeList.item(i);
					counts.put(value, 0);
					children = value.getChildNodes();
					for (int j = 0; j < children.getLength(); j++) {
						if (children.item(j).getNodeName().equals("LITERAL")) {
							counts.put(value, counts.get(value) + 1);
						}
					}
				}
				break;
			}
			case "/ROOT/SYNSET[count(ILR[TYPE='hypernym']) > 1]": {
				Node value;
				NodeList children;
				Map<Node, Integer> counts = new HashMap<>();
				NodeList nodeList = (NodeList) xpath.evaluate(query, document,
						XPathConstants.NODESET);

				for (int i = 0; i < nodeList.getLength(); i++) {
					value = nodeList.item(i);
					counts.put(value, 0);
					children = value.getChildNodes();
					for (int j = 0; j < children.getLength(); j++) {
						if (children.item(j).getNodeName().equals("ILR")
								&& children.item(j).getChildNodes().item(0)
										.getTextContent().equals("hypernym")) {
							counts.put(value, counts.get(value) + 1);
						}
					}
				}
				break;
			}
			default:
				xpath.evaluate(query, document);
				break;
			}
			timeCounter.saveETime();
		} catch (SAXException | IOException | ParserConfigurationException
				| XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	private void trackPaths(SynsetNode parent, SynsetNode synsetNode,
			StringBuilder sb, Map<String, SynsetNode> synsetIdToDbId,
			Map<SynsetNode, List<String>> paths) {
		boolean empty = true;
		String added = ";" + synsetNode.getDbId() + ";";

		sb.append(added);
		for (Relation relation : synsetNode.getRelations()) {
			for (String hyponym : HYPONYMS) {
				if (hyponym.equals(relation.getType())) {
					empty = false;
					trackPaths(parent,
							synsetIdToDbId.get(relation.getSynsetId()), sb,
							synsetIdToDbId, paths);
					break;
				}
			}
		}
		if (empty) {
			paths.get(parent).add(sb.toString());
		}
		sb.delete(sb.indexOf(added), sb.length());
	}
}
