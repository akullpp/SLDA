import java.io.*;
import java.util.*;


public class Model implements Serializable {
    public transient Options o;
    // word : id
    public HashMap<String, Integer> word2id;
    // training word : training id
    public HashMap<String, Integer> _word2id;
    // id : word
    public HashMap<Integer, String> id2word;
    // testing id : training id
    public HashMap<Integer, Integer> lid2gid;
    // docs[m][n] = document m word n
    public ArrayList<ArrayList<Integer>> docs;
    // labels[m] = labeleset k for document m
    public ArrayList<ArrayList<Integer>> labels;
    // number of documents
    public int M;
    // number of types
    public int V;
    // number of topics
    public int K;
    // dirichlet alpha for theta
    public double alpha;
    // asymmetric alphas for estimation, length K
    public double[] alphas;
    // dirichlet beta for phi
    public double beta;
    // theta[m][k] = probability of topic k in document m
    public double[][] theta;
    // phi[k][n] = probability of word n in topic k
    public double[][] phi;
    // n_w_k[w][k] = counts for word n under topic k
    public int[][] n_w_k;
    // n_d_k[m][k] = counts for topic k in document m
    public int[][] n_d_k;
    // s_w_k[k] = count of word n under topic k
    public int[] s_w_k;
    // s_w_d[m] = count of words n in document m
    public int[] s_w_d;
    // p[k] = probability of topic k
    public double[] p;
    // z_d[m] = topic assignments in document m
    public ArrayList<Integer>[] z_d;

    /** Constructor.
     *
     * @param o options
     * */
    public Model(Options o) {
        this(o, null);
    }

    /**
     * Constructor for testing.
     *
     * @param o options
     * @param train trainings model
     */
    public Model(Options o, Model train) {
        this.o = o;
        this.alpha = o.alpha;
        this.beta = o.beta;
        this.K = o.K;

        word2id = new HashMap<String, Integer>();
        id2word = new HashMap<Integer, String>();
        docs = new ArrayList<ArrayList<Integer>>();

        if (train != null) {
            lid2gid = new HashMap<Integer, Integer>();
            _word2id = train.word2id;
        }
        if (o.llda) {
            labels = new ArrayList<ArrayList<Integer>>();
        }
        readCorpus(o.corpus);
        initModel();
    }

    /** Initializes datastructures. */
    public void initModel() {
        M = docs.size();
        V = word2id.size();
        p = new double[K];
        theta = new double[M][K];
        phi = new double[K][V];
        n_w_k = new int[V][K];
        n_d_k = new int[M][K];
        s_w_k = new int[K];
        s_w_d = new int[M];
        z_d = new ArrayList[M];

        initTopics();
    }

    /** Initializes topics randomly. */
    public void initTopics() {
        Random r = new Random();

        for (int m = 0; m < M; m ++) {
            z_d[m] = new ArrayList<Integer>();
            ArrayList<Integer> doc = docs.get(m);
            int N = doc.size();

            for (int n = 0; n < N; n++) {
                int k = r.nextInt(K);
                z_d[m].add(k);
                n_w_k[doc.get(n)][k]++;
                n_d_k[m][k]++;
                s_w_k[k]++;
            }
            s_w_d[m] = N;
        }
    }

    /** Read the training or testing corpus. */
    public void readCorpus(String filename) {
        String line;
        int lll = 0;

        try {
            FileInputStream fis = new FileInputStream(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);

            while ((line = br.readLine()) != null) {
                line = line.trim();
                lll++;

                if (o.llda && o.estimation) {
                    ArrayList<Integer> labelArray = new ArrayList<Integer>();
                    String[] doc = line.split("] ");

                    int label = Integer.parseInt(doc[0].replace("[", ""));

                    if (label != 2 && o.bg) {
                        labelArray.add(2);
                    }
                    labelArray.add(label);
                    labels.add(labelArray);
                    line = doc[1];

                }
                String words[] = line.split(" ");
                ArrayList<Integer> ids = new ArrayList<Integer>();

                for (String word : words) {
                    if (_word2id == null) {
                        int id = word2id.size();

                        if (word2id.containsKey(word)) {
                            id  = word2id.get(word);
                        }
                        else {
                            word2id.put(word, id);
                        }
                        if (!id2word.containsKey(id)) {
                            id2word.put(id, word);
                        }
                        ids.add(id);
                    }
                    else {
                        if (_word2id.containsKey(word)) {
                            int _id = _word2id.get(word);
                            int id  = word2id.size();

                            if (word2id.containsKey(word)) {
                                id  = word2id.get(word);
                            }
                            else {
                                word2id.put(word, id);
                            }
                            if (!id2word.containsKey(id)) {
                                id2word.put(id, word);
                            }
                            lid2gid.put(id, _id);
                            ids.add(id);
                        }
                    }
                }
                docs.add(ids);
            }
            M = docs.size();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /** Save assignments, theta and phi to readable files. */
    public void collectData(String name) {
        saveAssignments(name);
        saveTheta(name);
        savePhi(name);
    }

    /** Save the assignments.
     *
     * For each document: word_1:topic_k ... word_n:topic_k
     */
    public void saveAssignments(String name) {
        try {
            FileOutputStream fos = new FileOutputStream("./data/" + name + "_" + "assignments.txt");
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            for (int i = 0; i < M; i++) {
                for (int j = 0; j < docs.get(i).size(); j++) {
                    bw.write(id2word.get(docs.get(i).get(j)) + ":" + z_d[i].get(j) + " ");
                }
                bw.write("\n");
            }
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /** Save theta.
     *
     * For each document: topic_0:theta_0 ... topic_k:theta_k
     */
    public void saveTheta(String name) {
        try {
            FileOutputStream fos = new FileOutputStream("./data/" + name + "_" + "theta.txt");
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            for (int i = 0; i < M; i++) {
                for (int j = 0; j < K; j++) {
                    bw.write(j + ":" + theta[i][j] + " ");
                }
                bw.write("\n");
            }
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /** Save phi.
     *
     * Saves 50 most common terms under topic k:

     * Topic 0:
     * term 0
     * ...
     * term 49
     *
     * ...
     *
     * Topic k:
     * term 0
     * ...
     * term 49
     */
    public void savePhi(String name) {
        class Tuple implements Comparable<Tuple> {
            public Object first;
            public Comparable second;

            public Tuple(Object o, Comparable c) {
                first = o;
                second = c;
            }

            public int compareTo(Tuple t) {
                return -this.second.compareTo(t.second);
            }
        }

        try {
            FileOutputStream fos = new FileOutputStream("./data/" + name + "_" + "phi.txt");
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            for (int i = 0; i < K; i++) {
                ArrayList<Tuple> p = new ArrayList<Tuple>();

                for (int j = 0; j < V; j++) {
                    p.add(new Tuple(j, phi[i][j]));
                }
                Collections.sort(p);
                bw.write("Topic " + i + "\n");

                for (int k = 0; k < 50; k++) {
                    bw.write(id2word.get(p.get(k).first) + "\t" + p.get(k).second + "\n");
                }
                bw.write("\n");
            }
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
