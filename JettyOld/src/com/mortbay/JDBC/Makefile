ROOTNAME := com
TESTS := Select.java TestHarness.java
JAVACOPTIONS := 
JAVAOPTIONS := $(JAVAOPTIONS) -DMSQL2
EXCLUDE := 
BINFILES := 

ifndef MORTBAY_HOME
MORTBAY_HOME := /usr/local/MortBay
endif
include $(MORTBAY_HOME)/etc/Global.mk

msql: all
	msqladmin -q drop TestDB || echo ok
	msqladmin -q create TestDB || echo ok
	runjava -r com CreateTables com.mortbay.JDBC.MsqlAdaptor com.mortbay.JDBC.TestTable com.mortbay.JDBC.MultiTestTable
	runjava -r com CreateTables com.mortbay.JDBC.MsqlAdaptor com.mortbay.JDBC.TestTable com.mortbay.JDBC.MultiTestTable | msql TestDB
	echo 'SELECT * FROM TestTable \p\g' | msql TestDB

ansi: all
	JAVAOPTIONS=-DDbDriver=com.imaginary.sql.msql.MsqlDriver \
	runjava -r com CreateTables com.mortbay.JDBC.DbAdaptor \
	com.mortbay.JDBC.TestTable com.mortbay.JDBC.MultiTestTable
