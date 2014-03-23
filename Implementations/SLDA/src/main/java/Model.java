import java.io.*;
import java.util.*;


/** Standard model structures for SLDA. */
public class Model {
    // Options
    public Options o;
    // word : id
    public HashMap<String, Integer> word2id;
    // training word : training id
    public HashMap<String, Integer> _word2id;
    // id : word
    public HashMap<Integer, String> id2word;
    // testing id : training id
    public HashMap<Integer, Integer> lid2gid;
    // docs[m][s][n] = word n in sentence s in document m
    public ArrayList<ArrayList<ArrayList<Integer>>> docs;
    // labels[m][s][l] = l-th label in sentence s in document m
    public ArrayList<ArrayList<ArrayList<Integer>>> labels;
    // Formality label f for sentence s in document m
    public ArrayList<ArrayList<Integer>> f_m_s;
    // Sum of formality label f in document m
    public int[][] s_m_f;
    // n_l_c[l] count of label l in corpus
    public int[] n_l_c;
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
    public int[][] n_m_k;
    // s_w_k[k] = count of word n under topic k
    public int[] s_w_k;
    // s_w_d[m] = count of words n in document m
    public int[] s_w_d;
    // p[k] = probability of topic k
    public double[] p;
    // z_m[m][s] = assignment of topic k to word n in in sentence s in document m
    public ArrayList<ArrayList<ArrayList<Integer>>> z_m;
    // Lost words due to fixed V
    int lost = 0;

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
     * @param _word2id training's word2id
     */
    public Model(Options o, HashMap<String, Integer> _word2id) {
        this.o = o;
        this.alpha = o.alpha;
        this.beta = o.beta;
        this.K = o.K;

        word2id = new HashMap<String, Integer>();
        id2word = new HashMap<Integer, String>();
        docs = new ArrayList<ArrayList<ArrayList<Integer>>>();

        if (_word2id != null) {
            lid2gid = new HashMap<Integer, Integer>();
            f_m_s = new ArrayList<ArrayList<Integer>>();
            this._word2id = _word2id;
        }
        labels = new ArrayList<ArrayList<ArrayList<Integer>>>();
        readCorpus(o.folder + o.corpus);
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
        n_m_k = new int[M][K];
        s_w_k = new int[K];
        s_w_d = new int[M];
        s_m_f = new int[M][K];
        z_m = new ArrayList<ArrayList<ArrayList<Integer>>>();

        initTopics();
    }

    /** Initializes topics randomly. */
    public void initTopics() {
        Random r = new Random();

        for (int m = 0; m < M; m++) {
            ArrayList<ArrayList<Integer>> documentTopicAssignments = new ArrayList<ArrayList<Integer>>();
            int N = 0;

            for (int s = 0; s < docs.get(m).size(); s++) {
                ArrayList<Integer> sentenceTopicAssignments = new ArrayList<Integer>();
                ArrayList<Integer> sentence = docs.get(m).get(s);
                N += sentence.size();

                for (int n = 0; n < sentence.size(); n++) {
                    int k = r.nextInt(K);

                    sentenceTopicAssignments.add(k);
                    n_w_k[sentence.get(n)][k]++;
                    n_m_k[m][k]++;
                    s_w_k[k]++;
                }
                documentTopicAssignments.add(sentenceTopicAssignments);
            }
            z_m.add(documentTopicAssignments);
            s_w_d[m] = N;
        }
    }

    /** Read the training or testing corpus. */
    public void readCorpus(String filename) {
        try {
            String line;
            n_l_c = new int[K];

            FileInputStream fis = new FileInputStream(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);

            while ((line = br.readLine()) != null) {
                ArrayList<ArrayList<Integer>> docSentences = new ArrayList<ArrayList<Integer>>();
                ArrayList<ArrayList<Integer>> sentenceLabels = new ArrayList<ArrayList<Integer>>();
                String[] sentences = line.split("\\.");

                for (String sentence : sentences) {
                    ArrayList<Integer> sentenceWords = new ArrayList<Integer>();
                    ArrayList<Integer> labelArray = new ArrayList<Integer>();
                    String[] words;

                    if (o.estimation) {
                        String[] tmp = sentence.split("\\|");
                        words = tmp[1].replace("]", "").split(" ");

                        int label = Integer.parseInt(tmp[0].replace("[", ""));

                        labelArray.add(label);

                        if (!o.bg && label != 2) {
                            labelArray.add(2);
                        }
                        else if (o.bg && label < 2) {
                            for (int k = 2; k < K; k++) {
                                labelArray.add(k);
                            }
                        }
                        for (int i = 0; i < labelArray.size(); i++) {
                            n_l_c[labelArray.get(i)]++;
                        }
                    }
                    else {
                        words = sentence.replace("]", "").split(" ");
                    }
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
                            sentenceWords.add(id);
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
                                sentenceWords.add(id);
                            }
                            else {
                                lost++;
                            }
                        }
                    }
                    sentenceLabels.add(labelArray);
                    docSentences.add(sentenceWords);
                }
                labels.add(sentenceLabels);
                docs.add(docSentences);
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

        if (name  == "test") {
            saveFormality();
        }
    }

    public void saveFormality() {
        try {
            FileOutputStream fos = new FileOutputStream(o.folder + "result");
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            for (int m = 0; m < M; m++) {
                for (Integer i : f_m_s.get(m)) {
                    bw.write(i + " ");
                }
                bw.write("\n");
            }
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /** Save the assignments.
     *
     * For each document: word_1:topic_k ... word_n:topic_k
     */
    public void saveAssignments(String name) {
        try {
            FileOutputStream fos = new FileOutputStream(o.folder + name + "_" + "assignments.txt");
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            for (int m = 0; m < M; m++) {
                for (int s = 0; s < docs.get(m).size(); s++) {
                    bw.write("[");

                    for (int n = 0; n < docs.get(m).get(s).size(); n++) {
                        bw.write(id2word.get(docs.get(m).get(s).get(n)) + ":" + z_m.get(m).get(s).get(n) + " ");
                    }
                    bw.write("]");
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
            FileOutputStream fos = new FileOutputStream(o.folder + name + "_" + "theta.txt");
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
            FileOutputStream fos = new FileOutputStream(o.folder + name + "_" + "phi.txt");
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
