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

import static org.digitalmediaserver.nsis.Utils.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.digitalmediaserver.nsis.io.ProcessOutputConsumer;
import org.digitalmediaserver.nsis.io.ProcessOutputHandler;

/**
 * Compiles a NSIS script and builds a Windows executable.
 *
 * @author Nadahar
 */
@Mojo(name = "make", defaultPhase = LifecyclePhase.PACKAGE)
public class MakeMojo extends AbstractMojo implements ProcessOutputConsumer {

	/** The static Windows line separator sequence */
	public static final String WINDOWS_LINE_SEPARATOR = "\r\n";

	/** The default compression {@link CompressionType} */
	public static final CompressionType DEFAULT_COMPRESSION = CompressionType.zlib;

	/** The default {@link CompressionType#lzma} dictionary size */
	public static final int DEFAULT_LZMA_DICT_SIZE = 8;

	/**
	 * Whether or not {@link #outputFile} should be attached to the Maven build.
	 * You probably want an installer to be attached, but if you build another
	 * executable that might not be the case.
	 */
	@Parameter(property = "nsis.attachArtifact", defaultValue = "true")
	private boolean attachArtifact;

	/**
	 * Whether or not to automatically set the {@code NSISDIR} environment
	 * variable based on the folder where the {@code makensis} executable is
	 * located. Useful when {@code makensis} is compiled with
	 * {@code NSIS_CONFIG_CONST_DATA_PATH=no}.
	 */
	@Parameter(property = "nsis.auto.nsisdir", defaultValue = "true", required = true)
	private boolean autoNsisDir;

	/** The classifier to append to {@link outputFile}'s name. */
	@Parameter(property = "nsis.classifier")
	private String classifier;

	/**
	 * The {@link CompressionType} to apply to {@link #scriptFile}.
	 */
	@Parameter(property = "nsis.compression")
	private CompressionType compression;

	/**
	 * The dictionary size to use if {@link #compression} is
	 * {@link CompressionType#lzma}. Defaults to
	 * {@value #DEFAULT_LZMA_DICT_SIZE}.
	 */
	@Parameter(property = "nsis.compression.lzma.dictsize")
	private int compressionDictSize = DEFAULT_LZMA_DICT_SIZE;

	/**
	 * Whether or not the compression defined in {@link #compression} is
	 * {@code FINAL}.
	 */
	@Parameter(property = "nsis.compression.final")
	private boolean compressionIsFinal;

	/**
	 * Whether or not the compression defined in {@link #compression} is
	 * {@code SOLID}.
	 */
	@Parameter(property = "nsis.compression.solid")
	private boolean compressionIsSolid;

	/**
	 * Indicates if the NSIS make operation should be disabled. If {@code true},
	 * no action will be taken when executing {@link MakeMojo}.
	 */
	@Parameter(property = "nsis.disabled", defaultValue = "false")
	private boolean disabled;

	/**
	 * A map of environment variables which will be passed to the execution of
	 * {@code makensis}.
	 */
	@Parameter
	private Map<String, String> environmentVariables = new HashMap<String, String>();

	/** The path of the generated header file. */
	@Parameter(property = "nsis.headerfile", defaultValue = "${project.build.directory}/project.nsh", required = true)
	private File headerFile;

	/**
	 * Whether or not {@link #headerFile} should be automatically injected as an
	 * {@code !include} in {@link #scriptFile}.
	 */
	@Parameter(property = "nsis.headerfile.inject", defaultValue = "true", required = true)
	private boolean injectHeaderFile;

	/**
	 * The folder to use as the working folder when running {@code makensis}.
	 * Relative paths will be resolved from this folder. By default this is the
	 * folder where {@link #scriptFile} is located.
	 */
	@Parameter(property = "nsis.makefolder")
	private String makeFolder;

	/**
	 * The path of the {@code makensis} executable to use. The default assumes
	 * that {@code makensis} can be found in the OS path.
	 */
	@Parameter(property = "nsis.makensis.executable", defaultValue = "makensis", required = true)
	private String makensisExecutable;

	/**
	 * The path of the {@code makensis} executable to use if the build platform
	 * is Linux. If {@code null} {@link #makensisExecutable} will be used also
	 * on Linux.
	 */
	@Parameter(property = "nsis.makensis.executable.linux")
	private String makensisExecutableLinux;

	/**
	 * The path of the {@code makensis} executable to use if the build platform
	 * is macOS. If {@code null} {@link #makensisExecutable} will be used also
	 * on macOS.
	 */
	@Parameter(property = "nsis.makensis.executable.macos")
	private String makensisExecutableMacOS;

	/**
	 * The value to use as {@code NSISDIR}. This will override
	 * {@link #autoNsisDir} and the environment variable named {@code NSISDIR}
	 * if set.
	 */
	@Parameter(property = "nsis.nsisdir")
	private String nsisDir;

	/** The path of the executable file to build. */
	@Parameter(property = "nsis.output.file", defaultValue = "${project.build.finalName}.exe", required = true)
	private String outputFile;

	/** The path of the NSIS script file to compile. */
	@Parameter(property = "nsis.scriptfile", defaultValue = "setup.nsi", required = true)
	private String scriptFile;

	/** The verbosity level to pass to {@code makensis}. */
	@Parameter(property = "nsis.verbosity", defaultValue = "2", required = true)
	private int verbosityLevel;

	/** The Maven project itself. */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	/** Internal project helper component. */
	@Component
	private MavenProjectHelper projectHelper;

	/** For internal use */
	private Path outputFilePath;

	@Override
	public void execute() throws MojoExecutionException {
		if (disabled) {
			getLog().info("NSIS make: plugin is disabled, not doing anything");
			return;
		}

		validate();

		ProcessBuilder builder = new ProcessBuilder(commandBuilder());
		// Set the working directory
		if (makeFolder == null) {
			builder.directory(project.getBasedir());
		} else {
			builder.directory(new File(makeFolder));
		}
		builder.redirectErrorStream(true);

		if (!isBlank(nsisDir)) {
			if (environmentVariables == null) {
				environmentVariables = new HashMap<>();
			}
			environmentVariables.put("NSISDIR", nsisDir);
		}

		if (environmentVariables != null) {
			builder.environment().putAll(environmentVariables);
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug("directory:  " + builder.directory().getAbsolutePath());
			getLog().debug("commands  " + builder.command().toString());
			if (builder.environment() != null) {
				getLog().debug("environment variables: ");
				for (Map.Entry<String, String> entry : builder.environment().entrySet()) {
					getLog().debug("  " + entry.getKey() + ": " + entry.getValue());
				}
			}
		}

		Charset charset = IS_WINDOWS ? Charset.defaultCharset() : StandardCharsets.UTF_8;
		try {
			long start = System.currentTimeMillis();
			Process process = builder.start();
			ProcessOutputHandler output = new ProcessOutputHandler(process.getInputStream(), this, charset);
			output.startThread();

			int status;
			try {
				status = process.waitFor();
			} catch (InterruptedException e) {
				getLog().error("Makensis execution was interrupted before it could finish");
				process.destroy();
				status = -1;
			}

			if (status != 0) {
				throw new MojoExecutionException("Execution of makensis compiler failed. See output above for details.");
			}

			long end = System.currentTimeMillis();

			consumeOutputLine("Execution completed in " + (end - start) + "ms");

			if (attachArtifact) {
				// Attach the exe to the install tasks.
				if (outputFilePath == null) {
					getLog().warn("Could not attach output file because outputFilePath isn't set");
				} else {
					projectHelper.attachArtifact(project, "exe", classifier, outputFilePath.toFile());
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to execute makensis", e);
		}
	}

	@Override
	public void consumeOutputLine(String line) {
		getLog().info("[MAKENSIS] " + line);
	}

	private void validate() throws MojoExecutionException {
		// Use the correct makensis binary
		if (OSTYPE == OSType.LINUX && !isBlank(makensisExecutableLinux)) {
			makensisExecutable = makensisExecutableLinux;
		} else if (OSTYPE == OSType.MACOS && !isBlank(makensisExecutableMacOS)) {
			makensisExecutable = makensisExecutableMacOS;
		}

		// Convert path separators
		try {
			Path path = Paths.get(makensisExecutable).toRealPath();
			makensisExecutable = path.toString();
			if (autoNsisDir && isBlank(nsisDir)) {
				path = path.getParent();
				if (path != null) {
					path = path.toRealPath();
					Path fileName = path.getFileName();
					if (fileName != null && "bin".equals(fileName.toString().toLowerCase(Locale.ROOT))) {
						path = path.getParent();
					}

					// `/usr/local/bin` installations or macOS Homebrew keg-only
					Path shareNsis = path.resolve("share/nsis");
					if (Files.exists(shareNsis)) {
						path = shareNsis;
					}
				}
				nsisDir = path == null ? null : path.toString();
			} else {
				path = Paths.get(nsisDir);
				nsisDir = path.toRealPath().toString();
			}
			if (isBlank(makeFolder)) {
				makeFolder = null;
			} else {
				path = Paths.get(makeFolder);
				makeFolder = path.toRealPath().toString();
			}
			if (isBlank(outputFile)) {
				outputFile = null;
			} else {
				path = Paths.get(outputFile);
				outputFile = path.toString();
			}
			path = Paths.get(scriptFile);
			scriptFile = path.toRealPath().toString();
		} catch (IOException e) {
			throw new MojoExecutionException("An error occurred while resolving paths: " + e.getMessage(), e);
		}

		// Check if the script file contains properties that conflict with the plugin configuration
		boolean finalCompression = compression != null && compressionIsFinal;
		boolean outputFileSet = outputFile != null;
		if (finalCompression || outputFileSet) {
			Pattern compressionPattern = finalCompression ? Pattern.compile("^\\s*SetCompressor\\s") : null;
			Pattern outputPattern = outputFileSet ? Pattern.compile("^\\s*OutFile\\s") : null;
			try (
				BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(scriptFile), StandardCharsets.UTF_8)
				)
			) {
				int lineNo = 1;
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					if (compressionPattern != null && compressionPattern.matcher(line).find()) {
						throw new MojoExecutionException(
							"Final compression is set in the plugin configuration but is also " +
							"defined in script file \"" + scriptFile + "\" on line " + lineNo + "."
						);
					}
					if (outputPattern != null && outputPattern.matcher(line).find()) {
						throw new MojoExecutionException(
							"The output file is set in the plugin configuration but is also " +
							"defined in script file \"" + scriptFile + "\" on line " + lineNo + "."
						);
					}
					lineNo++;
				}
			} catch (IOException e) {
				throw new MojoExecutionException(
					"An error occurred while validating script file \"" + scriptFile + "\": " + e.getMessage(),
					e
				);
			}
		}
	}

	/**
	 * Builds the command to use when launching {@code makensis}.
	 *
	 * @return The command {@link List}.
	 * @throws MojoExecutionException If an error occurs during the operation.
	 */
	@Nonnull
	protected List<String> commandBuilder() throws MojoExecutionException {
		List<String> result = new ArrayList<String>();

		// Makensis binary
		result.add(makensisExecutable);

		outputFilePath = getOutputFile();
		String optionPrefix = IS_WINDOWS ? "/" : "-";

		// Include header file
		if (injectHeaderFile && headerFile.exists()) {
			result.add(optionPrefix + "X!include " + formatStringArgument(headerFile.getAbsolutePath(), false));
		}

		// Installer output file
		if (outputFilePath != null) {
			result.add(optionPrefix + "XOutFile " + formatStringArgument(outputFilePath.toAbsolutePath(), false));
		}

		// Working folder
		if (makeFolder != null) {
			result.add(optionPrefix + "NOCD");
		}

		// Verbosity level
		if (verbosityLevel < 0) {
			verbosityLevel = 0;
		} else if (verbosityLevel > 4) {
			verbosityLevel = 4;
		}
		result.add(optionPrefix + "V" + verbosityLevel);

		// Compression
		if (
			compression != null && (
				compression != DEFAULT_COMPRESSION || compressionIsFinal || compressionIsSolid
		)) {
			StringBuilder setCompressor = new StringBuilder(optionPrefix + "XSetCompressor");
			if (compressionIsFinal) {
				setCompressor.append(" /FINAL");
			}
			if (compressionIsSolid) {
				setCompressor.append(" /SOLID");
			}
			setCompressor.append(' ').append(compression.name());
			result.add(setCompressor.toString());

			if (compression == CompressionType.lzma && compressionDictSize != DEFAULT_LZMA_DICT_SIZE) {
				result.add(optionPrefix + "XSetCompressorDictSize " + compressionDictSize);
			}
		}

		// Script file
		result.add(scriptFile);
		getLog().debug("Processing Script file: \"" + scriptFile + "\"");

		return result;
	}

	/**
	 * Resolves the {@code OutFile} {@link Path}. If the destination folder
	 * doesn't exist, it will be created.
	 *
	 * @return The resolved {@code OutFile} {@link Path}.
	 * @throws MojoExecutionException If an error occurs during the operation.
	 */
	@Nullable
	protected Path getOutputFile() throws MojoExecutionException {
		if (isBlank(outputFile)) {
			return null;
		}
		Path path = Paths.get(outputFile);
		Path baseFolder = path.isAbsolute() ? null : Paths.get(project.getBuild().getDirectory());
		Path outputFolder = path.getParent();
		if (outputFolder == null) {
			outputFolder = baseFolder;
		} else if (baseFolder != null) {
			outputFolder = baseFolder.resolve(outputFolder);
		}

		// Make sure the output folder exists
		if (outputFolder != null && !Files.exists(outputFolder)) {
			try {
				Files.createDirectories(outputFolder);
			} catch (IOException e) {
				throw new MojoExecutionException(
					"Couldn't create target folder \"" + outputFolder.toAbsolutePath() + "\": " + e.getMessage(),
					e
				);
			}
		}

		if (classifier == null || classifier.isEmpty()) {
			return baseFolder == null ? path : baseFolder.resolve(path);
		}
		String classifierWithHyphenPrefix = classifier;
		if (classifier.charAt(0) != '-') {
			classifierWithHyphenPrefix = "-" + classifier;
		}
		int dot = outputFile.lastIndexOf('.');
		outputFile = dot >= 0 ?
			outputFile.substring(0, dot) + classifierWithHyphenPrefix + outputFile.substring(dot) :
			outputFile + classifierWithHyphenPrefix;
		return baseFolder == null ? Paths.get(outputFile) : baseFolder.resolve(outputFile);
	}
}
