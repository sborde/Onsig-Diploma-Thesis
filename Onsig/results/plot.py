#!/usr/bin/env python2

import os
import matplotlib
matplotlib.use('Agg') # No X
import matplotlib.pyplot as plt
from numpy import array, append, std, mean

def plotfile(fname):

  x = array([])
  y = array([])
  y2 = array([])

  # Gather data into numpy arrays.
  with open(fname) as f:
    index = 0
    for line in f:
      w = line.split()
      if index == 0 :
        title = w[0]
      else:
        x = append(x, float(w[0]))
        y = append(y, float(w[1]))
        y2 = append(y2, float(w[2]))
      index = index + 1

  # Labels.
  plt.xlabel('X')
  plt.ylabel('Y')
  plt.title(title)
  plt.grid(True, which="majorminor")

  color = "red"
  color2 = "blue"
  # Plot and save as a png.
  plt.plot(x, y, c=color)
  plt.plot(x, y2, c=color2)
  plt.savefig("./" + fname + ".png")
  plt.close()

plotfile("eer02.txt")
