package at.porscheinformatik.sonarqube.licensecheck.pip;

public class RequirementsTxtDependencyScanner implements Scanner{

  private static final Logger LOGGER = Loggers.get(RequirementsTxtDependencyScanner.class);

  private final LicenseMappingService licenseMappingService;
  private final boolean resolveTransitiveDeps;

  public RequirementsTxtDependencyScanner(LicenseMappingService licenseMappingService, boolean resolveTransitiveDeps)
  {
      this.licenseMappingService = licenseMappingService;
      this.resolveTransitiveDeps = resolveTransitiveDeps;
  }

}