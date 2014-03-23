/** Abstract class for common methods and structures for estimation and inference.
 */
public abstract class Sampling {
    protected Options o;
    protected int samples;

    public Sampling(Options o) {
        this.o = o;
    }

    /**
     * Document/topic distribution.
     */
    protected void computeTheta(Model model) {
        for (int m = 0; m < model.M; m++) {
            for (int k  = 0; k < model.K; k++) {
                model.theta[m][k] = ((model.n_m_k[m][k] + model.alpha) /
                                     (model.s_w_d[m] + model.K * model.alpha));
            }
        }
    }

    protected void printStats(Model model) {
        if (o.inference) {
            System.out.println(String.format("Mode: Inference\nGamma: %f", o.gamma));
        }
        else{
            System.out.println("Mode: Estimation");
        }

        System.out.println(String.format("Alpha: %f\nBeta: %f\nIterations: %d\nK: %d\nV: %d\nM: %d",
                                         model.alpha, model.beta, o.iter, model.K, model.V, model.M));

        if (o.method == "extended") {
            System.out.println(String.format("Extended matrix calculation method\nBurn-in: %d\nLag: %d",
                    o.burn, o.lag));
        }
        else {
            System.out.println("Simple matrix calculation method");
        }
        if (o.v && o.inference) {
            System.out.println("Resampling f active");
        }
        else if (!o.v && o.inference) {
            System.out.println("Resampling f inactive");
        }
        if (o.bg) {
            System.out.println("Background topics active\n");
        }
        else {
            System.out.println("Background topics inactive\n");
        }
    }

    /**
     * Computes perplexity.
     *
     * Blei 2003:16 */
    protected void printPerplexity(Model model) {
        double loglik = 0.0;
        int N = 0;

        for (int m = 0; m < model.docs.size(); m++) {
            for (int s = 0; s < model.docs.get(m).size(); s++) {
                for (int n = 0; n < model.docs.get(m).get(s).size(); n++) {
                    double sum = 0.0;
                    int w = model.docs.get(m).get(s).get(n);

                    N++;

                    for (int k = 0; k < model.K; k++) {
                        sum += model.theta[m][k] * model.phi[k][w];
                    }
                    loglik += Math.log(sum);
                }
            }
        }
        System.out.println("Loglik: " + loglik);
        System.out.println("Perplexity: " + Math.exp(-loglik / N));
    }

    /**
     * Re-estimation of hyperparameters.
     *
     * The function was taken from Mallet and modified. Therefore it is under the license of CPL 1.0 which can be found
     * in the attached license_cpl.txt.
     *
     * Minka 2012:5ff. */
    protected void estimateAlpha(Model model) {
        int[] s_d_k = new int[model.n_m_k.length];
        double[] alphas_new = new double[model.alphas.length];

        for (int m = 0; m < model.M; m++) {
            int sum = 0;

            for (int k = 0; k < model.K; k++) { sum += model.n_m_k[m][k]; }

            s_d_k[m] = sum;
        }

        for (int i = 0; i < o.iter; i++) {
            double sum = 0.0;
            double den = 0.0;

            for (int k = 0; k < model.K; k++) { sum += model.alphas[k]; }
            for (int m = 0; m < model.M; m++) { den += s_d_k[m] / (s_d_k[m] - 1 + sum); }

            for (int k = 0; k < model.K; k++) {
                double num = 0.0;

                for (int m = 0; m < model.M; m++)
                    num += model.n_m_k[m][k] / (model.n_m_k[m][k] - 1 + model.alphas[k]);
                alphas_new[k] = model.alphas[k] * num / den;
            }
            model.alphas = alphas_new;
        }
    }

    abstract protected void estimate();
    abstract protected void computePhi();
}