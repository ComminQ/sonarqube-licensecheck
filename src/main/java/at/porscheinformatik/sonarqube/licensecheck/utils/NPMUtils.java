package at.porscheinformatik.sonarqube.licensecheck.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class NPMUtils {

	public static final String NPM_API = "https://api.npms.io/v2/package/%package%";

	/**
	 * 
	 * @param packageData A tuple with package name first and package version, and
	 *                    version must not be empty
	 * @return
	 * @throws IOException
	 */
	public static String fetchLicenseOfPackage(String packageName) throws IOException {

		var encodedPackageName = URLEncoder.encode(packageName, StandardCharsets.UTF_8);
		String localNPMApi = NPM_API
				.replaceAll("%package%", encodedPackageName);

		URL url = new URL(localNPMApi);
		JsonReader jReader = Json.createReader(url.openStream());
		JsonObject obj = jReader.readObject();
		if(obj.containsKey("collected")){
			JsonObject collected = obj.getJsonObject("collected");
			if(collected.containsKey("metadata")){
				JsonObject metadata = collected.getJsonObject("metadata");
				String license = metadata.getString("license", null);
				return license;
			}
		}
		return null;
	}

}
