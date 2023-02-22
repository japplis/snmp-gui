/*
* Copyright 2005 Tryllian Solutions B.V. 
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this Lesser; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 *
 * See license.html for more information.
 */
package com.tryllian.snmp.gui;

import java.io.IOException;

/**
 * <B><I>This was taken from
 * </I></B><A HREF=http://www.javaworld.com/javaworld/javatips/jw-javatip66.html>an article in javawrld</A><B><I>
* <P>
* A simple, static class to display a URL in the system browser.

 *
 * Under Unix, the system browser is hard-coded to be 'netscape'.
 * Netscape must be in your PATH for this to work.  This has been
 * tested with the following platforms: AIX, HP-UX and Solaris.

 *
 * Under Windows, this will bring up the default browser under windows,
 * usually either Netscape or Microsoft IE.  The default browser is
 * determined by the OS.  This has been tested under Windows 95/98/NT.

 *
 * Examples:
 * <PRE>
BrowserControl.displayURL("http://www.javaworld.com")
 BrowserControl.displayURL("file://c:\\docs\\index.html")
 BrowserContorl.displayURL("file:///user/joe/index.html");
</PRE>
*
 * Note - you must include the url type -- either "http://" or
 * "file://".
 */
public class BrowserControl
{
	/**
	 * Display a file in the system browser.  If you want to display a
	 * file, you must include the absolute path name.
	 *
	 * @param url the file's url (the url must start with either "http://" or
	 * "file://").
	 */
     
     
     /////////////////////////////////////////////////////////////
     ////////////////////////////////////////////////////////////
     /* TVE Testcase : 
     Due to URL display problems on IE, 
     we test the freely available BrowserLauncher here .
     http://www.stanford.edu/%7Eejalbert/software/BrowserLauncher/

     We reroute the request to browserlauncher, 
     so it can be removed immediately if further tests fail. 
     
     To do so, comment  the lines below in displayURL()
     
     -- 
     BrowserLauncher.BrowserStart.openURL(String URL); 
     return ; 
     --
     */
     
     
	public static void displayURL(String url)
	{
        ////////// Test of BrowserLauncher
        /*try {
            BrowserLauncher.openURL(url);
            }
            catch(Exception x){ 
                System.out.println("Error during browser launching !:"+ x);
                x.printStackTrace();
            }
        if (true)
            return ;*/
        ///////////////////////////////////////////////////////////////
            
		boolean windows = isWindowsPlatform();
		String cmd = null;

		try
		{
			if (windows)
			{
				// cmd = 'rundll32 url.dll,FileProtocolHandler http://...'
				cmd = WIN_PATH + " " + WIN_FLAG + " " + url;
				Process p = Runtime.getRuntime().exec(cmd);
			}
			else
			{
				// Under Unix, Netscape has to be running for the "-remote"
				// command to work.  So, we try sending the command and
				// check for an exit value.  If the exit command is 0,
				// it worked, otherwise we need to start the browser.

				// cmd = 'netscape -remote openURL(http://www.javaworld.com)'
				cmd = UNIX_PATH + " " + UNIX_FLAG + url;
                                System.err.println(cmd);
				Process p = Runtime.getRuntime().exec(cmd);

				/*try
				{
					// wait for exit code -- if it's 0, command worked,
					// otherwise we need to start the browser up.
					int exitCode = p.waitFor();

					if (exitCode != 0)
					{
						// Command failed, start up the browser

						// cmd = 'netscape http://www.javaworld.com'
						cmd = UNIX_PATH + " "  + url;
						p = Runtime.getRuntime().exec(cmd);
					}
				}
				catch(InterruptedException x)
				{
					System.err.println("Error bringing up browser, cmd='" +
									   cmd + "'");
					System.err.println("Caught: " + x);
				}*/
			}
		}
		catch(IOException x)
		{
			// couldn't exec browser
			System.err.println("Could not invoke browser, command=" + cmd);
			System.err.println("Caught: " + x);
		}
	}

	/**
	 * Try to determine whether this application is running under Windows
	 * or some other platform by examing the "os.name" property.
	 *
	 * @return true if this application is running under a Windows OS
	 */
	public static boolean isWindowsPlatform()
	{
		String os = System.getProperty("os.name");

		if ( os != null && os.startsWith(WIN_ID))
			return true;
		else
			return false;
	}

	// Used to identify the windows platform.
	private static final String WIN_ID = "Windows";

	// The default system browser under windows.
	private static final String WIN_PATH = "rundll32";

	// The flag to display a url.
	private static final String WIN_FLAG = "url.dll,FileProtocolHandler";

	// The default browser under unix.
	private static final String UNIX_PATH = "firefox";

	// The flag to display a url.
	private static final String UNIX_FLAG = "";
}


