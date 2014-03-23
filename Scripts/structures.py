# Log files and datastructures
f_misc = "./misc/"
# Statistics
f_stats = f_misc + "stats.txt"
# List of removed sentences during preprocessing
f_rem = f_misc + "removed.txt"
# Alignments
f_align_p = f_misc + "align.p"
# Projections
f_proj_p = f_misc + "proj.p"
# Sentence ids that included a "you" but were not classified
f_unknown_p = f_misc + "unknown.p"
# The real unknown sentences
f_unknown = f_misc + "unknown.txt"

# Corpus files
f_root = "./corpus/"
# Labled LDA format file
f_lda = f_root + "corpus"
# Clean format files
f_clean = f_root + "clean/"
# Documents with projections
f_proj = f_root + "proj/"
# Original corpus
f_corpus = f_root + "OpenSubtitles2011/"
# Gutenberg corpus
f_gutenberg = f_root + "Gutenberg/"
# Projections Gutenberg
f_gproj = f_root + "gproj/"
# Alignments
f_align = f_root + "srtalign_de-en.xml.gz"

# Gold standard file
f_gold = f_root + "gold"
# Training file
f_train = f_root + "train"
# Testing file
f_test = f_root + "test"
# Results file
f_result = f_root + "result"

# Language: foreign, english
langs = ["de", "en"]

# List of Stopwords
stopwords = "-,.,!,?,\',\",...,(,),[,],:,--," \
            "a,able,about,across,after,all,almost,also,am,among,an,and,any,are,as,at," \
            "be,because,been,but,by," \
            "can,cannot,could," \
            "didn,don,dear,did,do,does,doesn" \
            "either,else,ever,every," \
            "for,from," \
            "get,got," \
            "had,has,hasn,have,he,her,hers,him,his,how,however," \
            "i,ii,if,in,into,is,isn,it,its,il," \
            "just," \
            "least,let,like,likely,ll," \
            "m,may,me,might,most,must,my," \
            "nt,neither,no,nor,not," \
            "of,off,often,on,only,or,other,our,own," \
            "rather,re," \
            "s,said,say,says,she,should,since,so,some," \
            "t,than,that,the,their,them,then,there,these,they,this,tis,to,too,twas," \
            "us," \
            "ve," \
            "wants,was,wasn,we,were,what,when,where,which,while,who,whom,why,will,with,would,wouldn" \
            "your,yet".split(",")


# Return stopword list
def getStopwords():
    stopwords.append(",")

    return stopwords