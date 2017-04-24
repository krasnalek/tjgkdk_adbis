@echo off

REM Reduces full VisDic file to the desired elements
REM java -jar adbis.jar -sv <full-wordnet-xml-file> <simple-wordnet-xml-file-without-root-element>

REM Loads data from XML to the databases
REM NOTICE! It is assumed that the databases are named plwn_basic and plwn_mod.
REM NOTICE! It is assumed that database structure has already been prepared.
REM NOTICE! It is assumed that a user named 'plwn' with password 'plwn' has been created and assigned privileges
REM 		to the plwn_basic and plwn_mod databases.
REM java -jar adbis.jar -ld <simple-wordnet-xml-file-without-root-element> 1>> out-final.log 2>> err.log

REM Performs simple SQL queries on basic database
REM java -jar adbis.jar -sq true 1>> out.log 2>> err.log
REM Performs simple SQL queries on extended database
REM java -jar adbis.jar -sq false 1>> out.log 2>> err.log
REM Performs simple XPath queries on the XML file 
REM java -jar adbis.jar -sx <simple-wordnet-xml-file-with-root-element> 1>> out.log 2>> err.log

REM Performs complex SQL queries on basic database
REM java -jar adbis.jar -cq true <synset-id> <literal> <sense> 1>> out.log 2>> err.log
REM Performs complex SQL queries on extended database
REM java -jar adbis.jar -cq false <synset-id> <literal> <sense> 1>> out.log 2>> err.log
REM Performs complex queries on the XML file
REM java -jar adbis.jar -cx <simple-wordnet-xml-file-without-root-element> <synset-id> <literal> <sense> 1>> out.log 2>> err.log

REM Performs statistical tests
REM java -jar adbis.jar -st <simple-wordnet-xml-file-without-root-element> <synset-data-file> <literals-data-file> <literal-senses-data-file> 1>> out.log 2>> err.log

@echo on