Programmed in Java 7, tested on Windows 7.
License: GPLv2

Usage:
A. Use LLDA.java as main method
B. java -jar LLDA.jar [options]

The possible options can be found in Options.java and should be overwritten with your values. They are

-e      estimation
-i      inference
-c      location of corpus in folder
-a      alpha
-b      beta
-n      iterations
-k      number of topics
-llda   true uses LLDA, false just LDA
-step   reporting step
-m      "simple" calculates phi and theta with the last sample, "extended" each lag-step after burn-in
-burn   burn-in iterations, approx. 10% of iterations is a good value
-lag    sampling lag, approx 5% of the burn-in value 
-bg     usage of background topics, used when k > 3

Standard workflow:

java -jar LLDA.jar -e -c ./data/train -a 0.001 -b 0.01 -n 10000 -k 3 -m extended -burn 1000 -lag 50
java -jar LLDA.jar -i -c ./data/test -a 0.001 -b 0.01 -n 10000 -k 3 -m extended -burn 1000 -lag 50