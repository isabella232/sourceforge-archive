<html>
  <head>
    <title>JettyPlus Demo</title>
  </head>
  <body>
    <h1>JettyPlus Demo</h1>

    <%
	  if (null != request.getParameter("regular"))
	      org.mortbay.webapps.jettyplus.DBTest.doItNonXA();
	  else if (null!=request.getParameter("pooled"))
	    org.mortbay.webapps.jettyplus.DBTest.doItPooled();
	  else
	  {
        org.mortbay.webapps.jettyplus.DBTest.doIt(request.getParameter("completion")); 
	  }
    %>
    
    
<BR>
    foo is now: <B><%= org.mortbay.webapps.jettyplus.DBTest.readFoo() %></B>
    
<P>
    <A HREF="/jettyplus/test.jsp"> Go again</A>
  </body>
</html>
