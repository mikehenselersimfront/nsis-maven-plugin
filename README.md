# NSIS Maven Plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.digitalmediaserver/nsis-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.digitalmediaserver/nsis-maven-plugin)

This Maven plugin allows you to build NSIS executables during your Maven build. It is based on the `nsis-maven-plugin` that used to be hosted at [the CodeHaus SVN](http://svn.codehaus.org/mojo/trunk/mojo/nsis-maven-plugin/). Significant changes have been made, so this is not a drop-in replacement. This plugin should be able to do everything the original plugin can and more.

## Table of Contents
- [1. Configuration](#1-configuration)
  - [1.1 Goal `generate-headerfile`](#11-goal-generate-headerfile)
    - [1.1.1 Parameter description](#111-parameter-description)
  - [1.2 Goal `make`](#12-goal-make)
    - [1.2.1 Parameter description](#121-parameter-description)
    - [1.2.2 `compression` options](#122-compression-options)
  - [1.3 Skeleton project configuration](#13-skeleton-project-configuration)
- [2. Using the plugin](#2-using-the-plugin)
  - [2.1 Example configuration](#21-example-configuration)

## 1. Configuration

The plugin must be configured in the project's `pom.xml` to know what to do. The goals are executed using `execution` sections. A detailed description of the configuration options for the goals is given below.

### 1.1 Goal `generate-headerfile`

This goal generates a header file containing NSIS variables that represent common POM values. The resulting header file is automatically included in the NSIS script file.

Custom variables defined in `defines` will also be included in the generated header file.

The standard variables are:
* `PROJECT_BASEDIR`
* `PROJECT_BUILD_DIR`
 * `PROJECT_FINAL_NAME`
 * `PROJECT_CLASSIFIER`
 * `PROJECT_GROUP_ID`
 * `PROJECT_ARTIFACT_ID`
 * `PROJECT_NAME`
 * `PROJECT_VERSION`
 * `PROJECT_PACKAGING`
 * `PROJECT_URL`
 * `PROJECT_LICENSE`
 * `PROJECT_LICENSE_URL`
 * `PROJECT_ORGANIZATION_NAME`
 * `PROJECT_ORGANIZATION_URL`
 * `PROJECT_REG_KEY`
 * `PROJECT_REG_UNINSTALL_KEY`
 * `PROJECT_STARTMENU_FOLDER`

Variables where no value can be resolved will be omitted. If there are multiple licenses defined, the license defines will be `PROJECT_LICENSE<n>` and `PROJECT_LICENSE<n>_URL` instead, where `<n>` is the license number starting with 1.

#### 1.1.1 Parameter description

|<sub>Name</sub>|<sub>Property</sub>|<sub>Type</sub>|<sub>Default</sub>|<sub>Description</sub>|
|--|--|:--:|:--:|--|
|<sub>`classifier`</sub>|<sub>`nsis.classifier`</sub>|<sub>String</sub>| |<sub>The classifier to append to `outputFile`'s name.</sub>|
|<sub>`defines`</sub>| |<sub>Map</sub>| |<sub>A list of one or more elements. The element name will become the `!define` name (converted to upper-case) and the element value will become the `!define` value. Example:<br><br>`<foo>bar</foo>` will result in `!define FOO "bar"`|
|<sub>`disabled`</sub>|<sub>`nsis.disabled`</sub>|<sub>Boolean</sub>|<sub>`false`</sub>|<sub>Deactivates all goals.</sub>|
|<sub>`headerFile`</sub>|<sub>`nsis.headerfile`</sub>|<sub>String</sub>|<sub>`project.nsh`</sub>|<sub>The path of the header file to generate.</sub>|

### 1.2 Goal `make`

This goal compiles a NSIS script and builds a Windows executable.

#### 1.2.1 Parameter description

|<sub>Name</sub>|<sub>Property</sub>|<sub>Type</sub>|<sub>Default</sub>|<sub>Description</sub>|
|--|--|:--:|:--:|--|
|<sub>`attachArtifact`</sub>|<sub>`nsis.attachArtifact`</sub>|<sub>Boolean</sub>|<sub>`true`</sub>|<sub>Whether or not `outputFile` should be attached to the Maven build. You probably want an installer to be attached, but if you build another executable that might not be the case.</sub>|
|<sub>`autoNsisDir`</sub>|<sub>`nsis.auto.nsisdir`</sub>|<sub>Boolean</sub>|<sub>true</sub>|<sub>Whether or not to automatically set the `NSISDIR` environment variable based on the folder where the `makensis` executable is located. Useful when `makensis` is compiled with `NSIS_CONFIG_CONST_DATA_PATH=no`.</sub>|
|<sub>`classifier`</sub>|<sub>`nsis.classifier`</sub>|<sub>String</sub>| |<sub>The classifier to append to `outputFile`'s name.</sub>|
|<sub>`compression`</sub>|<sub>`nsis.compression`</sub>|<sub>Enum</sub>| <sub>`zlib`</sub>|<sub>The compression type to apply to `scriptFile` if ant. See [separate definition](#122-compression-options).</sub>|
|<sub>`compressionDictSize`</sub>|<sub>`nsis.compression.lzma.dictsize`</sub>|<sub>Integer</sub>|<sub>`8`</sub>|<sub>The dictionary size to use if `compression` is `lzma`.</sub>|
|<sub>`compressionIsFinal`</sub>|<sub>`nsis.compression.final`</sub>|<sub>Boolean</sub>|<sub>`false`</sub>|<sub>Whether or not the compression defined in `compression` is`FINAL`.</sub>|
|<sub>`compressionIsSolid`</sub>|<sub>`nsis.compression.solid`</sub>|<sub>Boolean</sub>|<sub>`false`</sub>|<sub>Whether or not the compression defined in `compression` is`SOLID`.</sub>|
|<sub>`disabled`</sub>|<sub>`nsis.disabled`</sub>|<sub>Boolean</sub>|<sub>`false`</sub>|<sub>Deactivates all goals.</sub>|
|<sub>`environmentVariables`</sub>| |<sub>Map</sub>| |<sub>A list of one or more elements. The element name will become the environment variable name and the element value will become the variable value. Example:<br><br>`<foo>bar</foo>` will result in an environment variable `foo` with the value `bar`.|
|<sub>`headerFile`</sub>|<sub>`nsis.headerfile`</sub>|<sub>String</sub>|<sub>`project.nsh`</sub>|<sub>The path of the generated header file.</sub>|
|<sub>`injectHeaderFile`</sub>|<sub>`nsis.headerfile.inject`</sub>|<sub>Boolean</sub>|<sub>`true`</sub>|<sub>Whether or not `headerFile` should be automatically injected as an `!include` in `scriptFile`. This is handy because it automatically makes sure that the correct path is used, but it means `scriptFile` won't compile if used without this plugin.</sub>|
|<sub>`makeFolder`</sub>|<sub>`nsis.makefolder`</sub>|<sub>String</sub>| |<sub>The folder to use as the working folder when running `makensis`. Relative paths will be resolved from this folder. By default this is the folder where `scriptFile` is located.</sub>|
|<sub>`makensisExecutable`</sub>|<sub>`nsis.makensis.executable`</sub>|<sub>String</sub>|<sub>`makensis`</sub>|<sub>The path of the `makensis` executable to use. The default assumes that `makensis` can be found in the OS path.</sub>|
|<sub>`makensisExecutableLinux`</sub>|<sub>`nsis.makensis.executable.linux`</sub>|<sub>String</sub>| |<sub>The path of the `makensis` executable to use if the build platform is Linux. If not configured, `makensisExecutable` will be used also on Linux.</sub>|
|<sub>`makensisExecutableMacOS`</sub>|<sub>`nsis.makensis.executable.macos`</sub>|<sub>String</sub>| |<sub>The path of the `makensis` executable to use if the build platform is macOS. If not configured, `makensisExecutable` will be used also on macOS.</sub>|
|<sub>`nsisDir`</sub>|<sub>`nsis.nsisdir`</sub>|<sub>String</sub>| |<sub>The value to use as `NSISDIR`. This will override `autoNsisDir` and the environment variable named `NSISDIR` if set.</sub>|
|<sub>`outputFile`</sub>|<sub>`nsis.output.file`</sub>|<sub>String</sub>|<sub>`${project.build.` `finalName}.exe`</sub>|<sub>The path of the executable file to build.</sub>|
|<sub>`scriptFile`</sub>|<sub>`nsis.scriptfile`</sub>|<sub>String</sub>|<sub>`setup.nsi`</sub>|<sub>The path of the NSIS script file to compile.</sub>|
|<sub>`verbosityLevel`</sub>|<sub>`nsis.verbosity`</sub>|<sub>Integer</sub>|<sub>`2`</sub>|<sub>The verbosity level to pass to `makensis`.</sub>|

#### 1.2.2 `compression` options

| Code | Default | Description |
|--|:--:|--|
|`zlib`|__*__|The [`DEFLATE`](https://en.wikipedia.org/wiki/DEFLATE) compression algorithm used in ZIP, gzip and others.|
|`bzip2`| |The [`bzip2`](https://en.wikipedia.org/wiki/Bzip2) compression using the [Burrows–Wheeler algorithm](https://en.wikipedia.org/wiki/Burrows%E2%80%93Wheeler_transform).|
|`lzma`| |The [Lempel–Ziv–Markov chain compression algorithm](https://en.wikipedia.org/wiki/Lempel%E2%80%93Ziv%E2%80%93Markov_chain_algorithm) used by 7-zip.|

### 1.3 Skeleton project configuration

Here is a skeleton project configuration showing the potential locations of all configuration options:

```xml
<project>
    <!-- ... -->
    <build>
        <!-- ... -->
        <plugins>
            <!-- ... -->
            <plugin>
                <groupId>org.digitalmediaserver</groupId>
                <artifactId>nsis-maven-plugin</artifactId>
                <version>...</version>
                <configuration>
                    <attachArtifact></attachArtifact>
                    <autoNsisDir></autoNsisDir>
                    <classifier></classifier>
                    <compression></compression>
                    <compressionDictSize></compressionDictSize>
                    <compressionIsFinal></compressionIsFinal>
                    <compressionIsSolid></compressionIsSolid>
                    <defines>
                        <FIRST_DEFINE>first define value</FIRST_DEFINE>
                        <SECOND_DEFINE>second define value</SECOND_DEFINE>
                        <!-- ... -->
                        <LAST_DEFINE>last define value</LAST_DEFINE>
                    </defines>
                    <disabled></disabled>
                    <environmentVariables>
                        <firstVariable>first variable value</firstVariable>
                        <secondVariable>second variable value</secondVariable>
                        <!-- ... -->
                        <lastVariable>last variable value</lastVariable>
                    </environmentVariables>
                    <headerFile></headerFile>
                    <injectHeaderFile></injectHeaderFile>
                    <makeFolder></makeFolder>
                    <makensisExecutable></makensisExecutable>
                    <makensisExecutableLinux></makensisExecutableLinux>
                    <makensisExecutableMacOS></makensisExecutableMacOS>
                    <nsisDir></nsisDir>
                    <outputFile></outputFile>
                    <scriptFile></scriptFile>
                    <verbosityLevel></verbosityLevel>
                </configuration>
                <executions>
                    <execution>
                        <id>execution-id</id>
                        <configuration>
                            <attachArtifact></attachArtifact>
                            <autoNsisDir></autoNsisDir>
                            <classifier></classifier>
                            <compression></compression>
                            <compressionDictSize></compressionDictSize>
                            <compressionIsFinal></compressionIsFinal>
                            <compressionIsSolid></compressionIsSolid>
                            <defines>
                                <FIRST_DEFINE>first define value</FIRST_DEFINE>
                                <SECOND_DEFINE>second define value</SECOND_DEFINE>
                                <!-- ... -->
                                <LAST_DEFINE>last define value</LAST_DEFINE>
                            </defines>
                            <disabled></disabled>
                            <environmentVariables>
                                <firstVariable>first variable value</firstVariable>
                                <secondVariable>second variable value</secondVariable>
                                <!-- ... -->
                                <lastVariable>last variable value</lastVariable>
                            </environmentVariables>
                            <headerFile></headerFile>
                            <injectHeaderFile></injectHeaderFile>
                            <makeFolder></makeFolder>
                            <makensisExecutable></makensisExecutable>
                            <makensisExecutableLinux></makensisExecutableLinux>
                            <makensisExecutableMacOS></makensisExecutableMacOS>
                            <nsisDir></nsisDir>
                            <outputFile></outputFile>
                            <scriptFile></scriptFile>
                            <verbosityLevel></verbosityLevel>
                        </configuration>
                        <goals>
                            <goal></goal>
                            <!-- ... -->
                        </goals>
                    </execution>
                    <!-- ... -->
                </executions>
            </plugin>
            <!-- ... -->
        </plugins>
        <!-- ... -->
    </build>
    <!-- ... -->
</project>
```

## 2. Using the plugin

This plugin performs two very different tasks in its two goals. The [`generate-headerfile` goal](#11-goal-generate-headerfile) generates a file that can be included in a NSIS script as an `!include` that contains values available in the POM. It defines a set of standard values plus any configured custom values. This header file is not needed by the [`make` goal](#12-goal-make), but can be very useful to avoid having to maintain the same information in multiple files. If the header file exists and `injectHeaderFile` is `true` (default), no explicit `!include` is required in the script compiled in the [`make` goal](#12-goal-make). While this is handy to not have to specify a valid path to where the header file is located in the NSIS script, it means that the NSIS script can no longer be compiled without using this plugin if any of the defined values is used in the script.

The [`make` goal](#12-goal-make) builds the Windows executable from the specified NSIS script. NSIS scripts can be used to make other types of executables than merely installers, like for example application launchers. The goals can be run multiple times in multiple `executions`, but unless the header file contains different values for the different NSIS scripts, there isn't much point in executing the [`generate-headerfile` goal](#11-goal-generate-headerfile) more than once. The generated file will stay available for reference or injection by subsequent `make` goals. The resulting `.exe` files will be attached to the Maven build like any other artifact unless `attachArtifact` is `false`.  In this case, the `.exe` file will simply be generated and left in its target folder.

### 2.1 Example configuration

Here is an example configuration showing how to generate a header file, a Windows application launcher and a Windows installer:

```xml
<project>
    <!-- ... -->
    <build>
        <!-- ... -->
        <plugins>
            <!-- ... -->
            <plugin>
                <groupId>org.digitalmediaserver</groupId>
                <artifactId>nsis-maven-plugin</artifactId>
                <version>...</version>
                <configuration>
                    <makensisExecutable>${project.external-resources}/third-party/nsis/Bin/makensis.exe</makensisExecutable>
                    <makensisExecutableLinux>${project.external-resources}/third-party/nsis/Bin/makensisLinux</makensisExecutableLinux>
                    <makensisExecutableMacOS>${project.external-resources}/third-party/nsis/Bin/makensisMacOS</makensisExecutableMacOS>
                    <compression>lzma</compression>
                    <compressionIsFinal>true</compressionIsFinal>
                    <compressionDictSize>64</compressionDictSize>
                    <defines>
                        <PROJECT_NAME_SHORT>${project.name.short}</PROJECT_NAME_SHORT>
                        <PROJECT_NAME_CAMEL>${project.name.camel}</PROJECT_NAME_CAMEL>
                    </defines>
                </configuration>
                <executions>
                    <execution>
                        <id>build-windows-launcher</id>
                        <configuration>
                            <scriptFile>${project.external-resources}/nsis/${project.artifactId}.nsi</scriptFile>
                            <outputFile>${project.name.short}.exe</outputFile>
                            <attachArtifact>false</attachArtifact>
                        </configuration>
                        <goals>
                            <goal>generate-headerfile</goal>
                            <goal>make</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>build-windows-installer</id>
                        <configuration>
                            <scriptFile>${project.external-resources}/nsis/setup.nsi</scriptFile>
                            <outputFile>${project.name.short}-setup.exe</outputFile>
                        </configuration>
                        <goals>
                            <goal>make</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <!-- ... -->
        </plugins>
        <!-- ... -->
    </build>
    <!-- ... -->
</project>
```
