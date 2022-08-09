package at.porscheinformatik.sonarqube.licensecheck;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.scanner.fs.InputProject;

import at.porscheinformatik.sonarqube.licensecheck.licensemapping.LicenseMappingService;
import at.porscheinformatik.sonarqube.licensecheck.pip.RequirementsTxtDependencyScanner;

public class RequirementsTxtDependencyScannerTest {
  private static final File RESOURCE_FOLDER = new File("src/test/resources");

  private SensorContext createContext(File folder) {
    return createContext(folder, "requirements.txt");
  }

  private SensorContext createContext(File folder, String fileName) {
    SensorContext context = mock(SensorContext.class);
    InputFile pipRequirements = mock(InputFile.class);
    when(pipRequirements.language()).thenReturn("txt");
    when(pipRequirements.filename()).thenReturn("requirements.txt");
    when(pipRequirements.relativePath()).thenReturn("/requirements.txt");
    when(pipRequirements.type()).thenReturn(InputFile.Type.MAIN);
    try {
      when(pipRequirements.inputStream()).thenAnswer(i -> new FileInputStream(new File(folder, fileName)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    FileSystem fileSystem = new DefaultFileSystem(folder.toPath()).add(pipRequirements);
    when(context.fileSystem()).thenReturn(fileSystem);
    InputProject project = mock(InputProject.class);
    when(project.key()).thenReturn("test-project");
    when(context.project()).thenReturn(project);
    return context;
  }

  private Scanner createScanner() {
    LicenseMappingService licenseMappingService = mock(LicenseMappingService.class);
    when(licenseMappingService.mapLicense(anyString())).thenCallRealMethod();
    return new RequirementsTxtDependencyScanner(licenseMappingService);
  }

  @Test
  public void testNoRequirementsTxt() {
    Set<Dependency> dependencies = createScanner().scan(createContext(new File("src")));

    assertThat(dependencies, hasSize(0));
  }

  @Test
  public void testRequirementsTxt(){
    Set<Dependency> dependencies = createScanner()
      .scan(createContext(RESOURCE_FOLDER, "requirements.txt"));

    assertThat(dependencies, hasSize(6));
    
    assertThat(dependencies, containsInAnyOrder(
      new Dependency("docopt", "0.6.1","MIT"),
      new Dependency("psycopg2", "2.9.3","LGPL with exceptions"),
      new Dependency("Flask", "2.2.1","BSD-3-Clause"),
      new Dependency("flask_cors", "3.0.10", "MIT"),
      new Dependency("zipp", "3.8.0", "MIT"),
      new Dependency("speaklater", "1.3", "BSD")));
  }

  @Test
  public void testOnlyComments() {
    Set<Dependency> dependencies = createScanner()
        .scan(createContext(RESOURCE_FOLDER, "requirements_comments.txt"));

    assertThat(dependencies, hasSize(0));
  }

}
