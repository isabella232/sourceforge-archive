


function getMember(listItem) 
{
  var member = listItem.innerHTML;
  ajaxEngine.sendRequest('getMember',"member=" + member);
}


var JoinHandler = 
{ 
  join: function()
  {
    var name = $('joinname').value;
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
     // switch the input form
     $('join').className='hidden';
     $('joined').className='';
     Behaviour.apply();
     
     // start polling for events
     ajaxEngine.sendRequest('getEvents');
  }
};

var EventHandler = 
{
  ajaxUpdate: function(ajaxResponse) 
  {
     alert("event "+ajaxResponse);
     
     // Poll again for events
     ajaxEngine.sendRequest('getEvents');
  }
};

function initPage()
{
  ajaxEngine.registerRequest('join', "?ajax=join");
  ajaxEngine.registerAjaxObject('joined', JoinHandler);
  
  ajaxEngine.registerAjaxElement('members');
  ajaxEngine.registerRequest('getMember', "?ajax=getMember&id=member");
  ajaxEngine.registerAjaxElement('member');
  
  ajaxEngine.registerRequest('getEvents', "?ajax=getEvents&id=event");
  ajaxEngine.registerAjaxObject('event', EventHandler);
}


var behaviours = 
{
  '#members li' : function(element)
  {
    element.onclick = function()
    {
      getMember(this);
    }
  },
  
  '#joinbutton' : function(element)
  {
    element.onclick = function()
    {
      JoinHandler.join();
    }
  }
};

Behaviour.register(behaviours);
Behaviour.addLoadEvent(initPage);  

