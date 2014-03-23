import java.io.*;
import java.util.Arrays;
import java.util.Map;


/**
 * Estimation of training model.
 */
public class Estimation extends Sampling {
    private Model train;

    /** Constructor. */
    public Estimation(Options o) {
        super(o);

        train = new Model(o);
        // Initialization counts as sample
        samples = 1;

        printStats(train);
        estimate();

        // Compute theta and phi from the last sample
        if (o.method == "simple") {
            computeTheta(train);
            computePhi();
        }
        saveModel();

        train.collectData("train");
        printPerplexity(train);
    }

    /**
     * Serializes model for inference with minimal overhead.
     *
     * It saves the count of words w under topic k, the parameters alpha and beta, the number of types, the count of
     * words w under topic k, the count of label l in the entire corpus and finally the words and their ids.
     */
    public void saveModel() {
        try {
            FileOutputStream fos = new FileOutputStream(o.folder + "n_w_k");
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            bw.write(Arrays.deepToString(train.n_w_k));
            bw.close();

            fos = new FileOutputStream(o.folder + "param");
            osw = new OutputStreamWriter(fos, "UTF-8");
            bw = new BufferedWriter(osw);

            bw.write(train.alpha + "\n" +
                    train.beta + "\n" +
                    train.V + "\n" +
                    Arrays.toString(train.s_w_k) + "\n" +
                    Arrays.toString(train.n_l_c));
            bw.close();

            fos = new FileOutputStream(o.folder + "words");
            osw = new OutputStreamWriter(fos, "UTF-8");
            bw = new BufferedWriter(osw);

            for (Map.Entry<String, Integer> e : train.word2id.entrySet()) {
                bw.write(e.getKey() + ":" + e.getValue() + "\n");
            }
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /** The Gibbs sampler. */
    protected void estimate() {
        for (int i = 1; i < o.iter; i++) {
            if (i % o.step == 0) { System.out.println("Iteration: " + i); }

            for (int m = 0; m < train.M; m++) {
                for (int s = 0; s < train.docs.get(m).size(); s++) {
                    for (int n = 0; n < train.docs.get(m).get(s).size(); n++) {
                        sample(m, s, n);
                    }
                }
            }
            if (o.method == "extended") extended(i);
        }
    }

    /**
     * Computes theta and phi after a burn in period with a certain lag.
     *
     * @param i iteration
     */
    public void extended(int i) {
        if ((i == o.iter - 1) || (((i > o.burn) && (i % o.lag == 0)))) {
            for (int m = 0; m < train.M; m++) {
                for (int k = 0; k < train.K; k++) {
                    if (samples > 1) train.theta[m][k] *= samples - 1;

                    train.theta[m][k] += ((train.n_m_k[m][k] + train.alpha) / (train.s_w_d[m] + train.K * train.alpha));

                    if (samples > 1) train.theta[m][k] /= samples;
                }
            }
            for (int k = 0; k < train.K; k++) {
                for (int w = 0; w < train.V; w++) {
                    if (samples > 1) train.phi[k][w] *= samples - 1;

                    train.phi[k][w] += ((train.n_w_k[w][k] + train.beta) / (train.s_w_k[k] + train.V * train.beta));

                    if (samples > 1) train.phi[k][w] /= samples;
                }
            }
            samples++;
        }
    }

    /**
     * Update function of the Gibbs sampler.
     *
     * @param m document
     * @param s sentence
     * @param n position
     */
    protected void sample(int m, int s, int n) {
        int z = train.z_m.get(m).get(s).get(n);
        int w = train.docs.get(m).get(s).get(n);
        int K = train.labels.get(m).get(s).size();

        train.n_w_k[w][z] -= 1;
        train.n_m_k[m][z] -= 1;
        train.s_w_k[z] -= 1;
        train.s_w_d[m] -= 1;

        for (int k = 0; k < K; k++) {
            z = train.labels.get(m).get(s).get(k);

            train.p[k] = (train.n_m_k[m][z] + train.alpha) *
                         (train.n_w_k[w][z] + train.beta) /
                         (train.s_w_k[z] + train.V * train.beta);
        }
        for (int k = 1; k < K; k++) {
            train.p[k] += train.p[k - 1];
        }
        double p = Math.random() * train.p[K - 1];

        for (z = 0; z < K; z++) {
            if (train.p[z] > p)
                break;
        }
        z = train.labels.get(m).get(s).get(z);

        train.n_w_k[w][z] += 1;
        train.n_m_k[m][z] += 1;
        train.s_w_k[z] += 1;
        train.s_w_d[m] += 1;
        train.z_m.get(m).get(s).set(n, z);
    }

    /**
     * Topic/word distribution.
     */
    protected void computePhi() {
        for (int k = 0; k < train.K; k++) {
            for (int w = 0; w < train.V; w++) {
                train.phi[k][w] = (train.n_w_k[w][k] + train.beta) /
                                  (train.s_w_k[k] + train.V * train.beta);
            }
        }
    }
}
