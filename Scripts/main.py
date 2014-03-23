"""
This file combines several processing steps.

The datastrucutres and paths can be found in structures.py
"""
from alignments import *
from preprocess import *
from process import *
from postprocess import *
from structures import *
from stats import *
from utility import *

# Preprocessing
""" Uses Regex. """
# extractAlignmentsRX(f_align, f_align_p, f_stats)
""" Uses LXML. """
# extractAlignmentsLXML(f_align, f_align_p, f_stats)
""" Copies OpenSubtitles corpus and cleans it. """
# cleanCopyDocuments(f_align_p, f_corpus, f_clean, f_stats, f_rem, True)
""" Computes the sentence length in the corpus. """
# computSentenceLength(f_align_p, f_clean)
""" Creates the projection from German to English. """
# createProjection(f_align_p, f_stats, f_clean, f_proj, f_unknown_p)
""" Processing specific to Gutenberg corpus. """
# processGutenberg(f_gutenberg, f_gproj)

""" Convert Gutenberg corpus to SLDA/LLDA format. """
# convertGutenberg(f_root, "train", f_gproj)
# convertGutenberg(f_root, "test", f_gproj)

""" Convert OpenSubtitles2011 corpus to SLDA/LLDA format. """
# convertFormat(f_lda, f_proj)

""" Splits the text in 75% training and 25% testing. """
# splitText(f_lda, 75, f_train, f_gold)
""" Removes labels from gold orpus. """
# removeLabels(f_gold, f_test)
""" Converts from SLDA to LLDA format to ensure same corpora. """
# convertSLDA2LLDA(f_train, f_test, f_gold)

""" Evaluates LLDA. """
# evaluateLLDA(f_gold, f_result)
""" Evaluates SLDA. """
# evaluateSLDA(f_gold, f_result)
""" Recovers unknown sentences """
# recoverUnknown(f_unknown_p, f_unknown, f_align_p, f_clean)
""" Prints stats about the Project Gutenberg Corpus """
# gutenbergStats(f_gproj)