/*
   Android FreeRDP JNI Wrapper

   Copyright 2013 Thinstuff Technologies GmbH, Author: Martin Fleisz

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
   If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/

package com.freerdp.afreerdp.services;


import com.freerdp.afreerdp.application.GlobalApp;
import com.freerdp.afreerdp.application.SessionState;
import com.freerdp.afreerdp.domain.BookmarkBase;
import com.freerdp.afreerdp.domain.ManualBookmark;

import android.graphics.Bitmap;

public class LibFreeRDP
{
	private static native int freerdp_new();
	private static native void freerdp_free(int inst);
	private static native boolean freerdp_connect(int inst);
	private static native boolean freerdp_disconnect(int inst);
	private static native void freerdp_cancel_connection(int inst);
	
	private static native void freerdp_set_connection_info(int inst,
		String hostname, String username, String password, String domain,
		int width, int height, int color_depth, int port, boolean console,
		int security, String certname);

	private static native void freerdp_set_performance_flags(int inst,
		boolean remotefx, boolean disableWallpaper, boolean disableFullWindowDrag,
		boolean disableMenuAnimations, boolean disableTheming,
		boolean enableFontSmoothing, boolean enableDesktopComposition);

	private static native void freerdp_set_advanced_settings(int inst, String remoteProgram, String workDir);
	
	private static native void freerdp_set_data_directory(int inst, String directory);
	
	private static native boolean freerdp_update_graphics(int inst,
			Bitmap bitmap, int x, int y, int width, int height);
	
	private static native void freerdp_send_cursor_event(int inst, int x, int y, int flags);
	private static native void freerdp_send_key_event(int inst, int keycode, boolean down);
	private static native void freerdp_send_unicodekey_event(int inst, int keycode);

	private static native String freerdp_get_version();
	
	private static final String TAG = "LibFreeRDP";

	public static interface EventListener
	{
		void OnConnectionSuccess(int instance);
		void OnConnectionFailure(int instance);
		void OnDisconnecting(int instance);
		void OnDisconnected(int instance);
	}

	public static interface UIEventListener
	{
		void OnSettingsChanged(int width, int height, int bpp);
		boolean OnAuthenticate(StringBuilder username, StringBuilder domain, StringBuilder password);
		boolean OnVerifiyCertificate(String subject, String issuer, String fingerprint);
		void OnGraphicsUpdate(int x, int y, int width, int height);		
		void OnGraphicsResize(int width, int height);		
	}

	private static EventListener listener;

	public static void setEventListener(EventListener l)
	{
		listener = l;		
	}
	
	public static int newInstance()
	{
		return freerdp_new();
	}

	public static void freeInstance(int inst)
	{
		freerdp_free(inst);
	}

	public static boolean connect(int inst)
	{
		return freerdp_connect(inst);
	}

	public static boolean disconnect(int inst)
	{
		return freerdp_disconnect(inst);
	}

	public static void cancelConnection(int inst)
	{
		freerdp_cancel_connection(inst);
	}

	public static boolean setConnectionInfo(int inst, BookmarkBase bookmark)
	{
		BookmarkBase.ScreenSettings screenSettings = bookmark.getActiveScreenSettings();
		
		int port;
		String hostname;
		String certName = "";
		if(bookmark.getType() == BookmarkBase.TYPE_MANUAL)
		{
			port = bookmark.<ManualBookmark>get().getPort();			
			hostname = bookmark.<ManualBookmark>get().getHostname();			
		}
		else
		{
			assert false;
			return false;
		}

		freerdp_set_connection_info(inst,
			hostname,
			bookmark.getUsername(),
			bookmark.getPassword(),
			bookmark.getDomain(),
			screenSettings.getWidth(),
			screenSettings.getHeight(),
			screenSettings.getColors(),
			port,
			bookmark.getAdvancedSettings().getConsoleMode(),
			bookmark.getAdvancedSettings().getSecurity(),
			certName);

		BookmarkBase.PerformanceFlags flags = bookmark.getActivePerformanceFlags();
		freerdp_set_performance_flags(inst,
				flags.getRemoteFX(),
				!flags.getWallpaper(),
				!flags.getFullWindowDrag(),
				!flags.getMenuAnimations(),
				!flags.getTheming(),
				flags.getFontSmoothing(),
				flags.getDesktopComposition());
		
		BookmarkBase.AdvancedSettings advancedSettings = bookmark.getAdvancedSettings();
		freerdp_set_advanced_settings(inst, advancedSettings.getRemoteProgram(), advancedSettings.getWorkDir());

		return true;
	}
	
	public static void setDataDirectory(int inst, String directory)
	{
		freerdp_set_data_directory(inst, directory);
	}
	
	public static boolean updateGraphics(int inst, Bitmap bitmap, int x, int y, int width, int height)
	{
		return freerdp_update_graphics(inst, bitmap, x, y, width, height);
	}		
	
	public static void sendCursorEvent(int inst, int x, int y, int flags)
	{
		freerdp_send_cursor_event(inst, x, y, flags);
	}

	public static void sendKeyEvent(int inst, int keycode, boolean down)
	{
		freerdp_send_key_event(inst, keycode, down);
	}

	public static void sendUnicodeKeyEvent(int inst, int keycode)
	{
		freerdp_send_unicodekey_event(inst, keycode);
	}

	private static void OnConnectionSuccess(int inst)
	{		
		if (listener != null)
			listener.OnConnectionSuccess(inst);
	}

	private static void OnConnectionFailure(int inst)
	{
		if (listener != null)
			listener.OnConnectionFailure(inst);
	}

	private static void OnDisconnecting(int inst)
	{		
		if (listener != null)
			listener.OnDisconnecting(inst);
	}

	private static void OnDisconnected(int inst)
	{		
		if (listener != null)
			listener.OnDisconnected(inst);
	}

	private static void OnSettingsChanged(int inst, int width, int height, int bpp)
	{		
		SessionState s = GlobalApp.getSession(inst);
		if (s == null)
			return;
		UIEventListener uiEventListener = s.getUIEventListener();
		if (uiEventListener != null)
			uiEventListener.OnSettingsChanged(width, height, bpp);
	}

	private static boolean OnAuthenticate(int inst, StringBuilder username, StringBuilder domain, StringBuilder password)
	{
		SessionState s = GlobalApp.getSession(inst);
		if (s == null)
			return false;
		UIEventListener uiEventListener = s.getUIEventListener();
		if (uiEventListener != null)
			return uiEventListener.OnAuthenticate(username, domain, password);
		return false;
	}

	private static boolean OnVerifyCertificate(int inst, String subject, String issuer, String fingerprint)
	{
		SessionState s = GlobalApp.getSession(inst);
		if (s == null)
			return false;
		UIEventListener uiEventListener = s.getUIEventListener();
		if (uiEventListener != null)
			return uiEventListener.OnVerifiyCertificate(subject, issuer, fingerprint);
		return false;
	}

	private static void OnGraphicsUpdate(int inst, int x, int y, int width, int height)
	{
		SessionState s = GlobalApp.getSession(inst);
		if (s == null)
			return;
		UIEventListener uiEventListener = s.getUIEventListener();
		if (uiEventListener != null)
			uiEventListener.OnGraphicsUpdate(x, y, width, height);
	}

	private static void OnGraphicsResize(int inst, int width, int height)
	{
		SessionState s = GlobalApp.getSession(inst);
		if (s == null)
			return;
		UIEventListener uiEventListener = s.getUIEventListener();
		if (uiEventListener != null)
			uiEventListener.OnGraphicsResize(width, height);
	}
	
	public static String getVersion()
	{
		return freerdp_get_version();
	}
}
