/*
 * Copyright 2018 Digital Media Server
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.digitalmediaserver.nsis;

import java.nio.file.Path;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A static utility class with a few utility methods.
 *
 * @author Nadahar
 */
public final class Utils {

	/** The static detected {@link OSType} */
	public static final OSType OSTYPE = OSType.resolve();

	/** A static boolean for whether or not the current OS is Windows */
	public static final boolean IS_WINDOWS = OSTYPE == OSType.WINDOWS;

	/** A static {@link Pattern} to determine if an argument needs quotes */
	public static final Pattern QUOTES_NEEDED = Pattern.compile("\\s|\"|'|`");

	/**
	 * Not to be instantiated.
	 */
	private Utils() {
	}

	/**
	 * Evaluates if the specified character sequence is {@code null}, empty or
	 * only consists of whitespace.
	 *
	 * @param cs the {@link CharSequence} to evaluate.
	 * @return true if {@code cs} is {@code null}, empty or only consists of
	 *         whitespace, {@code false} otherwise.
	 */
	public static boolean isBlank(@Nullable CharSequence cs) {
		if (cs == null) {
			return true;
		}
		int strLen = cs.length();
		if (strLen == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(cs.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Formats the specified {@link Path} so that it's suitable to use as a
	 * command line {@code /X} parameter value. A {@code null} argument will
	 * return a quoted empty string.
	 *
	 * @param path the {@link Path} to format.
	 * @param alwaysQuote if {@code true} the returned {@link String} will
	 *            always be quoted, if {@code false} it will only be quoted if
	 *            required by the content.
	 * @return The formatted {@link String}.
	 */
	@Nonnull
	public static String formatStringArgument(@Nullable Path path, boolean alwaysQuote) {
		return formatStringArgument(path == null ? null : path.toString(), alwaysQuote);
	}

	/**
	 * Formats the specified {@link String} so that it's suitable to use as a
	 * command line {@code /X} parameter value. A {@code null} argument will
	 * return a quoted empty string.
	 *
	 * @param source the {@link String} to format.
	 * @param alwaysQuote if {@code true} the returned {@link String} will
	 *            always be quoted, if {@code false} it will only be quoted if
	 *            required by the content.
	 * @return The formatted {@link String}.
	 */
	@Nonnull
	public static String formatStringArgument(@Nullable String source, boolean alwaysQuote) {
		String quote = IS_WINDOWS ? "\\\"" : "\"";
		if (source == null || source.isEmpty()) {
			return quote + quote;
		}

		if (!alwaysQuote && !QUOTES_NEEDED.matcher(source).find()) {
			return source;
		}

		source = IS_WINDOWS ? source.replace("\\", "\\\\").replace("\"", "$\\\\\\\"") : source.replace("\"", "$\\\"");
		return quote + source + quote;
	}

	/**
	 * An enum representing the supported platforms.
	 *
	 * @author Nadahar
	 */
	public enum OSType {

		/** The Linux platform */
		LINUX,

		/** The macOS platform */
		MACOS,

		/** The Windows platform */
		WINDOWS,

		/** Unsupported platforms */
		OTHER;

		private static OSType resolve() {
			String osName = System.getProperty("os.name");
			if (osName.startsWith("Linux")) {
				return LINUX;
			}
			if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
				return MACOS;
			}
			if (osName.startsWith("Windows")) {
				return WINDOWS;
			}
			return OTHER;
		}
	}

	/**
	 * An enum representing the supported compression types.
	 *
	 * @author Nadahar
	 */
	public enum CompressionType {

		/**
		 * The {@code DEFLATE} compression algorithm used in ZIP, gzip and
		 * others
		 */
		zlib,

		/** The bzip2 compression using the Burrows–Wheeler algorithm */
		bzip2,

		/** The Lempel–Ziv–Markov chain compression algorithm used by 7-zip */
		lzma
	}
}
