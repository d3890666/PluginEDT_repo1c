package dev.zigr.dt.team.ui.storage;

import org.osgi.framework.Bundle;

import com._1c.g5.wiring.AbstractGuiceAwareExecutableExtensionFactory;
import com.google.inject.Injector;

public class StorageUiExecutableExtensionFactory extends AbstractGuiceAwareExecutableExtensionFactory {

	@Override
	protected Bundle getBundle() {
		return StorageUiPlugin.getDefault().getBundle();
	}

	@Override
	protected Injector getInjector() {
		return StorageUiPlugin.getDefault().getInjector();
	}

}
