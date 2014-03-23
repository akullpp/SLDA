/**
 * The following class is based on a method from Gregor Heinrich's Infinite LDA which is distributed under GPLv2 which
 * can be found in license_gpl.txt.
 **/
import java.util.ArrayList;
import java.util.Random;


/** Sampling from a Dirichlet via random samples from a Gamma. */
public class Sampler {
    public Random r;
    Model model;

    public Sampler(Model model) {
        r = new Random();
        this.model = model;

        initialize();
    }

    /** Initializes the sentence-formality structures. */
    public void initialize() {
        for (int m = 0; m < model.M; m++) {
            ArrayList<Integer> f_m = new ArrayList<Integer>();

            for (int s = 0; s < model.docs.get(m).size(); s++) {
                int f_s = sampleFormality(model.alphas);

                model.s_m_f[m][f_s]++;

                f_m.add(f_s);
            }
            model.f_m_s.add(f_m);
        }
    }

    /**
     * Sample from Dirichlet via sampling from Gamma.
     *
     * @param alphas vector of K alpha values
     * @return formality label
     */
    public int sampleFormality(double[] alphas) {
        double sum = 0.0;

        double[] gamma = new double[alphas.length];

        for (int i = 0; i < gamma.length; i++) {
            gamma[i] = sampleGamma(alphas[i]);
        }
        for (int i = 0; i < gamma.length; i++) {
            sum += gamma[i];
        }
        for (int i = 0; i < gamma.length; i++) {
            gamma[i] /= sum;
        }
        for (int i = 1; i < gamma.length; i++) {
            gamma[i] += gamma[i - 1];
        }
        double p = Math.random() * gamma[gamma.length - 1];

        int f;

        for (f = 0; f < model.K; f++) {
            if (gamma[f] > p)
                break;
        }
        return f;
    }

    /**
     * Generates sample from Gamma.
     *
     * The random sample depends on the alpha value which is further explained in the thesis.
     *
     * @param a scalar alpha
     * @return random sample from a gamma
     */
    public double sampleGamma(double a) {
        double b, c, d;
        double u, v, w, x, y, z;

        if (a == 1.0) {
            return -Math.log(r.nextDouble());
        }
        else if (a > 1.0) {
            b = a - 1;
            c = 3.0 * a - 0.75;

            while (true) {
                u = r.nextDouble();
                v = r.nextDouble();
                w = u * (1.0 - u);
                y = Math.sqrt(c / w) * (u - 0.5);
                x = b + y;

                if (x >= 0) {
                    z = 64.0 * w * w * w * v * v;
                    assert z > 0 && b != 0 && x / b > 0;

                    if ((z <= (1.0 - 2.0 * y * y /x)) ||
                            (Math.log(z) <= 2.0 * (b * Math.log(x / b) - y))) {
                        return x;
                    }
                }
            }
        }
        else {
            c = 1.0 / a;
            d = 1.0 / (1.0 - a);

            while (true) {
                x = Math.pow(r.nextDouble(), c);
                y = x + Math.pow(r.nextDouble(), d);

                if (y <= 1.0) {
                    assert y != 0 && x / y > 0;
                    return -Math.log(r.nextDouble()) * x / y;
                }
            }
        }
    }
}

