package patchpump.libsass;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Compilation of all scss files from inputpath to outputpath using includePaths
 */
@Mojo(
	name = "compile",
	defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
	requiresDependencyResolution = ResolutionScope.COMPILE,
	threadSafe = true
)
public class CompilationMojo extends AbstractSassMojo {

	/**
	 * Returns project relative input path for sass files. If input path is absolute, remove base dir from string.
	 * @return project relative input path. 
	 */
	private String getRelativeInputPath() {
		String relativeInputPath = inputPath;
		String baseDirPath = project.getBasedir().getPath();
		if (relativeInputPath.startsWith(baseDirPath)) {
			relativeInputPath = relativeInputPath.substring(baseDirPath.length() + 1);
		}
		return relativeInputPath;
	}
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		validateConfig();
		if ((buildContext!=null) && (!isIncremental() || (hasDelta(getRelativeInputPath())))) {

			initCompiler();

			inputPath = inputPath.replaceAll("\\\\", "/");

			getLog().debug("Input Path=" + inputPath);
			getLog().debug("Output Path=" + outputPath);
		
			try {
				compile();
			} catch (Exception e) {
				throw new MojoExecutionException("Failed", e);
			}
		}
	}

}