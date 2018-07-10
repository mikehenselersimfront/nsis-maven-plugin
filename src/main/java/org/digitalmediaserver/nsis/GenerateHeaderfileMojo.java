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
import java.nio.file.Path;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.digitalmediaserver.nsis.io.FormattedWriter;

/**
 * Generates a header file containing NSIS variables that represent common POM
 * values. The resulting header file is automatically included in the NSIS
 * script file.
 * <p>
 * Custom variables defined in {@link #defines} will also be included in the
 * generated header file.
 * <p>
 * The standard variables are:
 * <ul>
 * <li>PROJECT_BASEDIR</li>
 * <li>PROJECT_BUILD_DIR</li>
 * <li>PROJECT_FINAL_NAME</li>
 * <li>PROJECT_CLASSIFIER</li>
 * <li>PROJECT_GROUP_ID</li>
 * <li>PROJECT_ARTIFACT_ID</li>
 * <li>PROJECT_NAME</li>
 * <li>PROJECT_VERSION</li>
 * <li>PROJECT_PACKAGING</li>
 * <li>PROJECT_URL</li>
 * <li>PROJECT_LICENSE</li>
 * <li>PROJECT_LICENSE_URL</li>
 * <li>PROJECT_ORGANIZATION_NAME</li>
 * <li>PROJECT_ORGANIZATION_URL</li>
 * <li>PROJECT_REG_KEY</li>
 * <li>PROJECT_REG_UNINSTALL_KEY</li>
 * <li>PROJECT_STARTMENU_FOLDER</li>
 * </ul>
 * <p>
 * Variables where no value can be resolved will be omitted. If there are
 * multiple licenses defined, the license defines will be
 * {@code PROJECT_LICENSE<n>} and {@code PROJECT_LICENSE<n>_URL} instead, where
 * {@code <n>} is the license number starting with 1.
 *
 * @author Nadahar
 */
@Mojo(name = "generate-headerfile", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class GenerateHeaderfileMojo extends AbstractMojo {

	/** The classifier to append to {@link MakeMojo#outputFile}'s name. */
	@Parameter(property = "nsis.classifier")
	private String classifier;

	/**
	 * A map of additional defines which will be passed to the execution of
	 * {@code makensis}.
	 */
	@Parameter
	private Map<String, String> defines = new HashMap<String, String>();

	/**
	 * Indicates if the NSIS make operation should be disabled. If {@code true},
	 * no action will be taken when executing {@link MakeMojo}.
	 */
	@Parameter(property = "nsis.disabled", defaultValue = "false")
	private boolean disabled;

	/** The path of the header file to generate. */
	@Parameter(property = "nsis.headerfile", defaultValue = "${project.build.directory}/project.nsh", required = true)
	private File headerFile;

	/** The Maven project itself. */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (disabled) {
			getLog().info("NSIS generate-headerfile: plugin is disabled, not doing anything");
			return;
		}

		Path headerPath = headerFile.toPath();
		Path parentPath = headerPath.getParent();
		if (parentPath != null && !Files.exists(parentPath)) {
			try {
				Files.createDirectories(parentPath);
			} catch (IOException e) {
				throw new MojoFailureException(
					"Couldn't create parent folder \"" + parentPath + "\" for header file \"" +
					headerPath.getFileName() + "\": " + e.getMessage(),
					e
				);
			}
		}

		try (FormattedWriter writer = new FormattedWriter(headerPath, MakeMojo.WINDOWS_LINE_SEPARATOR)) {
			writer.writeln("; Header file with project details for %s", project.getName());
			writer.writeln("; Generated from pom.xml version %1$s on %2$tF %2$tT", project.getVersion(), Calendar.getInstance());
			writer.newLine();

			writer.writeln("!define PROJECT_BASEDIR \"%s\"", project.getBasedir());
			writer.writeln("!define PROJECT_BUILD_DIR \"%s\"", project.getBuild().getDirectory());
			writer.writeln("!define PROJECT_FINAL_NAME \"%s\"", project.getBuild().getFinalName());

			if (!Utils.isBlank(classifier)) {
				writer.writeln("!define PROJECT_CLASSIFIER \"%s\"", classifier);
			}

			writer.writeln("!define PROJECT_GROUP_ID \"%s\"", project.getGroupId());
			writer.writeln("!define PROJECT_ARTIFACT_ID \"%s\"", project.getArtifactId());
			writer.writeln("!define PROJECT_NAME \"%s\"", project.getName());
			writer.writeln("!define PROJECT_VERSION \"%s\"", project.getVersion());
			writer.writeln("!define PROJECT_PACKAGING \"%s\"", project.getPackaging());

			if (!Utils.isBlank(project.getUrl())) {
				writer.writeln("!define PROJECT_URL \"%s\"", project.getUrl());
			}

			List<License> licenses = project.getLicenses();
			if (licenses != null && !licenses.isEmpty()) {
				if (licenses.size() == 1) {
					writer.writeln("!define PROJECT_LICENSE \"%s\"", licenses.get(0).getName());
					if (licenses.get(0).getUrl() != null) {
						writer.writeln("!define PROJECT_LICENSE_URL \"%s\"", licenses.get(0).getUrl());
					}
				} else {
					for (int i = 0; i < licenses.size(); i++) {
						writer.writeln("!define PROJECT_LICENSE%d \"%s\"", i + 1, licenses.get(i).getName());
						if (licenses.get(i).getUrl() != null) {
							writer.writeln("!define PROJECT_LICENSE%d_URL \"%s\"", i + 1, licenses.get(i).getUrl());
						}
					}
				}
			}

			if (project.getOrganization() == null) {
				writer.writeln("; The project organization section is missing from your pom.xml");
			} else {
				writer.writeln("!define PROJECT_ORGANIZATION_NAME \"%s\"", project.getOrganization().getName());
				writer.writeln("!define PROJECT_ORGANIZATION_URL \"%s\"", project.getOrganization().getUrl());
				writer.writeln(
					"!define PROJECT_REG_KEY \"SOFTWARE\\%1$s\\%2$s\\%3$s\"",
					project.getOrganization().getName(),
					project.getName(),
					project.getVersion()
				);
				writer.writeln(
					"!define PROJECT_REG_UNINSTALL_KEY \"Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\%1$s %2$s\"",
					project.getName(),
					project.getVersion()
				);
				writer.writeln(
					"!define PROJECT_STARTMENU_FOLDER \"%1$s\\%2$s\\%3$s %4$s\"",
					"$SMPROGRAMS",
					project.getOrganization().getName(),
					project.getName(), project.getVersion()
				);
			}

			if (defines != null) {
				for (Entry<String, String> entry : defines.entrySet()) {
					writer.writeln("!define %1$s \"%2$s\"", entry.getKey().toUpperCase(Locale.ROOT), entry.getValue());
				}
			}
		} catch (IOException e) {
			throw new MojoFailureException(
				"An error occurred while writing header file \"" + headerPath + "\": " + e.getMessage(),
				e
			);
		}
	}
}
