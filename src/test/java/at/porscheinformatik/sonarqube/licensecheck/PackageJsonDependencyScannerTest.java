package at.porscheinformatik.sonarqube.licensecheck;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
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
import at.porscheinformatik.sonarqube.licensecheck.npm.PackageJsonDependencyScanner;

public class PackageJsonDependencyScannerTest {
    private static final File RESOURCE_FOLDER = new File("src/test/resources");

    private SensorContext createContext(File folder) {
        SensorContext context = mock(SensorContext.class);
        InputFile packageJson = mock(InputFile.class);
        when(packageJson.language()).thenReturn("json");
        when(packageJson.filename()).thenReturn("package.json");
        when(packageJson.relativePath()).thenReturn("/package.json");
        when(packageJson.type()).thenReturn(InputFile.Type.MAIN);
        try {
            when(packageJson.inputStream()).thenAnswer(i -> new FileInputStream(new File(folder, "package.json")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileSystem fileSystem = new DefaultFileSystem(folder.toPath()).add(packageJson);
        when(context.fileSystem()).thenReturn(fileSystem);
        InputProject project = mock(InputProject.class);
        when(project.key()).thenReturn("test-project");
        when(context.project()).thenReturn(project);
        return context;
    }

    @Test
    public void testGlobal() {
        Set<Dependency> dependencies = createScanner(true).scan(createContext(RESOURCE_FOLDER));

        assertThat(dependencies, hasSize(4));
        assertThat(dependencies, containsInAnyOrder(
                new Dependency("angular", "^1.4.3", "MIT"),
                new Dependency("angular-ui-router", "~0.2.18", "MIT"),
                new Dependency("angular-ui-bootstrap", "1.1.2", "MIT"),
                new Dependency("arangojs", "5.6.0", "Apache-2.0")));
    }
    @Test
    public void testNoPackageJson() {
        Set<Dependency> dependencies = createScanner().scan(createContext(new File("src")));

        assertThat(dependencies, hasSize(0));
    }

    private Scanner createScanner() {
        return createScanner(false);
    }

    private Scanner createScanner(boolean resolveTransitiveDeps) {
        LicenseMappingService licenseMappingService = mock(LicenseMappingService.class);
        when(licenseMappingService.mapLicense(anyString())).thenCallRealMethod();
        return new PackageJsonDependencyScanner(licenseMappingService, resolveTransitiveDeps);
    }
}
