package at.porscheinformatik.sonarqube.licensecheck.utils;

import java.io.IOException;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.core.List;

/**
 * Utility class for handling PIP requirements description
 */
public class PipUtils {

  public static final String PYPI_API = "https://pypi.org/pypi/%package%/%version%/json";

  private PipUtils() {
  }

  /**
   * 
   * @param line a line of a pip requirements file
   * @return A tuple with package name first and package version
   */
  public static Pair<String, String> getPackage(String line) throws ArrayIndexOutOfBoundsException {
    // Replace all spaces with nothing
    String[] datas = line.replaceAll("\\s+", "")
        // Split by non alphanumeric char excluding '-'
        .split("[^a-zA-Z0-9\\-']+");

    String name = datas[0];
    String version = String
        .join(".", List.of(datas).subList(1, datas.length));
    return new ImmutablePair<>(name, version);
  }

  /**
   * 
   * @param packageData A tuple with package name first and package version, and
   *                    version must not be empty
   * @return
   * @throws IOException
   */
  public static String fetchLicenseOfPackage(Pair<String, String> packageData) throws IOException {

    String localPypiApi = PYPI_API
        .replaceAll("%package%", packageData.getKey())
        .replaceAll("%version%", packageData.getValue());

    URL url = new URL(localPypiApi);
    JsonReader jReader = Json.createReader(url.openStream());
    JsonObject obj = jReader.readObject();
    if (obj.containsKey("info")) {
      JsonObject packageInfo = obj.getJsonObject("info");
      if (packageInfo.containsKey("license")) {
        return packageInfo
          .getString("license", (String) null);
      }
    }
    return null;
  }

}
