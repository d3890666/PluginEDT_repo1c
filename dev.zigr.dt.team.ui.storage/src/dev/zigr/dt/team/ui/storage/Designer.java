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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.xtext.naming.QualifiedName;

import com._1c.g5.v8.dt.common.Pair;
import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseAccessManager;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseAccessSettings;
import com._1c.g5.v8.dt.platform.services.core.infobases.InfobaseAssociationContext;
import com._1c.g5.v8.dt.platform.services.core.infobases.sync.IConfigDumpInfoStore;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.ComponentExecutorInfo;
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

	private ServiceSupplier<IInfobaseAccessManager> infobaseAccessManagerSupplier = ServiceAccess
			.supplier(IInfobaseAccessManager.class, StorageUiPlugin.class);
	private ServiceSupplier<IRuntimeComponentManager> runtimeComponentManagerSupplier = ServiceAccess
			.supplier(IRuntimeComponentManager.class, StorageUiPlugin.class);
	private ServiceSupplier<IV8ProjectManager> v8ProjectManagerSupplier = ServiceAccess
			.supplier(IV8ProjectManager.class, StorageUiPlugin.class);
	private ServiceSupplier<IConfigDumpInfoStore> projectSettingsStoreSupplier = ServiceAccess
			.supplier(IConfigDumpInfoStore.class, StorageUiPlugin.class);

	private IGitBranchIssueDescriptor issueDescriptor;
	private IProject project;
	private Path rootDirectory;
	private Version version;
	private Pair<ILaunchableRuntimeComponent, IDesignerSessionThickClientLauncher> thickClient;
	private String extensionName;

	public Designer(IGitBranchIssueDescriptor issueDescriptor, String projectName, Path rootDirectory)
			throws CoreException, IOException, InterruptedException, RuntimeExecutionException {
		this.issueDescriptor = issueDescriptor;
		this.project = getV8ProjectManager().getProject(projectName).getProject();
		this.rootDirectory = rootDirectory;

		Optional<IResolvableRuntimeInstallation> installationOpt = getInfobaseAccessManager()
				.loadSelectedInstallation(project, issueDescriptor.getInfobase());
		if (installationOpt.isEmpty()) {
			IStatus status = StorageUiPlugin
					.createErrorStatus("Runtime installation not found for project: " + projectName);
			throw new CoreException(status);
		}
		IResolvableRuntimeInstallation actualInstallation = installationOpt.get();
		RuntimeInstallation thickClientComponent = actualInstallation
				.resolve(java.util.List.of(IRuntimeComponentTypes.THICK_CLIENT), null);
		version = thickClientComponent.getVersion();
		ComponentExecutorInfo<ILaunchableRuntimeComponent, IDesignerSessionThickClientLauncher> info = getRuntimeComponentManager()
				.resolveExecutor(ILaunchableRuntimeComponent.class, IDesignerSessionThickClientLauncher.class,
						thickClientComponent, IRuntimeComponentTypes.THICK_CLIENT);
		thickClient = new Pair<>(info.getComponent(), info.getExecutor());
		extensionName = getExtensionName();
	}

	private IInfobaseAccessManager getInfobaseAccessManager() {
		return infobaseAccessManagerSupplier.get();
	}

	private IRuntimeComponentManager getRuntimeComponentManager() {
		return runtimeComponentManagerSupplier.get();
	}

	private IV8ProjectManager getV8ProjectManager() {
		return v8ProjectManagerSupplier.get();
	}

	private IConfigDumpInfoStore getProjectSettingsStore() {
		return projectSettingsStoreSupplier.get();
	}

	public void dispose() {
		infobaseAccessManagerSupplier.close();
		runtimeComponentManagerSupplier.close();
		v8ProjectManagerSupplier.close();
		projectSettingsStoreSupplier.close();
	}

	public Version getVersion() {
		return version;
	}

	public void closeDesignerSession() throws RuntimeExecutionException {
		thickClient.second.closeDesignerSession(thickClient.first, issueDescriptor.getInfobase(), null);
	}

	private RuntimeExecutionCommandBuilder getCommandBuilder(Path log) throws CoreException {
		InfobaseReference infobase = issueDescriptor.getInfobase();
		IInfobaseAccessSettings settings = getInfobaseAccessManager().resolveSettings(infobase);
		File launchFile = thickClient.first.getFile();

		RuntimeExecutionCommandBuilder result = new RuntimeExecutionCommandBuilder(launchFile,
				RuntimeExecutionCommandBuilder.ThickClientMode.DESIGNER)
				.forInfobase(infobase, false).userName(settings.userName()).userPassword(settings.password())
				.disableStartupDialogs().interfaceLanguage("ru").logTo(log.toFile(), true);

		return result;
	}

	public boolean configDumpInfoOnly() throws CoreException, IOException, InterruptedException {
		if (!extensionName.isEmpty()) {
			return false;
		}

		Path log = rootDirectory.resolve("cdiOnlyOut.txt");

		RuntimeExecutionCommandBuilder command = getCommandBuilder(log)
				.exportXmlFromInfobase(rootDirectory).additionalParameters("-configDumpInfoOnly");

		Process process = command.start();
		int returnCode = process.waitFor();
		if (returnCode != 0) {
			IStatus status = StorageUiPlugin.createErrorStatus(readLogFile(log));
			throw new CoreException(status);
		}

		return true;
	}

	public void loadConfigurationFromXml(Path sourceFolder, Path fileList)
			throws CoreException, IOException, InterruptedException {
		Path log = rootDirectory.resolve("loadCfgOut.txt");

		RuntimeExecutionCommandBuilder command = getCommandBuilder(log)
				.importXmlToInfobase(sourceFolder).fileList(fileList).updateConfigDumpInfo();

		if (!extensionName.isEmpty()) {
			command.forExtension(extensionName);
		}

		Process process = command.start();
		int returnCode = process.waitFor();
		if (returnCode == 0) {
			// актуализация ConfigDumpInfo.xml в ветке хранилища
			// В EDT 2025.2.3 метод storeConfigDumpInfo удалён платформой.
			// Обновление файла ConfigDumpInfo.xml конфигуратора всё равно происходит в
			// связи с вызовом .updateConfigDumpInfo() в команде пакетного режима 1С.
		} else {
			IStatus status = StorageUiPlugin.createErrorStatus(readLogFile(log));
			throw new CoreException(status);
		}
	}

	public void lockObjects(Map<QualifiedName, Boolean> lockObjects)
			throws IOException, CoreException, InterruptedException {
		// формирование файла со списком объектов для захвата
		Path lockObjectsList = rootDirectory.resolve("lockObjectsList.xml");
		String strTemplate = "<Object fullName = \"{0}\" includeChildObjects = \"{1}\" />";
		try (BufferedWriter writer = new BufferedWriter(
				new FileWriter(lockObjectsList.toString(), StandardCharsets.UTF_8))) {
			writer.append(
					"<Objects xmlns=\"http://v8.1c.ru/8.3/config/objects\" version=\"1.0\">" + System.lineSeparator());
			for (Map.Entry<QualifiedName, Boolean> entry : lockObjects.entrySet()) {
				QualifiedName key = entry.getKey();
				Boolean val = entry.getValue();
				if ("Configuration".equals(key.toString())) {
					writer.append("<Configuration includeChildObjects = \"false\" />" + System.lineSeparator());
				} else {
					writer.append(
							MessageFormat.format(strTemplate, key.toString(), val.toString()) + System.lineSeparator());
				}
			}
			writer.append("</Objects>");
		} catch (IOException e) {
			throw e;
		}

		Path log = rootDirectory.resolve("lockObjectsOut.txt");
		RuntimeExecutionCommandBuilder command = getCommandBuilder(log);
		Settings storageSettings = new Settings(project.getName());
		String additionalStartupParameters = "/ConfigurationRepositoryF " + storageSettings.getAddress()
				+ " /ConfigurationRepositoryN " + storageSettings.getUser()
				+ (storageSettings.getPassword().isEmpty() ? ""
						: " /ConfigurationRepositoryP " + storageSettings.getPassword())
				+ " /ConfigurationRepositoryLock -Objects " + lockObjectsList.toString()
				+ "{0}";

		if (!extensionName.isEmpty()) {
			command.additionalParameters(
					MessageFormat.format(additionalStartupParameters, " -Extension " + extensionName));
		} else {
			command.additionalParameters(MessageFormat.format(additionalStartupParameters, ""));
		}

		Process process = command.start();
		int returnCode = process.waitFor();
		if (returnCode != 0) {
			if (!extensionName.isEmpty()) { // имя расширения могло быть переименовано в EDT
				Path logListExtNames = rootDirectory.resolve("listExtNamesOut.txt");
				command = getCommandBuilder(logListExtNames);
				command.listConfigurationExtensions();
				process = command.start();
				returnCode = process.waitFor();
				if (returnCode != 0) {
					IStatus status = StorageUiPlugin.createErrorStatus(Files.readString(logListExtNames));
					throw new CoreException(status);
				} else {
					try (BufferedReader reader = new BufferedReader(
							new FileReader(logListExtNames.toString(), StandardCharsets.UTF_8))) {
						String line;
						boolean extensionIsFound = false;
						while ((line = reader.readLine()) != null) {
							Path logExtensionLockObjects = rootDirectory.resolve("extensionObjectsOut.txt");
							command = getCommandBuilder(logExtensionLockObjects);
							command.additionalParameters(
									MessageFormat.format(additionalStartupParameters, " -Extension " + line));
							process = command.start();
							returnCode = process.waitFor();
							if (returnCode == 0) {
								extensionName = line;
								extensionIsFound = true;
								break;
							}
						}
						if (!extensionIsFound) {
							IStatus status = StorageUiPlugin
									.createErrorStatus("В ИБ не обнаружено расширение, подключенное к хранилищу "
											+ storageSettings.getAddress());
							throw new CoreException(status);
						}
					} catch (IOException e) {
						throw e;
					}
				}
			} else {
				String logContent = readLogFile(log);
				IStatus status = StorageUiPlugin.createErrorStatus("Ошибка при захвата объектов в хранилище:\n" + logContent);
				throw new CoreException(status);
			}
		}
	}

	private String readLogFile(Path log) {
		if (!Files.exists(log)) {
			return "Файл лога не найден.";
		}
		try {
			// Конфигуратор 1С часто пишет логи в зависимости от ОС или настроек в разных кодировках.
			// Попробуем прочитать как UTF-8, если не выйдет - как системную или UTF-16.
			List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
			return lines.stream()
					.filter(line -> !line.isBlank())
					.filter(line -> !line.contains("Завершение сеанса"))
					.filter(line -> !line.contains("Объект захвачен для редактирования:"))
					.filter(line -> !line.contains("Объект уже захвачен для редактирования:"))
					.filter(line -> !line.contains("Начало операции с хранилищем"))
					.filter(line -> !line.contains("Окончание операции с хранилищем"))
					.collect(Collectors.joining("\n"));
		} catch (Exception e) {
			try {
				return Files.readString(log);
			} catch (IOException e1) {
				return "Не удалось прочитать файл лога: " + e.getMessage();
			}
		}
	}

	public boolean isConfigurationSame() throws CoreException, IOException, InterruptedException {
		Path log = rootDirectory.resolve("compareCfgOut.txt");
		Path reportFile = rootDirectory.resolve("compareCfgReport.txt");

		RuntimeExecutionCommandBuilder command = getCommandBuilder(log);

		String additionalStartupParameters = "/CompareCfg "
				+ "-FirstConfigurationType {0} -SecondConfigurationType {1} "
				+ "-IncludeChangedObjects -IncludeDeletedObjects -IncludeAddedObjects "
				+ "-ReportType Brief -ReportFormat txt "
				+ "-ReportFile " + reportFile.toString();

		if (!extensionName.isEmpty()) {
			additionalStartupParameters = MessageFormat.format(additionalStartupParameters,
					"ExtensionConfiguration -FirstName " + extensionName,
					"ExtensionDBConfiguration -SecondName " + extensionName);
		} else {
			additionalStartupParameters = MessageFormat.format(additionalStartupParameters, "MainConfiguration",
					"DBConfiguration");
		}

		command.additionalParameters(additionalStartupParameters);

		Process process = command.start();
		int returnCode = process.waitFor();
		if (returnCode != 0) {
			IStatus status = StorageUiPlugin.createErrorStatus(readLogFile(log));
			throw new CoreException(status);
		}

		long lineCount = 0;
		try (BufferedReader reader = new BufferedReader(
				new FileReader(reportFile.toString(), StandardCharsets.UTF_16))) { // тут UTF_16 почему-то
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

	public String getExtensionName() throws CoreException, IOException, InterruptedException {
		String result = "";
		IV8Project v8Project = getV8ProjectManager().getProject(project);
		if (v8Project instanceof IExtensionProject extensionProject) {
			result = extensionProject.getConfiguration().getName();
		}

		return result;
	}
}
