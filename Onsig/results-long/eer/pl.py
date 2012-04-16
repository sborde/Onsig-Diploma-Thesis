#!/usr/bin/env python2

import os
import matplotlib
import math
matplotlib.use('Agg') # No X
import matplotlib.pyplot as plt
from numpy import array, append, std, mean

def plotfile(fname, name):
  
  x = array([])
  k = array([])

  # Gather data into numpy arrays.
  with open(fname) as f:
    lines = f.readlines()
    title = lines.pop(0)
    for line in lines:
      w = line.split()
      k = append(k, float(w[0]))
      x = append(x, math.fabs(float(w[1])-float(w[2])))
      
  # Labels.
  plt.xlabel('K')
  plt.ylabel('Y')
  plt.title("EER")

  # Plot and save as a png.
  plt.plot(k, x, c="red")
  plt.savefig("./plots/" + name + ".png")
  plt.close()
  
for datafile in os.listdir("./datas/"):
  plotfile("./datas/"+datafile, datafile)
