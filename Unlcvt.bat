@ECHO OFF

SETLOCAL

REM Unlcvt.bat -- 
REM		Runs psdi.configure.Unlcvt.
REM		The default name of the output file is Unlcvt.ora (Oracle), Unlcvt.sqs (SqlServer), or Unlcvt.ddl (DB2).
REM		The default database is defined in the maximo.properties file.
REM 		The commandline parameters for overriding the defaults are listed below.  
REM		Also see javadocs for psdi.configure.Unlcvt.
REM
REM -a (db alias)	Database alias. If not specified, uses mxe.db.url property.
REM -f (filename)	Filename for properties file.  If not specified, uses maximo.properties.
REM			(Also see -k parameter for propfile directory.)
REM -k (propfile dir)	Directory for properties file.
REM			(Also see -f parameter for propfile filename.)
REM -o (filename)	Filename of output file (without path or extension).
REM -p (password)	Password for database connection.
REM			If not specified, uses mxe.db.password property, or "maximo".
REM -u (username)	Username for database connection.
REM			If not specified, uses mxe.db.user property, or "maximo".
REM -x (db platform)    Output to a different db platform that the one being used for input.
REM			(The default is to output to the same platform.)
REM			Values for platform are: 1=Oracle, 2=SqlServer, 3=DB2.

call commonEnv.bat

@..\..\java\jre\bin\java  -classpath %MAXIMO_CLASSPATH% psdi.configure.Unlcvt -k..\..\..\applications\maximo\properties %1 %2 %3 %4 %5 %6 %7

