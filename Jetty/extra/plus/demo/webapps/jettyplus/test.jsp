<html>
  <head>
    <title>JettyPlus Demo</title>
  </head>
  <body>
    <h1>JettyPlus XADataSource Demo</h1>

This demo increments of the value of <B>foo</B> and stores it in
a database. The increment and store occur in a transactional
context. Use "commit" to store the increment, or "rollback" to
perform the increment, but rollback the store operation leaving
the value unchanged.
<P>

    <%
     int fooValue = org.mortbay.webapps.jettyplus.DBTest.readFoo();
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
    
    
    <h1> JettyPlus non-XA DataSource demo</h1>
    
    This demo increments the value of <B>foo</B> to <B><%= String.valueOf(fooValue+1) %></B> and stores it in the database by using an ordinary
    (ie non-XA) DataSource.
    <P>
    <B>foo is currently</B> <B><%= String.valueOf(fooValue) %></B>. Click "submit" to reset it:
    
    
    <form action="testResult.jsp" method="get">
      <input type="hidden" name="regular" value="regular"/>
      <button type="submit">Go</button>
    </form>
    
    
    <h1> JettyPlus Pooled DataSource demo </h1>
    
    This demo increments the value of <B>foo</B> to <B><%= String.valueOf(fooValue+1) %></B> and stores it in the database using a connection pooling 
    datasource.
    <P>
    <B>foo is currently</B> <B><%= String.valueOf(fooValue) %></B>. Click "submit" to reset it:
        
    <form action="testResult.jsp" method="get">
      <input type="hidden" name="pooled" value="pooled"/>
      <button type="submit">Go</button>
    </form>
    
    </P>
  </body>
</html>
