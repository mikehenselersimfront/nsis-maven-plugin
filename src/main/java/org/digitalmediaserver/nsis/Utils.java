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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.plugin.logging.Log;

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
	 * Tries to find the specified relative file using the system {@code PATH}
	 * environment variable. Returns the first match in the order of the system
	 * {@code PATH} or {@code null} if no match was found.
	 *
	 * @param relativePath the relative {@link Path} describing the file to
	 *            return.
	 * @param log the {@link Log} to log to, or {@code null} for no logging.
	 * @param options the {@link LinkOption}s to use when resolving.
	 * @return The matched {@link Path} or {@code null} if no match was found.
	 * @throws IllegalArgumentException if {@code relativePath} is absolute.
	 */
	@Nullable
	public static Path findInOSPath(
		@Nullable Path relativePath,
		@Nullable Log log,
		LinkOption... options
	) {
		if (relativePath == null) {
			return null;
		}
		if (relativePath.isAbsolute()) {
			throw new IllegalArgumentException("relativePath must be relative");
		}

		List<Path> osPath = new ArrayList<>();
		osPath.add(null);
		osPath.addAll(getOSPath(log));
		Path result = null;
		List<String> extensions = new ArrayList<>();
		extensions.add(null);
		if (IS_WINDOWS && getExtension(relativePath) == null) {
			for (String s : getWindowsPathExtensions()) {
				if (!isBlank(s)) {
					extensions.add("." + s);
				}
			}
		}
		for (String extension : extensions) {
			for (Path path : osPath) {
				if (path == null) {
					path = Paths.get("").toAbsolutePath();
				}
				if (extension == null) {
					result = path.resolve(relativePath);
				} else {
					result = path.resolve(relativePath.toString() + extension);
				}
				if (Files.exists(result, options) && Files.isRegularFile(result, options)) {
					if (log != null && log.isDebugEnabled()) {
						log.debug("Resolved \"" + result + "\" from \"" + relativePath + "\" using OS path");
					}
					try {
						return result.toRealPath(options);
					} catch (IOException e) {
						if (log != null) {
							log.warn("Could not get the real path of \"" + result + "\": " + e.getMessage(), e);
						}
						return result;
					}
				}
			}
		}
		if (log != null && log.isDebugEnabled()) {
			log.debug("Failed to resolve \"" + relativePath + "\" using OS path");
		}
		return null;
	}

	/**
	 * Returns the OS {@code PATH} environment variable as a {@link List} of
	 * {@link Path}s.
	 *
	 * @param log the {@link Log} to log to, or {@code null} for no logging.
	 * @return The {@link List} of {@link Path}s.
	 */
	@Nonnull
	public static List<Path> getOSPath(@Nullable Log log) {
		List<Path> result = new ArrayList<>();
		String osPath = System.getenv("PATH");
		if (isBlank(osPath)) {
			return result;
		}
		String[] paths = osPath.split(File.pathSeparator);
		for (String path : paths) {
			if (isBlank(path)) {
				continue;
			}
			try {
				result.add(Paths.get(path));
			} catch (InvalidPathException e) {
				if (log != null) {
					if (log.isDebugEnabled()) {
						log.warn(
							"Unable to resolve PATH element \"" + path +
							"\" to a folder, it will be ignored: " + e.getMessage(),
							e
						);
					} else {
						log.warn(
							"Unable to resolve PATH element \"" + path +
							"\" to a folder, it will be ignored"
						);
					}
				}
			}
		}
		return result;
	}

	/**
	 * @return The Windows {@code PATHEXT} environment variable as a
	 *         {@link List} of {@link String}s containing the extensions without
	 *         the {@code .}.
	 */
	@Nonnull
	public static List<String> getWindowsPathExtensions() {
		List<String> result = new ArrayList<>();
		String osPathExtensions = System.getenv("PATHEXT");
		if (isBlank(osPathExtensions)) {
			return result;
		}
		String[] extensions = osPathExtensions.split(File.pathSeparator);
		for (String extension : extensions) {
			result.add(extension.replace(".", ""));
		}
		return result;
	}

	/**
	 * Returns the file extension from {@code file} or {@code null} if
	 * {@code file} has no extension.
	 *
	 * @param file the {@link Path} from which to extract the extension.
	 * @return The extracted extension or {@code null}.
	 */
	@Nullable
	public static String getExtension(@Nullable Path file) {
		if (file == null) {
			return null;
		}
		Path fileName = file.getFileName();
		String name;
		if (fileName == null || isBlank((name = fileName.toString()))) {
			return null;
		}

		int point = name.lastIndexOf('.');
		if (point == -1) {
			return null;
		}

		String extension = name.substring(point + 1);
		if (extension.contains("/") || IS_WINDOWS && extension.contains("\\")) {
			// It's not an extension, it's a dot in a folder name
			return null;
		}
		return extension;
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
