


function getMember(listItem) 
{
  var member = listItem.innerHTML;
  ajaxEngine.sendRequest('getMember',"member=" + member);
}


var JoinHandler = 
{ 
  join: function()
  {
    var name = $('username').value;
    if (name == null || name.length==0 )
    {
      alert("Please enter a username!");
    }
    else
    {
      ajaxEngine.sendRequest('join',"name=" + name);
    }
  },
  
  ajaxUpdate: function(ajaxResponse) 
  {
     if ("left" == ajaxResponse.getAttribute('id'))
     {
       // switch the input form
       $('join').className='';
       $('joined').className='hidden';
     }
     else
     {
       // switch the input form
       $('join').className='hidden';
       $('joined').className='';
       
       // start polling for events
       ajaxEngine.sendRequest('getEvents');
     }
     
     Behaviour.apply();
     
  }
};

var EventHandler = 
{
  last: "",
  
  ajaxUpdate: function(ajaxResponse) 
  {
     var event=ajaxResponse.childNodes[0];
     document.myevent=event;
     
     var chat=$('chat')
     var from=event.attributes['from'].value;
     var alert=event.attributes['alert'].value;
     var text=event.childNodes[0].data;
     
     if ( from == this.last )
        from="...";
     else
     {
        this.last=from;
        from+=":";
     }
     
     if (alert!="true")
       chat.innerHTML += "<span class=\"from\">"+from+"&nbsp;</span><span class=\"text\">"+text+"</span><br/>";
     else
       chat.innerHTML += "<span class=\"alert\"><span class=\"from\">"+from+"&nbsp;</span><span class=\"text\">"+text+"</span></span><br/>";
     
     chat.scrollTop = chat.scrollHeight - chat.clientHeight;
     
  }
};

var PollHandler = 
{
  ajaxUpdate: function(ajaxResponse) 
  {
     // Poll again for events
     ajaxEngine.sendRequest('getEvents');
  }
};




function initPage()
{
  ajaxEngine.registerRequest('join', "?ajax=join");
  ajaxEngine.registerRequest('leave', "?ajax=leave");
  ajaxEngine.registerRequest('chat', "?ajax=chat"); 
  ajaxEngine.registerRequest('getMember', "?ajax=getMember&id=member");
  ajaxEngine.registerRequest('getEvents', "?ajax=getEvents&id=event");
  
  ajaxEngine.registerAjaxElement('members');
  
  ajaxEngine.registerAjaxObject('joined', JoinHandler);
  ajaxEngine.registerAjaxObject('left', JoinHandler);
  ajaxEngine.registerAjaxObject('event', EventHandler);
  ajaxEngine.registerAjaxObject('poll', PollHandler);
  
  
}


var behaviours = 
{ 
  '#joinB' : function(element)
  {
    element.onclick = function()
    {
      JoinHandler.join();
    }
  },
  
  '#sendB' : function(element)
  {
    element.onclick = function()
    {
    	var text = $('phrase').value;
    	if (text != null && text.length>0 )
	ajaxEngine.sendRequest('chat',"text=" + text); // TODO encode ??
	$('phrase').value="";
    }
  },
  
  '#phrase' : function(element)
  {
    element.setAttribute("autocomplete","OFF");
    element.onkeypress = function(event)
    {
        if (event && (event.keyCode==13 || event.keyCode==10))
        {
          var phrase = $('phrase');
    	  var text = phrase.value;
    	  phrase.value='';
    	  if (text != null && text.length>0 )
	  ajaxEngine.sendRequest('chat',"text=" + text); // TODO encode ??
	  return false;
	}
	return true;
    }
  },
  
  '#leaveB' : function(element)
  {
    element.onclick = function()
    {
      ajaxEngine.sendRequest('leave');
    }
  },
};

Behaviour.register(behaviours);
Behaviour.addLoadEvent(initPage);  

