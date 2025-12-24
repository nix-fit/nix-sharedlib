import org.nix.sharedlib.pipeline.build.BuildDotnetLibraryPipeline

/**
 * Build .Net library pipeline
 */
void call(Map args = [:]) {
    new BuildDotnetLibraryPipeline(this).run(args)
}
