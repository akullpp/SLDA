import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;


/**
 * Inference new documents, uses training as prior.
 */
public class Inference extends Sampling {
    public Model test;

    public Inference(Options o) {
        super(o);

        train = loadModel();
        test = new Model(o, train);

        printStats(test);
        estimate(test);

        if (o.method == "simple") {
            computePhi();
            computeTheta(test);
        }
        test.collectData("test");
    }

    /**
     * Load Model from training.
     *
     * @return Training model.
     */
    public Model loadModel() {
        FileInputStream fis;
        Model m = null;

        try {
            fis = new FileInputStream("training.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            m = (Model) ois.readObject();
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return m;
    }

    /**
     * Gibbs sampling.
     */
    protected void sample(int m, int n) {
        int z = test.z_d[m].get(n);
        int w = test.docs.get(m).get(n);
        int _w = test.lid2gid.get(w);

        test.n_w_k[w][z] -= 1;
        test.n_d_k[m][z] -= 1;
        test.s_w_k[z] -= 1;
        test.s_w_d[m] -= 1;

        for (int k = 0; k < test.K; k++) {
            test.p[k] = (test.n_d_k[m][k] + train.alpha) *
                    (train.n_w_k[_w][k] + test.n_w_k[w][k] + train.beta) /
                    (train.s_w_k[k] + test.s_w_k[k] + train.V * train.beta);
        }
        for (int k = 1; k < test.K; k++) {
            test.p[k] += test.p[k - 1];
        }
        double p = Math.random() * test.p[test.K - 1];

        for (z = 0; z < test.K; z++) {
            if (test.p[z] > p)
                break;
        }
        test.n_w_k[w][z] += 1;
        test.n_d_k[m][z] += 1;
        test.s_w_k[z] += 1;
        test.s_w_d[m] += 1;
        test.z_d[m].set(n, z);
    }

    /**
     * Topic/word distribution.
     */
    protected void computePhi() {
        for (int k = 0; k < test.K; k++) {
            for (int w = 0; w < test.V; w++) {
                int _w = test.lid2gid.get(w);

                test.phi[k][w] = (train.n_w_k[_w][k] + test.n_w_k[w][k] + train.beta) /
                                 (test.s_w_k[k] + train.s_w_k[k] + train.V * test.beta);
            }
        }
    }
}
