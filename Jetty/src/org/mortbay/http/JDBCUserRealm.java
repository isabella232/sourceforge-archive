package org.mortbay.http;

import org.mortbay.util.Code;
import org.mortbay.util.Loader;
import org.mortbay.util.Resource;
import org.mortbay.http.HashUserRealm;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;

/* ------------------------------------------------------------ */
/** HashMapped User Realm with JDBC as data source.
 * JDBCUserRealm extends HashUserRealm and adds a method to fetch user
 * information from database.
 * The getUser() method checks the inherited HashMap and if the user
 * is found, then it is returned.
 * Otherwise fetch information from database and populate inherited
 * hash.
 * Periodically (controled by configuration parameter), internal
 * hashes are cleared. Caching can be disabled by setting cache
 * refresh interval to zero.
 * Uses one database connection that is initialized at startup. Reconnect
 * on failures. getUser() is 'synchronized'.
 *
 * An example properties file for configuration is in
 * $JETTY_HOME/etc/jdbcRealm.properties
 *
 * @see Password
 * @version $Id$
 * @author Arkadi Shishlov (arkadi)
 * @author Fredrik Borgh
 * @author Greg Wilkins (gregw)
 */

public class JDBCUserRealm extends HashUserRealm
{
    private String _jdbcDriver;
    private String _url;
    private String _userName;
    private String _password;
    private String _userTable;
    private String _userTableKey;
    private String _userTableUserField;
    private String _userTablePasswordField;
    private String _roleTable;
    private String _roleTableKey;
    private String _roleTableRoleField;
    private String _userRoleTable;
    private String _userRoleTableUserKey;
    private String _userRoleTableRoleKey;
    private int _cacheTime;
    
    private long _lastHashPurge;
    private Connection _con;
    private String _userSql;
    private String _roleSql;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param name 
     */
    public JDBCUserRealm(String name)
    {
        super(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param name Realm name
     * @param config Filename or url of JDBC connection properties file.
     * @exception IOException 
     * @exception ClassNotFoundException 
     */
    public JDBCUserRealm(String name, String config)
        throws IOException, ClassNotFoundException
    {
        super(name);
        loadConfig(config);
        Loader.loadClass(this.getClass(),_jdbcDriver);
        connectDatabase();
    }
    
    /* ------------------------------------------------------------ */
    /** Load JDBC connection configuration from properties file.
     *
     * @param config Filename or url of user properties file.
     * @exception IOException 
     */
    public void loadConfig(String config)
        throws IOException
    {
        Properties properties = new Properties();
        Resource resource=Resource.newResource(config);
        properties.load(resource.getInputStream());
        
        _jdbcDriver = properties.getProperty("jdbcdriver");
        _url = properties.getProperty("url");
        _userName = properties.getProperty("username");
        _password = properties.getProperty("password");
        _userTable = properties.getProperty("usertable");
        _userTableKey = properties.getProperty("usertablekey");
        _userTableUserField = properties.getProperty("usertableuserfield");
        _userTablePasswordField = properties.getProperty("usertablepasswordfield");
        _roleTable = properties.getProperty("roletable");
        _roleTableKey = properties.getProperty("roletablekey");
        _roleTableRoleField = properties.getProperty("roletablerolefield");
        _userRoleTable = properties.getProperty("userroletable");
        _userRoleTableUserKey = properties.getProperty("userroletableuserkey");
        _userRoleTableRoleKey = properties.getProperty("userroletablerolekey");
        _cacheTime = new Integer(properties.getProperty("cachetime")).intValue();
        
        if (_jdbcDriver == null || _jdbcDriver.equals("")
            || _url == null || _url.equals("")
            || _userName == null || _userName.equals("")
            || _password == null
            || _cacheTime < 0)
        {
            Code.debug("UserRealm " + getName()
                        + " has not been properly configured");
        }
        _cacheTime *= 1000;
        _lastHashPurge = 0;
        _userSql = "select " + _userTableKey + ","
            + _userTablePasswordField + " from "
            + _userTable + " where "
            + _userTableUserField + " = ?";
        _roleSql = "select " + _roleTableRoleField
            + " from " + _roleTable + " r, "
            + _userRoleTable + " u where u."
            + _userRoleTableUserKey + " = ?"
            + " and r." + _roleTableKey + " = "
            + _userRoleTableRoleKey;
    }
    
    /* ------------------------------------------------------------ */
    /** (re)Connect to database with parameters setup by loadConfig()
     *
     * @exception Exception
     */
    public void connectDatabase()
    {
        try 
        {
            _con = DriverManager.getConnection(_url, _userName, _password);
        }
        catch(SQLException e)
        {
            e.printStackTrace(System.err);
            Code.debug("UserRealm " + getName()
                      + " could not connect to database; will try later");
        }
    }
    
    /* ------------------------------------------------------------ */
    public synchronized UserPrincipal getUser(String username)
    {
        long now = System.currentTimeMillis();
        if (now - _lastHashPurge > _cacheTime || _cacheTime == 0)
        {
            super.clear();
            _roles.clear();
            _lastHashPurge = now;
        }
        UserPrincipal user = (UserPrincipal)super.get(username);
        if (user == null)
        {
            loadUser(username);
            user = (UserPrincipal)super.get(username);
        }
        return user;
    }
    
    /* ------------------------------------------------------------ */
    private void loadUser(String username)
    {
        try
        {
            PreparedStatement stat = _con.prepareStatement(_userSql);
            stat.setObject(1, username);
            ResultSet rs = stat.executeQuery();
    
            if (rs.next())
            {
                Object key = rs.getObject(_userTableKey);
                put(username, rs.getString(_userTablePasswordField));
                stat.close();
                
                stat = _con.prepareStatement(_roleSql);
                stat.setObject(1, key);
                rs = stat.executeQuery();

                while (rs.next())
                    addUserToRole(username, rs.getString(_roleTableRoleField));
                
                stat.close();
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace(System.err);
            Code.debug("UserRealm " + getName()
                      + " could not load user information from database");
            connectDatabase();
        }
    }
}
