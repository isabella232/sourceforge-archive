//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package com.acme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mortbay.ajax.AjaxFilter;
import org.mortbay.jetty.util.Continuation;
import org.mortbay.jetty.util.ContinuationSupport;

public class ChatFilter extends AjaxFilter
{
    public static final String 
    JOIN_CHAT="join",    
    GET_EVENTS="getEvents",
    CHAT="chat",
    LEAVE_CHAT="leave";
        
    private final String mutex="mutex";
    private Map chatroom;
    

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.ajax.AjaxFilter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        chatroom=new HashMap();
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.ajax.AjaxFilter#destroy()
     */
    public void destroy()
    {
        super.destroy();
        chatroom=null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.ajax.AjaxFilter#handle(java.lang.String, javax.servlet.http.HttpServletRequest, org.mortbay.ajax.AjaxFilter.AjaxResponse)
     */
    public void handle(String method, HttpServletRequest request, AjaxResponse response)
    {
        if (JOIN_CHAT.equals(method))
            doJoinChat(request,response);
        else if (GET_EVENTS.equals(method))
            doGetEvents(request,response);
        else if (JOIN_CHAT.equals(method))
            doLeaveChat(request,response);
        else
            super.handle(method, request, response);                
    }

    /* ------------------------------------------------------------ */
    private void doJoinChat(HttpServletRequest request, AjaxResponse response)
    {
        HttpSession session = request.getSession(true);
        String id = session.getId();
        String name=request.getParameter("name");
        if (name==null || name.length()==0)
            name="Newbie";
        Member member=null;
        
        synchronized (mutex)
        {
            if (chatroom.containsKey(id))
            {
                // exists already, so just update name
                member=(Member)chatroom.get(id);
                member.rename(name);
            }
            else
            {
                member = new Member(session,name);
                chatroom.put(session.getId(),member);
                member.joinChat();
            }
            
            sendMembers(response);
            response.objectResponse("joined", "<ok/>");
        }
    }
    


    /* ------------------------------------------------------------ */
    private void doLeaveChat(HttpServletRequest request, AjaxResponse response)
    {
        // TODO Auto-generated method stub
        
    }


    /* ------------------------------------------------------------ */
    private void doGetEvents(HttpServletRequest request, AjaxResponse response)
    {
        HttpSession session = request.getSession(true);
        String id = session.getId();
        
        synchronized (mutex)
        {
            Member member = (Member)chatroom.get(id);
            if (member==null || !member.isValid())
                return;
            
            synchronized (member)
            {
                // Do we have a continuation (ie has this request been tried before)?
                Continuation continuation = ContinuationSupport.getContinutaion(request, false);
        
                // If we don't have a continuation, do we need one (because we have no events to return)?
                if (continuation==null && !member.hasEvents())
                    continuation = ContinuationSupport.getContinutaion(request, true);
                
                System.err.println("continuation="+continuation+" isNew="+continuation.isNew());
                
                // If we have a new continuation, put it in the member object so it can be resumed.
                if (continuation.isNew())
                    member.setContinuation(continuation);
                
                // Get the continuation object (may wait and/or retry request here).  For this demo we don't need return value. 
                if (continuation!=null) continuation.getObject(10000L);
                
                member.setContinuation(null);
                member.sendEvents(response);
            }
        } 
    }

    /* ------------------------------------------------------------ */
    private void sendEvent(Member member, String text, boolean alert)
    {
        Event event=new Event(member.getName(),text,alert);
        
        ArrayList invalids=null;
        synchronized (mutex)
        {
            Iterator iter = chatroom.values().iterator();
            while (iter.hasNext())
            {
                Member m = (Member)iter.next();
                if (m.isValid())
                    m.addEvent(event);
                else
                {
                    if (invalids==null)
                        invalids=new ArrayList();
                    invalids.add(m);
                    iter.remove();
                }
            }
        }
            
        for (int i=0;invalids!=null && i<invalids.size();i++)
        {
            Member m = (Member)invalids.get(i);
            m.leaveChat();
        }
    }
    
    private void sendMembers(AjaxResponse response)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<ul>\n");
        synchronized (mutex)
        {
            Iterator iter = chatroom.values().iterator();
            while (iter.hasNext())
            {
                Member m = (Member)iter.next();
                buf.append("<li>");
                buf.append(encodeText(m.getName()));
                buf.append("</li>\n");
            }
        }
        buf.append("</ul>\n");
        response.elementResponse("members", buf.toString());
    }

    
    private static class Event
    {
        private String _from;
        private String _text;
        private boolean _alert;
        
        Event(String from, String text, boolean alert)
        {
            _from=from;
            _text=text;
            _alert=alert;
        }
        
        public String toString()
        {
            return "<event from=\""+_from+"\" text=\""+_text+"\" alert=\""+_alert+"\"/>";
        }
        
    }

    private class Member
    {
        private HttpSession _session;
        private String _name;
        private List _events = new ArrayList();
        private Continuation _continuation;
        
        Member(HttpSession session, String name)
        {
            _session=session;
            _name=name;
        }

        /* ------------------------------------------------------------ */
        public boolean isValid()
        {
            return _session!=null;
        }
        
        /* ------------------------------------------------------------ */
        /**
         * @return Returns the name.
         */
        public String getName()
        {
            return _name;
        }

        /* ------------------------------------------------------------ */
        /**
         * @param name The name to set.
         */
        public void setName(String name)
        {
            _name = name;
        }

        /* ------------------------------------------------------------ */
        /**
         * @return Returns the session.
         */
        public HttpSession getSession()
        {
            return _session;
        }

        /* ------------------------------------------------------------ */
        /**
         * @return Returns the continuation.
         */
        public Continuation getContinuation()
        {
            return _continuation;
        }

        /* ------------------------------------------------------------ */
        /**
         * @param continuation The continuation to set.
         */
        public void setContinuation(Continuation continuation)
        {
            _continuation = continuation;
        }
        
        /* ------------------------------------------------------------ */
        public void addEvent(Event event)
        {
            synchronized (this)
            {
                _events.add(event);
                System.err.print("addEvent "+_continuation);
                if (_continuation!=null)
                {
                    _continuation.resume(event);
                }
            }
        }

        /* ------------------------------------------------------------ */
        public boolean hasEvents()
        {
            System.err.println("hasEvents "+_events);
            return _events!=null && _events.size()>0;
        }
        
        /* ------------------------------------------------------------ */
        public void joinChat()
        {
            ChatFilter.this.sendEvent(this,getName()+" has joined the chat",true);
        }
        
        /* ------------------------------------------------------------ */
        public void leaveChat()
        {
            ChatFilter.this.sendEvent(this,getName()+" has left the chat",true);
            _session=null;
            _events=null;
            _continuation=null;
        }
        
        /* ------------------------------------------------------------ */
        public void rename(String name)
        {
            String oldName = getName();
            setName(name);
            ChatFilter.this.sendEvent(this,name+" has changed their name from "+oldName,true);
        }

        /* ------------------------------------------------------------ */
        public void sendEvents(AjaxResponse response)
        {
            synchronized (this)
            {
                for (int i=0;i<_events.size();i++)
                {
                    Event event = (Event)_events.get(i);
                    response.objectResponse("event", event.toString());
                }
                _events.clear();
            }
        }

    }
}
