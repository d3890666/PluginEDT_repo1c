package dev.zigr.dt.team.ui.storage;

import org.eclipse.core.runtime.Plugin;

import com._1c.g5.v8.activitytracking.core.ISystemIdleService;
import com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IDerivedDataManagerProvider;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.wiring.AbstractServiceAwareModule;

public class ExternalDependenciesModule extends AbstractServiceAwareModule {

	public ExternalDependenciesModule(Plugin bundle) {
		super(bundle);
	}

	@Override
	protected void doConfigure() {
		this.bind(IQualifiedNameFilePathConverter.class).toService();
		this.bind(IBmModelManager.class).toService();
		//++ IExportOperationFactory
		this.bind(IDerivedDataManagerProvider.class).toService();
		this.bind(IResourceLookup.class).toService();
		this.bind(IRuntimeVersionSupport.class).toService();
		this.bind(ISystemIdleService.class).toService();
		//-- IExportOperationFactory
	}

}
