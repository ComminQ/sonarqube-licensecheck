package at.porscheinformatik.sonarqube.licensecheck.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import at.porscheinformatik.sonarqube.licensecheck.pip.PipUtils;

public class PipUtilsTest {
 
  @Test
  public void testPackageSplitingCompatible() {

    String pipRequirementLine = "psycopg2 ~= 2.9.4";

    var packageData = PipUtils.getPackage(pipRequirementLine);
    String name = packageData.getKey();
    String version = packageData.getValue();

    assertEquals("psycopg2", name);
    assertEquals("2.9.4", version);
  }

  @Test
  public void testPackageSplitingEquals() {

    String pipRequirementLine = "psycopg2 == 2.9.4";

    var packageData = PipUtils.getPackage(pipRequirementLine);
    String name = packageData.getKey();
    String version = packageData.getValue();

    assertEquals("psycopg2", name);
    assertEquals("2.9.4", version);
  }

  @Test
  public void testPackageSplitingMinimum() {

    String pipRequirementLine = "psycopg2 >= 2.9.4";

    var packageData = PipUtils.getPackage(pipRequirementLine);
    String name = packageData.getKey();
    String version = packageData.getValue();

    assertEquals("psycopg2", name);
    assertEquals("2.9.4", version);
  }

  @Test
  public void testPackageSplitingExclusion() {

    String pipRequirementLine = "psycopg2 != 2.9.4";

    var packageData = PipUtils.getPackage(pipRequirementLine);
    String name = packageData.getKey();
    String version = packageData.getValue();

    assertEquals("psycopg2", name);
    assertEquals("2.9.4", version);
  }

  @Test
  public void testPackageSplitingNoVersion() {

    String pipRequirementLine = "psycopg2";

    var packageData = PipUtils.getPackage(pipRequirementLine);
    String name = packageData.getKey();
    String version = packageData.getValue();

    assertEquals("psycopg2", name);
    assertEquals("", version);
  }

}
