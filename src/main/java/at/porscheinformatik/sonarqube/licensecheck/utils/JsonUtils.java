package at.porscheinformatik.sonarqube.licensecheck.utils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.JsonArray;

public class JsonUtils {

	private JsonUtils() {
	}

	/**
	 * Convert {@link JsonArray} to {@link List} of string
	 * @param jsonArray
	 * @return List of strings
	 */
	public static List<String> jsonArrayToStringList(JsonArray jsonArray) {
		return IntStream.range(0, jsonArray.size())
				.mapToObj(i -> jsonArray.getString(i))
				.collect(Collectors.toList());
	}

}
