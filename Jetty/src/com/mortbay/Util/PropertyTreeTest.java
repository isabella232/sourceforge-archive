// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import java.io.FileInputStream;
import java.util.*;

public class PropertyTreeTest
{
    /* ------------------------------------------------------------ */
    public static void test()
    {
        Test test = new Test("PropertyTree");
        try
        {
            PropertyTree props = new PropertyTree();
            props.load(new FileInputStream("PropertyTreeTest.prp"));
            Code.debug("tree=",props);
            
            test.checkEquals(props.get("a"), "1", "a");
            test.checkEquals(props.get("a.b"), "2", "a.b");
            test.checkEquals(props.get("a.B"), "3", "a.B");
            test.checkEquals(props.get("a.b.c"), "4", "a.b.c");
            test.checkEquals(props.get("a.b.C"), "5", "a.b.C");
            test.checkEquals(props.get("a.B.c"), "6", "a.B.c");
            test.checkEquals(props.get("a.B.C"), "7", "a.B.C");
            test.checkEquals(props.get("a.*.C"), null, "a.*.C");
            test.checkEquals(props.get("X.X.X"), null, "X.X.X");


            test.checkEquals(props.get("X"), "10", "X");
            test.checkEquals(props.get("X.y.z"),"11", "X.y.z");
            test.checkEquals(props.get("x.Y.z"),"12", "x.Y.z");
            test.checkEquals(props.get("x.y.Z"),"13", "x.y.Z");
            test.checkEquals(props.get("x.y.z"),"14", "x.y.z");
            test.checkEquals(props.get("X.Y"),"15", "X.Y");
            
            test.checkEquals(props.get("A.b.C.d.E"), "21", "A.b.C.d.E");
            test.checkEquals(props.get("A.B.C.D.E"), "24", "A.B.C.D.E");

            PropertyTree sub = props.getTree("a.b");
            Code.debug("getTree(a.b)=",sub);
            
            test.checkEquals(sub.get("c"), "4", "[a.b.]c");
            test.checkEquals(sub.get("C"), "5", "[a.b.]C");
            test.checkEquals(sub.get("*.C"), null, "[a.*.]C");
            
            
            sub = props.getTree("a");
            Code.debug("getTree(a)=",sub);
            
            test.checkEquals(sub.get("b"), "2", "[a.]b");
            test.checkEquals(sub.get("B"), "3", "[a.]B");
            test.checkEquals(sub.get("b.c"), "4", "[a.]b.c");
            test.checkEquals(sub.get("b.C"), "5", "[a.]b.C");
            test.checkEquals(sub.get("B.c"), "6", "[a.]B.c");
            test.checkEquals(sub.get("B.C"), "7", "[a.]B.C");
            test.checkEquals(sub.get("*.C"), null, "[a.]*.C");
            test.checkEquals(sub.get("X.X"), null, "[X.]X.X");
            test.checkEquals(sub.get("Y.z"),null, "[x.]Y.z");
            test.checkEquals(sub.get("y.Z"),null, "[x.]y.Z");
            test.checkEquals(sub.get("y.z"),"11", "[x.]y.z");
            test.checkEquals(sub.get("Y"),"15", "[X.]Y");
            
            test.checkEquals(sub.get("b.C.d.E"), null, "[A.]b.C.d.E");
            test.checkEquals(sub.get("B.C.D.E"), "22", "[A.]B.C.D.E");

            
            props=new PropertyTree();
            String[] init =
            {//  0   1       2       3       4       5     6     7     8       9       10
                "*","*.b.c","a.*.c","a.b.*","*.b.*","a.*","*.b","*.B","a.b.c","a.*.b","a.*.B"
            };
            for (int i=0;i<init.length;i++)
                props.put(init[i],new Integer(i));

            sub=props.getTree("a.b");
            
            Code.debug("getTree(a.b)=",sub);
            test.checkEquals(sub.toString(),
                             "{c=8, b=9, B=10, *=3}", // XXX could be platform order?
                             "SubTree get");
        
            sub=props.getTree("a");
        
            test.checkEquals(sub.get("*.b"),new Integer(9),"subtree get");
        
        
            sub=sub.getTree("b");       
            test.checkEquals(sub.toString(),
                             "{c=8, b=9, B=10, *=3}", // XXX could be platform order?
                             "SubTree");

            Enumeration e=sub.getRealNodes();
            test.check(e.hasMoreElements(),"getRealNodes");
            test.checkEquals(e.nextElement(),"*","getRealNodes");
            test.check(e.hasMoreElements(),"getRealNodes");
            test.checkEquals(e.nextElement(),"c","getRealNodes");
            test.check(!e.hasMoreElements(),"getRealNodes");
        
        
            Properties clone = (Properties)sub.clone();
            test.checkEquals(clone.toString(),
                             "{c=8, b=9, B=10, *=3}",
                             "Clone");
        
            sub.put("C","C");
            test.checkContains(props.toString(),"a.b.C=C","Subtree changed");
            clone.put("C","X");
            test.checkContains(props.toString(),"a.b.C=C","clone changed");
            sub.put("*.B","B");
            test.checkContains(props.toString(),"a.b.*.B=B","Subtree changed");
            test.checkContains(props.toString(),"a.*.B=10","Subtree changed");

            e=sub.elements();
            String v=sub.toString();
            while(e.hasMoreElements())
            {
                String ev="="+e.nextElement();
                test.checkContains(v,ev,"Elements");
                v=v.substring(0,v.indexOf(ev))+v.substring(v.indexOf(ev)+ev.length());
            }
        
            Vector nodes;
            nodes=enum2vector(props.getNodes(""));
            test.checkEquals(nodes.size(),2,"Get root node");
            test.check(nodes.contains("a"),"Get root node");
            test.check(nodes.contains("*"),"Get root node");
        
            nodes=enum2vector(props.getNodes("a"));
            test.checkEquals(nodes.size(),2,"Get a node");
            test.check(nodes.contains("b"),"Get a node");
            test.check(nodes.contains("*"),"Get a node");
        
            nodes=enum2vector(props.getNodes("*"));
            test.checkEquals(nodes.size(),2,"Get wild node");
            test.check(nodes.contains("b"),"Get wild node");
            test.check(nodes.contains("B"),"Get wild node");
        
            nodes=enum2vector(props.getNodes("a.*"));
            test.checkEquals(nodes.size(),3,"Get node");
            test.check(nodes.contains("b"),"Get node");
            test.check(nodes.contains("B"),"Get node");
            test.check(nodes.contains("c"),"Get node");

            // wild sub trees
            props=new PropertyTree();
            String[] init2 =
            {//  0   1     2     3       4       5       6
                "*","*.A","*.C","a.*.A","a.*.B","a.b.A","a.*"
            };
            for (int i=0;i<init2.length;i++)
                props.put(init2[i],new Integer(i));
            sub=props.getTree("a.*");
            String subs=sub.toString();
            Code.debug(subs);
        
            test.checkContains(subs,"A=3","wild tree A=3");
            test.checkContains(subs,"B=4","wild tree B=4");
        
            sub.put("*.C",new Integer(7));
            sub.put("*",new Integer(8));
            test.checkContains(sub.toString(),"*.C=7","mod wild tree *.C=7");
            test.checkContains(sub.toString(),"*=8","mod wild tree *=8");

            String propss=props.toString();
            Code.debug(propss);
            test.checkContains(propss,"*.C=2","mod wild tree *.C=2");
            test.checkContains(propss,"a.*.*.C=7","mod wild tree a.*.C=7");
            test.checkContains(propss,"*=0","mod wild tree *=0");
            test.checkContains(propss,"a.*.*=8","mod wild tree a.*=8");
        
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }
    
    
    /* ------------------------------------------------------------ */
    static Vector enum2vector(Enumeration e)
    {
        Vector v = new Vector();
        while (e.hasMoreElements())
            v.addElement(e.nextElement());
        return v;
    }
    
};

