// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Servlets;

import com.mortbay.Base.Code;
import com.mortbay.Util.Converter;
import com.mortbay.Util.ConverterSet;
import com.mortbay.Util.DictionaryConverter;
import com.mortbay.Util.ArrayConverter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Array;

/** Class to aid in servlet method dispatching and argument parsing
 * <h4>Method Dispatching</h4>
 * This class takes a HttpRequest and looks through the pathInfo trying
 * to call a matching method on the given object. Methods are matched by
 * looking for a method matching the first part of the path info in the
 * request. The ServletDispatch object caches the path info so that it can be
 * called again to call out to another function on a lower level object.
 *
 * There is no requirement for the ServletDispatch method to dispatch to a
 * Servlet Object - It can dispatch calls to any Object (althoug one
 * implementing ServletDispatchHandler is often best...).
 *
 * The matching method has its signature checked before it is called. The
 * parameters the method takes are checked and the following objects may be
 * taken as parameters: The HttpServletRequest, the HttpServletResponse, The
 * ServletDispath object itself and the context argument passed to the
 * dispatch call. These arguments can be accepted in any order. In addition,
 * other parameters can be accepted by the method under certain conditions
 * (see below).
 *
 * If no matching method is found, then if the Object implements
 * ServletDispatchHandler, its defaultHandler method is called to handle the
 * request. If not, the dispatch method returns null.
 *
 * The method can return any type which will be returned by the dispatch
 * method.
 *
 * <p><h4>Argument Parsing</h4>
 * The ServletDispatch object has several methods to aid in parsing the
 * parameters out of the HttpServletRequest object.
 *
 * The first is the static method parseArg. Given the HttpServletRequest,
 * different variables can be parsed and converted from their string values
 * into native java types. See the parseArg function for an example. The
 * functions parseLongArg, parseDoubleArg and parseBooleanArg handle the native
 * java types.
 *
 * Alternatively, an Object can be defined which has the required types
 * defined as public data members and it can be passed to the initArgObject
 * method on the ServletDispatch object which will populate the data members
 * of the function from the request parameters. See the initArgObject method
 * for an example.
 *
 * The final method allows the programmer to define an Argument object and
 * list it as one of the parameters accepted by their function. The dispatch
 * method will create and initialise the object and pass it to the called
 * function.
 *
 * <p><h4>Usage</h4>
 * <pre>
 * public void doGet(HttpServletRequest req, HttpServletResponse res) {
 *     ServletDispatch disp = new ServletDispatch(req, res);
 *     if (!disp.dispatch(this, null)){
 *         // handle error...
 *     }
 * }
 * public boolean Add(ServletDispatch disp,
 *                    HttpServletRequest req,
 *                    HttpServletResponse res) {
 *     int foo[] = null;
 *     foo = (int[])ServletDispatch.parseArg(foo, "foo", req);
 *     int blah = (int)ServletDispatch.parseLongArg(3, "blah", req);
 *     String var = "not set";
 *     String var = ServletDispatch.parseArg(var, "var", req);
 *     // ...
 *     return disp.dispatch(this, null); // Call out to next part of path
 * }
 * public static Object Args{
 *     int count = 0;
 *     String name = null;
 *     int values[] = null;
 * }
 * public void Delete(ServletDispatch disp, HttpServletResponse res){
 *     Args args = new Args();
 *     disp.initArgObject(args);
 *     // ...
 * }
 * public boolean defaultDispatch(String method,
 *				  ServletDispatch dispatch,
 *				  Object context,
 *				  HttpServletRequest req,
 *				  HttpServletResponse res) {
 *     // ...
 * }
 * </pre>
 *
 * @see com.mortbay.Util.ServletNode
 * @see com.mortbay.Util.ServletDispatchHandler
 * @version $Version: $
 * @author Matthew Watson (watsonm)
 */
public class ServletDispatch
{
    /* ------------------------------------------------------------ */
    private HttpServletRequest req;
    private HttpServletResponse res;
    private Vector path = null;
    private String processedPath = null;
    private Hashtable paramsHT = null;
    private static Converter converter = null;
    /* ------------------------------------------------------------ */
    static {
	ConverterSet cs = new ConverterSet();
	// The basic types
	cs.registerPrimitiveConverters();
	// For the complex objects
	cs.register(new DictionaryConverter());
	// And for multi-value params to arrays
	cs.register(new ArrayConverter(","));
	converter = cs;
    }
    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param req
     * @param res 
     */
    public ServletDispatch(HttpServletRequest req, HttpServletResponse res){
	this.req = req;
	this.res = res;
    }
    /* ------------------------------------------------------------ */
    /** Dispatch the servlet request to a named method on the given object
     * @param obj The object to dispatch the request to
     * @param context The context object to pass to the called function.
     * @return null if it cannot be dispatched
     * @exception java.lang.reflect.InvocationTargetException 
     * @exception java.lang.IllegalAccessException
     * @exception java.lang.InstantiationException
     */
    public Object dispatch(Object obj, Object context)
	throws java.lang.reflect.InvocationTargetException,
	       java.lang.IllegalAccessException,
	       java.lang.InstantiationException
    {
	if (path == null){
	    path = new Vector();
	    String pathi = req.getPathInfo();
	    StringTokenizer st = new StringTokenizer(pathi, "/");
	    while (st.hasMoreTokens())
		path.addElement(st.nextToken());
	}
	if (path.size() == 0) 
	    return doDefaultDispatch(obj, null, context, req, res);
	String funcName = path.firstElement().toString();
	processedPath = processedPath + "/" + funcName;
	path.removeElementAt(0);
	Method[] methods = obj.getClass().getMethods();
	int i;
	for (i = 0; i < methods.length; i++){
	    if (methods[i].getName().equals(funcName)) break;
	}
	if (i == methods.length)
	    return doDefaultDispatch(obj, funcName, context, req, res);
	Method method = methods[i];
	Class paramTypes[] = method.getParameterTypes();
	Object params[] = new Object[paramTypes.length];
	for (i = 0; i < paramTypes.length; i++){
	    if (paramTypes[i].isInstance(this))
		params[i]  = this;
	    else if (paramTypes[i].isInstance(req))
		params[i]  = req;
	    else if (paramTypes[i].isInstance(res))
		params[i]  = res;
	    else if (paramTypes[i].isInstance(context)){
		params[i] = context;
	    } else {
		// Handle an arbitrary param type
		Object param = paramTypes[i].newInstance();
		initArgObject(param);
		params[i] = param;
	    }
	}
	return method.invoke(obj, params);
    }
    /* ------------------------------------------------------------ */
    /** 
     * @return The part of the request pathInfo that has been processed so
     *		far in the dispatch process.
     */
    public String getProcessedPathInfo(){
	return processedPath;
    }
    /* ------------------------------------------------------------ */
    /** 
     * @return The part of the request path (including the servlet path) that
     *		has been processed so far in the dispatch process.
     */
    public String getProcessedPath(){
	return req.getServletPath() + processedPath;
    }
    /* ------------------------------------------------------------ */
    /** Initialise an argument from the request parameters
     * <p> E.g. <pre>
     * {
     *        int foo[];
     *        foo = (int[])ServletDispatch(foo, "foo", req);
     *        //...
     * </pre>
     * @param defaultValue The default value to give the object (must be the
     * same type as the object, since it is used to determine the type to
     * convert the parameter to...)
     * @param name The name of the parameter
     * @param req The request
     * @return The value for the object
     */
    public static Object parseArg(Object defaultValue, String name,
				  HttpServletRequest req)
    {
	try {
	    String param = req.getParameter(name);
	    if (param == null) return null;
	    Object val =
		converter.convert(param, defaultValue.getClass(), converter);
	    if (val != null) return val;
	} catch (Exception ex){
	    Code.debug(ex);
	}
	return defaultValue;
    }
    /* ------------------------------------------------------------ */
    /** Version of parseArg to handle longs (and short and int)
     * @param defaultValue If the param is not set or not parseable
     * @param name Name of the param
     * @param req The Request
     * @return The value
     */
    public static long parseLongArg(long defaultValue,
				    String name,
				    HttpServletRequest req)
    {
	Number n = (Number)parseArg(new Long(1), name, req);
	if (n == null) return defaultValue;
	return n.longValue();
    }
    /* ------------------------------------------------------------ */
    /** Version of parseArg to handle doubles (and floats)
     * @param defaultValue If the param is not set or not parseable
     * @param name Name of the param
     * @param req The Request
     * @return The value
     */
    public static double parseDoubleArg(double defaultValue,
					String name,
					HttpServletRequest req)
    {
	Number n = (Number)parseArg(new Double(1.0), name, req);
	if (n == null) return defaultValue;
	return n.doubleValue();
    }
    /* ------------------------------------------------------------ */
    /** Version of parseArg to handle booleans
     * @param defaultValue If the param is not set or not parseable
     * @param name Name of the param
     * @param req The Request
     * @return The value
     */
    public static boolean parseBooleanArg(boolean defaultValue,
					String name,
					HttpServletRequest req)
    {
	Boolean b = (Boolean)parseArg(Boolean.TRUE, name, req);
	if (b == null) return defaultValue;
	return b.booleanValue();
    }
    /* ------------------------------------------------------------ */
    /** Initialise an arbitrary Object from the request parameters.
     * @param toInit The object to initialise. If parameters exist
     * corresponding to the names of the public data members of this Object,
     * then they will be initialised.
     * E.g. <pre>
     *    public static Object Args{
     *        int count = 0;
     *        String name = null;
     *        int values[] = null;
     *    }
     *    public void Add(ServletDispatch disp, HttpServletResponse res){
     *        Args args = new Args();
     *        disp.initArgObject(args);
     *        // ...
     * </pre>
     */
    public void initArgObject(Object toInit) {
	if (paramsHT == null){
	    paramsHT = new Hashtable();
	    for (Enumeration enum = req.getParameterNames();
		 enum.hasMoreElements();)
	    {
		Object key = enum.nextElement();
		paramsHT.put(key, req.getParameter(key.toString()));
	    }
	}
	DictionaryConverter.fillObject(toInit, paramsHT, converter);
    }
    /* ------------------------------------------------------------ */
    private Object doDefaultDispatch(Object obj,
				     String funcName,
				     Object context,
				     HttpServletRequest req,
				     HttpServletResponse res)
	throws InvocationTargetException,
	       IllegalAccessException,
	       InstantiationException
    {
	if (obj instanceof ServletDispatchHandler)
	    try {
		return ((ServletDispatchHandler)obj).defaultDispatch(funcName,
								     this,
								     context,
								     req,
								     res);
	    } catch (InvocationTargetException ex){
		throw ex;
	    } catch (IllegalAccessException ex){
		throw ex;
	    } catch (InstantiationException ex){
		throw ex;
	    } catch (Exception ex){
		throw new InvocationTargetException(ex);
	    }
	else
	    return null;
    }
    /* ------------------------------------------------------------ */
};
