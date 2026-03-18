package dev.zigr.dt.team.ui.storage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com._1c.g5.v8.bm.core.BmPlatform;
import com._1c.g5.v8.bm.core.IBmNamespace;
import com._1c.g5.v8.bm.core.IBmPlatformTransaction;
import com._1c.g5.v8.dt.common.FileUtil;
import com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.export.IExportOperation;
import com._1c.g5.v8.dt.export.IExportOperationFactory;
import com._1c.g5.v8.dt.export.IExportStrategy;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionException;
import com._1c.g5.v8.dt.team.git.infobases.IGitBranchIssueDescriptor;
import com.google.inject.Inject;

import java.lang.reflect.InvocationTargetException;

public class ExportHandler implements IHandler {

	@Inject
	private IQualifiedNameFilePathConverter qualifiedNameFilePathConverter;
	@Inject
	private IBmModelManager modelManager;
	@Inject
	private IExportOperationFactory exportOperationFactory;

	private Shell shell;
	private IGitBranchIssueDescriptor issueDescriptor;
	private Settings storageSettings;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		shell = HandlerUtil.getActiveShell(event);
		final Display display = shell.getDisplay();

		IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
		Object firstElement = selection.getFirstElement();
		issueDescriptor = (IGitBranchIssueDescriptor) Adapters.adapt(firstElement, IGitBranchIssueDescriptor.class);

		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(shell);

		try {
			progressDialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						performExport(monitor, display);
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			StorageUiPlugin.logError(cause.getMessage(), cause);
			display.syncExec(() -> {
				MessageDialog.openError(shell, "Ошибка помещения",
						cause.getMessage() + "\n\n" +
								"ЧТО ДЕЛАТЬ:\n" +
								"1. Зайдите в Конфигуратор 1С.\n" +
								"2. Отмените захват тех объектов, которые не удалось поместить (если они остались захвачены).\n"
								+
								"3. Либо примите другое решение непосредственно в хранилище 1С.");
			});
		} catch (InterruptedException e) {
			StorageUiPlugin.logInfo("Операция прервана пользователем");
		}

		return null;
	}

	private void performExport(IProgressMonitor monitor, Display display) throws Exception {
		monitor.beginTask("Помещение в хранилище", 100);

		monitor.subTask("Вычисление различий в Git...");
		Map<String, List<DiffEntry>> allDiff = getBranchDiff();
		monitor.worked(10);

		if (allDiff == null || allDiff.isEmpty()) {
			display.asyncExec(() -> {
				MessageDialog.openInformation(shell, "Нет изменений",
						"Не обнаружено изменений для помещения в хранилище.");
			});
			return;
		}

		final Set<String> objectsToLock = new HashSet<>();
		for (Map.Entry<String, List<DiffEntry>> entry : allDiff.entrySet()) {
			for (DiffEntry diffEntry : entry.getValue()) {
				String pathStr = diffEntry.getNewPath();
				if (pathStr.equals(DiffEntry.DEV_NULL)) {
					pathStr = diffEntry.getOldPath();
				}
				try {
					QualifiedName qName = qualifiedNameFilePathConverter.getFqn(pathStr);
					if (qName != null) {
						objectsToLock.add(qName.toString());
					}
				} catch (Exception e) {
					// Игнорируем файлы, которые не являются объектами метаданных
				}
			}
		}

		final boolean[] confirmed = { false };
		display.syncExec(() -> {
			StringBuilder sb = new StringBuilder();
			objectsToLock.stream().sorted().forEach(name -> sb.append("- ").append(name).append("\n"));

			MessageDialog dialog = new MessageDialog(shell, "Подтверждение помещения", null,
					"Список объектов для захвата в хранилище (можно прокручивать):", MessageDialog.QUESTION,
					new String[] { "Да", "Нет" }, 0) {
				@Override
				protected Control createCustomArea(Composite parent) {
					Text text = new Text(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.READ_ONLY);
					GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
					gd.heightHint = 400; // Ограничение высоты для длинных списков
					gd.widthHint = 600;
					text.setLayoutData(gd);
					text.setText(sb.toString());
					return text;
				}
			};
			confirmed[0] = dialog.open() == 0;
		});

		if (!confirmed[0]) {
			throw new InterruptedException("Операция отменена пользователем");
		}

		for (Map.Entry<String, List<DiffEntry>> entry : allDiff.entrySet()) {
			if (monitor.isCanceled())
				throw new InterruptedException();

			String projectName = entry.getKey();
			List<DiffEntry> diff = entry.getValue();
			storageSettings = new Settings(projectName);

			Path rootDirectory = FileUtil.createTempDirectory("Zigr").toPath();

			try {
				if (pushBranchDiff(projectName, diff, rootDirectory, monitor, display)) {
					String message = MessageFormat.format(
							"Операция помещения в хранилище выполнена. ИБ={0}. Проект={1}",
							issueDescriptor.getInfobase().getName(), projectName);
					StorageUiPlugin.logInfo(message);
				}
			} finally {
				try {
					FileUtil.deleteRecursivelyWithRetries(rootDirectory);
				} catch (IOException e) {
					StorageUiPlugin.logError(e.getMessage(), e);
				}
			}
		}

		monitor.done();
		display.asyncExec(() -> {
			MessageDialog.openInformation(shell, "Успешно помещено",
					"Операция помещения в хранилище успешно выполнена.\n\n" +
							"ОБЯЗАТЕЛЬНО:\n" +
							"1. Зайдите в Конфигуратор 1С.\n" +
							"2. Проверьте изменения в хранилище.\n" +
							"3. Примите решение о завершении помещения (Confirm/Commit) в самом хранилище 1С.");
		});
	}

	private boolean pushBranchDiff(String projectName, List<DiffEntry> diff, Path rootDirectory,
			IProgressMonitor monitor, Display display)
			throws IOException, CoreException, RuntimeExecutionException, InterruptedException {
		monitor.subTask(MessageFormat.format("Подключение к базе {0}...", projectName));
		Path exportDirectory = FileUtil.createTempDirectory("Export", rootDirectory).toPath();
		Designer designer = new Designer(issueDescriptor, projectName, rootDirectory);

		try {
			if (monitor.isCanceled())
				return false;

			designer.closeDesignerSession();

			Map<QualifiedName, Boolean> lockObjects = getLockObjects(diff);
			if (lockObjects.isEmpty()) {
				IStatus status = StorageUiPlugin.createErrorStatus("Не удалось определить объекты для захвата");
				throw new CoreException(status);
			}

			monitor.subTask("Захват объектов в хранилище (пакетный режим 1С)...");
			designer.lockObjects(lockObjects);
			monitor.worked(30);

			if (monitor.isCanceled())
				return false;

			monitor.subTask("Сравнение конфигурации с конфигурацией БД...");
			if (!designer.isConfigurationSame()) {
				if (storageSettings.getPushIfConfigurationChanged()) {
					final boolean[] userProceed = new boolean[1];
					display.syncExec(() -> {
						MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
						dialog.setText("Внимание!!!");
						String textMessage = textMessageIfConfigurationChanged(projectName)
								+ System.lineSeparator() + System.lineSeparator()
								+ "Все равно продолжить помещение?";
						dialog.setMessage(textMessage);
						userProceed[0] = (dialog.open() == SWT.YES);
					});

					if (!userProceed[0]) {
						String message = MessageFormat.format(
								"Операция помещения в хранилище отменена пользователем. ИБ={0}. Проект={1}",
								issueDescriptor.getInfobase().getName(), projectName);
						StorageUiPlugin.logInfo(message);
						return false;
					}
				} else {
					String textMessage = textMessageIfConfigurationChanged(projectName);
					IStatus status = StorageUiPlugin.createErrorStatus(textMessage);
					throw new CoreException(status);
				}
			}
			monitor.worked(10);

			if (monitor.isCanceled())
				return false;

			monitor.subTask("Экспорт измененных объектов в формат XML...");
			EObject[] topObjects = getTopObjects(projectName, diff);
			IExportOperation exportOperation = exportOperationFactory.createExportOperation(exportDirectory,
					designer.getVersion(), new IncrementalExportStrategy(), topObjects);
			IStatus status = exportOperation.run(monitor);
			if (status.getSeverity() == 4) {
				throw new CoreException(status);
			}
			monitor.worked(30);

			if (monitor.isCanceled())
				return false;

			V8FileBuilder v8FileBuilder = new V8FileBuilder(exportDirectory, projectName);
			v8FileBuilder.setSourceFiles(diff);
			Set<Path> exportFiles = v8FileBuilder.getExportFiles();
			Path listFiles = rootDirectory.resolve("listFiles.txt");
			try (BufferedWriter writer = new BufferedWriter(
					new FileWriter(listFiles.toString(), StandardCharsets.UTF_8))) {
				for (Path exportFile : exportFiles) {
					writer.append(exportFile.toString() + System.lineSeparator());
				}
			} catch (IOException e) {
				throw e;
			}

			monitor.subTask("Помещение объектов в хранилище (загрузка XML)...");
			designer.loadConfigurationFromXml(exportDirectory, listFiles);
			monitor.worked(20);

			return true;
		} finally {
			designer.dispose();
		}
	}

	private EObject[] getTopObjects(String projectName, List<DiffEntry> diff) {
		Set<EObject> topObjects = new HashSet<EObject>();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

		Set<String> sourceFiles = new HashSet<String>();
		for (DiffEntry entry : diff) {
			String sourceFile = entry.getNewPath();
			if (V8FileBuilder.isV8File(sourceFile)) {
				sourceFiles.add(sourceFile);
			}
		}

		Set<String> fqnStrings = new HashSet<String>();
		for (String sourceFile : sourceFiles) {
			QualifiedName fqn = qualifiedNameFilePathConverter.getFqn(sourceFile);
			if (fqn == null) {
				continue;
			}
			int segmentCount = fqn.getSegmentCount();
			if ("Configuration".equals(fqn.getFirstSegment())) {
				fqnStrings.add("Configuration");
			} else if (segmentCount >= 2) {
				fqnStrings.add(fqn.skipLast(segmentCount - 2).toString());
			}
		}

		BmPlatform platform = modelManager.getBmPlatform();
		IBmNamespace ns = modelManager.getBmNamespace(project);
		IBmPlatformTransaction transaction = platform.beginReadOnlyTransaction(true);
		try {
			for (String fqnString : fqnStrings) {
				EObject topObject = (EObject) transaction.getTopObjectByFqn(ns, fqnString);
				if (topObject != null) {
					topObjects.add(topObject);
				}
			}
			transaction.commit();
		} finally {
			transaction.rollback(); // For safety if commit fails
		}

		EObject[] result = new EObject[topObjects.size()];
		topObjects.toArray(result);
		return result;
	}

	private Map<QualifiedName, Boolean> getLockObjects(List<DiffEntry> diff) {
		Map<QualifiedName, Boolean> result = new HashMap<QualifiedName, Boolean>();

		Set<String> sourceFiles = new HashSet<String>();
		for (DiffEntry entry : diff) {
			String oldPath = entry.getOldPath();
			String newPath = entry.getNewPath();
			String sourceFile;
			if (oldPath.equals(DiffEntry.DEV_NULL) && newPath.endsWith(".aindex")) {
				sourceFile = newPath;
			} else {
				sourceFile = oldPath;
			}

			if (V8FileBuilder.isV8File(sourceFile)) {
				sourceFiles.add(sourceFile);
			}
		}

		for (String sourceFile : sourceFiles) {
			QualifiedName fqn = qualifiedNameFilePathConverter.getFqn(sourceFile);
			if (fqn == null) {
				continue;
			}
			int segmentCount = fqn.getSegmentCount();
			String firstSegment = fqn.getFirstSegment();
			if ("Configuration".equals(firstSegment)) {
				result.put(fqn.skipLast(segmentCount - 1), false);
			} else if ("Subsystem".equals(firstSegment)) {
				int firstCount = 0;
				for (int i = 0; i < segmentCount; i = i + 2) {
					if ("Subsystem".equals(fqn.getSegment(i))) {
						firstCount = i + 2;
					} else {
						break;
					}
				}
				if (firstCount > 0) {
					result.put(fqn.skipLast(segmentCount - firstCount), false);
				}
			} else if ("ExternalDataSource".equals(firstSegment)) {
				result.put(fqn.skipLast(segmentCount - 2), true);
			} else if ("CalculationRegister".equals(firstSegment) && sourceFile.endsWith(".mdo")) {
				result.put(fqn.skipLast(segmentCount - 2), true);
			} else if (segmentCount >= 4
					&& ("Form".equals(fqn.getSegment(2)) || "Template".equals(fqn.getSegment(2)))) {
				result.put(fqn.skipLast(segmentCount - 4), false);
			} else if (storageSettings.getExportMDWithMDO()) {
				if (sourceFile.endsWith(".mdo")) {
					result.put(fqn.skipLast(segmentCount - 2), true);
				} else {
					if (result.get(fqn.skipLast(segmentCount - 2)) == null) {
						result.put(fqn.skipLast(segmentCount - 2), false);
					}
				}
			} else {
				if (segmentCount >= 2) {
					result.put(fqn.skipLast(segmentCount - 2), false);
				}
			}
		}

		return result;
	}

	public Map<String, List<DiffEntry>> getBranchDiff() {
		Map<String, List<DiffEntry>> result = new HashMap<String, List<DiffEntry>>();

		List<DiffEntry> allDiff;
		Repository repository = issueDescriptor.getRepository();
		try (Git git = new Git(repository)) {
			String importBranch = issueDescriptor.getBranch().getName();
			String currentBranch = repository.getFullBranch();
			if (importBranch.equals(currentBranch)) {
				shell.getDisplay().syncExec(() -> {
					MessageDialog.openWarning(shell, "Внимание", "Нельзя выбирать текущую ветку");
				});
				return null;
			}
			AbstractTreeIterator oldTreeParser = prepareTreeParser(repository, importBranch);
			AbstractTreeIterator newTreeParser = prepareTreeParser(repository, currentBranch);
			allDiff = git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).call();
			RenameDetector rd = new RenameDetector(repository);
			rd.addAll(allDiff);
			allDiff = rd.compute();
			if (allDiff.isEmpty()) {
				shell.getDisplay().syncExec(() -> {
					MessageDialog.openWarning(shell, "Внимание", "Ветки не различаются");
				});
				return null;
			}
		} catch (GitAPIException | IOException e) {
			StorageUiPlugin.logError(e.getMessage(), e);
			shell.getDisplay().syncExec(() -> {
				MessageDialog.openError(shell, "Ошибка", "Не удалось определить различия веток: " + e.getMessage());
			});
			return null;
		}

		for (DiffEntry entry : allDiff) {
			String oldPath = entry.getOldPath();
			String newPath = entry.getNewPath();
			String sourceFile;
			if (newPath.equals(DiffEntry.DEV_NULL)) {
				sourceFile = oldPath;
			} else {
				sourceFile = newPath;
			}

			org.eclipse.core.runtime.IPath path = new org.eclipse.core.runtime.Path(sourceFile);
			if (path.segmentCount() < 2 || !path.segment(1).equals("src")) {
				continue;
			}

			String projectName = path.segment(0);
			List<DiffEntry> projectDiff = result.computeIfAbsent(projectName, k -> new ArrayList<>());
			projectDiff.add(entry);
		}

		return result;
	}

	private static AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException {
		Ref head = repository.exactRef(ref);
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(head.getObjectId());
			RevTree tree = walk.parseTree(commit.getTree().getId());

			CanonicalTreeParser treeParser = new CanonicalTreeParser();
			try (ObjectReader reader = repository.newObjectReader()) {
				treeParser.reset(reader, tree.getId());
			}

			walk.dispose();
			return treeParser;
		}
	}

	private static final class IncrementalExportStrategy implements IExportStrategy {
		@Override
		public boolean exportSubordinatesObjects(EObject eObject) {
			return !(eObject instanceof Configuration);
		}

		@Override
		public boolean exportExternalProperties(EObject eObject) {
			return true;
		}

		public boolean exportUnknown() {
			return false;
		}
	}

	private String textMessageIfConfigurationChanged(String projectName) {
		String textMessage = "Проект:{0}. После захвата объектов в хранилище обнаружено отличие конфигурации от конфигурации БД!"
				+ System.lineSeparator() + System.lineSeparator()
				+ "Это могут быть изменения, полученные из хранилища во время захвата. Во избежание потерь этих изменений "
				+ "нужно переключиться на ветку хранилища, импортировать туда все изменения, переключиться на текущую ветку "
				+ "и влить изменения из ветки хранилища.";
		return MessageFormat.format(textMessage, projectName);
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}
}
