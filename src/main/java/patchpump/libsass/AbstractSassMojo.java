package patchpump.libsass;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Output;

public abstract class AbstractSassMojo extends AbstractMojo {

	@Parameter(property = "project.build.directory", required = true)
	protected File outputPath;

	@Parameter(defaultValue = "src/main/sass")
	protected String inputPath;

	@Parameter
	protected String includePath;

	@Parameter(defaultValue = "nested")
	private SassCompiler.OutputStyle outputStyle;

	@Parameter(defaultValue = "false")
	private boolean generateSourceComments;

	@Parameter(defaultValue = "true")
	private boolean generateSourceMap;

	@Parameter(property = "project.build.directory")
	private String sourceMapOutputPath;

	@Parameter(defaultValue = "false")
	private boolean omitSourceMapingURL;

	@Parameter(defaultValue = "false")
	private boolean embedSourceMapInCSS;

	@Parameter(defaultValue = "false")
	private boolean embedSourceContentsInSourceMap;

	@Parameter(defaultValue = "scss")
	private SassCompiler.InputSyntax inputSyntax;

	@Parameter(defaultValue = "5")
	private int precision;

	@Parameter(defaultValue = "true")
	protected boolean failOnError;

	@Parameter(defaultValue = "false")
	private boolean copySourceToOutput;

	@Parameter(property = "project", required = true, readonly = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${buildContext}", readonly = true)
	protected Object buildContext;

	private SassCompiler compiler;

	private static final Pattern PATTERN_ERROR_JSON_LINE = Pattern.compile("[\"']line[\"'][:\\s]+([0-9]+)");
	private static final Pattern PATTERN_ERROR_JSON_COLUMN = Pattern.compile("[\"']column[\"'][:\\s]+([0-9]+)");

	protected void compile() throws IOException {

		final Path root = project.getBasedir().toPath().resolve(Paths.get(inputPath));
		String fileExt = getFileExtension();
		String globPattern = "glob:{**/,}*."+fileExt;
		getLog().debug("Glob = " + globPattern);
	
		final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);
		final AtomicInteger errorCount = new AtomicInteger(0);
		final AtomicInteger fileCount = new AtomicInteger(0);
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (matcher.matches(file) && !file.getFileName().toString().startsWith("_")) {
					fileCount.incrementAndGet();
					if(!processFile(root, file)){
						errorCount.incrementAndGet();
					}
				}
	
				return FileVisitResult.CONTINUE;
			}
	
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	
		getLog().info("Compiled " + fileCount + " files");
		if (errorCount.get() > 0) {
			if (failOnError) {
				throw new IOException("Failed with " + errorCount.get() + " errors");
			} else {
				getLog().error("Failed with " + errorCount.get() + " errors. Continuing due to failOnError=false.");
			}
		}
	}

	protected String getFileExtension() {
		return inputSyntax.toString();
	}

	protected void validateConfig() {
		if (!generateSourceMap) {
			if (embedSourceMapInCSS) {
				getLog().warn("embedSourceMapInCSS=true is ignored. Cause: generateSourceMap=false");
			}
			if (embedSourceContentsInSourceMap) {
				getLog().warn("embedSourceContentsInSourceMap=true is ignored. Cause: generateSourceMap=false");
			}
		}
		if (outputStyle != SassCompiler.OutputStyle.compressed && outputStyle != SassCompiler.OutputStyle.nested) {
			getLog().warn("outputStyle=" + outputStyle + " is replaced by nested. Cause: libsass 3.1 only supports compressed and nested");
		}
	}

	private void setCompileClasspath() {
		try {
			Set<URL> urls = new HashSet<>();
			List<String> elements = project.getCompileClasspathElements();
			for (String element : elements) {
				urls.add(new File(element).toURI().toURL());
			}

			ClassLoader contextClassLoader = URLClassLoader.newInstance(
					urls.toArray(new URL[0]),
					Thread.currentThread().getContextClassLoader());

			Thread.currentThread().setContextClassLoader(contextClassLoader);

		} catch (DependencyResolutionRequiredException e) {
			throw new RuntimeException(e);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	protected void initCompiler() {

		setCompileClasspath();
		
		compiler = new SassCompiler();
		compiler.setEmbedSourceMapInCSS(this.embedSourceMapInCSS);
		compiler.setEmbedSourceContentsInSourceMap(this.embedSourceContentsInSourceMap);
		compiler.setGenerateSourceComments(this.generateSourceComments);
		compiler.setGenerateSourceMap(this.generateSourceMap);
		compiler.setIncludePaths(this.includePath);
		compiler.setInputSyntax(this.inputSyntax);
		compiler.setOmitSourceMappingURL(this.omitSourceMapingURL);
		compiler.setOutputStyle(this.outputStyle);
		compiler.setPrecision(this.precision);
	}

	protected boolean processFile(Path inputRootPath, Path inputFilePath) throws IOException {
		getLog().debug("Processing File " + inputFilePath);
	
		Path relativeInputPath = inputRootPath.relativize(inputFilePath);
	
		Path outputRootPath = this.outputPath.toPath();
		Path outputFilePath = outputRootPath.resolve(relativeInputPath);
		String fileExtension = getFileExtension();
		outputFilePath = Paths.get(outputFilePath.toAbsolutePath().toString().replaceFirst("\\."+fileExtension+"$", ".css"));
	
		Path sourceMapRootPath = Paths.get(this.sourceMapOutputPath);
		Path sourceMapOutputPath = sourceMapRootPath.resolve(relativeInputPath);
		sourceMapOutputPath = Paths.get(sourceMapOutputPath.toAbsolutePath().toString().replaceFirst("\\.scss$", ".css.map"));
	
		if (copySourceToOutput) {
			Path inputOutputPath = outputRootPath.resolve(relativeInputPath);
			inputOutputPath.toFile().mkdirs();
			Files.copy(inputFilePath, inputOutputPath, REPLACE_EXISTING);
			refresh(inputOutputPath.toFile());
			inputFilePath = inputOutputPath;
		}
		
		Output out;
		try {
			out = compiler.compileFile(
					inputFilePath.toAbsolutePath().toString(),
					outputFilePath.toAbsolutePath().toString(),
					sourceMapOutputPath.toAbsolutePath().toString()
			);
		}
		catch (CompilationException e) {
			getLog().error(e.getMessage());
			getLog().debug(e);

			// we need this info from json:
			// "line": 4,
			// "column": 1,
			// - a full blown parser for this would probably be an overkill, let's just regex
			String errorJson = e.getErrorJson();
			int line = 0, column = 0;
			if (errorJson != null) { // defensive, in case we don't always get it
				Matcher lineMatcher = PATTERN_ERROR_JSON_LINE.matcher(errorJson);
				if (lineMatcher.find())
					try {
						line = Integer.parseInt(lineMatcher.group(1));
					} catch (IndexOutOfBoundsException | NumberFormatException e1) { // in case regex doesn't cut it anymore
						getLog().error("Failed to parse error json line: " + e1.getMessage());
						getLog().debug(e1);
					}
				Matcher columnMatcher = PATTERN_ERROR_JSON_COLUMN.matcher(errorJson);
				if (columnMatcher.find())
					try {
						column = Integer.parseInt(columnMatcher.group(1));
					} catch (IndexOutOfBoundsException | NumberFormatException e1) { // in case regex doesn't cut it anymore
						getLog().error("Failed to parse error json column: " + e1.getMessage());
						getLog().debug(e1);
					}
			}
			addMessage(inputFilePath.toFile(), line, column, e.getErrorMessage(), e);

			return false;
		}
	
		getLog().debug("Compilation finished.");
	
		writeContentToFile(outputFilePath, out.getCss());
		if (out.getSourceMap() != null) {
			writeContentToFile(sourceMapOutputPath, out.getSourceMap());
		}
		return true;
	}

	private void writeContentToFile(Path outputFilePath, String content) throws IOException {
		File f = outputFilePath.toFile();
		f.getParentFile().mkdirs();
		f.createNewFile();
		OutputStreamWriter os = null;
		try{
			os = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
			os.write(content);
			os.flush();
		} finally {
			if (os != null)
				os.close();
		}
		refresh(outputFilePath.toFile());
		getLog().debug("Written to: " + f);
	}

	private void addMessage(File file, int line, int column, String message, Throwable error) {

		if (buildContext == null)
			return;

		try {
			buildContext.getClass().getMethod("addMessage", File.class, int.class, int.class, String.class,
				int.class, Throwable.class).invoke(buildContext, file, line, column, message, 2, error);
		} catch (ReflectiveOperationException ignored) {
		}
	}
	protected boolean isIncremental() {
		if (buildContext == null)
			return false;

		try {
			return (Boolean) buildContext.getClass()
					.getMethod("isIncremental")
					.invoke(buildContext);
		} catch (ReflectiveOperationException e) {
			return false;
		}
	}

	protected boolean hasDelta(String file) {
		if (buildContext == null) {
			return true;
		}
		try {
			return (Boolean) buildContext.getClass()
					.getMethod("hasDelta", File.class)
					.invoke(buildContext, file);
		} catch (ReflectiveOperationException e) {
			return true;
		}
	}

	protected void refresh(File file) {
		if (buildContext == null) {
			return;
		}
		try {
			buildContext.getClass()
					.getMethod("refresh", File.class)
					.invoke(buildContext, file);
		} catch (ReflectiveOperationException ignored) {
		}
	}
}