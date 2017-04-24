1. 	The plWordNet version 2.3 can be obtained from http://nlp.pwr.wroc.pl/plwordnet/download/?lang=pl.

2. 	The VisDic version of plWordNet should be extracted from the archive downloaded in p. 1. 
	It should be simplified by uncommenting the following line in bin/manual.bat and running the file:
	
	REM java -jar adbis.jar -sv <full-wordnet-xml-file> <simple-wordnet-xml-file-without-root-element>
	
	where: 	<full-wordnet-xml-file> is the downloaded XML file, and 
			<simple-wordnet-xml-file-without-root-element> is the name of the output file.
	
3. 	A user called 'plwn' with password 'plwn' should be created in MySQL database. 
	He should have full access privileges on databases plwn_basic and plwn_mod.
	
4.	SQL scripts sql/plwn_basic_norel.sql and sql/plwn_mod_norel.sql should be executed to 
	create the required schemas.

5.	Both databases (plwn_basic and plwn_mod) should be loaded with data.
	It should be done by uncommenting the following line in bin/manual.bat and running the file: 
	
	REM java -jar adbis.jar -ld <simple-wordnet-xml-file-without-root-element> 1>> out-final.log 2>> err.log
	
	where: 	<simple-wordnet-xml-file-without-root-element> is the name of the output file from p. 2.
			
6. 	SQL scripts sql/plwn_basic_relations.sql and sql/plwn_mod_relations.sql should be executed to 
	add referential constraints and indexes to the tables. This should be done after p. 5 has completed.
	
7.	Given the data is loaded and the constraints were applied, either of the following lines can be 
	uncommented in bin/manual.bat prior to running the file:
	
	REM java -jar adbis.jar -sq true 1>> out.log 2>> err.log
	REM java -jar adbis.jar -sq false 1>> out.log 2>> err.log
	REM java -jar adbis.jar -sx <simple-wordnet-xml-file-with-root-element> 1>> out.log 2>> err.log
	
	where: 	<simple-wordnet-xml-file-with-root-element> is the name of the modified file from p. 2,
			which contains the document root element called ROOT.

	REM java -jar adbis.jar -cq true <synset-id> <literal> <sense> 1>> out.log 2>> err.log
	
	where: 	<synset-id> is an arbitrary <ID> element from the wordnet XML document,
			<literal> is an arbitrary <LITERAL> element from the wordnet XML document (without sense),
			<sense> is an arbitrary <SENSE> element from the wordnet XML document (for some given literal).
			
	REM java -jar adbis.jar -cq false <synset-id> <literal> <sense> 1>> out.log 2>> err.log

	where: 	<synset-id> is an arbitrary <ID> element from the wordnet XML document,
			<literal> is an arbitrary <LITERAL> element from the wordnet XML document (without sense),
			<sense> is an arbitrary <SENSE> element from the wordnet XML document (for some given literal).

	REM java -jar adbis.jar -cx <simple-wordnet-xml-file-without-root-element> <synset-id> <literal> <sense> 1>> out.log 2>> err.log

	where: 	<simple-wordnet-xml-file-without-root-element> is the name of the output file from p. 2,
			<synset-id> is an arbitrary <ID> element from the wordnet XML document,
			<literal> is an arbitrary <LITERAL> element from the wordnet XML document (without sense),
			<sense> is an arbitrary <SENSE> element from the wordnet XML document (for some given literal).
	
	REM java -jar adbis.jar -st <simple-wordnet-xml-file-without-root-element> <synset-data-file> <literals-data-file> <literal-senses-data-file> 1>> out.log 2>> err.log
	
	where: 	<simple-wordnet-xml-file-without-root-element> is the name of the output file from p. 2,
			<synset-data-file> is the name of the file containing values of <ID> elements from the wordnet XML document,
			<literals-data-file> is the name of the file containing values of <LITERAL> elements from the wordnet XML document (without senses),
			<literal-senses-data-file> is the name of the file containing values of <LITERAL> elements from the wordnet XML document (with senses).