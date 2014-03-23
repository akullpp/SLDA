from codecs import open as copen
from re import match
from xml.dom.minidom import parse
from os import listdir

from utility import createPath, loadStruct, dumpStruct


def createProjection(f_align_p, f_stats, f_clean, f_proj, f_unknown_p):
    """ Creates the projection based on rules.
    """
    fcount = 0
    de_count = 0
    en_count = 0
    pos = 0
    neg = 0
    lost = 0
    nn = 0
    on = 0
    no = 0
    oo = 0
    de_lost = 0
    scount = 0
    align_p_f = loadStruct(f_align_p)
    total = len(align_p_f)
    unknown = {}

    for lang, rels in align_p_f.iteritems():
        fcount += 1

        if fcount % 500 == 0 or fcount == total or fcount == 1:
            print "Documents: %d/%d" % (fcount, total)

        with copen(f_clean + lang[0].replace(".gz", "")) as xml_f:
            proj = {}
            dom = parse(xml_f)
            nodes = dom.getElementsByTagName("s")
            de_count += len(nodes)

            for link in rels:
                for node in nodes:
                    id_de = node.getAttribute("id")
                    links_de = link[0].split(" ")

                    if id_de in links_de and link[1] != "":
                        sentence = node.firstChild.nodeValue.split(" ")
                        meta = "<s id=\"0\" f=\"0\" i=\"0\">"

                        if "du" in sentence or "Du" in sentence:
                            meta = meta.replace("i=\"0\"", "i=\"1\"")
                        if "Sie" in sentence[1:]:
                            meta = meta.replace("f=\"0\"", "f=\"1\"")

                        if "f=\"0\" i=\"0\"" in meta:
                            nn += 1
                        elif "f=\"1\" i=\"0\"" in meta:
                            on += 1
                        elif "f=\"0\" i=\"1\"" in meta:
                            no += 1
                        elif "f=\"1\" i=\"1\"" in meta:
                            oo += 1

                        if "f=\"1\" i=\"1\"" not in meta:
                            for id_en in link[1].split(" "):
                                proj[id_en] = meta.replace("id=\"0\"", "id=\"%s\"" % id_en)
                    else:
                        de_lost += 1
            en_count += len(proj)

        with copen(f_clean + lang[1].replace(".gz", "")) as xml_e:
            unknown.setdefault(lang, [])
            fname_e = f_proj + "_".join(lang[1].split("/")).replace(".xml.gz", ".txt").replace("en_", "")
            createPath(fname_e)

            with copen(fname_e, "w", encoding="utf-8") as txt_e:
                txt_e.write("<d src=\"%s\">\n" % lang[0].replace(".gz", ""))
                dom_e = parse(xml_e)
                nodes_e = dom_e.getElementsByTagName("s")

                for node in nodes_e:
                    id_e = node.getAttribute("id")
                    sent_e = node.firstChild.nodeValue

                    if id_e in proj:
                        proj_e = proj[id_e]
                        s_sent_e = sent_e.split(" ")

                        if "you" in s_sent_e and "f=\"0\" i=\"0\"" not in proj_e:
                            pos += 1
                            scount += 1
                            txt_e.write("%s%s</s>\n" % (proj_e, sent_e))
                        elif "you" in s_sent_e and "f=\"0\" i=\"0\"" in proj_e:
                            neg += 1
                            unknown[lang].append(id_e)
                        elif "you" not in s_sent_e and "f=\"0\" i=\"0\"" in proj_e:
                            scount += 1
                            txt_e.write("%s%s</s>\n" % (proj_e, sent_e))
                        elif "you" not in s_sent_e and "f=\"0\" i=\"0\"" not in proj_e:
                            lost += 1
                txt_e.write("</d>\n")
                txt_e.flush()

    with open(f_stats, "a") as stats:
        stats.write("PROJECTED DE_%d TO %d_EN\n"
                    "DE 0 0: %d\n"
                    "DE 1 0: %d\n"
                    "DE 0 1: %d\n"
                    "DE 1 1: %d\n"
                    "Y-Found: %d\n"
                    "Y-NotFound: %d\n"
                    "F-Lost: %d\n"
                    "Sentences: %d\n"
                    "DE no EN: %d" %
                   (de_count, en_count, nn, on, no, oo, pos, neg, lost, scount, de_lost))

    dumpStruct(f_unknown_p, unknown)


def processGutenberg(f_gutenberg, f_gproj):
    """
    Processing the Project Gutenberg corpus.
    """
    for f_g in ["test/", "train/"]:
        createPath(f_gproj + f_g)

        for f_novel in listdir(f_gutenberg + f_g):
            if f_novel.endswith("_en.txt"):

                with copen(f_gproj + f_g + f_novel, "w", encoding="utf-8") as gproj_f:
                    gproj_f.write("<d src=\"%s\">\n" % f_novel)

                    with copen(f_gutenberg + f_g + f_novel, encoding="utf-8") as novel_f:
                        j = 2

                        for i, line in enumerate(novel_f.readlines()):
                            if i in xrange(j - 2, j):
                                line = line.strip()

                                if line.startswith("<S"):
                                    m = match(".*sentNum:([0-9]+).*F:([0|1]) I:([0|1])", line)
                                    gproj_f.write("<s id=\"%s\" f=\"%s\" i=\"%s\">" % (m.group(1), m.group(2), m.group(3)))
                                else:
                                    gproj_f.write("%s</s>\n" % line)
                            elif i == j:
                                j += 4
                    gproj_f.write("</d>\n")