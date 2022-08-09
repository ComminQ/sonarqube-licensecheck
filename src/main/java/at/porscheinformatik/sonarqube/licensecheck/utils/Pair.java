package at.porscheinformatik.sonarqube.licensecheck.utils;

import java.util.Map;

public class Pair<F, S> implements Map.Entry<F, S> {

	private F first;
	private S second;

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public F getKey() {
		return this.first;
	}

	@Override
	public S getValue() {
		return this.second;
	}

	@Override
	public S setValue(S value) {
		this.second = value;
		return this.second;
	}

}
