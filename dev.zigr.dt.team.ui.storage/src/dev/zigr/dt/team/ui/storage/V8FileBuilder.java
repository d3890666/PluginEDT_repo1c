package dev.zigr.dt.team.ui.storage;

import com._1c.g5.v8.dt.compare.core.V8FileChecker;
import com._1c.g5.v8.dt.team.git.infobases.IGitBranchIssueDescriptor;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.diff.DiffEntry;

public class V8FileBuilder {
	private static final String SOURCE_FOLDER = "src";
	private static final Set<String> CONFIGURATION_ITEMS = getImmutableSetBuilder().add("Configuration.mdo")
			.add("ManagedApplicationModule.bsl").add("OrdinaryApplicationModule.bsl").add("SessionModule.bsl")
			.add("ExternalConnectionModule.bsl").add("MainSectionCommandInterface.cmi").add("StartPageWorkingArea.spwa")
			.add("HomePageWorkArea.hpwa").add("CommandInterface.cmi").add("ClientApplicationInterface.cai")
			.add("ParentConfigurations.bin").add("MobileClientSign.bin").add("MobileApplicationContent.scc")
			.build();
	private static final Set<String> FORM_ATTRIBUTE_ITEMS = getImmutableSetBuilder().add("ListSettings.dcss")
			.add("SpreadsheetData.mxlx").add("GeographicalSchema.geos").add("Chart.chart").add("GanttChart.chart")
			.add("Dendrogram.chart").add("PlannerSettings.pnrs").add("GraphicalScheme.scheme").build();
	private static final Set<String> TEMPLATE_EXTENSIONS = getImmutableSetBuilder().add("bin").add("mxlx").add("dcs")
			.add("txt").add("htmldoc").add("addin").add("scheme").add("axdt").add("geos").add("dcsat").build();
	private static final Set<String> PICTURE_EXTENSIONS = getImmutableSetBuilder().add("bmp").add("dib").add("rle")
			.add("gif").add("jpg").add("jpeg").add("png").add("tif").add("ico").add("wmf").add("emf").add("svg")
			.add("zip").add("picture").build();
	
	private Path exportDirectory;
	private Set<String> sourceFiles;
	private Set<Path> exportFiles;
	private boolean exportMDWithMDO;
	
	public V8FileBuilder(Path exportDirectory, IGitBranchIssueDescriptor issueDescriptor) {
		this.exportDirectory = exportDirectory;
		sourceFiles = new HashSet<String>();
		exportFiles = new HashSet<Path>();
		Settings storageSettings = new Settings(issueDescriptor.getInfobase());
		exportMDWithMDO = storageSettings.getExportMDWithMDO();
	}

	public void setSourceFiles(List<DiffEntry> diff) {
		sourceFiles.clear();
		V8FileChecker v8FileChecker = new V8FileChecker();
		
		for (DiffEntry entry : diff) {
			String sourceFile = entry.getNewPath();
			if (sourceFile == DiffEntry.DEV_NULL
					|| sourceFile.endsWith("suppress")
					|| !v8FileChecker.isV8File(sourceFile)) {
				continue;
			}
			sourceFiles.add(sourceFile);
		}
	}

	public Set<Path> getExportFiles() throws IOException {
		exportFiles.clear();
		
		for (String sourceFile : sourceFiles) {
			getV8Files(sourceFile);
		}
		
		return exportFiles;
	}
	
	private void getV8Files(String sourceFile) throws IOException {
		sourceFile = this.cutSourceFolder(sourceFile);
		IPath path = IPath.fromOSString(sourceFile);
		String extension = path.getFileExtension();
		String[] segments = path.segments();
		
		String firstSegment = segments[0];
		if ("Configuration".equals(firstSegment)) {
			getConfigurationFile(segments, extension);
		} else if ("Subsystems".equals(firstSegment)) {
			getSubsystemFile(segments, extension, 1);
		} else if ("CommonModules".equals(firstSegment)) {
			getCommonModuleFile(segments, extension);
		} else if ("SessionParameters".equals(firstSegment)) {
			getSessionParameterFile(segments, extension);
		} else if ("Roles".equals(firstSegment)) {
			getRoleFile(segments, extension);
		} else if ("CommonAttributes".equals(firstSegment)) {
			getCommonAttributeFile(segments, extension);
		} else if ("ExchangePlans".equals(firstSegment)) {
			getExchangePlanFile(segments, extension);
		} else if ("FilterCriteria".equals(firstSegment)) {
			getFilterCriteriaFile(segments, extension);
		} else if ("EventSubscriptions".equals(firstSegment)) {
			getEventSubscriptionFile(segments, extension);
		} else if ("ScheduledJobs".equals(firstSegment)) {
			getScheduledJobFile(segments, extension);
		} else if ("FunctionalOptions".equals(firstSegment)) {
			getFunctionalOptionFile(segments, extension);
		} else if ("FunctionalOptionsParameters".equals(firstSegment)) {
			getFunctionalOptionsParameterFile(segments, extension);
		} else if ("DefinedTypes".equals(firstSegment)) {
			getDefinedTypeFile(segments, extension);
		} else if ("SettingsStorages".equals(firstSegment)) {
			getSettingsStorageFile(segments, extension);
		} else if ("CommonForms".equals(firstSegment)) {
			getCommonFormFile(segments, extension);
		} else if ("CommonCommands".equals(firstSegment)) {
			getCommonCommandFile(segments, extension);
		} else if ("CommandGroups".equals(firstSegment)) {
			getCommandGroupFile(segments, extension);
		} else if ("CommonTemplates".equals(firstSegment)) {
			getCommonTemplateFile(segments, extension);
		} else if ("CommonPictures".equals(firstSegment)) {
			getCommonPictureFile(segments, extension);
		} else if ("XDTOPackages".equals(firstSegment)) {
			getXDTOPackageFile(segments, extension);
		} else if ("WebServices".equals(firstSegment)) {
			getWebServiceFile(segments, extension);
		} else if ("HTTPServices".equals(firstSegment)) {
			getHTTPServiceFile(segments, extension);
		} else if ("WSReferences".equals(firstSegment)) {
			getWSReferenceFile(segments, extension);
		} else if ("IntegrationServices".equals(firstSegment)) {
			getIntegrationServiceFile(segments, extension);
		} else if ("Bots".equals(firstSegment)) {
			getBotFile(segments, extension);
		} else if ("WebSocketClients".equals(firstSegment)) {
			getWebSocketClientsFile(segments, extension);
		} else if ("StyleItems".equals(firstSegment)) {
			getStyleItemFile(segments, extension);
		} else if ("Styles".equals(firstSegment)) {
			getStyleFile(segments, extension);
		} else if ("Constants".equals(firstSegment)) {
			getConstantFile(segments, extension);
		} else if ("Catalogs".equals(firstSegment)) {
			getCatalogFile(segments, extension);
		} else if ("Documents".equals(firstSegment)) {
			getDocumentFile(segments, extension);
		} else if ("DocumentNumerators".equals(firstSegment)) {
			getDocumentNumeratorFile(segments, extension);
		} else if ("Sequences".equals(firstSegment)) {
			getSequenceFile(segments, extension);
		} else if ("DocumentJournals".equals(firstSegment)) {
			getDocumentJournalFile(segments, extension);
		} else if ("Enums".equals(firstSegment)) {
			getEnumFile(segments, extension);
		} else if ("Reports".equals(firstSegment)) {
			getReportFile(segments, extension);
		} else if ("DataProcessors".equals(firstSegment)) {
			getDataProcessorFile(segments, extension);
		} else if ("ChartsOfCharacteristicTypes".equals(firstSegment)) {
			getChartOfCharacteristicTypesFile(segments, extension);
		} else if ("ChartsOfAccounts".equals(firstSegment)) {
			getChartOfAccountsFile(segments, extension);
		} else if ("ChartsOfCalculationTypes".equals(firstSegment)) {
			getChartOfCalculationTypesFile(segments, extension);
		} else if ("InformationRegisters".equals(firstSegment)) {
			getInformationRegisterFile(segments, extension);
		} else if ("AccumulationRegisters".equals(firstSegment)) {
			getAccumulationRegisterFile(segments, extension);
		} else if ("AccountingRegisters".equals(firstSegment)) {
			getAccountingRegisterFile(segments, extension);
		} else if ("CalculationRegisters".equals(firstSegment)) {
			getCalculationRegisterFile(segments, extension);
		} else if ("BusinessProcesses".equals(firstSegment)) {
			getBusinessProcesseFile(segments, extension);
		} else if ("Tasks".equals(firstSegment)) {
			getTaskFile(segments, extension);
		} else if ("ExternalDataSources".equals(firstSegment)) {
			getExternalDataSourceFile(segments, extension);
		}
	}

	private void getConfigurationFile(String[] segments, String extension) throws IOException {
		if ("Configuration.mdo".equals(segments[1])) {
			Path exportFile = Paths.get("Configuration.xml");
			exportFiles.add(exportFile);
		}
		else if ("bsl".equals(extension) && segments.length == 2 && CONFIGURATION_ITEMS.contains(segments[1])) {
			Path exportFile = Paths.get("Ext", segments[1]);
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get("Ext");
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith(".bsl")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getSubsystemFile(String[] segments, String extension, int index) throws IOException {
		if (segments.length == index + 2) {
			if ("mdo".equals(extension)) {
				Path exportFile = Paths.get(segments[0]);
				for (int i = 1; i < index; i++) {
					exportFile = exportFile.resolve(segments[i]);
				}
				exportFile = exportFile.resolve(segments[index]+".xml");
				exportFiles.add(exportFile);
			}
			else if ("CommandInterface.cmi".equals(segments[index + 1])) {
				Path exportFile = Paths.get(segments[0]);
				for (int i = 1; i <= index; i++) {
					exportFile = exportFile.resolve(segments[i]);
				}
				exportFile = exportFile.resolve("Ext");
				exportFile = exportFile.resolve("CommandInterface.xml");
				exportFiles.add(exportFile);
			}
		}
		else if (segments.length > index + 2 && "Subsystems".equals(segments[index + 1])) {
			getSubsystemFile(segments, extension, index + 2);
		}
		else {
			Path scanDirectory = Paths.get(segments[0]);
			for (int i = 1; i <= index; i++) {
				scanDirectory = scanDirectory.resolve(segments[i]);
			}
			scanDirectory = scanDirectory.resolve("Ext");
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith("CommandInterface.xml")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getCommonModuleFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (segments.length == 3 && "Module.bsl".equals(segments[2])) {
			Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
			exportFiles.add(exportFile);
		}
	}

	private void getSessionParameterFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
	}

	private void getRoleFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)
				|| segments.length == 3 && "Rights.rights".equals(segments[2])) {
			getTopMdObjectFile(segments, extension);
		}
	}

	private void getCommonAttributeFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
	}

	private void getExchangePlanFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getFilterCriteriaFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
	}

	private void getEventSubscriptionFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
	}

	private void getScheduledJobFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)
				|| segments.length == 3 && "Schedule.schedule".equals(segments[2])) {
			getTopMdObjectFile(segments, extension);
		}
	}

	private void getFunctionalOptionFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
	}

	private void getFunctionalOptionsParameterFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
	}

	private void getDefinedTypeFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
	}

	private void getSettingsStorageFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getCommonFormFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else if (segments.length == 3 && "Module.bsl".equals(segments[2])) {
			Path exportFile = Paths.get(segments[0], segments[1], "Ext", "Form", segments[2]);
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith(".bsl")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getCommonCommandFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else if (segments.length == 3 && "CommandModule.bsl".equals(segments[2])) {
			Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith(".bsl")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getCommandGroupFile(String[] segments, String extension) {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
	}

	private void getCommonTemplateFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getCommonPictureFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getXDTOPackageFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getWebServiceFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else if (segments.length == 3 && "Module.bsl".equals(segments[2])) {
			Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith(".bsl")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getHTTPServiceFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else if (segments.length == 3 && "Module.bsl".equals(segments[2])) {
			Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith(".bsl")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getWSReferenceFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getIntegrationServiceFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else if (segments.length == 3 && "Module.bsl".equals(segments[2])) {
			Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith(".bsl")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getBotFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else if (segments.length == 3 && "Module.bsl".equals(segments[2])) {
			Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith(".bsl")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getWebSocketClientsFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else if (segments.length == 3 && "Module.bsl".equals(segments[2])) {
			Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith(".bsl")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getStyleItemFile(String[] segments, String extension) {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
	}

	private void getStyleFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			Path exportFile = Paths.get(segments[0], segments[1]+".xml");
			exportFiles.add(exportFile);
		}
		else {
			Path scanDirectory = Paths.get(segments[0], segments[1]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getConstantFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (segments.length == 3 && "ValueManagerModule.bsl".equals(segments[2])) {
			Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
			exportFiles.add(exportFile);
		}
	}

	private void getCatalogFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getDocumentFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getDocumentNumeratorFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
	}

	private void getSequenceFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isRecordSetModuleFile(segments, 2)) {
			getRecordSetModuleFile(segments, 2);
		}
	}

	private void getDocumentJournalFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getEnumFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getReportFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getDataProcessorFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getChartOfCharacteristicTypesFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getChartOfAccountsFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getChartOfCalculationTypesFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getInformationRegisterFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isRecordSetModuleFile(segments, 2)) {
			getRecordSetModuleFile(segments, 2);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getAccumulationRegisterFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isRecordSetModuleFile(segments, 2)) {
			getRecordSetModuleFile(segments, 2);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getAccountingRegisterFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isRecordSetModuleFile(segments, 2)) {
			getRecordSetModuleFile(segments, 2);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getCalculationRegisterFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isRecordSetModuleFile(segments, 2)) {
			getRecordSetModuleFile(segments, 2);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
		else if (isRecalculationFile(segments)) {
			getRecalculationFile(segments);
		}
	}

	private void getBusinessProcesseFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension)
				|| isHelpFile(segments, 2, extension)
				|| segments.length == 3 && "Flowchart.scheme".equals(segments[2])) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
//		else if (segments.length == 3 && "Flowchart.scheme".equals(segments[2])) {
//			// TODO файлы карты маршрута можно отдельно выгружать
//		}
	}

	private void getTaskFile(String[] segments, String extension) throws IOException {
		if (isTopMdObjectFile(segments, extension) || isHelpFile(segments, 2, extension)) {
			getTopMdObjectFile(segments, extension);
		}
		else if (isManagerModuleFile(segments)) {
			getManagerModuleFile(segments);
		}
		else if (isObjectModuleFile(segments)) {
			getObjectModuleFile(segments);
		}
		else if (isInnerCommandFile(segments)) {
			getInnerCommandFile(segments);
		}
		else if (isInnerFormFile(segments, extension)) {
			getInnerFormFile(segments, extension);
		}
		else if (isInnerTemplateFile(segments, extension)) {
			getInnerTemplateFile(segments, extension);
		}
	}

	private void getExternalDataSourceFile(String[] segments, String extension) throws IOException {
		// xml-файл с описанием объекта
		Path exportFile = Paths.get(segments[0], segments[1]+".xml");
		exportFiles.add(exportFile);
		// все подчиненные объекты
		Path scanDirectory = Paths.get(segments[0], segments[1]);
		scanDirectory = exportDirectory.resolve(scanDirectory);
		Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
		for (Path fileInDirectory : filesInDirectory) {
			exportFiles.add(exportDirectory.relativize(fileInDirectory));
		}
		// TODO подчиненные объекты можно отдельно выгружать
	}

	private boolean isTopMdObjectFile(String[] segments, String extension) {
		return segments.length == 3 && "mdo".equals(extension);
	}

	private boolean isManagerModuleFile(String[] segments) {
		return segments.length == 3 && "ManagerModule.bsl".equals(segments[2]);
	}

	private boolean isObjectModuleFile(String[] segments) {
		return segments.length == 3 && "ObjectModule.bsl".equals(segments[2]);
	}

	private boolean isRecordSetModuleFile(String[] segments, int index) {
		return segments.length == index + 1 && "RecordSetModule.bsl".equals(segments[index]);
	}

	private boolean isInnerCommandFile(String[] segments) {
		return segments.length == 5 && "Commands".equals(segments[2]) && this.isCommandFile(segments, 2);
	}

	private boolean isCommandFile(String[] segments, int index) {
		return "CommandModule.bsl".equals(segments[index + 2]);
	}

	private boolean isInnerFormFile(String[] segments, String extension) {
		return segments.length > 4 && "Forms".equals(segments[2]) && this.isFormFile(segments, 2, extension);
	}

	private boolean isFormFile(String[] segments, int index, String extension) {
		if (segments.length == index + 3) {
			String lastSegment = segments[segments.length - 1];
			return "Form.form".equals(lastSegment) || "Module.bsl".equals(lastSegment)
					|| "ConditionalAppearance.dcssca".equals(lastSegment) || "Form.oform".equals(lastSegment);
		} else if (segments.length > index + 3 && "BaseForm".equals(segments[index + 2])) {
			return this.isBaseFormFile(segments, index + 1, extension);
		} else {
			return this.isFormAttributeFile(segments, index + 2)
					|| this.isFormElementPicture(segments, index + 2, extension)
					|| this.isHelpFile(segments, index + 2, extension);
		}
	}

	private boolean isBaseFormFile(String[] segments, int index, String extension) {
		if (segments.length == index + 3) {
			return "Form.form".equals(segments[index + 2])
					|| "ConditionalAppearance.dcssca".equals(segments[index + 2]);
		} else {
			return this.isFormAttributeFile(segments, index + 2)
					|| this.isFormElementPicture(segments, index + 2, extension);
		}
	}

	private boolean isFormAttributeFile(String[] segments, int index) {
		return segments.length == index + 4 && "Attributes".equals(segments[index])
				&& "ExtInfo".equals(segments[index + 2]) && FORM_ATTRIBUTE_ITEMS.contains(segments[index + 3]);
	}

	private boolean isInnerTemplateFile(String[] segments, String extension) {
		return segments.length > 4 && "Templates".equals(segments[2]) && this.isTemplateFile(segments, 2, extension);
	}

	private boolean isTemplateFile(String[] segments, int index, String extension) {
		String lastSegmentWitoutExtension;
		if (segments.length == index + 3) {
			if ("html".equals(extension)) {
				return true;
			} else {
				lastSegmentWitoutExtension = segments[segments.length - 1];
				lastSegmentWitoutExtension = this.removeExtension(lastSegmentWitoutExtension);
				if (TEMPLATE_EXTENSIONS.contains(extension) && "Template".equals(lastSegmentWitoutExtension)) {
					return true;
				} else {
					return false;
				}
			}
		} else if (segments.length == index + 4 && "_files".equals(segments[index + 2])) {
			return true;
		} else if (segments.length == index + 5 && "Items".equals(segments[index + 2])) {
			lastSegmentWitoutExtension = this.removeExtension(segments[index + 4]);
			return "Picture".equals(lastSegmentWitoutExtension) && PICTURE_EXTENSIONS.contains(extension);
		} else {
			return false;
		}
	}

	private boolean isRecalculationFile(String[] segments) {
		return segments.length == 5 && "Recalculations".equals(segments[2]) && this.isRecordSetModuleFile(segments, 4);
	}

	private boolean isHelpFile(String[] segments, int index, String extension) {
		if (segments.length > index + 1 && "Help".equals(segments[index])) {
			return segments.length == index + 2 && "html".equals(extension)
					|| segments.length == index + 3 && "_files".equals(segments[index + 1]);
		} else {
			return false;
		}
	}

	private boolean isFormElementPicture(String[] segments, int index, String extension) {
		return segments.length == index + 3 && "Items".equals(segments[index])
				&& PICTURE_EXTENSIONS.contains(extension);
	}

	private String removeExtension(String lastSegment) {
		int dotIndex = lastSegment.lastIndexOf(46);
		if (dotIndex != -1) {
			lastSegment = lastSegment.substring(0, dotIndex);
		}

		return lastSegment;
	}

	private String cutSourceFolder(String path) {
		int sourceFolderIndex = path.indexOf(SOURCE_FOLDER);
		return sourceFolderIndex >= 0 ? path.substring(sourceFolderIndex + SOURCE_FOLDER.length() + 1) : null;
	}
	
	private static Builder<String> getImmutableSetBuilder() {
		Builder<String> result = ImmutableSet.builder();
		return result;
	}
	
	private Set<Path> getAllFilesInDirectory(Path scanDirectory) throws IOException {
		return getAllFilesInDirectory(scanDirectory, 0);
	}
	
	private Set<Path> getAllFilesInDirectory(Path scanDirectory, int maxDepth) throws IOException {
		Set<Path> result = new HashSet<Path>();
		
		if (!Files.exists(scanDirectory)) {
			return result;
		}
		
		if (maxDepth < 1) {
			Files.walk(scanDirectory).filter(Files::isRegularFile).forEach(result::add);
		} else {
			Files.walk(scanDirectory, maxDepth).filter(Files::isRegularFile).forEach(result::add);
		}
		
		return result;
	}
	
	private void getTopMdObjectFile(String[] segments, String extension) throws IOException {
		
		// xml-файл с описанием объекта
		Path exportFile = Paths.get(segments[0], segments[1]+".xml");
		exportFiles.add(exportFile);
		//
		Path scanDirectory = Paths.get(segments[0], segments[1]);
		scanDirectory = exportDirectory.resolve(scanDirectory);
		Path formsDirectory = scanDirectory.resolve("Forms");
		Path templatesDirectory = scanDirectory.resolve("Templates");
		// подчиненные объекты, кроме Forms, Templates и модулей
		Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
		for (Path fileInDirectory : filesInDirectory) {
			if (fileInDirectory.toString().endsWith(".bsl")
					|| fileInDirectory.startsWith(formsDirectory)
					|| fileInDirectory.startsWith(templatesDirectory)) {
				continue;
			}
			exportFiles.add(exportDirectory.relativize(fileInDirectory));
		}
		if (exportMDWithMDO) {
			// xml-файл с описанием формы
			Set<Path> filesInFormsDirectory = getAllFilesInDirectory(formsDirectory, 1);
			for (Path fileInFormsDirectory : filesInFormsDirectory) {
				exportFiles.add(exportDirectory.relativize(fileInFormsDirectory));
			}
			// xml-файл с описанием шаблона
			Set<Path> filesInTemplatesDirectory = getAllFilesInDirectory(templatesDirectory, 1);
			for (Path fileInTemplatesDirectory : filesInTemplatesDirectory) {
				exportFiles.add(exportDirectory.relativize(fileInTemplatesDirectory));
			}
		}
	}

	private void getManagerModuleFile(String[] segments) {
		Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
		exportFiles.add(exportFile);
	}
	
	private void getObjectModuleFile(String[] segments) {
		Path exportFile = Paths.get(segments[0], segments[1], "Ext", segments[2]);
		exportFiles.add(exportFile);
	}

	private void getRecordSetModuleFile(String[] segments, int index) {
		Path exportFile = Paths.get(segments[0]);
		for (int i = 1; i < index; i++) {
			exportFile = exportFile.resolve(segments[i]);
		}
		exportFile = exportFile.resolve("Ext");
		exportFile = exportFile.resolve(segments[index]);
		exportFiles.add(exportFile);
	}

	private void getInnerCommandFile(String[] segments) {
		Path exportFile = Paths.get(segments[0], segments[1], segments[2], segments[3], "Ext", segments[4]);
		exportFiles.add(exportFile);
	}

	private void getInnerFormFile(String[] segments, String extension) throws IOException {
		if (segments.length == 5 && "Module.bsl".equals(segments[4])) {
			Path exportFile = Paths.get(segments[0], segments[1], segments[2], segments[3], "Ext", "Form", segments[4]);
			exportFiles.add(exportFile);
		}
		else {
			if (!exportMDWithMDO) {
				// xml-файл с описанием объекта
				Path exportFile = Paths.get(segments[0], segments[1], segments[2], segments[3]+".xml");
				exportFiles.add(exportFile);
			}
			
			Path scanDirectory = Paths.get(segments[0], segments[1], segments[2], segments[3]);
			scanDirectory = exportDirectory.resolve(scanDirectory);
			Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
			for (Path fileInDirectory : filesInDirectory) {
				if (fileInDirectory.toString().endsWith(".bsl")) {
					continue;
				}
				exportFiles.add(exportDirectory.relativize(fileInDirectory));
			}
		}
	}

	private void getInnerTemplateFile(String[] segments, String extension) throws IOException {
		Path scanDirectory = Paths.get(segments[0], segments[1], segments[2], segments[3]);
		scanDirectory = exportDirectory.resolve(scanDirectory);
		Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
		for (Path fileInDirectory : filesInDirectory) {
			exportFiles.add(exportDirectory.relativize(fileInDirectory));
		}
	}

	private void getRecalculationFile(String[] segments) {
		getRecordSetModuleFile(segments, 4);
	}

	private void getHelpFile(String[] segments, int index, String extension) throws IOException {
		// TODO файлы справки можно отдельно выгружать
		Path helpDirectory = Paths.get(segments[0]);
		for (int i = 1; i < index; i++) {
			helpDirectory = helpDirectory.resolve(segments[i]);
		}
		helpDirectory = helpDirectory.resolve("Ext");
		// Help.xml
		exportFiles.add(helpDirectory.resolve("Help.xml"));
		// подпапка Help
		Path scanDirectory = helpDirectory.resolve("Help");
		scanDirectory = exportDirectory.resolve(scanDirectory);
		Set<Path> filesInDirectory = getAllFilesInDirectory(scanDirectory);
		for (Path fileInDirectory : filesInDirectory) {
			exportFiles.add(exportDirectory.relativize(fileInDirectory));
		}
	}

}