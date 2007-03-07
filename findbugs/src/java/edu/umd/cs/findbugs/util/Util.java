/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author pugh
 */
public class Util {
    public static final boolean LOGGING = SystemProperties.getBoolean("findbugs.shutdownLogging");
    
    public static void runLogAtShutdown(Runnable r) {
        if (LOGGING) Runtime.getRuntime().addShutdownHook(new Thread(r));
        
    }
	
	public static int  nullSafeHashcode(@CheckForNull Object o) {
		if (o == null) return 0;
		return o.hashCode();		
	}
	public static <T> boolean  nullSafeEquals(@CheckForNull T o1, @CheckForNull T o2) {
		if (o1 == o2) return true;
		if (o1 == null || o2 == null) return false;
		return o1.equals(o2);
	}
    
    public static Reader getReader(InputStream in) throws UnsupportedEncodingException {
        return new InputStreamReader(in, "UTF-8");
    }
    public static Reader getFileReader(String filename) throws UnsupportedEncodingException, FileNotFoundException {
        return getReader(new FileInputStream(filename));
    }
    public static Reader getFileReader(File filename) throws UnsupportedEncodingException, FileNotFoundException {
        return getReader(new FileInputStream(filename));
    }
    public static Writer getWriter(OutputStream out) throws UnsupportedEncodingException, FileNotFoundException {
        return new OutputStreamWriter(out, "UTF-8");
    }

    public static Writer getFileWriter(String filename) throws UnsupportedEncodingException, FileNotFoundException {
        return  getWriter(new FileOutputStream(filename));
    }


}
