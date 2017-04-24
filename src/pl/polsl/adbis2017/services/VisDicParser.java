package pl.polsl.adbis2017.services;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.polsl.adbis2017.model.Literal;
import pl.polsl.adbis2017.model.Relation;
import pl.polsl.adbis2017.model.SynsetNode;

public class VisDicParser {

	private List<SynsetNode> synsetNodes;

	public VisDicParser() {
		this.synsetNodes = new ArrayList<>();
	}

	public List<SynsetNode> getSynsetNodes() {
		return synsetNodes;
	}

	public void parseVisDic(String file) throws IOException {
		SynsetNode node;
		List<String> lines;
		String id, word, type, sense, relation;
		Pattern idPattern = Pattern.compile("<ID>[^<]+</ID>");
		Pattern relPattern = Pattern
				.compile("<ILR>[^<]+<TYPE>[^<]+</TYPE></ILR>");
		Pattern typePattern = Pattern.compile("<TYPE>[^<]+</TYPE>");
		Pattern sensePattern = Pattern.compile("<SENSE>[^<]+</SENSE>");
		Pattern literalPattern = Pattern
				.compile("<LITERAL>[^<]+<SENSE>[^<]+</SENSE></LITERAL>");
		Matcher idMatcher, relMatcher, typeMatcher, senseMatcher, literalMatcher;

		lines = Files.readAllLines(Paths.get(file));
		for (String line : lines) {
			idMatcher = idPattern.matcher(line);
			idMatcher.find();
			id = idMatcher.group();
			id = id.replaceAll("</?ID>", "");
			synsetNodes.add(node = new SynsetNode(id));
			literalMatcher = literalPattern.matcher(line);
			while (literalMatcher.find()) {
				word = literalMatcher.group();
				senseMatcher = sensePattern.matcher(word);
				senseMatcher.find();
				sense = senseMatcher.group();
				word = word.replace(sense, "");
				sense = sense.replaceAll("</?SENSE>", "");
				word = word.replaceAll("</?LITERAL>", "");
				node.addLiteral(word, Integer.parseInt(sense));
			}
			relMatcher = relPattern.matcher(line);
			while (relMatcher.find()) {
				relation = relMatcher.group();
				typeMatcher = typePattern.matcher(relation);
				typeMatcher.find();
				type = typeMatcher.group();
				relation = relation.replace(type, "");
				type = type.replaceAll("</?TYPE>", "");
				relation = relation.replaceAll("</?ILR>", "");
				if (!relation.endsWith("?")) {
					node.addRelation(type, relation);
				}
			}
		}
	}

	public void writeNewVisDic(String file) throws IOException {
		StringBuilder sb = new StringBuilder();

		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(file),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			for (SynsetNode synsetNode : synsetNodes) {
				sb.delete(0, sb.length());
				sb.append("<SYNSET>");
				sb.append("<ID>");
				sb.append(synsetNode.getId());
				sb.append("</ID>");
				for (Literal literal : synsetNode.getLiterals()) {
					sb.append("<LITERAL>");
					sb.append(literal.getWord());
					sb.append("<SENSE>");
					sb.append(literal.getSenseId());
					sb.append("</SENSE>");
					sb.append("</LITERAL>");
				}
				for (Relation relation : synsetNode.getRelations()) {
					sb.append("<ILR>");
					sb.append(relation.getSynsetId());
					sb.append("<TYPE>");
					sb.append(relation.getType());
					sb.append("</TYPE>");
					sb.append("</ILR>");
				}
				sb.append("</SYNSET>");
				bw.write(sb.toString());
				bw.newLine();
			}
		}
	}

}
