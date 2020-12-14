# -*- coding: utf-8 -*-
"""ARIMA_exec.ipynb

# Auto Regressive Integrated Moving Average (ARIMA)
"""


#p, d, q = 2, 0, 2

#filename = path + filename

import pandas as pd
from pandas import read_csv
from pandas import datetime
from matplotlib import pyplot
from statsmodels.tsa.arima_model import ARIMA
from sklearn.metrics import mean_squared_error, r2_score
from math import sqrt
import numpy as np
import math
import sys,os
import datetime


#Define Hyperparameter

x= int(sys.argv[2])
if x == 1: p, d, q = 0, 1, 2 
elif x == 5: p, d, q = 2, 0, 1 
elif x == 10: p, d, q = 0, 1, 1 
elif x == 15: p, d, q = 0, 2, 1 
elif x == 20: p, d, q = 1, 0, 2 
elif x == 25: p, d, q = 4, 0, 1 
elif x == 30: p, d, q = 2, 0, 2 
elif x == 35: p, d, q = 4, 0, 1 
elif x == 40: p, d, q = 8, 0, 1 
elif x == 45: p, d, q = 2, 0, 2 
elif x == 50: p, d, q = 2, 0, 1 
elif x == 55: p, d, q = 10, 0, 2 
elif x == 60: p, d, q = 2, 1, 2 

#perc = float(sys.argv[5])
#print p,d,q

#read the csv file
#dataset = read_csv(filename, header=0, parse_dates=[0], index_col=0, squeeze=True)

serie = sys.argv[1].split(',')
dataset = pd.DataFrame({'serie':serie})

#ds = dataset.values
#rolling = ds.rolling(window=3)
#rolling = rolling.mean()
#X = rolling[2:]


# split into train and test sets
X = dataset.values
X = X.astype('float32')
#size = int(len(X) * 0.67)
#train, test = X[0:size], X[size:len(X)]


train = X
series = [x for x in train]
predictions = list()

#training_size = len(train)

#test with less samples	
#size = int(training_size * (perc /100))
#series = series[size:training_size:]
#timer = start = datetime.datetime.now() 
# walk-forward validation
#print 'current, prediction'

'''for t in range(len(test)):
  model = ARIMA(series, order=(p,d,q))
  model_fit = model.fit(disp=0)
  output = model_fit.forecast()

  yhat = output[0]
  predictions.append(yhat)
  current = test[t]
  series.append(current)
 
  #print('%.2f, %.2f' % (current,yhat))
'''
# evaluate forecasts
model = ARIMA(series, order=(p,d,q))
model_fit = model.fit(disp=0)
output = model_fit.forecast()

yhat = str(output[0])
yhat = yhat.replace('[','')
yhat = yhat.replace(']','')
print(yhat)

#print "Timer: ", datetime.datetime.now() - timer
#score = mean_squared_error(test, predictions)
#r2 = r2_score(test, predictions)
#print ('R2: %.2f, Testscore: %.2f MSE (%.2f RMSE)' %(r2,score, math.sqrt(score)))

#plot the execution
#pyplot.plot(test)
#pyplot.plot(predictions, color='green')
#pyplot.show()
