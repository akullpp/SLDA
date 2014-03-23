Programmed in Java 7, tested on Windows 7.
License: GPLv2 and partially CPL

Usage:
A. Use SLDA.java as main method
B. java -jar SLDA.jar [options]

The possible options can be found in Options.java and should be overwritten with your values. They are

-e      estimation
-i      inference
-d      folder for files, e.g. corpus
-c      location of corpus in folder
-a      alpha
-b      beta
-g      gamma
-n      iterations
-k      number of topics
-step   reporting step
-m      "simple" calculates phi and theta with the last sample, "extended" each lag-step after burn-in
-burn   burn-in iterations, approx. 10% of iterations is a good value
-lag    sampling lag, approx 5% of the burn-in value 
-v      false doesn't resample f during inference, while true does
-bg     usage of background topics, used when k > 3


Standard workflow:
java -jar SLDA.jar -e -d ./data/ -c train -a 0.001 -b 0.01 -n 10000 -k 3 -burn 1000 -lag 50
java -jar SLDA.jar -i -d ./data/ -c test -a 0.001 -b 0.01 -g 0.001 -n 10000 -k 3 -burn 1000 -lag 50 -v