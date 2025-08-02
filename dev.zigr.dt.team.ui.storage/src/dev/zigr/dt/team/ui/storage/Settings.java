package dev.zigr.dt.team.ui.storage;

import java.io.IOException;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com._1c.g5.v8.dt.platform.services.model.InfobaseReference;

public class Settings {
	private Preferences preferences;
	private ISecurePreferences secPreferences;

	public Settings(InfobaseReference infobase) {
		preferences = InstanceScope.INSTANCE.getNode(StorageUiPlugin.PLUGIN_ID)
				.node("Settings").node(infobase.getUuid().toString());
		
		secPreferences = SecurePreferencesFactory.getDefault().node(StorageUiPlugin.PLUGIN_ID)
				.node("Settings").node(infobase.getUuid().toString());
	}

	public String getAddress() {
		return preferences.get("address", "");
	}

	public void setAddress(String address) {
		preferences.put("address", address);
	}

	public String getUser() {
		return preferences.get("user", "");
	}

	public void setUser(String user) {
		preferences.put("user", user);
	}

	public String getPassword() {
		try {
			return secPreferences.get("pw", "");
		} catch (StorageException e) {
			StorageUiPlugin.logError(e.getMessage(), e);
			return "";
		}
	}

	public void setPassword(String pw) throws StorageException {
		secPreferences.put("pw", pw, true);
	}

	public boolean getExportMDWithMDO() {
		return preferences.get("exportMDWithMDO", "").equals("true");
	}

	public void setExportMDWithMDO(boolean exportMDWithMDO) {
		preferences.put("exportMDWithMDO", exportMDWithMDO ? "true" : "false");
		
	}

	public void flush() throws BackingStoreException, IOException {
		preferences.flush();
		secPreferences.flush();
	}
}
