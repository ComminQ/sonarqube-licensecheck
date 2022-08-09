package at.porscheinformatik.sonarqube.licensecheck.utils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.codehaus.plexus.util.StringUtils;

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
        // Split by non alphanumeric char excluding '-' and '_'
        .split("[^a-zA-Z0-9\\-\\_']+");

    String name = datas[0];
    String version = String
        .join(".", List.of(datas).subList(1, datas.length));
    return new Pair<>(name, version);
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
      if (packageInfo.containsKey("license") &&
          !StringUtils.isBlank(packageInfo.getString("license")) &&
          !packageInfo.getString("license").equals("UNKNOWN")) {
        return packageInfo
            .getString("license", (String) null);
      } else if (packageInfo.containsKey("classifiers")) {
        // Search in classifiers, License
        JsonArray classifiers = packageInfo.getJsonArray("classifiers");
        var classifiersAsStrings = IntStream.range(0,classifiers.size())
          .mapToObj(i -> classifiers.getString(i))
          .collect(Collectors.toList());
        return classifiersAsStrings
            .stream()
            .filter(s -> s.contains("License :: OSI Approved :: "))
            .map(s -> s.replace("License :: OSI Approved :: ", "").replace(" License", ""))
            .findFirst()
            .orElse(null);
      }
    }
    return null;
  }

}
