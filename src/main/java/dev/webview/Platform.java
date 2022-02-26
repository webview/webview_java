package dev.webview;

import java.util.regex.Pattern;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Platform {
	// @formatter:off
    MACOSX   ("macOS",    "mac|darwin"),
    LINUX    ("Linux",    "nux"),
    WINDOWS  ("Windows",  "win");
    // @formatter:on

	private String str;
	private String regex;

	static Platform get() {
		String osName = System.getProperty("os.name").toLowerCase();

		for (Platform os : values()) {
			if (Pattern.compile(os.regex).matcher(osName).find()) {
				return os;
			}
		}

		throw new UnsupportedOperationException("Unknown operating system: " + osName);
	}

	@Override
	public String toString() {
		return this.str;
	}

}