from sys import maxint
from xml.dom.minidom import parse
from structures import getStopwords
from codecs import open as copen
from os import listdir
from re import match
from utility import loadStruct


def gutenbergStats(f_gproj):
    """
    Statistics for Project Gutenberg corpus.
    """
    stopwords = getStopwords()
    lost_words = 0
    lost_sents = 0
    words = 0
    sents = 0
    sent_max = 0
    sent_min = maxint

    # Change between testing and training folder manually
    corpus_type = "test/"

    for f in listdir(f_gproj + corpus_type):
        with copen(f_gproj + corpus_type + f, encoding="utf-8") as proj_f:
            for line in proj_f:
                m = match(".*>(.+)</.*", line)

                if m:
                    sentence = m.group(1).strip().split(" ")
                    clean = []

                    for word in sentence:
                        word = word.lower()

                        if word not in stopwords and len(word) > 1 and word.isalpha():
                            words += 1
                            clean.append(word)
                        else:
                            lost_words += 1
                    if len(clean) > 1:
                        sents += 1

                        if len(clean) > sent_max:
                            sent_max = len(clean)
                        if len(clean) < sent_min:
                            sent_min = len(clean)
                    else:
                        lost_sents += 1

    print "Lost Words: %d" % lost_words
    print "Lost Sentences: %d" % lost_sents
    print "Words: %d" % words
    print "Sentences: %d" % sents
    print "Sentence minimum length: %d" % sent_min
    print "Sentence maximum length: %d" % sent_max
    print "Sentence average length: %f" % (float(words) / float(sents))


def computSentenceLength(f_align_p, f_clean):
    align_p_f = loadStruct(f_align_p)
    min = maxint
    max = 0
    total = 0
    avg = 0.0
    longest = []

    for k in align_p_f.iterkeys():
        with open(f_clean + k[1].replace(".gz", "")) as clean_f:
            dom = parse(clean_f)
            nodes = dom.getElementsByTagName("s")

            for node in nodes:
                sentence = node.firstChild.nodeValue.split(" ")
                length = len(sentence)

                if length > max and "~" not in sentence and "subtitles" not in sentence and "subber" not in sentence:
                    max = length
                    longest = sentence

                if length != 0 and length < min:
                    min = length

                avg += length
            total += len(nodes)
    print "Min: %d\nMax: %d\nAvg: %f\n" % (min, max, (avg / total))
    print longest