/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.cloud.username;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.util.LaunchBrowser;
import edu.umd.cs.findbugs.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * @author pugh
 */
public class AppEngineNameLookup implements NameLookup {
	
	public static final String LOCAL_APPENGINE = "appengine.local";
	
    public static final String APPENGINE_LOCALHOST_PROPERTY_NAME = "appengine.host.local";
    public static final String APPENGINE_LOCALHOST_DEFAULT = "http://localhost:8080";
    public static final String APPENGINE_HOST_PROPERTY_NAME = "appengine.host";

    private static final Logger LOGGER = Logger.getLogger(AppEngineNameLookup.class.getName());

    /** if "true", prevents session info from being saved between launches. */
    private static final String SYSPROP_NEVER_SAVE_SESSION = "appengine.never_save_session";
    private static final String SYSPROP_APPENGINE_LOCAL = "appengine.local";
    
    private static final int USER_SIGNIN_TIMEOUT_SECS = 60;
    private static final String KEY_SAVE_SESSION_INFO = "save_session_info";
    private static final String KEY_APPENGINECLOUD_SESSION_ID = "appenginecloud_session_id";

	private Long sessionId;
	private String username;
	private String host;

    public boolean initialize(CloudPlugin plugin, BugCollection bugCollection) throws IOException {
        if (initializeSoftly(plugin))
            return true;

        if (sessionId == null)
            sessionId = loadOrCreateSessionId();

        LOGGER.info("Opening browser for session " + sessionId);
        URL u = new URL(host + "/browser-auth/" + sessionId);
        LaunchBrowser.showDocument(u);

        // wait 1 minute for the user to sign in
        for (int i = 0; i < USER_SIGNIN_TIMEOUT_SECS; i++) {
            if (checkAuthorized(getAuthCheckUrl(sessionId))) {
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        LOGGER.info("Sign-in timed out for " + sessionId);
        throw new IOException("Sign-in timed out");
	}

    public boolean initializeSoftly(CloudPlugin plugin) throws IOException {
        if (sessionId != null) {
            if (checkAuthorized(getAuthCheckUrl(sessionId))) {
                LOGGER.fine("Skipping soft init; session ID already exists - " + sessionId);
                return true;
            } else {
                sessionId = null;
            }
        }
        PropertyBundle pluginProps = plugin.getProperties();
        if (Boolean.getBoolean(SYSPROP_APPENGINE_LOCAL) || pluginProps.getBoolean(LOCAL_APPENGINE))
            host = pluginProps.getProperty(APPENGINE_LOCALHOST_PROPERTY_NAME, APPENGINE_LOCALHOST_DEFAULT);
        else
            host = pluginProps.getProperty(APPENGINE_HOST_PROPERTY_NAME);
        // check the previously used session ID
        long id = loadOrCreateSessionId();
        boolean authorized = checkAuthorized(getAuthCheckUrl(id));
        if (authorized) {
            LOGGER.info("Authorized with session ID: " + id);
            this.sessionId = id;
        }
        
        return authorized;
    }

    private URL getAuthCheckUrl(long sessionId) throws MalformedURLException {
        return new URL(host + "/check-auth/" + sessionId);
    }

    public static void setSaveSessionInformation(boolean save) {
        Preferences prefs = Preferences.userNodeForPackage(AppEngineNameLookup.class);
        prefs.putBoolean(KEY_SAVE_SESSION_INFO, save);
        if (!save) {
            clearSavedSessionInformation();
        }
    }

    public static boolean isSavingSessionInfoEnabled() {
        return !Boolean.getBoolean(SYSPROP_NEVER_SAVE_SESSION)
               && Preferences.userNodeForPackage(AppEngineNameLookup.class).getBoolean(KEY_SAVE_SESSION_INFO, true);
    }

    public static void clearSavedSessionInformation() {
        Preferences prefs = Preferences.userNodeForPackage(AppEngineNameLookup.class);
        prefs.remove(KEY_APPENGINECLOUD_SESSION_ID);
    }

    public static void saveSessionInformation(long sessionId) {
        Preferences.userNodeForPackage(AppEngineNameLookup.class).putLong(KEY_APPENGINECLOUD_SESSION_ID, sessionId);
    }

	public Long getSessionId() {
    	return sessionId;
    }

	public String getUsername() {
    	return username;
    }

	public String getHost() {
		return host;
	}
	
	// ======================= end of public methods =======================

	private long loadOrCreateSessionId() {
	    Preferences prefs = Preferences.userNodeForPackage(AppEngineNameLookup.class);
	    long id = prefs.getLong(KEY_APPENGINECLOUD_SESSION_ID, 0);
	    if (id == 0) {
	    	SecureRandom r = new SecureRandom();
	    	while (id == 0) 
	    		id = r.nextLong();
            if (isSavingSessionInfoEnabled())
                saveSessionInformation(id);
        } else {
            LOGGER.info("Using saved session ID: " + id);
        }
	    return id;
    }

    private boolean checkAuthorized(URL response) throws IOException {
	    HttpURLConnection connection = (HttpURLConnection) response.openConnection();

	    int responseCode = connection.getResponseCode();
	    if (responseCode == 200) {
	    	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	    	String status = in.readLine();
	    	sessionId = Long.parseLong(in.readLine());
	    	username = in.readLine();
	    	Util.closeSilently(in);
	    	if ("OK".equals(status)) {
                LOGGER.info("Authorized session " + sessionId);
	    		return true;
            }

	    }
	    connection.disconnect();
	    return false;
    }
}
