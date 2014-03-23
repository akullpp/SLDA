import java.io.*;


/**
 * Estimation of training model.
 */
public class Estimation extends Sampling {

    /** Constructor. */
    public Estimation(Options o) {
        super(o);

        train = new Model(o);

        printStats(train);
        estimate(train);

        if (o.method == "simple") {
            computeTheta(train);
            computePhi();
        }
        saveModel();

        train.collectData("train");

        System.out.println(computePerplexity(train));
    }

    /**
     * Serialize model for loading in inference.
     */
    public void saveModel() {
        FileOutputStream fos;

        try {
            fos = new FileOutputStream("training.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(train);
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Gibbs sampling.
     */
    protected void sample(int m, int n) {
        int z = train.z_d[m].get(n);
        int w = train.docs.get(m).get(n);

        train.n_w_k[w][z] -= 1;
        train.n_d_k[m][z] -= 1;
        train.s_w_k[z] -= 1;
        train.s_w_d[m] -= 1;

            int K = (o.llda) ? train.labels.get(m).size() : train.K;

        for (int k = 0; k < K; k++) {
            z = (o.llda) ? train.labels.get(m).get(k) : k;

            train.p[k] = (train.n_d_k[m][z] + train.alpha) *
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
        if (o.llda) { z = train.labels.get(m).get(z); }

        train.n_w_k[w][z] += 1;
        train.n_d_k[m][z] += 1;
        train.s_w_k[z] += 1;
        train.s_w_d[m] += 1;
        train.z_d[m].set(n, z);
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
