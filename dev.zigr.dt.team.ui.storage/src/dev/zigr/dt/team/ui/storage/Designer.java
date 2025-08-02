package dev.zigr.dt.team.ui.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.xtext.naming.QualifiedName;

import com._1c.g5.v8.dt.common.Pair;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseAccessManager;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseAccessSettings;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IDesignerSessionThickClientLauncher;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.ILaunchableRuntimeComponent;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IRuntimeComponentManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IRuntimeComponentTypes;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionException;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.impl.RuntimeExecutionCommandBuilder;
import com._1c.g5.v8.dt.platform.services.model.InfobaseReference;
import com._1c.g5.v8.dt.platform.services.model.RuntimeInstallation;
import com._1c.g5.v8.dt.platform.version.Version;
import com._1c.g5.v8.dt.team.git.infobases.IGitBranchIssueDescriptor;
import com._1c.g5.wiring.ServiceAccess;
import com._1c.g5.wiring.ServiceSupplier;

public class Designer {
	
	private ServiceSupplier<IInfobaseAccessManager> infobaseAccessManagerSupplier = 
			ServiceAccess.supplier(IInfobaseAccessManager.class, StorageUiPlugin.getDefault());	
	private ServiceSupplier<IRuntimeComponentManager> runtimeComponentManagerSupplier = 
			ServiceAccess.supplier(IRuntimeComponentManager.class, StorageUiPlugin.getDefault());	
	
	private Version version;
	private IGitBranchIssueDescriptor issueDescriptor;
	private Pair<ILaunchableRuntimeComponent, IDesignerSessionThickClientLauncher> thickClient;
	
	public Designer(IGitBranchIssueDescriptor issueDescriptor) throws CoreException {
		this.issueDescriptor = issueDescriptor;
		IResolvableRuntimeInstallation actualInstallation = getInfobaseAccessManager().getInstallation(issueDescriptor.getProject(), issueDescriptor.getInfobase());
		RuntimeInstallation thickClientComponent = actualInstallation.get(new String[]{IRuntimeComponentTypes.THICK_CLIENT});
		version = thickClientComponent.getVersion();
		thickClient = getRuntimeComponentManager().getComponentAndExecutor(thickClientComponent, IRuntimeComponentTypes.THICK_CLIENT);
	}
	
	private IInfobaseAccessManager getInfobaseAccessManager() {
		return infobaseAccessManagerSupplier.get();
	}
	
	private IRuntimeComponentManager getRuntimeComponentManager() {
		return runtimeComponentManagerSupplier.get();
	}
	
	public void dispose() {
		infobaseAccessManagerSupplier.close();
		runtimeComponentManagerSupplier.close();
	}
	public Version getVersion() {
		return version;
	}
	
	public void closeDesignerSession() throws RuntimeExecutionException {
		thickClient.second.closeDesignerSession(thickClient.first, issueDescriptor.getInfobase(), null);
	}

	private RuntimeExecutionCommandBuilder getCommandBuilder(Path log) throws CoreException {
		InfobaseReference infobase = issueDescriptor.getInfobase();
		IInfobaseAccessSettings settings = getInfobaseAccessManager().getSettings(infobase);
		File launchFile = thickClient.first.getFile();
		
		RuntimeExecutionCommandBuilder result = new RuntimeExecutionCommandBuilder(launchFile, RuntimeExecutionCommandBuilder.ThickClientMode.DESIGNER)
				.forInfobase(infobase, false).userName(settings.userName()).userPassword(settings.password())
				.disableStartupDialogs().logTo(log.toFile(), true);
		
		return result;
	}

	public void configDumpInfoOnly(Path rootDirectory) throws CoreException, IOException, InterruptedException {
		Path log = rootDirectory.resolve("cdiOnlyOut.txt");
		
		RuntimeExecutionCommandBuilder command = getCommandBuilder(log)
				.dumpConfigurationToXml(rootDirectory).additionalParameters("-configDumpInfoOnly");
		
		Process process = command.start();
		int returnCode = process.waitFor();
		if (returnCode != 0) {
			IStatus status = StorageUiPlugin.createErrorStatus(Files.readString(log));
			throw new CoreException(status);
		}
	}

	public void loadConfigurationFromXml(Path sourceFolder, Path fileList, Path rootDirectory) throws CoreException, IOException, InterruptedException {
		Path log = rootDirectory.resolve("loadCfgOut.txt");
		
		RuntimeExecutionCommandBuilder command = getCommandBuilder(log)
				.loadConfigurationFromXml(sourceFolder).fileList(fileList);
		
		Process process = command.start();
		int returnCode = process.waitFor();
		if (returnCode != 0) {
			IStatus status = StorageUiPlugin.createErrorStatus(Files.readString(log));
			throw new CoreException(status);
		}
	}
	
	public void lockObjects(Map<QualifiedName, Boolean> lockObjects, Path rootDirectory) throws IOException, CoreException, InterruptedException {
		Path log = rootDirectory.resolve("lockObjectsOut.txt");
		Path lockObjectsList = rootDirectory.resolve("lockObjectsList.xml");
		
		String strTemplate = "<Object fullName = \"{0}\" includeChildObjects = \"{1}\" />";
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(lockObjectsList.toString(), StandardCharsets.UTF_8))){
			writer.append("<Objects xmlns=\"http://v8.1c.ru/8.3/config/objects\" version=\"1.0\">"+System.lineSeparator());
			for (Map.Entry<QualifiedName, Boolean> entry : lockObjects.entrySet()) {
				QualifiedName key = entry.getKey();
				Boolean val = entry.getValue();
				if ("Configuration".equals(key.toString())) {
					writer.append("<Configuration includeChildObjects = \"false\" />"+System.lineSeparator());
				} else {
					writer.append(MessageFormat.format(strTemplate, key.toString(), val.toString())+System.lineSeparator());
				}
			}
			writer.append("</Objects>");
		} catch (IOException e) {
			throw e;
		}
		
		RuntimeExecutionCommandBuilder command = getCommandBuilder(log);
		Settings storageSettings = new Settings(issueDescriptor.getInfobase());
		String additionalStartupParameters = "/ConfigurationRepositoryF "+storageSettings.getAddress()
		+ " /ConfigurationRepositoryN "+storageSettings.getUser()
		+ (storageSettings.getPassword().isEmpty() ? "" : " /ConfigurationRepositoryP "+storageSettings.getPassword())
		+ " /ConfigurationRepositoryLock "
		+ " -Objects " + lockObjectsList.toString();
		command.additionalParameters(additionalStartupParameters);
		
		Process process = command.start();
		int returnCode = process.waitFor();
		if (returnCode != 0) {
			IStatus status = StorageUiPlugin.createErrorStatus(Files.readString(log));
			throw new CoreException(status);
		}
	}

	public boolean isConfigurationSame(Path rootDirectory) throws CoreException, IOException, InterruptedException {
		Path log = rootDirectory.resolve("compareCfgOut.txt");
		Path reportFile = rootDirectory.resolve("compareCfgReport.txt");
		
		RuntimeExecutionCommandBuilder command = getCommandBuilder(log);
		String additionalStartupParameters = "/CompareCfg -FirstConfigurationType MainConfiguration "
		+ " -SecondConfigurationType DBConfiguration "
		+ " -ReportType Brief -ReportFormat txt "
		+ " -ReportFile " + reportFile.toString();
		command.additionalParameters(additionalStartupParameters);
		
		Process process = command.start();
		int returnCode = process.waitFor();
		if (returnCode != 0) {
			IStatus status = StorageUiPlugin.createErrorStatus(Files.readString(log));
			throw new CoreException(status);
		}
		
		long lineCount = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(reportFile.toString(),StandardCharsets.UTF_16))) { // тут UTF_16 почему-то
			lineCount = reader.lines().count();
		} catch (IOException e) {
			throw e;
		}
		
		if (lineCount == 6) { // нет изменений
			return true;
		} else {
			return false;
		}
	}
}
