public abstract class Sampling {
    protected Options o;
    protected Model train;
    int samples = 1;

    public Sampling(Options o) {
        this.o = o;
    }

    /**
     * Document/topic distribution.
     */
    protected void computeTheta(Model model) {
        for (int m = 0; m < model.M; m++) {
            for (int k  = 0; k < model.K; k++) {
                model.theta[m][k] = ((model.n_d_k[m][k] + model.alpha) /
                                     (model.s_w_d[m] + model.K * model.alpha));
            }
        }
    }

    protected void printStats(Model model) {
        System.out.println(String.format("Alpha: %f\nBeta: %f\nIterations: %d\nK: %d\nV: %d\nM: %d\n",
                                         model.alpha, model.beta, o.iter, model.K, model.V, model.M));
    }

    /**
     * Gibbs sampling.
     */
    protected void estimate(Model model) {
        for (int i = 1; i < o.iter; i++) {
            if (i % o.step == 0) { System.out.println("Iteration: " + i); }

            for (int m = 0; m < model.M; m++) {
                for (int n = 0; n < model.docs.get(m).size(); n++) {
                    sample(m, n);
                }
            }
            if (o.method == "extended") extended(i, model);
        }
    }

    /**
     * Computes theta and phi after a burn in period with a certain lag.
     *
     * @param i iteration
     * @param model test or training model
     */
    protected void extended(int i, Model model) {
        if ((i == o.iter - 1) || (((i > o.burn) && (i % o.lag == 0)))) {
            double Kalpha = model.K * model.alpha;
            double Vbeta = model.V * model.beta;

            for (int m = 0; m < model.M; m++) {
                for (int k = 0; k < model.K; k++) {
                    if (samples > 1) model.theta[m][k] *= samples - 1;

                    model.theta[m][k] += ((model.n_d_k[m][k] + model.alpha) / (model.s_w_d[m] + Kalpha));

                    if (samples > 1) model.theta[m][k] /= samples;
                }
            }
            for (int k = 0; k < model.K; k++) {
                for (int w = 0; w < model.V; w++) {
                    if (o.estimation) {
                        if (samples > 1) model.phi[k][w] *= samples - 1;

                        model.phi[k][w] += ((model.n_w_k[w][k] + model.beta) / (model.s_w_k[k] + Vbeta));

                        if (samples > 1) model.phi[k][w] /= samples;
                    }
                    else if (o.inference) {
                        Vbeta = train.V * model.beta;

                        if (model.lid2gid.containsKey(w)) {
                            int _w = model.lid2gid.get(w);

                            if (samples > 1) model.phi[k][w] *= samples - 1;

                            model.phi[k][w] += (train.n_w_k[_w][k] + model.n_w_k[w][k] + model.beta) / (train.s_w_k[k] + model.s_w_k[k] + Vbeta);

                            if (samples > 1) model.phi[k][w] /= samples;
                        }
                    }
                }
            }
            samples++;
        }
    }

    /**
     * Computes perplexity.
     *
     * Blei 2003:16 */
    protected double computePerplexity(Model model) {
        double loglik = 0.0;
        int N = 0;

        for (int m = 0; m < model.docs.size(); m++) {
            for (int n = 0; n < model.docs.get(m).size(); n++) {
                double sum = 0.0;
                N++;

                for (int k = 0; k < model.K; k++) {
                    int w = model.docs.get(m).get(n);

                    sum += model.theta[m][k] * model.phi[k][w];
                }
                loglik += Math.log(sum);
            }
        }
        return Math.exp(-loglik / N);
    }

    abstract protected void computePhi();
    abstract protected void sample(int m, int n);
}