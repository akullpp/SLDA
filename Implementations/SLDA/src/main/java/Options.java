import org.kohsuke.args4j.Option;


/** Commandline options */
public class Options {
    @Option(name="-e", usage="Estimation")
    public boolean estimation = false;

    @Option(name="-i", usage="Inference")
    public boolean inference = false;

    @Option(name="-d", usage="Folder")
    public String folder = "./data/";

    @Option(name="-c", usage="Corpus")
    public String corpus = "train";

    @Option(name="-a", usage="Alpha")
    public double alpha = 0.001;

    @Option(name="-b", usage="Beta")
    public double beta = 0.001;

    @Option(name="-g", usage="Gamma")
    public double gamma = 0.001;

    @Option(name="-n", usage="Iterations")
    public int iter = 1000;

    @Option(name="-k", usage="Topics")
    public int K = 3;

    @Option(name="-step", usage="Reporting step")
    public int step = 100;

    /** Calculates phi and theta incrementally according to lag. */
    @Option(name="-m", usage="Matrix calculation method")
    // extended or simple
    public String method = "extended";

    @Option(name="-burn", usage="Burn-in")
    // only relevant for extended
    public int burn = 100;

    @Option(name="-lag", usage="Sampling lag")
    // only relevant for extended
    public int lag = 5;

    @Option(name="-v", usage="Version")
    // 0 for basic model, 1 for re-sampling f
    public boolean v = true;

    @Option(name="-bg", usage="Background topics")
    public boolean bg = false;
}
