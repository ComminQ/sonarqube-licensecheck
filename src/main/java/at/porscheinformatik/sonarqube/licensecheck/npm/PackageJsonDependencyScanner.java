package at.porscheinformatik.sonarqube.licensecheck.npm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

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
import at.porscheinformatik.sonarqube.licensecheck.utils.JsonUtils;
import at.porscheinformatik.sonarqube.licensecheck.utils.NPMUtils;

public class PackageJsonDependencyScanner implements Scanner {
  private static final Logger LOGGER = Loggers.get(PackageJsonDependencyScanner.class);

  private final LicenseMappingService licenseMappingService;
  private final boolean resolveTransitiveDeps;

  public PackageJsonDependencyScanner(LicenseMappingService licenseMappingService, boolean resolveTransitiveDeps) {
    this.licenseMappingService = licenseMappingService;
    this.resolveTransitiveDeps = resolveTransitiveDeps;
  }

  @Override
  public Set<Dependency> scan(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicate packageJsonPredicate = fs.predicates().matchesPathPattern("**/package.json");

    Set<Dependency> allDependencies = new HashSet<>();

    LOGGER.info("Starting NPM scan for {}",
        context.project().key());
    for (InputFile packageJsonFile : fs.inputFiles(packageJsonPredicate)) {
      context.markForPublishing(packageJsonFile);

      LOGGER.info("Scanning for NPM dependencies (dir={})", fs.baseDir());
      allDependencies.addAll(dependencyParser(fs.baseDir(), packageJsonFile)
          .stream()
          .peek(dependency -> {
            dependency.setInputComponent(packageJsonFile);
            dependency.setTextRange(packageJsonFile.newRange(1, 0,
                packageJsonFile.lines(), 0));
          }).collect(Collectors.toList()));
    }

    LOGGER.info("NPM scan finished for {}",
        context.project().key());

    return allDependencies;
  }

  private Set<Dependency> dependencyParser(File baseDir, InputFile packageJsonFile) {
    Set<Dependency> dependencies = new HashSet<>();

    try (InputStream fis = packageJsonFile.inputStream();
        JsonReader jsonReader = Json.createReader(fis)) {
      JsonObject packageJson = jsonReader.readObject();

      JsonObject packageJsonDependencies = packageJson.getJsonObject("dependencies");
      if (packageJsonDependencies != null) {
        var localJsonDeps = packageJsonDependencies.keySet();
        for (String localDep : localJsonDeps) {
          String version = packageJsonDependencies.getString(localDep);
          String license = NPMUtils.fetchLicenseOfPackage(localDep);
          if (license == null) {
            LOGGER.warn("Npm Package " + localDep
                + " has no license, based on npm API, be careful while using this package.");
          } else {
            license = licenseMappingService.mapLicense(license);
          }
          var deps = new Dependency(
              localDep,
              version,
              license,
              LicenseCheckRulesDefinition.LANG_JS);

          dependencies.add(deps);
        }

        /*
         * "dependencies": {
         * "angular": "^1.4.3",
         * "angular-ui-router" : "~0.2.18",
         * "angular-ui-bootstrap" : "1.1.2",
         * "arangojs" : "5.6.0"
         * },
         * "devDependencies": {
         * "gulp": "^3.9.1"
         * },
         */

        // Set<Dependency> localDeps =
        // .forEach(dependency -> {
        // dependency.setInputComponent(packageJsonFile);
        // dependency.setTextRange(packageJsonFile.newRange(1, 0,
        // packageJsonFile.lines(), 0));
        // });
      }
    } catch (IOException e) {
      LOGGER.error("Error reading package.json", e);
    }

    return dependencies;
  }

}
