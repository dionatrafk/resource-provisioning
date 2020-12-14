# -*- coding: utf-8 -*-

import pandas as pd
import datetime
import matplotlib.pyplot as plt
import numpy as np
from keras.models import Sequential, load_model
from keras.layers import Dense, Dropout
from sklearn.metrics import mean_squared_error, r2_score
import math
import os, sys

def create_dataset(dataset, lookback=1):
    dataX, dataY = [], [] # create 2 empty list

    # go through the lenght of dataset, subtract the lookback and 1. 
    #2 steps before the end of dataset, because we predict 1 step to the future
    for i in range(len(dataset)-lookback-1):
        a = dataset[i:(i+lookback),0]
        dataX.append(a)
        dataY.append(dataset[i+lookback,0]) # get the next value
    return np.array(dataX), np.array(dataY)

def save_model(model,filename):
    filename = os.path.abspath(__file__)
    filename = filename.replace('.py','.h5')
    #print(filename)
    model.save(filename)
    print("model saved")
    print(filename)
	

def create_model(filename):    
    #definir hiperparâmetros
    x= int(sys.argv[3]) #interval time
    if x == 1: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =200, 130, 100, 10
    elif x == 5: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =110, 180, 50, 25	#	5
    elif x == 10: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =200, 130, 100, 10	#	10
    elif x == 15: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =130, 200, 60, 30	#	15
    elif x == 20: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =110, 190, 50, 16	#	20
    elif x == 25: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =130, 200, 60, 30	#	25
    elif x == 30: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =130, 200, 60, 30	#	30
    elif x == 35: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =130, 200, 60, 30	#	35
    elif x == 40: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =130, 200, 60, 30	#	40
    elif x == 45: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =130, 200, 60, 30	#	45
    elif x == 50: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =130, 200, 60, 30	#	50
    elif x == 55: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =130, 200, 30, 60	#	55
    elif x == 60: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 =130, 130, 30, 30	#	60

    # Data preparation
    serie = sys.argv[1].split(',')
    
    np.random.seed(7)
    dataset = pd.DataFrame({'serie':serie})
    dataset = dataset.values #convert to the array
    dataset = dataset.astype('float32') # convert to float

    train = dataset
    # one time step to the future
    lookback = 1
    trainX, trainY = create_dataset(train, lookback)

    # create the model
    model_name = 'requests_MLP'
    model=Sequential()
    model.add(Dense(LAYER1, input_dim=lookback, activation='relu'))
    model.add(Dropout(0.2))
    model.add(Dense(LAYER2, activation='relu'))
    model.add(Dropout(0.2))
    model.add(Dense(1))

    #Compilation and training
    model.compile(loss = 'mean_squared_error', optimizer = "adam") 

    model.fit(trainX,trainY,epochs=NB_EPOCHS, batch_size=BATCH_SIZE, verbose=0) 
    #print("model created")
    save_model(model,filename)

def make_prediction(model):
    val = sys.argv[2]
 
    yhat = model.predict([float(val)])

    y = str(yhat)
    y = y.replace('[','')
    y = y.replace(']','')
    print(y)
    #print("prediction made")

filename = os.path.abspath(__file__)
filename = filename.replace('.py','.h5')
print(filename)
try: 
    model = load_model(filename)
    print("loaded model")
except:
    print("creating model")
    create_model(filename)
    model = load_model(filename)

make_prediction(model)
 
