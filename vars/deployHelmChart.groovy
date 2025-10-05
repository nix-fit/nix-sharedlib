import org.nix.sharedlib.pipeline.deploy.DeployHelmChartPipeline

/**
 * Deploy Helm chart pipeline
 */
void call(Map args = [:]) {
    new DeployHelmChartPipeline(this).run(args)
}
