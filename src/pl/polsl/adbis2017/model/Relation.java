package pl.polsl.adbis2017.model;

public class Relation {

	private final String type;
	private final String synsetId;

	public Relation(String type, String synsetId) {
		this.type = type;
		this.synsetId = synsetId;
	}

	public String getType() {
		return type;
	}

	public String getSynsetId() {
		return synsetId;
	}

}
