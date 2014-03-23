from codecs import open as copen
from os import listdir
from re import match, sub, findall
from structures import getStopwords
from utility import loadStruct
from xml.dom.minidom import parse


def convertFormat(f_lda, f_proj, slda=True):
    """ Format to either SLDA or LLDA.

    LLDA: [l_1] w_1 ... w_n
    SLDA: [l_1 ... l_k|w_1 ... w_n] ... [l_1 ... l_k|w_1 ... w_n]
    """
    print "Converting to SLDA format"

    formal = 0
    informal = 0
    neutral = 0
    double = 0
    nn_lost = 0
    on_lost = 0
    no_lost = 0
    total = 0

    with copen(f_lda, "w", encoding="utf-8") as lda_f:
        for f in listdir(f_proj):
            with copen(f_proj + f, encoding="utf-8") as proj_f:
                for line in proj_f:
                    m = match(".*>(.+)</.*", line)

                    if m:
                        clean = m.group(1).strip().split(" ")

                        if len(clean) > 1:
                            sentence = " ".join(clean)

                            if "f=\"0\" i=\"0\"" in line:
                                if not "you" in clean:
                                    neutral += 1
                                    total += 1
                                    if slda:
                                        lda_f.write("[2|%s]." % sentence)
                                    else:
                                        lda_f.write("[2] %s\n" % sentence)
                                else:
                                    nn_lost += 1
                            elif "f=\"1\" i=\"0\"" in line:
                                if not "you" in clean:
                                    on_lost += 1
                                else:
                                    formal += 1
                                    total += 1
                                    if slda:
                                        lda_f.write("[1|%s]." % sentence)
                                    else:
                                        lda_f.write("[1] %s\n" % sentence)
                            elif "f=\"0\" i=\"1\"" in line:
                                if not "you" in clean:
                                    no_lost += 1
                                else:
                                    informal += 1
                                    total += 1
                                    if slda:
                                        lda_f.write("[0|%s]." % sentence)
                                    else:
                                        lda_f.write("[0] %s\n" % sentence)
                            elif "f=\"1\" i=\"1\"" in line:
                                double += 0
                if slda:
                    lda_f.write("\n")
    print "Formal: %d" % formal
    print "Informal: %d" % informal
    print "Neutral: %d" % neutral
    print "Formal + Informal: %d" % double
    print "You in Neutral: %d" % nn_lost
    print "You not in Formal: %d" % on_lost
    print "You not in Informal: %d" % no_lost
    print "Total sentences %d" % total


def convertGutenberg(f_root, f_type, f_gproj, slda=True, filter=True):
    """ Converts the Gutenberg corps to SLDA or LLDA format. """
    stopwords = getStopwords()
    formal = 0
    informal = 0
    neutral = 0
    double = 0
    nn_lost = 0
    on_lost = 0
    no_lost = 0
    total = 0
    form = "SLDA"
    f_ttype = f_type

    if not slda:
        form = "LLDA"

    if f_type == "test":
        f_ttype = "gold"

    print "Converting Gutenberg corpus to %s format for %sing" % (form, f_type)

    with copen(f_root + f_ttype, "w", encoding="utf-8") as lda_f:
        for f in listdir(f_gproj + f_type):
            with copen(f_gproj + f_type + "/" + f, encoding="utf-8") as proj_f:
                for line in proj_f:
                    m = match(".*>(.+)</.*", line)

                    if m:
                        sentence = m.group(1).strip().split(" ")
                        clean = []

                        for word in sentence:
                            word = word.lower()

                            if filter:
                                if word not in stopwords and len(word) > 1 and word.isalpha():
                                    clean.append(word)
                            else:
                                if word.isalpha():
                                    clean.append(word)

                        if len(clean) > 1:
                            sentence = " ".join(clean)

                            if "f=\"0\" i=\"0\"" in line:
                                if not "you" in clean:
                                    neutral += 1
                                    total += 1
                                    if slda:
                                        lda_f.write("[2|%s]." % sentence)
                                    else:
                                        lda_f.write("[2] %s\n" % sentence)
                                else:
                                    nn_lost += 1
                            elif "f=\"1\" i=\"0\"" in line:
                                if not "you" in clean:
                                    on_lost += 1
                                else:
                                    formal += 1
                                    total += 1
                                    if slda:
                                        lda_f.write("[1|%s]." % sentence)
                                    else:
                                        lda_f.write("[1] %s\n" % sentence)
                            elif "f=\"0\" i=\"1\"" in line:
                                if not "you" in clean:
                                    no_lost += 1
                                else:
                                    informal += 1
                                    total += 1
                                    if slda:
                                        lda_f.write("[0|%s]." % sentence)
                                    else:
                                        lda_f.write("[0] %s\n" % sentence)
                            elif "f=\"1\" i=\"1\"" in line:
                                double += 0
                lda_f.write("\n")
    print "Formal: %d" % formal
    print "Informal: %d" % informal
    print "Neutral: %d" % neutral
    print "Formal + Informal: %d" % double
    print "You in Neutral: %d" % nn_lost
    print "You not in Formal: %d" % on_lost
    print "You not in Informal: %d" % no_lost
    print "Total sentences %d" % total
    print


def splitText(f_lda, a, f_train, f_gold):
    """ Splitting the text in a certain percentage. """
    with copen(f_lda, encoding="utf-8") as corpus_f:
        corpus = corpus_f.readlines()

    x = len(corpus) / 100 * a

    train = corpus[0:x]
    gold = corpus[x:]

    with copen(f_train, "w", encoding="utf-8") as train_f:
        for line in train:
            train_f.write(line)

    with copen(f_gold, "w", encoding="utf-8") as gold_f:
        for line in gold:
            gold_f.write(line)


def removeLabels(f_gold, f_test, slda=True):
    """ Remove labels from the gold for inference.

    Preprocessing:
    tail -n $(25%) corpus > gold
    sed -i "$(total - 25%),$(total)d"
    """
    print "Removing labels"

    with copen(f_gold, "r", encoding="utf-8") as gold_f:
        with copen(f_test, "w", encoding="utf-8") as test_f:
            for line in gold_f:
                if not slda:
                    test_f.write(sub("\[[0-9]\] ", "", line))
                else:
                    line = sub("\]", "", line)
                    test_f.write(sub("\[[0-9]\|", "", line))


def evaluateLLDA(f_gold, f_results):
    """ Evaluation Metrics for LLDA. """
    print "Starting LLDA evaluation"

    counts = {"0": [0., 0., 0., 0.], "1": [0., 0., 0., 0.], "2": [0., 0., 0., 0.]}
    conf = [[0, 0, 0], [0, 0, 0], [0, 0, 0]]

    with copen(f_gold, encoding="utf-8") as gold_f:
        with open(f_results) as results_f:
            for g, r in zip(gold_f, results_f):
                g_topic = match(".*\[([0-9])\].*", g).group(1)
                r_topic = -1
                p_max = 0.0

                for t in r.split(" "):
                    if len(t) > 1:
                        tid, p = t.split(":")
                        p = float(p)

                        if p > p_max:
                            p_max = p
                            r_topic = tid

                for k in counts:
                        if k == r_topic:
                            if k == g_topic:
                                # TP
                                conf[int(k)][int(g_topic)] += 1.
                                counts[k][0] += 1.
                            elif k != g_topic:
                                # FP
                                conf[int(k)][int(g_topic)] += 1.
                                counts[k][1] += 1
                        elif k != r_topic:
                            if k == g_topic:
                                # FN
                                counts[k][2] += 1.
                            elif k != g_topic:
                                # TN
                                counts[k][3] += 1.

    for k in counts:
        precision = (counts[k][0] / (counts[k][0] + counts[k][1]))
        recall = (counts[k][0] / (counts[k][0] + counts[k][2]))

        print "Precision of %s:\t\t\t%.2f" % (k, round((precision * 100), 2))
        print "Recall of %s:\t\t\t%.2f" % (k, round((recall * 100), 2))
        print "Specificity of %s:\t\t\t%.2f" % (k, round(((counts[k][3] / (counts[k][3] + counts[k][1])) * 100), 2))
        print "Accuracy of %s:\t\t\t%.2f" % (k, round((((counts[k][0] + counts[k][3]) / (counts[k][0] + counts[k][1] + counts[k][2] + counts[k][3])) * 100), 2))
        print "F1-Score of %s:\t\t\t%.2f" % (k, round((2 * ((precision * recall) / (precision + recall))), 2))
        print


def evaluateSLDA(f_gold, f_results):
    """ Evaluation Metrics for SLDA. """
    print "Starting evaluation"

    counts = {"0": [0., 0., 0., 0.], "1": [0., 0., 0., 0.], "2": [0., 0., 0., 0.]}
    conf = [[0, 0, 0], [0, 0, 0], [0, 0, 0]]

    with copen(f_gold, encoding="utf-8") as gold_f:
        with open(f_results) as results_f:
            for g, r in zip(gold_f, results_f):
                g_topics = findall("\[([0-9])\|", g)
                r_topics = r.split()

                for gold, pred in zip(g_topics, r_topics):
                    for k in counts:
                        if k == pred:
                            if k == gold:
                                # TP
                                conf[int(k)][int(gold)] += 1.
                                counts[k][0] += 1.
                            elif k != gold:
                                # FP
                                conf[int(k)][int(gold)] += 1.
                                counts[k][1] += 1
                        elif k != pred:
                            if k == gold:
                                # FN
                                counts[k][2] += 1.
                            elif k != gold:
                                # TN
                                counts[k][3] += 1.


    for k in counts:
        precision = (counts[k][0] / (counts[k][0] + counts[k][1]))
        recall = (counts[k][0] / (counts[k][0] + counts[k][2]))

        print "Precision of %s:\t\t\t%.2f" % (k, round((precision * 100), 2))
        print "Recall of %s:\t\t\t%.2f" % (k, round((recall * 100), 2))
        print "Specificity of %s:\t\t\t%.2f" % (k, round(((counts[k][3] / (counts[k][3] + counts[k][1])) * 100), 2))
        print "Accuracy of %s:\t\t\t%.2f" % (k, round((((counts[k][0] + counts[k][3]) / (counts[k][0] + counts[k][1] + counts[k][2] + counts[k][3])) * 100), 2))
        print "F1-Score of %s:\t\t\t%.2f" % (k, round((2 * ((precision * recall) / (precision + recall))), 2))
        print





def convertSLDA2LLDA(f_train, f_test, f_gold):
    """ Converts SLDA to LLDA format. """
    with copen(f_train, encoding="utf-8") as slda_f:
        with copen(f_train + "_llda", "w", encoding="utf-8") as llda_f:
            for sentence in slda_f.read().split("."):
                sentence = sentence.strip()
                if len(sentence) > 1:
                    llda_f.write(sentence.replace("]", "").replace("|", "] ") + "\n")

    with copen(f_gold, encoding="utf-8") as slda_f:
        with copen(f_gold + "_llda", "w", encoding="utf-8") as llda_f:
            for sentence in slda_f.read().split("."):
                sentence = sentence.strip()
                if len(sentence) > 1:
                    llda_f.write(sentence.replace("]", "").replace("|", "] ") + "\n")

        with copen(f_test, encoding="utf-8") as slda_f:
            with copen(f_test + "_llda", "w", encoding="utf-8") as llda_f:
                for sentence in slda_f.read().split("."):
                    sentence = sentence.strip()
                    if len(sentence) > 1:
                        llda_f.write(sentence + "\n")


def recoverUnknown(f_unknown_p, f_unknown, f_align_p, f_clean):
    """ Shows us the Foreign sentence that produced no formality while the English sentence had a "you". """
    print "Recovering unknown sentences"

    unknown = loadStruct(f_unknown_p)
    align = loadStruct(f_align_p)

    with copen(f_unknown, "w", encoding="utf-8") as unknown_f:
        for doc, proj in unknown.iteritems():
            if len(proj) > 0:
                de = []
                links = align[doc]

                for p in proj:
                    for link in links:
                        if p in link[1].split(" "):
                            de.extend(link[0].split(" "))

                with copen(f_clean + doc[0].replace(".gz", "")) as doc_f:
                    dom = parse(doc_f)
                    nodes = dom.getElementsByTagName("s")

                    for node in nodes:
                        if node.getAttribute("id") in de:
                            unknown_f.write("%s\n" % node.firstChild.nodeValue)