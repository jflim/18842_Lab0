#!/usr/bin/python

import subprocess
import shlex
import sys
import os
import time

config = "/Users/flimth/Dropbox/18842/config.yaml"

if len(sys.argv) != 2:
   print "Usage: ./testrun.py [local_name]"
   sys.exit(1)

local_name = sys.argv[1]

command="/Library/Java/JavaVirtualMachines/jdk1.7.0_75.jdk/Contents/Home/bin/java -Dfile.encoding=UTF-8 -classpath /Users/flimth/CMU/18842/Lab0/bin:/Users/flimth/CMU/18842/Lab0/lib/snakeyaml-1.14.jar core.Main "

command += config
command += " "
command += local_name

args = shlex.split(command)

#Run the instance
ps_lab0 = subprocess.Popen(args)
ps_lab0.wait()

