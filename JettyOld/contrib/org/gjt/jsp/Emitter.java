/*
  GNUJSP - a free JSP1.0 implementation
  Copyright (C) 1999, Yaroslav Faybishenko <yaroslav@cs.berkeley.edu>

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

package org.gjt.jsp;

import java.io.Writer;
import java.io.IOException;
import java.util.Hashtable;
   
abstract class Emitter 
    implements JspMsg 
{
    private static Hashtable concreteEmitters = new Hashtable();

    /** Register an emitter for a specific language. */
    public static void setEmitter(String language, Emitter emitter) {
	concreteEmitters.put(language.toLowerCase(), emitter);
    }

    /** Retrieve an emitter for a specific language. */
    public static Emitter getEmitter(String language) {
	return (Emitter) concreteEmitters.get(language.toLowerCase());
    }

    /** Generate a language-specific source file on the Writer for the page. */
    public static void emit(Writer w, JspNode node) throws IOException {
	String language = node.getAttribute("language");
	if (language == null) language = "java";

	Emitter em = getEmitter(language);
	if (em == null) 
	    throw new ParseException(
		JspConfig.getLocalizedMsg(ERR_sp10_2_7_1_no_mapping_for_language)
		+ ": " + language);
	emit(em,w,node);
    }

    private static void emit(Emitter em, Writer w, JspNode node) throws IOException {
	if (node == null) return;
	switch (node.getType()) {
	case JspNode.JSP_BODY:
	    em.emitBeginPage(w, node);
	    emit(em, w, node.getFirstChild());
	    em.emitEndPage(w, node);
	    break;
	case JspNode.TEMPLATE_TEXT:
	    em.emitTemplateText(w, node, node.getAttribute("text"));
	    break;
	case JspNode.JSP_EXPRESSION:
	    em.emitBeginPrintExprCall(w, node);
	    em.emitPrintExprCall(w, node, node.getFirstChild().getAttribute("text"));
	    em.emitEndPrintExprCall(w);
	    break;
	case JspNode.JSP_SCRIPTLET:
	    em.emitBeginScriptlet(w, node);
	    em.emitScriptlet(w, node, node.getFirstChild().getAttribute("text"));
	    em.emitEndScriptlet(w);
	    break;
	case JspNode.JSP_INCLUDE:
	    em.emitInclude(w, node, node.getAttribute("page"), node.getAttribute("flush"));
	    break;
	case JspNode.JSP_FORWARD:
	    em.emitForward(w, node, node.getAttribute("page"));
	    break;
	case JspNode.JSP_USEBEAN:
	    em.emitBeginUseBean(w, node,
				node.getAttribute("id"),
				node.getAttribute("scope"),
				node.getAttribute("type"),
				node.getAttribute("class"),
				node.getAttribute("beanName"));
	    emit(em,w,node.getFirstChild());
	    em.emitEndUseBean(w);
	    break;
	case JspNode.JSP_GETPROPERTY:
	    em.emitGetProperty(w, node,
			       node.getAttribute("name"),
			       node.getAttribute("property"));
	    break;
	case JspNode.JSP_SETPROPERTY:
	    em.emitSetProperty(w, 
			       node,
			       node.getAttribute("name"),
			       node.getAttribute("property"),
			       node.getAttribute("param"),
			       node.getAttribute("value"));
	    break;
	}
	emit(em,w,node.getNextSibling());
    }

    /** Emit the beginning chunk of a page. */
    abstract void emitBeginPage (Writer w, JspNode n) throws IOException;

    /** Emit the ending chunk of a page. */
    abstract void emitEndPage   (Writer w, JspNode n) throws IOException;

    /** Emit the beginning of a JSP scriptlet. */
    abstract void emitBeginScriptlet (Writer w, JspNode n) throws IOException;

    /** Emit a JSP scriptlet. */
    abstract void emitScriptlet (Writer w, JspNode n, String code) throws IOException;

    /** Emit the end of a JSP scriptlet. */
    abstract void emitEndScriptlet (Writer w) throws IOException;

    /** Emit the beginning of a JSP expression. */
    abstract void emitBeginPrintExprCall (Writer w, JspNode n) throws IOException;

    /** Emit a JSP expression. */
    abstract void emitPrintExprCall (Writer w, JspNode n, String expr) throws IOException;

    /** Emit the end of a JSP expression, after the body. */
    abstract void emitEndPrintExprCall   (Writer w) throws IOException;

    /** Emit a print statement for template text. */
    abstract void emitTemplateText (Writer w, JspNode n, String t) throws IOException;

    /** Emit code for a dynamic include. */
    abstract void emitInclude (Writer w, JspNode n, String path, String flush) throws IOException;
    
    /** Emit code for dynamic forwarding. */
    abstract void emitForward (Writer w, JspNode n, String path) throws IOException;

    /** Emit the beginning of a useBean section. */
    abstract void emitBeginUseBean (Writer w, JspNode n, String id, String scope,
			   String type, String clas, String beanName)
	throws IOException ;

    /** Emit the end of a useBean section (closing its scope). */
    abstract void emitEndUseBean   (Writer w)
	throws IOException;

    /** Emit code for a getProperty statement. */
    abstract void emitGetProperty (Writer w, JspNode n, String name, String property)
	throws IOException;

    /** Emit code for a setProperty statement. */
    abstract void emitSetProperty (Writer w, JspNode n, String name, String property,
			  String param, String value)
	throws IOException;
}
