package pl.polsl.adbis2017.model;

public class Literal {

	private Integer dbId;
	private final String word;
	private final Integer senseId;

	public Literal(String word, Integer senseId) {
		this.word = word;
		this.senseId = senseId;
	}

	public Integer getDbId() {
		return dbId;
	}

	public void setDbId(Integer dbId) {
		this.dbId = dbId;
	}

	public String getWord() {
		return word;
	}

	public Integer getSenseId() {
		return senseId;
	}

	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;

		result = prime * result + ((word == null) ? 0 : word.hashCode());
		result = prime * result + ((senseId == null) ? 0 : senseId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Literal)) {
			return false;
		}
		Literal other = (Literal) obj;
		if (senseId == null) {
			if (other.senseId != null) {
				return false;
			}
		} else if (!senseId.equals(other.senseId)) {
			return false;
		}
		if (word == null) {
			if (other.word != null) {
				return false;
			}
		} else if (!word.equals(other.word)) {
			return false;
		}
		return true;
	}

}
