# -*- coding: utf-8 -*-

import pandas as pd
from keras.layers.core import Dense, Dropout
from keras.layers.recurrent import GRU
from keras.models import Sequential, load_model
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_squared_error, r2_score
import math, datetime
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
    model.save(filename)
    print("model saved")
    print(filename)
	

def create_model(filename):    
    #definir hiperparâmetros
    #BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 140, 150, 60, 30
    x= int(sys.argv[3]) #interval time
    if x == 1: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 140, 150, 8, 64	#	1
    elif x == 5: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 130, 130, 128, 64	#	5
    elif x == 10: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 140, 100, 16, 32	#	10
    elif x == 15: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 140, 190, 5, 40	#	15
    elif x == 20: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 140, 200, 50, 45	#	20
    elif x == 25: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 180, 200, 10, 80	#	25
    elif x == 30: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 140, 190, 5, 40	#	30
    elif x == 35: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 180, 200, 10, 80	#	35
    elif x == 40: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 130, 130, 256, 32	#	40
    elif x == 45: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 140, 150, 8, 64	#	45
    elif x == 50: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 130, 130, 60, 60	#	50
    elif x == 55: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 130, 130, 128, 64	#	55
    elif x == 60: BATCH_SIZE, NB_EPOCHS, LAYER1, LAYER2 = 130, 130, 128, 64	#	60
 
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

    # Scaling dataset
    x_train, y_train = trainX, trainY 

    # scaling values for model
    scaleX = MinMaxScaler()
    scaleY = MinMaxScaler()

    trainX = scaleX.fit_transform(x_train)
    trainX = trainX.reshape((-1,1,1))

    trainY = scaleY.fit_transform(y_train.reshape(-1,1))

    #print(trainY)
    # create the model
    model = Sequential()
    model.add(GRU(units=32,
              return_sequences=True,
              input_shape=(1, 1)))
    model.add(Dropout(0.2))
    model.add(GRU(units=16))
    model.add(Dropout(0.2))
    model.add(Dense(1, activation='sigmoid'))

    #Compilation and training
    model.compile(loss = 'mean_squared_error', optimizer = "adam") 

    model.fit(trainX,trainY,epochs=NB_EPOCHS, batch_size=BATCH_SIZE, verbose=0) 
    #print("model created")
    save_model(model,filename)

def make_prediction(model):
    serie = sys.argv[1].split(',')
    
    dataset = pd.DataFrame({'serie':serie})
    dataset = dataset.values #convert to the array
    dataset = dataset.astype('float32') # convert to float

    #normalize
    scale = MinMaxScaler()
    time_serie  = scale.fit_transform(dataset)
    time_serie = time_serie.reshape((-1,1,1))
    # take the last value
    yhat = model.predict(time_serie[-1:])
    yhat = scale.inverse_transform(yhat)

    y = str(yhat)
    y = y.replace('[','')
    y = y.replace(']','')
    print(y)

filename = os.path.abspath(__file__)
filename = filename.replace('.py','.h5')

try: 
    model = load_model(filename)
except:
    create_model(filename)
    model = load_model(filename)

make_prediction(model)
