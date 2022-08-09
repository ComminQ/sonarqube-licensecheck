package at.porscheinformatik.sonarqube.licensecheck.pip;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import at.porscheinformatik.sonarqube.licensecheck.Dependency;
import at.porscheinformatik.sonarqube.licensecheck.LicenseCheckRulesDefinition;
import at.porscheinformatik.sonarqube.licensecheck.Scanner;
import at.porscheinformatik.sonarqube.licensecheck.licensemapping.LicenseMappingService;
import at.porscheinformatik.sonarqube.licensecheck.utils.PipUtils;

public class RequirementsTxtDependencyScanner implements Scanner {

  private static final Logger LOGGER = Loggers.get(RequirementsTxtDependencyScanner.class);

  private final LicenseMappingService licenseMappingService;

  public RequirementsTxtDependencyScanner(LicenseMappingService licenseMappingService) {
    this.licenseMappingService = licenseMappingService;
  }

  private Set<Dependency> dependencyParser(File baseDir, InputFile requirementsTxtFile) {
    Set<Dependency> dependencies = new HashSet<>();

    try (InputStream inStream = requirementsTxtFile.inputStream()) {
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inStream));
      String lineBuffer = null;
      // For each line in the pip requirements file
      int lineCount = 0;
      while ((lineBuffer = bufferedReader.readLine()) != null) {
        String line = lineBuffer.trim();
        lineCount++;
        // Line is comment
        if (line.isEmpty() || line.startsWith("#"))
          continue;
        try {
          var packageData = PipUtils.getPackage(line);
          if (packageData.getValue().isBlank()) {
            // TODO Fetch the latest version for this python package
            // keep track of following issue https://github.com/pypi/warehouse/issues/4663
            // and this PEP https://peps.python.org/pep-0691/
            // We can't pull the latest from the pypi API, because there is no endpoint for
            // doing this
            continue;
          }
          var license = PipUtils.fetchLicenseOfPackage(packageData);
          if (license == null) {
            LOGGER.warn("Python Package " + packageData.getKey()
                + " has no license, based on pypi API, be careful while using this package.");
          } else {
            license = licenseMappingService.mapLicense(license);
          }
          var deps = new Dependency(
              packageData.getKey(),
              packageData.getValue(),
              license,
              LicenseCheckRulesDefinition.LANG_PYTHON);
          dependencies.add(deps);
        } catch (ArrayIndexOutOfBoundsException e) {
          // Might be invalid or invisible character
          LOGGER.error("PIP file " + requirementsTxtFile.filename() + " has a weird line (line " + lineCount + ")");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return dependencies;
  }

  @Override
  public Set<Dependency> scan(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicate requirementsTxtPredicate = fs.predicates().matchesPathPattern("**/requirements.txt");

    Set<Dependency> allDependencies = new HashSet<>();
    LOGGER.info("Starting PIP scan for {}",
        context.project().key());

    for (InputFile requirementsTxtFile : fs.inputFiles(requirementsTxtPredicate)) {
      context.markForPublishing(requirementsTxtFile);

      LOGGER.info("Scanning for PIP dependencies (dir={})", fs.baseDir());
      var dependencies = dependencyParser(fs.baseDir(), requirementsTxtFile);
      dependencies.forEach(dep -> {
        dep.setInputComponent(requirementsTxtFile);
        dep.setTextRange(requirementsTxtFile.newRange(1, 0, requirementsTxtFile.lines(), 0));
      });
      allDependencies.addAll(dependencies);
    }

    LOGGER.info("PIP scan finished for {}", context.project().key());

    return allDependencies;
  }

}