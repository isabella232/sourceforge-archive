package org.mortbay.jndi;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Properties;
import javax.naming.Binding;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.NameParser;
import javax.naming.spi.NamingManager;
import javax.naming.OperationNotSupportedException;
import javax.naming.NotContextException;
import javax.naming.Reference;
import org.mortbay.util.Code;


/*------------------------------------------------*/    
/** NamingContext
 * <p>Implementation of Context interface.
 *
 * <p><h4>Notes</h4>
 * <p>All Names are expected to be Compound, not Composite.
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
* </pre>
*
* @see
*
* @author <a href="mailto:janb@mortbay.com">Jan Bartel</a>
* @version 1.0
*/
public class NamingContext implements Context, Cloneable
{
    public static final Enumeration EMPTY_ENUM = new Enumeration ()
        {       
            public boolean hasMoreElements()
            {
                return false;
            }
            
            public Object nextElement ()
            {
                throw new NoSuchElementException();
            }
        };

    protected Context _parent = null;
    protected String _name = null;
    protected Hashtable _env = null;
    protected Hashtable _bindings = new Hashtable();
    protected NameParser _parser = null;



    /*------------------------------------------------*/    
    /** NameEnumeration
     * <p>Implementation of NamingEnumeration interface.
     *
     * <p><h4>Notes</h4>
     * <p>Used for returning results of Context.list();
     *
     * <p><h4>Usage</h4>
     * <pre>
     */
    /*
     * </pre>
     *
     * @see
     *
     */
    public class NameEnumeration implements NamingEnumeration
    {
        Enumeration _delegate;

        public NameEnumeration (Enumeration e)
        {
            _delegate = e;
        }

        public void close()
            throws NamingException
        {
        }

        public boolean hasMore ()
            throws NamingException
        {
            return _delegate.hasMoreElements();
        }

        public Object next()
            throws NamingException
        {
            Binding b = (Binding)_delegate.nextElement();
            return new NameClassPair (b.getName(), b.getClassName(), true);
        }

        public boolean hasMoreElements()
        {
            return _delegate.hasMoreElements();
        }

        public Object nextElement()
        {
            Binding b = (Binding)_delegate.nextElement();
            return new NameClassPair (b.getName(), b.getClassName(), true);
        }
    }






    /*------------------------------------------------*/    
    /** BindingEnumeration
     * <p>Implementation of NamingEnumeration
     *
     * <p><h4>Notes</h4>
     * <p>Used to return results of Context.listBindings();
     *
     * <p><h4>Usage</h4>
     * <pre>
     */
    /*
     * </pre>
     *
     * @see
     *
     */
    public class BindingEnumeration implements NamingEnumeration
    {       
        Enumeration _delegate;

        public BindingEnumeration (Enumeration e)
        {
            _delegate = e;
        }

        public void close()
            throws NamingException
        {
        }

        public boolean hasMore ()
            throws NamingException
        {
            return _delegate.hasMoreElements();
        }

        public Object next()
            throws NamingException
        {
            Binding b = (Binding)_delegate.nextElement();
            return new Binding (b.getName(), b.getClassName(), b.getObject(), true);
        }

        public boolean hasMoreElements()
        {
            return _delegate.hasMoreElements();
        }

        public Object nextElement()
        {
            Binding b = (Binding)_delegate.nextElement();
            return new Binding (b.getName(), b.getClassName(), b.getObject(),true);
        }
    }



    /*------------------------------------------------*/    
    /**
     * Constructor
     *
     * @param env environment properties
     * @param name relative name of this context
     * @param parent immediate ancestor Context (can be null)
     * @param parser NameParser for this Context
     */
    public NamingContext(Hashtable env, 
                         String name, 
                         Context parent, 
                         NameParser parser) 
    {
        _env = env;
        _name = name;
        _parent = parent;
        _parser = parser;
    } 



    /*------------------------------------------------*/
    /**
     * Creates a new <code>NamingContext</code> instance.
     *
     * @param env a <code>Hashtable</code> value
     */
    public NamingContext (Hashtable env)
    {
        _env = env;
    }




    /*------------------------------------------------*/
    /**
     * Constructor
     *
     */
    public NamingContext ()
    {
        _env = new Hashtable();
    }


    /*------------------------------------------------*/
    /**
     * Clone this NamingContext
     *
     * @return copy of this NamingContext
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone ()
        throws CloneNotSupportedException
    {
        NamingContext ctx = (NamingContext)super.clone();
        ctx._env = (Hashtable)_env.clone();
        ctx._bindings = (Hashtable)_bindings.clone();
        return ctx;
    }


    /*------------------------------------------------*/
    /**
     * Getter for _name
     *
     * @return name of this Context (relative, not absolute)
     */
    public String getName ()
    {
        return _name;
    }

    /*------------------------------------------------*/
    /**
     * Getter for _parent
     *
     * @return parent Context
     */
    public Context getParent()
    {
        return _parent;
    }

    /*------------------------------------------------*/
    /**
     * Setter for _parser
     *
     * 
     */
    public void setNameParser (NameParser parser)
    {
        _parser = parser;
    }



    /*------------------------------------------------*/
    /**
     * Bind a name to an object
     *
     * @param name Name of the object
     * @param obj object to bind
     * @exception NamingException if an error occurs
     */
    public void bind(Name name, Object obj) 
        throws NamingException
    {
        
        if (name == null)
            throw new NamingException ("Name is null");
        
        if (name.size() == 0)
            throw new NamingException ("Name is empty");


        //if no subcontexts, just bind it
        if (name.size() == 1)
        {
            Binding binding = getBinding (name);
            if (binding == null)
                addBinding (name, obj);
            else
                throw new NameAlreadyBoundException (name.toString());
        }
        else
        {
            Code.debug ("Checking for existing binding for name="+name+" for first element of name="+name.getPrefix(0));
            //walk down the subcontext hierarchy
            Binding  binding = getBinding (name.getPrefix(1));
            if (binding == null)
                throw new NameNotFoundException (name.get(0)+ " is not bound");
            
            Object ctx = binding.getObject();


            if (ctx instanceof Reference)
            {  
                //deference the object
                try
                {
                    ctx = NamingManager.getObjectInstance(ctx, name, this, _env);
                }
                catch (NamingException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new NamingException (e.getMessage());
                }
            }
        
            if (ctx instanceof Context)
            {
                ((Context)ctx).bind (name.getSuffix(1), obj);
            }
            else
                throw new NotContextException ("Object bound at "+name.get(0) +" is not a Context");
        }
    }



    /*------------------------------------------------*/
    /**
     * Bind a name (as a String) to an object
     *
     * @param name a <code>String</code> value
     * @param obj an <code>Object</code> value
     * @exception NamingException if an error occurs
     */
    public void bind(String name, Object obj) 
        throws NamingException
    {
        bind (_parser.parse(name), obj);
    }


    /*------------------------------------------------*/
    /**
     * Create a context as a child of this one
     *
     * @param name a <code>Name</code> value
     * @return a <code>Context</code> value
     * @exception NamingException if an error occurs
     */
    public Context createSubcontext (Name name)
        throws NamingException
    {
        if (name == null)
            throw new NamingException ("Name is null");
        if (name.size() == 0)
            throw new NamingException ("Name is empty");

        if (name.size() == 1)
        {
            //not permitted to bind if something already bound at that name
            Binding binding = getBinding (name);
            if (binding != null)
                throw new NameAlreadyBoundException (name.toString());

            Context ctx = new NamingContext ((Hashtable)_env.clone(), name.get(0), this, _parser);
            addBinding (name, ctx);
            return ctx;
        }
        

        //If the name has multiple subcontexts, walk the hierarchy by
        //fetching the first one. All intermediate subcontexts in the 
        //name must already exist.
        Binding binding = getBinding (name.getPrefix(0));
        if (binding == null)
            throw new NameNotFoundException (name.get(0) + " is not bound");
        
        Object ctx = binding.getObject();

        if (ctx instanceof Reference)
        {  
            //deference the object
            Code.debug ("Object bound at "+name.getPrefix(0) +" is a Reference");
            try
            {
               ctx = NamingManager.getObjectInstance(ctx, name, this, _env);
            }
            catch (NamingException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new NamingException (e.getMessage());
            }
        }
        

        if (ctx instanceof Context)
        {
            return ((Context)ctx).createSubcontext (name.getSuffix(1));
        }
        else
            throw new NotContextException (name.get(0) +" is not a Context");
    }


    /*------------------------------------------------*/
    /**
     * Create a Context as a child of this one
     *
     * @param name a <code>String</code> value
     * @return a <code>Context</code> value
     * @exception NamingException if an error occurs
     */
    public Context createSubcontext (String name)
        throws NamingException
    {
        return createSubcontext(_parser.parse(name));
    }



    /*------------------------------------------------*/
    /**
     * Not supported
     *
     * @param name name of subcontext to remove
     * @exception NamingException if an error occurs
     */
    public void destroySubcontext (String name)
        throws NamingException
    {
        throw new OperationNotSupportedException();
    }



    /*------------------------------------------------*/
    /**
     * Not supported
     *
     * @param name name of subcontext to remove
     * @exception NamingException if an error occurs
     */
    public void destroySubcontext (Name name)
        throws NamingException
    {
         throw new OperationNotSupportedException();       
    }

    /*------------------------------------------------*/
    /**
     * Lookup a binding by name
     *
     * @param name name of bound object
     * @exception NamingException if an error occurs
     */
    public Object lookup(Name name)
        throws NamingException
    {
        Code.debug ("Looking up name="+name);

        if ((name == null) || (name.size() == 0))
        {
            NamingContext ctx = new NamingContext (_env, _name, _parent, _parser);
            ctx._bindings = _bindings;
            return ctx;
        }
      

        if (name.size() == 1)
        {
            Binding binding = getBinding (name);
            if (binding == null)
                throw new NameNotFoundException();

            Object o = binding.getObject();

            //handle links by looking up the link
            if (o instanceof LinkRef)
            {
                //if link name starts with ./ it is relative to current context
                String linkName = ((LinkRef)o).getLinkName();
                if (linkName.startsWith("./"))
                    return this.lookup (linkName.substring(2));
                else
                {
                    //link name is absolute
                    InitialContext ictx = new InitialContext();
                    return ictx.lookup (linkName);
                }
            }
            else if (o instanceof Reference)
            {
                //deference the object
                try
                {
                    return NamingManager.getObjectInstance(o, name, this, _env);
                }
                catch (NamingException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new NamingException (e.getMessage());
                }
            }
            else
                return o;
        }

        //it is a multipart name, recurse to the first subcontext
        Binding binding = getBinding (name.getPrefix(1));
        if (binding == null)
            throw new NameNotFoundException ();
        
        //as we have bound a reference to an object factory 
        //for the component specific contexts
        //at "comp" we need to resolve the reference
        Object ctx = binding.getObject();

        if (ctx instanceof Reference)
        {  
            //deference the object
            try
            {
               ctx = NamingManager.getObjectInstance(ctx, name, this, _env);
            }
            catch (NamingException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new NamingException (e.getMessage());
            }
        }
        
        if (!(ctx instanceof Context))
            throw new NotContextException();

        return ((Context)ctx).lookup (name.getSuffix(1));
    }


    /*------------------------------------------------*/
    /**
     * Lookup binding of an object by name
     *
     * @param name name of bound object
     * @return object bound to name
     * @exception NamingException if an error occurs
     */
    public Object lookup (String name)
        throws NamingException
    {
        return lookup (_parser.parse(name));
    }



    /*------------------------------------------------*/
    /**
     * Lookup link bound to name
     *
     * @param name name of link binding
     * @return LinkRef or plain object bound at name
     * @exception NamingException if an error occurs
     */
    public Object lookupLink (Name name)
        throws NamingException 
    {      

        if (name == null)
        {
            NamingContext ctx = new NamingContext (_env, _name, _parent, _parser);
            ctx._bindings = _bindings;
            return ctx;
        }
        if (name.size() == 0)
            throw new NamingException ("Name is empty");

        if (name.size() == 1)
        {
            Binding binding = getBinding (name);
            if (binding == null)
                throw new NameNotFoundException();

            Object o = binding.getObject();

            //handle links by looking up the link
            if (o instanceof Reference)
            {
                //deference the object
                try
                {
                    return NamingManager.getObjectInstance(o, name.getPrefix(1), this, _env);
                }
                catch (NamingException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new NamingException (e.getMessage());
                }
            }
            else
            {
                //object is either a LinkRef which we don't dereference
                //or a plain object in which case spec says we return it
                return o;
            }
        }

        //it is a multipart name, recurse to the first subcontext
        Binding binding = getBinding (name.getPrefix(1));
        if (binding == null)
            throw new NameNotFoundException ();
        
        Object ctx = binding.getObject();

        if (ctx instanceof Reference)
        {  
            //deference the object
            try
            {
               ctx = NamingManager.getObjectInstance(ctx, name, this, _env);
            }
            catch (NamingException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new NamingException (e.getMessage());
            }
        }
        
        if (!(ctx instanceof Context))
            throw new NotContextException();

        return ((Context)ctx).lookup (name.getSuffix(1));
    }


    /*------------------------------------------------*/
    /**
     * Lookup link bound to name
     *
     * @param name name of link binding
     * @return LinkRef or plain object bound at name
     * @exception NamingException if an error occurs
     */
    public Object lookupLink (String name)
        throws NamingException
    {
        return lookupLink (_parser.parse(name));
    }


    /*------------------------------------------------*/
    /**
     * List all names bound at Context named by Name
     *
     * @param name a <code>Name</code> value
     * @return a <code>NamingEnumeration</code> value
     * @exception NamingException if an error occurs
     */
    public NamingEnumeration list(Name name)
        throws NamingException
    {
        Code.debug ("list() on Context="+getName()+" for name="+name);
        
        Enumeration enumerator = _bindings.elements();
        while (enumerator.hasMoreElements())
        {
            Binding b = (Binding)enumerator.nextElement();
            Code.debug (b.getName()+" : "+b.getClassName());
        }

        if (name == null)
        {
            return new NameEnumeration(EMPTY_ENUM);
        }

        //recurse down subcontexts until we reach the final one
        if (name.size() == 0)
        {
           return new NameEnumeration (_bindings.elements()); 
        }

        //multipart name
        Binding binding = getBinding (name.getPrefix(1));
        if (binding == null)
            throw new NameNotFoundException ();
        
        Object ctx = binding.getObject();

        if (ctx instanceof Reference)
        {  
            //deference the object
            Code.debug ("Dereferencing Reference for "+name.getPrefix(1));
            try
            {
               ctx = NamingManager.getObjectInstance(ctx, name.getPrefix(1), this, _env);
            }
            catch (NamingException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new NamingException (e.getMessage());
            }
        }
        
        if (!(ctx instanceof Context))
            throw new NotContextException();

        return ((Context)ctx).list (name.getSuffix(1));       
    }


    /*------------------------------------------------*/
    /**
     * List all names bound at Context named by Name
     *
     * @param name a <code>Name</code> value
     * @return a <code>NamingEnumeration</code> value
     * @exception NamingException if an error occurs
     */       
    public NamingEnumeration list(String name)
        throws NamingException
    {
        return list(_parser.parse(name));
    }



    /*------------------------------------------------*/
    /**
     * List all Bindings present at Context named by Name
     *
     * @param name a <code>Name</code> value
     * @return a <code>NamingEnumeration</code> value
     * @exception NamingException if an error occurs
     */
    public NamingEnumeration listBindings(Name name)
        throws NamingException
    {
        if (name == null)
        {
            return new BindingEnumeration(EMPTY_ENUM);
        }

        if (name.size() == 0)
        {
           return new BindingEnumeration (_bindings.elements()); 
        }

        //multipart name
        Binding binding = getBinding (name.getPrefix(1));
        if (binding == null)
            throw new NameNotFoundException ();
        
        Object ctx = binding.getObject();

        if (ctx instanceof Reference)
        {  
            //deference the object
            try
            {
               ctx = NamingManager.getObjectInstance(ctx, name, this, _env);
            }
            catch (NamingException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new NamingException (e.getMessage());
            }
        }
        
        if (!(ctx instanceof Context))
            throw new NotContextException();

        return ((Context)ctx).listBindings (name.getSuffix(1));
    }



    /*------------------------------------------------*/
    /**
     * List all Bindings at Name
     *
     * @param name a <code>String</code> value
     * @return a <code>NamingEnumeration</code> value
     * @exception NamingException if an error occurs
     */
    public NamingEnumeration listBindings(String name)
        throws NamingException
    {
        return listBindings (_parser.parse(name));
    }


    /*------------------------------------------------*/
    /**
     * Overwrite or create a binding
     *
     * @param name a <code>Name</code> value
     * @param obj an <code>Object</code> value
     * @exception NamingException if an error occurs
     */
    public void rebind(Name name,
                       Object obj)
        throws NamingException
    { 
                if (name == null)
            throw new NamingException ("Name is null");
        
        if (name.size() == 0)
            throw new NamingException ("Name is empty");


        //if no subcontexts, just bind it
        if (name.size() == 1)
        {
            Binding binding = getBinding (name);           
            addBinding (name, obj);
        }
        else
        {
            Code.debug ("Checking for existing binding for name="+name+" for first element of name="+name.getPrefix(0));
            //walk down the subcontext hierarchy
            Binding  binding = getBinding (name.getPrefix(1));
            if (binding == null)
                throw new NameNotFoundException (name.get(0)+ " is not bound");
            
            Object ctx = binding.getObject();


            if (ctx instanceof Reference)
            {  
                //deference the object
                try
                {
                    ctx = NamingManager.getObjectInstance(ctx, name, this, _env);
                }
                catch (NamingException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new NamingException (e.getMessage());
                }
            }
        
            if (ctx instanceof Context)
            {
                ((Context)ctx).rebind (name.getSuffix(1), obj);
            }
            else
                throw new NotContextException ("Object bound at "+name.get(0) +" is not a Context");
        }
    }


    /*------------------------------------------------*/
    /**
     * Overwrite or create a binding from Name to Object
     *
     * @param name a <code>String</code> value
     * @param obj an <code>Object</code> value
     * @exception NamingException if an error occurs
     */
    public void rebind (String name,
                        Object obj)
        throws NamingException
    {
        rebind (_parser.parse(name), obj);
    }

    /*------------------------------------------------*/
    /**
     * Not supported.
     *
     * @param name a <code>String</code> value
     * @exception NamingException if an error occurs
     */
    public void unbind (String name)
        throws NamingException
    {
        throw new OperationNotSupportedException();
    }

    /*------------------------------------------------*/
    /**
     * Not supported.
     *
     * @param name a <code>String</code> value
     * @exception NamingException if an error occurs
     */
    public void unbind (Name name)
        throws NamingException
    {
        throw new OperationNotSupportedException();
    }

    /*------------------------------------------------*/
    /**
     * Not supported
     *
     * @param oldName a <code>Name</code> value
     * @param newName a <code>Name</code> value
     * @exception NamingException if an error occurs
     */
    public void rename(Name oldName,
                       Name newName)
        throws NamingException
    {
        throw new OperationNotSupportedException();
    }

    
    /*------------------------------------------------*/
    /**
     * Not supported
     *
     * @param oldName a <code>Name</code> value
     * @param newName a <code>Name</code> value
     * @exception NamingException if an error occurs
     */    public void rename(String oldName,
                       String newName)
        throws NamingException
    {
        throw new OperationNotSupportedException();
    }



    /*------------------------------------------------*/
    /** Join two names together. These are treated as
     * CompoundNames.
     *
     * @param name a <code>Name</code> value
     * @param prefix a <code>Name</code> value
     * @return a <code>Name</code> value
     * @exception NamingException if an error occurs
     */
    public Name composeName(Name name,
                            Name prefix)
        throws NamingException
    {
        if (name == null)
            throw new NamingException ("Name cannot be null");
        if (prefix == null)
            throw new NamingException ("Prefix cannot be null");

        Name compoundName = (CompoundName)prefix.clone();
        compoundName.addAll (name);
        return compoundName;
    }



    /*------------------------------------------------*/    
    /** Join two names together. These are treated as
     * CompoundNames.
     *
     * @param name a <code>Name</code> value
     * @param prefix a <code>Name</code> value
     * @return a <code>Name</code> value
     * @exception NamingException if an error occurs
     */
    public String composeName (String name,
                               String prefix)
        throws NamingException
    {       
        if (name == null)
            throw new NamingException ("Name cannot be null");
        if (prefix == null)
            throw new NamingException ("Prefix cannot be null");

        Name compoundName = _parser.parse(prefix);
        compoundName.add (name);
        return compoundName.toString();
    }


    /*------------------------------------------------*/    
    /**
     * Not supported.
     *
     * @exception NamingException if an error occurs
     */
    public void close ()
        throws NamingException
    {
        throw new OperationNotSupportedException();       
    }


    /*------------------------------------------------*/    
    /**
     * Return a NameParser for this Context.
     *
     * @param name a <code>Name</code> value
     * @return a <code>NameParser</code> value
     */
    public NameParser getNameParser (Name name)
    {
        return _parser;
    }

    /*------------------------------------------------*/    
    /**
     * Return a NameParser for this Context.
     *
     * @param name a <code>Name</code> value
     * @return a <code>NameParser</code> value
     */    
    public NameParser getNameParser (String name)
    {
        return _parser;
    }
    

    /*------------------------------------------------*/    
    /**
     * Get the full name of this Context node
     * by visiting it's ancestors back to root.
     *
     * NOTE: if this Context has a URL namespace then
     * the URL prefix will be missing
     *
     * @return the full name of this Context
     * @exception NamingException if an error occurs
     */
    public String getNameInNamespace ()
        throws NamingException
    {
        Name name = _parser.parse("");

        NamingContext c = this;
        while (c != null)
        {
            String str = c.getName();
            if (str != null)
                name.add(0, str);
            c = (NamingContext)c.getParent();
        }
        return name.toString();
    }


    /*------------------------------------------------*/    
    /**
     * Add an environment setting to this Context
     *
     * @param propName name of the property to add
     * @param propVal value of the property to add
     * @return propVal or previous value of the property
     * @exception NamingException if an error occurs
     */
    public Object addToEnvironment(String propName,
                                   Object propVal)
        throws NamingException
    {
        return _env.put (propName, propVal);
    }


    /*------------------------------------------------*/    
    /**
     * Remove a property from this Context's environment.
     *
     * @param propName name of property to remove
     * @return value of property or null if it didn't exist
     * @exception NamingException if an error occurs
     */
    public Object removeFromEnvironment(String propName)
        throws NamingException
    {
        return _env.remove (propName);
    }


    /*------------------------------------------------*/    
    /**
     * Get the environment of this Context.
     *
     * @return a copy of the environment of this Context.
     */
    public Hashtable getEnvironment ()
    {
        return (Hashtable)_env.clone();
    }


    /*------------------------------------------------*/    
    /**
     * Add a name to object binding to this Context.
     *
     * @param name a <code>Name</code> value
     * @param obj an <code>Object</code> value
     */
    protected void addBinding (Name name, Object obj)
    {
        String key = name.toString();
        Code.debug ("Adding binding with key="+key+" obj="+obj+" for context="+_name);
        _bindings.put (key, new Binding (key, obj));
    }

    /*------------------------------------------------*/    
    /**
     * Get a name to object binding from this Context
     *
     * @param name a <code>Name</code> value
     * @return a <code>Binding</code> value
     */
    protected Binding getBinding (Name name)
    {
        Code.debug ("Looking up binding for "+name.toString()+" for context="+_name);
        return (Binding) _bindings.get(name.toString());
    }


} 
