import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 * Inference for new documents, uses training as prior.
 */
public class Inference extends Sampling {
    public Model test;
    public Train train;
    public Sampler sampler;

    /** Minimal structure for the training model. */
    public static class Train {
        // see Model.word2id
        HashMap<String, Integer> _word2id;
        // see Model.n_w_k
        int[][] _n_w_k;
        // see Model.s_w_k
        int[] _s_w_k;
        // see Model.n_l_c
        int[] _n_l_c;
        // see Model.V
        int _V;
        // see Model.alpha
        double _alpha;
        // see Model.beta
        double _beta;

        /** Reads training data from files */
        Train(Options o) {
            try {
                FileInputStream fis = new FileInputStream(o.folder + "param");
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader br = new BufferedReader(isr);

                _word2id = new HashMap<String, Integer>();
                _alpha = Double.parseDouble(br.readLine());
                _beta = Double.parseDouble(br.readLine());
                _V = Integer.parseInt(br.readLine());

                _n_w_k = new int[_V][o.K];
                _s_w_k = new int[o.K];
                _n_l_c = new int[o.K];

                String[] tmp1 = br.readLine().replace("[", "").replace("]", "").split(", ");
                String[] tmp2 = br.readLine().replace("[", "").replace("]", "").split(", ");

                for (int i = 0; i < tmp1.length; i++) {
                    _s_w_k[i] = Integer.parseInt(tmp1[i]);
                    _n_l_c[i] = Integer.parseInt(tmp2[i]);
                }
                br.close();

                fis = new FileInputStream("./data/n_w_k");
                isr = new InputStreamReader(fis, "UTF-8");
                br = new BufferedReader(isr);

                String[] tmp = br.readLine().split("], \\[");

                for (int i = 0; i < tmp.length; i++) {
                    String[] ss = tmp[i].replace("]", "").replace("[[", "").replace("[", "").split(", ");

                    for (int j = 0; j < ss.length; j++) {
                        _n_w_k[i][j] = Integer.parseInt(ss[j]);
                    }
                }
                br.close();

                String line;
                fis = new FileInputStream("./data/words");
                isr = new InputStreamReader(fis, "UTF-8");
                br = new BufferedReader(isr);

                while((line = br.readLine()) != null) {
                    String[] tmp3 = line.split(":");

                    _word2id.put(tmp3[0], Integer.parseInt(tmp3[1]));
                }
                br.close();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /** Constructor */
    public Inference(Options o) {
        super(o);

        train = new Train(o);
        test = new Model(o, train._word2id);
        // Initialization counts as sample
        samples = 1;

        // Preparing vector alphas with a scalar alpha for sampling from Gamma
        prepareAlphas(o.alpha);
        // Sample sentence formality labels
        sampler = new Sampler(test);

        printStats(test);
        estimate();

        // Compute theta and phi from the last sample
        if (o.method == "simple") {
            computePhi();
            computeTheta(test);
        }

        test.collectData("test");
        printPerplexity(test);
    }

    /**
     * Creates a K-long vector with scalar alpha plus the label counts from training as prior.
     *
     * @param a scalar alpha
     */
    public void prepareAlphas(double a) {
        test.alphas = new double[o.K];

        Arrays.fill(test.alphas, a);

        for (int i = 0; i < o.K; i++) {
            test.alphas[i] += train._n_l_c[i];
        }
    }

    /** Gibbs sampler for inference. */
    protected void estimate() {
        for (int i = 1; i < o.iter; i++) {
            if (i % o.step == 0) { System.out.println("Iteration: " + i); }

            for (int m = 0; m < test.M; m++) {
                for (int s = 0; s < test.docs.get(m).size(); s++) {
                    int f = sample(m, s);

                    for (int n = 0; n < test.docs.get(m).get(s).size(); n++) {
                        sample(m, s, n, f);
                    }
                }
            }
            if (o.method == "extended") extended(i, train);
        }
    }

    /**
     * Computes theta and phi after a burn in period with a certain lag.
     *
     * @param i         iteration
     * @param train     training model
     */
    public void extended(int i, Train train) {
        if ((i == o.iter - 1) || (((i > o.burn) && (i % o.lag == 0)))) {
            for (int m = 0; m < test.M; m++) {
                for (int k = 0; k < test.K; k++) {
                    if (samples > 1) test.theta[m][k] *= samples - 1;

                    test.theta[m][k] += ((test.n_m_k[m][k] + test.alpha) / (test.s_w_d[m] + test.K * test.alpha));

                    if (samples > 1) test.theta[m][k] /= samples;
                }
            }
            for (int k = 0; k < test.K; k++) {
                for (int w = 0; w < test.V; w++) {

                    if (test.lid2gid.containsKey(w)) {
                        int _w = test.lid2gid.get(w);

                        if (samples > 1) test.phi[k][w] *= samples - 1;

                        // Train as prior counts
                        test.phi[k][w] += (train._n_w_k[_w][k] + test.n_w_k[w][k] + test.beta) /
                                          (train._s_w_k[k] + test.s_w_k[k] + train._V * test.beta);

                        if (samples > 1) test.phi[k][w] /= samples;
                    }
                }
            }
            samples++;
        }
    }

    /**
     * Samples f depending on the variation.
     *
     * If variation = 0, just f is returned.
     * If variation = 1, f is re-sampled like z.
     *
     * @param m document
     * @param s sentence
     * @return formality label
     */
    protected int sample(int m, int s) {
        int f = test.f_m_s.get(m).get(s);

        if (o.v) {
            double[] p = new double[test.K];

            int sum = 0;

            for (int i = 0; i < test.K; i++) {
                sum += test.s_m_f[m][i];
            }

            test.s_m_f[m][f] -= 1;

            for (int k = 0; k < test.K; k++) {
                p[k] = (test.s_m_f[m][f] + o.gamma) / (o.gamma * test.K + sum - 1);
            }

            for (int k = 1; k < test.K; k++) {
                p[k] += p[k-1];
            }

            double u = Math.random() * p[test.K - 1];

            for (f = 0; f < test.K; f++) {
                if (p[f] > u)
                    break;
            }

            test.s_m_f[m][f] += 1;
            test.f_m_s.get(m).set(s, f);

            return f;
        }
        else {
            return f;
        }
    }

    /**
     * Gibbs update for the topic-assignments.
     *
     * @param m document
     * @param s sentence
     * @param n position
     * @param f sentence-formality
     */
    protected void sample(int m, int s, int n, int f) {
        int z = test.z_m.get(m).get(s).get(n);
        int w = test.docs.get(m).get(s).get(n);
        int _w = test.lid2gid.get(w);
        ArrayList<Integer> labels = new ArrayList<Integer>();

        labels.add(f);

        if (f != 2 && !o.bg) {
            labels.add(2);
        }
        else if (o.bg && f < 2) {
            for (int k = 2; k < o.K; k++) {
                labels.add(k);
            }
        }

        test.n_w_k[w][z] -= 1;
        test.n_m_k[m][z] -= 1;
        test.s_w_k[z] -= 1;
        test.s_w_d[m] -= 1;

        int K = labels.size();

        for (int k = 0; k < K; k++) {
            z = labels.get(k);

            // Counts from Training as prior
            test.p[k] = (test.n_m_k[m][z] + train._alpha) *
                        (train._n_w_k[_w][z] + test.n_w_k[w][z] + train._beta) /
                        (train._s_w_k[z] + test.s_w_k[z] + train._V * train._beta);
        }
        for (int k = 1; k < K; k++) {
            test.p[k] += test.p[k - 1];
        }
        double p = Math.random() * test.p[K - 1];

        for (z = 0; z < K; z++) {
            if (test.p[z] > p)
                break;
        }
        z = labels.get(z);

        test.n_w_k[w][z] += 1;
        test.n_m_k[m][z] += 1;
        test.s_w_k[z] += 1;
        test.s_w_d[m] += 1;
        test.z_m.get(m).get(s).set(n, z);
    }

    /**
     * Topic/word distribution.
     */
    protected void computePhi() {
        for (int k = 0; k < test.K; k++) {
            for (int w = 0; w < test.V; w++) {
                int _w = test.lid2gid.get(w);

                // Counts from Training as prior
                test.phi[k][w] = (train._n_w_k[_w][k] + test.n_w_k[w][k] + train._beta) /
                                 (test.s_w_k[k] + train._s_w_k[k] + train._V * test.beta);
            }
        }
    }
}
