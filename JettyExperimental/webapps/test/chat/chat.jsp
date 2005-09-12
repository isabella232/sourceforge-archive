<%@ page contentType="text/html; charset=UTF-8" %>
<html>
<head>
    <title>Jetty chat</title>
    <script type="text/javascript" src="/test/js/default.js"></script>
    <script type="text/javascript" src="chat.js"></script>
    <link rel="stylesheet" type="text/css" href="chat.css"></link>
</head>
<body>
<h1>chat</h1>

<div id="chatroom">

 <div id="chat">
 </div>
 
 <div id="member">
  member
 </div>
 
 <div id="members">
  members
 </div>
 
 <div id="input">
   <div id="join" >
     Username:&nbsp;<input class="input" id="joinname" type="text"/><input id="joinbutton" type="submit" name="join" value="Join"/>
   </div>
   <div id="joined" class="hidden">
     Chat:&nbsp;<input class="input" type="text"></input> 
   </div>
 </div>
 
</body>
