#!/usr/bin/python

import subprocess
import shlex
import sys
import os
import time

config = "config.yaml"
local_name = "p2"

command="/Library/Java/JavaVirtualMachines/jdk1.7.0_75.jdk/Contents/Home/bin/java -Dfile.encoding=UTF-8 -classpath /Users/flimth/CMU/18842/Lab0/bin:/Users/flimth/CMU/18842/Lab0/lib/snakeyaml-1.14.jar Main "

command += config
command += " "
command += local_name

args = shlex.split(command)

#Run the instance
ps_lab0 = subprocess.Popen(args)
ps_lab0.wait()

