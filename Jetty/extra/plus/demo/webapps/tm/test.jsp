<html>
  <head>
    <title>JettyPlus Demo</title>
  </head>
  <body>
    <h1>JettyPlus Demo</h1>

This demo increments of the value of <B>foo</B> and stores it in
a database. The increment and store occur in a transactional
context. Use "commit" to store the increment, or "rollback" to
perform the increment, but rollback the store operation leaving
the value unchanged.
<P>

    <%
      org.mortbay.webapps.tm.DBTest test = new org.mortbay.webapps.tm.DBTest();   
     
      int fooValue = test.readFoo();
    %>
    foo is now: <B><%= String.valueOf(fooValue) %></B>
    
    <P>
Select "commit" to increment it by one to <B> <%= String.valueOf(fooValue+1) %></B>, or "rollback" to leave
the value at <B><%= String.valueOf(fooValue) %></B>:

    <form action="testResult.jsp" method="get">
      <input type="radio" name="completion" value="commit" checked="true"> Commit<BR>
      <input type="radio" name="completion" value="rollback"> Rollback<BR>
      <P>
      <button type="submit">Completion</button>
    </form>
  </body>
</html>
