<html>
<body>
	<h1>The Famous JSP Hello Program</h1>
	<% String s = "Jesper"; %>
	<% if (s!=null) throw new NullPointerException(); %>
The following line should contain the text "Hello Jesper World!".
<br>If thats not the case start debugging ...
<p>
Hello <%= s %> World!
</body>
</html>
 
