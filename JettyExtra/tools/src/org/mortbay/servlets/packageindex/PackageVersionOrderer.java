// ========================================================================
// Copyright 2000 (c) Mortbay Consulting Ltd.
// $Id$
// ========================================================================

package org.mortbay.servlets.packageindex;

//import org.mortbay.util.Code;
import java.util.Comparator;

// Order version names, containing sections seperated by "." and
// alphanumerics.
public class PackageVersionOrderer implements Comparator
{
    public int compare(Object first, Object second){
	String v1 = getVersion(first);
	String v2 = getVersion(second);
	
	// Cut up the version into strings separated by chars
	String[] s1 = versions(v1);
	String[] s2 = versions(v2);
	int i;
	for (i = 0; i < s1.length && i < s2.length; i++){
	    if (!s1[i].equals(s2[i])) return strcompare(s1[i], s2[i]);
	}
	return s1.length - s2.length;
    }
    public static String[] versions(String v){
	char[] chars = v.toCharArray();
	//if (chars.length() == 0) return new String[0];
	int strstart = 0;
	int version = 0;
	int i;
	for (i = 0; i < chars.length; i++)
	    if (chars[i] == '.' && i - strstart > 0){
		version++;
		strstart = i + 1;
	    }
	if (strstart != i) version++;
	String[] res = new String[version];
	version = 0;
	strstart = 0;
	for (i = 0; i < chars.length; i++)
	    if (chars[i] == '.'){
		res[version] = v.substring(strstart, i);
		version++; strstart = i+1;
	    }
	if (strstart != i) res[version] = v.substring(strstart, i);
	return res;
    }
    public static int strcompare(String s1, String s2){
        if (!Character.isDigit(s1.charAt(0))){
	    if (!Character.isDigit(s2.charAt(0)))
		// Both begin with chars - string compare!
		return s1.compareTo(s2);
	    // first begins with char
	    return -1;
	}
	if (!Character.isDigit(s2.charAt(0)))
	    return 1;
	// Both are digits
	try {
	    float f1 = getNum(s1);
	    float f2 = getNum(s2);
	    return (int)(f1 - f2);
	} catch (NumberFormatException ex){
	    return s1.compareTo(s2);
	}
    }
    public static float getNum(String s){
	float f = (float)Integer.parseInt(s);
	for (int i = 0; i < s.length() && s.charAt(i) == '0'; i++){
	    while (f > 10) f = f / 10;
	    f = f/10;
	}
	return f;
    }
    public static String getVersion(Object obj){
	String s = obj.toString();
	if (s.startsWith("jvm")) //XXX
	    return s.substring(s.lastIndexOf('/')+1);
	return s;
    }
}
