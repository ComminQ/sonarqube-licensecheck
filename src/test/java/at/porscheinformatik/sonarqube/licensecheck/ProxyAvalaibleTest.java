package at.porscheinformatik.sonarqube.licensecheck;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProxyAvalaibleTest {

  @Test
  public void testProxyParams() {
    assertEquals(
        "srv-pxy00.tisseo-exp.dom",
        System.getProperty("http.proxyHost"));
  }

}
