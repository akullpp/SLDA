import org.kohsuke.args4j.Option;


/** Commandline options */
public class Options {
    @Option(name="-e", usage="Estimation")
    public boolean estimation = false;

    @Option(name="-i", usage="Inference")
    public boolean inference = false;

    @Option(name="-c", usage="Corpus")
    public String corpus = "corpus.txt";

    @Option(name="-a", usage="Alpha")
    public double alpha = 0.01;

    @Option(name="-b", usage="Beta")
    public double beta = 0.01;

    @Option(name="-n", usage="Iterations")
    public int iter = 1000;

    @Option(name="-k", usage="Topics")
    public int K = 3;

    @Option(name="-llda", usage="Labeled LDA")
    public boolean llda = true;

    @Option(name="-step", usage="Reporting step")
    public int step = 100;

    @Option(name="-burn", usage="Burnin")
    // only relevant for extended
    public int burn = 100;

    @Option(name="-lag", usage="Sampling lag")
    // only relevant for extended
    public int lag = 5;

    @Option(name="-m", usage="Matrix calculation method")
    // extended or simple
    public String method = "extended";

    @Option(name="-bg", usage="Common background topic")
    public boolean bg = true;
}
