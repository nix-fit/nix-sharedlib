import org.nix.sharedlib.pipeline.build.BuildDockerImagePipeline

/**
 * Build Docker image pipeline
 */
void call(Map args = [:]) {
    new BuildDockerImagePipeline(this).run(args)
}
