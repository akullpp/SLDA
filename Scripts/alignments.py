from gzip import open as gopen
from re import search
from lxml import etree

from utility import dumpStruct, loadStruct, createPath
from structures import f_misc, langs


def extractAlignmentsRX(f_align, f_align_p, f_stats):
    """ Extracts the alignments with regex.

    Easier to parse HUN aligned files, which will be dropped due to inconsistencies. Mainly used for the small
    OpenSubtitles corpus not the 2011er one.
    """
    print "Extracting alignments"

    alignments = {}
    final = {}
    hun_files = set()
    doc_count = 0
    link_count = 0

    with gopen(f_align) as align_f:
        for line in align_f:
            line = line.strip()

            if line.startswith("<linkGrp"):
                doc_count += 1
                m = search("fromDoc=\"(.+)\"\stoDoc=\"(.+)\"", line)

                if m:
                    key = (m.group(1), m.group(2))
                elif not m:
                    m = search("toDoc=\"(.+)\"\sfromDoc=\"(.+)\"", line)
                    key = (m.group(2), m.group(1))
                alignments.setdefault(key, [])
            elif line.startswith("<link id="):
                link_count += 1
                m = search("xtargets=\"(.+?)\"", line)
                alignments[key].append(m.group(1).split(";"))
            elif line.startswith("<link certainty="):
                hun_files.add(key)

                if key in alignments:
                    del alignments[key]
                continue

    empty = set()

    for k, v in alignments.iteritems():
        if len(v) != 0:
            final.setdefault(k, v)
        else:
            empty.add(k)
    dumpStruct(f_align_p, final)
    createPath(f_stats)

    with open(f_stats, "w") as stats:
            stats.write("DOCS: %d\nHUN: %d\nEMPTY: %d\nLEFT: %d\nLINKS: %d\n\n" %
                       (doc_count, len(hun_files), len(empty), len(final), link_count))

            for k in hun_files:
                stats.write(k[0] + " || " + k[1] + "\n")
            stats.write("\n")


def extractAlignmentsLXML(f_align, f_align_p, f_stats):
    """ Extracts alignment information from the alignments file with LXML.

    Used for the large OpenSubtitles 2011 corpus for faster processing.
    """
    print "Extracting alignments"

    class Target(object):
        def __init__(self):
            self.d = dict()
            self.n_links = 0
            self.n_docs = 0

        def start(self, tag, attr):
            if tag == "linkGrp":
                self.n_docs += 1
                self.k = (attr["fromDoc"], attr["toDoc"])
                self.group = self.d[self.k] = []
            elif tag == "link":
                self.n_links += 1
                self.group.append(tuple(attr["xtargets"].split(";")))

                if "certainty" in attr:
                    print "Attention HUN: %s" % self.k

        def close(self):
            pass

    with gopen(f_align) as xml:
        targets = Target()
        parser = etree.XMLParser(target=targets)
        etree.parse(xml, parser)

    alignments = targets.d

    # Documents with no alignments
    empty = set()

    for k, v in alignments.iteritems():
        if not len(v):
            empty.add(k)
            del targets.d[k]

    dumpStruct(f_align_p, alignments)
    createPath(f_stats)

    with open(f_stats, "w") as stats:
        stats.write("DOCS: %d\nEMPTY: %d\nLEFT: %d\nLINKS: %d\n\n" %
                    (targets.n_docs, len(empty), len(alignments), targets.n_links))

        for k in empty:
            stats.write("!!! Empty files\n%s || %s\n" % (k[0], k[1]))
            stats.write("\n")


def countSentences(align, fout_stats):
    """ Count sentences from alignment structure.
    """
    print "Counting sentences"

    de_total = 0
    en_total = 0
    align_de = {}
    align_en = {}

    with open(fout_stats, "a") as stats:
        for doc, align in align.iteritems():
            de_doc = 0
            en_doc = 0

            for pro in align:
                for _ in pro[0].split(" "):
                    if _ != "":
                        de_doc += 1
                for _ in pro[1].split(" "):
                    if _ != "":
                        en_doc += 1
            align_de.setdefault(doc[0].rsplit("/", 1)[1].replace(".gz", ""), de_doc)
            align_en.setdefault(doc[1].rsplit("/", 1)[1].replace(".gz", ""), en_doc)
            stats.write("%s \t %s\n%d \t %d\n" % (doc[0], doc[1], de_doc, en_doc))
            de_total += de_doc
            en_total += en_doc
        stats.write("\nDE Sentences: %d\nEN Sentences: %d\n" % (de_total, en_total))
    dumpStruct(f_misc + "de_align.p", align_de)
    dumpStruct(f_misc + "en_align.p", align_en)


def compareSentenceCount(misc):
    """ Compares sentence count from grep -c "s id" for checking purposes.

    Save the grep output as lang_count.txt.
    """
    print "Comparing sentence counts"

    for lang in langs:
        a = loadStruct(misc + lang + "_align.p")
        b = {}

        with open(misc + "%s_count.txt" % lang) as counts:
            for line in counts:
                k, v = line.strip().split(":")
                b.setdefault(k, v)

        for k1, v1 in a.iteritems():
            for k2, v2 in b.iteritems():
                if k1 == k2:
                    if str(v1) != v2:
                        print k1, k2
                        print v1, v2