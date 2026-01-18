import org.nix.sharedlib.pipeline.build.BuildDotnetAppPipeline

/**
 * Build .Net app pipeline
 */
void call(Map args = [:]) {
    new BuildDotnetAppPipeline(this).run(args)
}
