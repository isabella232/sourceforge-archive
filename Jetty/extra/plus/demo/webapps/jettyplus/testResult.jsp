<html>
  <head>
    <title>JettyPlus Demo</title>
  </head>
  <body>
    <h1>JettyPlus Demo</h1>

    <%
      org.mortbay.webapps.jettyplus.DBTest test = new org.mortbay.webapps.jettyplus.DBTest();   
      test.doIt(request.getParameter("completion")); 
    %>
    
    You chose to: <B><%= request.getParameter("completion") %></B>
<BR>
    foo is now: <B><%= test.readFoo() %></B>
    
<P>
    <A HREF="/jettyplus/test.jsp"> Go again</A>
  </body>
</html>
