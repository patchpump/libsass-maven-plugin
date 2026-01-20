Libsass Maven Plugin
==========

Uses [jsass](https://github.com/bit3/jsass) to interface with libsass C-library.

## Changelog

**0.2.10-PATCHPUMP-R0**
- Upgrade to **jsass 5.11.1** (libsass **3.6.6**)
- Remove **susy** and **webjar** lookups
- **JDK 17** and **Maven 3.9**

**0.2.10**
- Upgrade libsass to **3.5.3**

**0.2.9**
- Upgrade libsass to **3.4.7**
- Refresh output files for Eclipse
- Enhance error output with failing files  
  *(thanks to @VsevolodGolovanov)*

**0.2.8**
- Upgrade libsass to **3.4.4**
- Use compilation classpath for including webjars
- Fix OS-dependent path separator issues
- Plugin is now aware of incremental builds

**0.2.7**
- Upgrade libsass to **3.4.3**
- Add webjar support  
  *(thanks to @flipp5b)*

**0.2.6**
- Upgrade libsass to **3.4.0**
- Add `libsass:watch` goal to watch and recompile include directories  
  *(thanks to @lorenzodee)*

**0.2.5**
- Add `copySourceToOutput`
- Change default output style to `nested`
- Upgrade libsass to **3.3.6**

**0.2.4**
- Fix bug with empty spaces in paths

**0.2.3**
- Upgrade libsass to **3.3.4**

**0.2.2**
- Minor bug fixes
- Re-add m2e lifecycle mapping

**0.2.1**
- Upgrade libsass to **3.3.3**

**0.2.0**
- Switch native bindings to **bit3** bindings (libsass **3.3.2**)
- **Java 8 only**

**0.1.7**
- Fix UTF-8 encoding issue
- Fix wrong file extension for sass style

**0.1.6**
- Add m2e Eclipse integration  
  *(thanks to @dashorst)*

**0.1.5**
- Re-add macOS binaries  
  *(thanks to @tommix1987)*

**0.1.4**
- Include libsass version in artifact version  
  (e.g. `0.1.4-libsass_3.2.4-SNAPSHOT`)
- Switch to new libsass API (`sass_context.h`)
- Remove `image_path` option (libsass issue #420)
- Add `failOnError` flag to continue build on errors

**0.1.3**
- Fix multi-module project handling (#10)

**0.1.2**
- Update libsass to **3.1** (Windows, Linux, macOS)
- Merge PR #4  
  *(thanks to @npiguet, @ogolberg)*

**0.1.1**
- Allow `.scss` files directly in `inputPath/`

**0.1.0**
- Change artifact group to `com.github.warmuuh`

Usage
-----
Configure plugin in your pom.xml:

```
<build>
   <plugins>
      <plugin>
         <groupId>com.github.warmuuh</groupId>
         <artifactId>libsass-maven-plugin</artifactId>
         <version>0.2.10-PATCHPUMP-R0</version>
         <executions>
            <execution>
               <phase>generate-resources</phase>
               <goals>
                  <goal>compile</goal>
               </goals>
            </execution>
         </executions>
         <configuration>
            <inputPath>${basedir}/src/main/sass/</inputPath>
            <outputPath>${basedir}/target/</outputPath>
            <includePath>${basedir}/src/main/sass/plugins/</includePath>
         </configuration>
      </plugin>
   </plugins>
</build>
```

Alternatively, you can use the `watch` goal to have the plugin watch your files and recompile on change:
```
mvn com.github.warmuuh:libsass-maven-plugin:0.2.10-PATCHPUMP-R0:watch
```

Configuration Elements
----------------------

<table>
  <thead>
    <tr>
       <td>Element</td>
       <td>Default value</td>
       <td>Documentation</td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>outputPath</td>
      <td><code>${project.build.directory}</code></td>
      <td>The directory in which the compiled CSS files will be placed.</td>
    </tr>
    <tr>
      <td>inputPath</td>
      <td><code>src/main/sass</code></td>
      <td>
        The directory from which the source <code>.scss</code> files will be read. This directory will be
        traversed recursively, and all <code>.scss</code> files found in this directory or subdirectories
        will be compiled.
      </td>
    </tr>
    <tr>
      <td>includePath</td>
      <td><code>null</code></td>
      <td>Additional include path, ';'-separated</td>
    </tr>
    <tr>
      <td>outputStyle</td>
      <td><code>nested</code></td>
      <td>
         Output style for the generated css code. One of <code>nested</code>, <code>expanded</code>,
         <code>compact</code>, <code>compressed</code>. Note that as of libsass 3.1, <code>expanded</code>
         and <code>compact</code> result in the same output as <code>nested</code>.
      </td>
    </tr>
    <tr>
      <td>generateSourceComments</td>
      <td><code>false</code></td>
      <td>
         Emit comments in the compiled CSS indicating the corresponding source line. The default
         value is <code>false</code>.
      </td>
    </tr>
    <tr>
      <td>generateSourceMap</td>
      <td><code>true</code></td>
      <td>
        Generate source map files. The generated source map files will be placed in the directory
        specified by <code>sourceMapOutputPath</code>.
      </td>
    </tr>
    <tr>
      <td>sourceMapOutputPath</td>
      <td><code>${project.build.directory}</code></td>
      <td>
        The directory in which the source map files that correspond to the compiled CSS will be placed
      </td>
    </tr>
    <tr>
      <td>omitSourceMapingURL</td>
      <td><code>false</code></td>
      <td>
        Prevents the generation of the <code>sourceMappingURL</code> special comment as the last
        line of the compiled CSS.
      </td>
    </tr>
    <tr>
      <td>embedSourceMapInCSS</td>
      <td><code>false</code></td>
      <td>
        Embeds the whole source map data directly into the compiled CSS file by transforming
        <code>sourceMappingURL</code> into a data URI.
      </td>
    </tr>
    <tr>
      <td>embedSourceContentsInSourceMap</td>
      <td><code>false</code></td>
      <td>
       Embeds the contents of the source <code>.scss</code> files in the source map file instead of the
       paths to those files
      </td>
    </tr>
    <tr>
      <td>inputSyntax</td>
      <td><code>scss</code></td>
      <td>
       Switches the input syntax used by the files to either <code>sass</code> or <code>scss</code>.
      </td>
    </tr>
    <tr>
      <td>precision</td>
      <td><code>5</code></td>
      <td>
       Precision for fractional numbers
      </td>
    </tr>
     <tr>
      <td>failOnError</td>
      <td><code>true</code></td>
      <td>
       should fail the build in case of compilation errors.
      </td>
    </tr>
    <tr>
      <td>copySourceToOutput</td>
      <td><code>false</code></td>
      <td>
       copies all files from source directory to output directory
      </td>
    </tr>
  </tbody>
</table>


License
-------

MIT License.
