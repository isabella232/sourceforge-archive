<HTML>
<HEAD>
	<TITLE>JSP date page</TITLE>
	<%@ page import="java.util.Date" %>
</HEAD>
<BODY>

<H1>JSP date page</H1>

The current date is <%= new Date() %>.
<P>
Include Hello World:<UL>
<%@ include file="hello.jsp"%> 
<HL>
<%@ include file="snoop.jsp?A=1&B=2"%> 
<HL>
</UL>Included
</BODY>
</HTML>
