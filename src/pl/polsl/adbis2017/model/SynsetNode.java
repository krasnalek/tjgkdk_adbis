package pl.polsl.adbis2017.model;

import java.util.ArrayList;
import java.util.List;

public class SynsetNode {

	private Integer dbId;
	private final String id;
	private List<Literal> literals;
	private List<Relation> relations;

	public SynsetNode(String id) {
		this.id = id;
		this.literals = new ArrayList<>();
		this.relations = new ArrayList<>();
	}

	public Integer getDbId() {
		return dbId;
	}

	public void setDbId(Integer dbId) {
		this.dbId = dbId;
	}

	public void addLiteral(String word, Integer senseId) {
		literals.add(new Literal(word, senseId));
	}

	public void addRelation(String type, String synsetId) {
		relations.add(new Relation(type, synsetId));
	}

	public String getId() {
		return id;
	}

	public List<Literal> getLiterals() {
		return literals;
	}

	public List<Relation> getRelations() {
		return relations;
	}

}
