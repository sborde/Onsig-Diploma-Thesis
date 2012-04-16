#!/usr/bin/env python2

# This file is used for visualizing signatures, mostly for amusement
# and manual inspection.

import os
import matplotlib
matplotlib.use('Agg') # No X
import matplotlib.pyplot as plt
from numpy import array, append, std, mean

# Do we want to show pressure values?
#PRESSURE=False
PRESSURE=True

# Data path
F = "./"

# Save path
#SPATH="../plots/regular/"
SPATH="../plots/pressure/"

def normalize(x):
  """Mean normalization of a vector."""
  mean = x.mean()
  ran = x.ptp()
  x = 1 + (x - mean) / ran
  return x

def plotfile(dataset, datafile, fname):
  """Plots a signature."""
  print "Plotting %s/%s." % (dataset, datafile)

  x = array([])
  k = array([])

  # Gather data into numpy arrays.
  with open(fname) as f:
    linenum = -1
    for line in f:
      linenum += 1
      if linenum == 0 :
      	title = line
      	continue;
      w = line.split()
      k = append(k, float(w[0]))
      x = append(x, float(w[1])-float(w[2]))
      
  # Labels.
  plt.xlabel('X')
  plt.ylabel('Y')
  plt.title("EER")
  # Color, blue for genuine, red for forgery.
  #color = "blue" if dataset == "genuine" else "red"

  # Plot and save as a png.
  plt.plot(k, x, c="red")
  plt.savefig("./" + datafile + ".png")
  plt.close()


# dataset in ['genuine', 'forgery']
for dataset in os.listdir(F):
  datadir = F + dataset + "/"
  # We'll need this directory for results, create it if it does not exists.
#  if not os.path.exists(SPATH + dataset):
#    os.mkdir(SPATH + dataset)
  # For each file in the dataset.
  for datafile in os.listdir("."):
    fname = "./" + datafile
    # Plot the file.
    plotfile(dataset, datafile, fname)
#    break
#  break
