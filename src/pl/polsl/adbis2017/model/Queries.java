package pl.polsl.adbis2017.model;

public class Queries {

	public static final String[] SIMPLE_QUERIES = {
			"SELECT COUNT(*) FROM synsets",
			"SELECT COUNT(*) FROM literals",
			"SELECT COUNT(*) FROM relations",
			"SELECT COUNT(DISTINCT literal) FROM literals",
			"SELECT COUNT(DISTINCT relation_type) FROM relations",
			"SELECT COUNT(*) + 1 FROM literals WHERE sense_id > 1 GROUP BY literal HAVING COUNT(*) > 1",
			"SELECT COUNT(*) FROM relations GROUP BY synset_id_p HAVING COUNT(*) > 1",
			"SELECT COUNT(*) FROM synsets s INNER JOIN relations r ON s.id = r.synset_id_p WHERE s.synset_id = 'PLWN-00000152-n'",
			"SELECT COUNT(*) FROM synsets s INNER JOIN relations r ON s.id = r.synset_id_c WHERE s.synset_id = 'PLWN-00000152-n'",
			"SELECT COUNT(*) FROM synsets s INNER JOIN synset_literals sl ON s.id = sl.id_synset GROUP BY s.id HAVING COUNT(*) > 1",
			"SELECT COUNT(*) FROM relations WHERE relation_type IN ('hypernym') GROUP BY synset_id_p HAVING COUNT(*) > 1;",
			"SELECT COUNT(*) FROM synsets s INNER JOIN synset_literals sl ON sl.id_synset = s.id INNER JOIN literals l ON sl.id_literal = l.id WHERE literal IN ('kot', 'gwiazda')",
			"SELECT COUNT(*) FROM literals l INNER JOIN synset_literals sl ON l.id = sl.id_literal INNER JOIN synsets s ON sl.id_synset = s.id WHERE s.synset_id = 'PLWN-00000152-n'",
			"SELECT COUNT(*) FROM relations r INNER JOIN synsets s ON r.synset_id_p = s.id INNER JOIN synset_literals sl ON s.id = sl.id_synset INNER JOIN literals l ON sl.id_literal = l.id WHERE l.literal LIKE 'gwi%'" };

	public static final String[] SIMPLE_XPATH_QUERIES = {
			"count(//SYNSET)",
			"count(//LITERAL)",
			"count(//ILR)",
			"/ROOT/SYNSET/LITERAL/text()",
			"/ROOT/SYNSET/ILR/TYPE/text()",
			"/ROOT/SYNSET/LITERAL[SENSE > 1]/text()",
			"/ROOT/SYNSET[count(ILR) > 1]",
			"count(/ROOT/SYNSET[ID = 'PLWN-00000152-n']/ILR)",
			"count(/ROOT/SYNSET[ILR/text()='PLWN-00000152-n'])",
			"/ROOT/SYNSET[count(LITERAL) > 1]",
			"/ROOT/SYNSET[count(ILR[TYPE='hypernym']) > 1]",
			"count(/ROOT/SYNSET[LITERAL/text()='kot']|SYNSET[LITERAL/text()='gwiazda'])",
			"count(/ROOT/SYNSET[ID='PLWN-00000152-n']/LITERAL)",
			"count(/ROOT/SYNSET[LITERAL[starts-with(., 'gwi')]]/ILR)" };

	public static final String[] COMPLEX_QUERIES = {
			"SELECT MAX(HYPERNYM PATH LENGTH)",
			"SELECT SYNSET WITH MIN(HYPONYM PATHS)",
			"SELECT SYNSET WITH MAX(HYPONYM PATHS)",
			"SELECT SYNSET WITH MIN(HYPERNYM PATHS)",
			"SELECT SYNSET WITH MAX(HYPERNYM PATHS)"};//,
			/*"SELECT TOPMOST HYPERNYM FOR SYNSET X",
			"SELECT TOPMOST HYPERNYM FOR LITERAL X",
			"SELECT BOTTOMMOST HYPONYM FOR SYNSET X",
			"SELECT BOTTOMMOST HYPONYM FOR LITERAL X",
			"SELECT COUNT(HYPONYM PATHS) FOR SYNSET X",
			"SELECT COUNT(HYPERNYM PATHS) FOR SYNSET X",
			"SELECT COUNT(HYPONYM PATHS) FOR LITERAL X",
			"SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X",
			"SELECT TOPMOST HYPERNYM FOR LITERAL X AND SENSE Y",
			"SELECT BOTTOMMOST HYPONYM FOR LITERAL X AND SENSE Y",
			"SELECT COUNT(HYPONYM PATHS) FOR LITERAL X AND SENSE Y",
			"SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X AND SENSE Y" };*/

	public static final String[] STATS_QUERIES_S = {
			"SELECT TOPMOST HYPERNYM FOR SYNSET X",
			"SELECT BOTTOMMOST HYPONYM FOR SYNSET X",
			"SELECT COUNT(HYPONYM PATHS) FOR SYNSET X",
			"SELECT COUNT(HYPERNYM PATHS) FOR SYNSET X" };

	public static final String[] STATS_QUERIES_L = {
			"SELECT TOPMOST HYPERNYM FOR LITERAL X",
			"SELECT BOTTOMMOST HYPONYM FOR LITERAL X",
			"SELECT COUNT(HYPONYM PATHS) FOR LITERAL X",
			"SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X" };

	public static final String[] STATS_QUERIES_LS = {
			"SELECT TOPMOST HYPERNYM FOR LITERAL X AND SENSE Y",
			"SELECT BOTTOMMOST HYPONYM FOR LITERAL X AND SENSE Y",
			"SELECT COUNT(HYPONYM PATHS) FOR LITERAL X AND SENSE Y",
			"SELECT COUNT(HYPERNYM PATHS) FOR LITERAL X AND SENSE Y" };
}