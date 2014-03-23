from shutil import copy
from codecs import open as copen
from gzip import open as gopen
from re import match
from xml.dom.minidom import parse
from sys import maxint

from utility import createPath, loadStruct, dumpStruct
from structures import getStopwords


def plainCopyDocuments(f_align_p, f_corpus, f_clean):
    """ Copies the files with alignments to a seperate folder.
    """
    align_p_f = loadStruct(f_align_p)

    print "Copying %d documents" % len(align_p_f)

    for key in align_p_f.iterkeys():
        to_de = f_clean + key[0]
        to_en = f_clean + key[1]
        createPath(to_de)
        createPath(to_en)
        copy(f_corpus + key[0], to_de)
        copy(f_corpus + key[1], to_en)


def cleanCopyDocuments(f_align_p, f_corpus, f_clean, f_stats, f_rem, filter=True):
    """ Copies the documents with alignment in a clean format to a new folder as text files.
    """
    align_p_f = loadStruct(f_align_p)
    stopwords = getStopwords()
    n_docs = len(align_p_f)
    words_total = 0
    words_lost = 0
    sents_lost = 0

    with open(f_rem, "w") as rem_f:
        for i, key in enumerate(align_p_f.iterkeys()):
            if i % 500 == 0:
                print "Documents: %d/%d" % (i, n_docs)
            elif i == 0 or i == n_docs - 1:
                print "Documents: %d/%d" % (i + 1, n_docs)

            for lang in key:
                fname = f_clean + lang.replace(".gz", "")
                createPath(fname)

                with copen(fname, "w", encoding="utf-8") as xml_f:
                    doc = []
                    last_id = 0
                    words = 0

                    with gopen(f_corpus + lang) as clean_f:
                        for line in clean_f:
                            line = line.strip()

                            if line.startswith("<s"):
                                last_id = match(".*id=\"([0-9]+)\"", line).group(1)
                                doc.append([])
                            if line.startswith("<w"):
                                m = match(".*>(.+)</", line)
                                if m:
                                    word = m.group(1)
                                    words += 1
                                    if lang.startswith("en"):
                                        words_total += 1
                                        word = word.strip().lower().replace("\'", "")

                                        if filter and word not in stopwords and len(word) > 1 and word.isalpha():
                                            doc[-1].append(word)
                                        elif not filter:
                                            doc[-1].append(word)
                                        else:
                                            words_lost += 1
                                    elif lang.startswith("de"):
                                        doc[-1].append(word)

                    xml_f.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<d s=\"%s\" w=\"%s\" f=\"%s\">\n" %
                               (last_id, words, lang.replace(".gz", "")))

                    for k, v in enumerate(doc):
                        sid = k + 1

                        if len(v) > 1:
                            xml_f.write("<s id=\"%s\">%s</s>\n" % (sid, " ".join(v).decode("utf-8")))
                        if len(v) <= 1:
                            sents_lost += 1
                            rem_f.write("[R] %s %s %s\n" % (str(key), lang[0:2], sid))

                            for projection in align_p_f[key]:
                                if lang.startswith("de") and str(sid) in projection[0].split(" "):
                                    align_p_f[key].remove(projection)
                                    break
                                elif lang.startswith("en") and str(sid) in projection[1].split(" "):
                                    align_p_f[key].remove(projection)
                                    break
                    xml_f.write("</d>\n")
                    xml_f.flush()
    with open(f_stats, "a") as stats_f:
        stats_f.write("Removed: %d sentences\n" % sents_lost)
        scount = 0

        for v in align_p_f.itervalues():
            scount += len(v)

        stats_f.write("Remaining: %d sentences\n" % scount)
        stats_f.write("Total words: %d\n" % words_total)
        stats_f.write("Words lost: %d\n" % words_lost)
        stats_f.write("Words remmaining: %d\n" % (words_total - words_lost))

    dumpStruct(f_align_p, align_p_f)