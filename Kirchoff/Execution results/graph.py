# -*- coding: utf-8 -*-
# command to execute: python graph.py proactive_5_arima

import pandas as pd
import datetime
import re
import sys,os
file_name = sys.argv[1]
file = open(file_name+".txt","r");
#file = open("reactive.txt","r");
#file = open("proactive_autoArima_60min.txt","r");
interv_time = '60'
lines = file.readlines()
log ="";

def timeToStr(totalSecs):
  #return str(datetime.timedelta(seconds=float(totalSecs)))
  m, s = divmod(float(totalSecs), 60)
  h, m = divmod(m, 60)
  return '{:d}:{:02d}:{:02d}'.format(int(h), int(m), int(s)) # Python 3


"""# **Data frame by minutes (dfm)**"""

# manipulation of dataframe

log2="";  
log2 = log2 + "Time;accepted;rejected;idle;qos"+'\n'

for i in lines:
  index = i.find("Process Request:")
  if index > -1 :
    line = i[index:len(i)];
    line = re.sub('([aA-zZ])', '', line)
    line = line.replace(" ",'')
    line = line.replace(":",';')
    line = line.replace(",",'.')
    line = line.replace("-∞",'0')
    
    #print(line[2:])
    log2 = log2 + timeToStr(i[0:index-2])+';'+line[2:]
    

dfm = pd.DataFrame([x.split(';') for x in log2.split('\n')[1:]], columns=[x for x in log2.split('\n')[0].split(';')])
#df = pd.DataFrame([x.split(';') for x in log.split('\n')])
#pd.to_datetime(df['date_col'], format='%d/%m/%Y')
#df['Time'] = df['Time'].astype(datetime)

dfm['accepted'] = dfm['accepted'].astype(float)
dfm['rejected'] = dfm['rejected'].astype(float)
dfm['idle'] = dfm['idle'].astype(float)
dfm['qos'] = dfm['qos'].astype(float)
dfm['capacity'] = ((dfm['accepted'].astype(float) + dfm['idle'].astype(float)) /15)


import matplotlib.pyplot as plt
import numpy as np
from matplotlib.ticker import (MultipleLocator, FormatStrFormatter,
                               AutoMinorLocator)

fig, ax = plt.subplots(figsize=(19, 10))
df = dfm  #[:200]

x =df['Time']
y_a = df['accepted']
y_r = df['rejected']
capacity = df['accepted'] + df['idle']
load = df['accepted'] + df['rejected']
#ax.plot(x, capacity, label = 'resources')
ax.fill_between( x, capacity, color="limegreen", alpha=0.3, label = 'Resources')
ax.plot(x, load, label='Workload', color='black', linestyle=':')
ax.plot(x, y_a, label='Accepted')
#ax.bar(x, y_r, label='Rejected', color="red")

#ax.xaxis.set_major_locator( MultipleLocator(1440))

#ax.xaxis.set_minor_locator(MultipleLocator(720))
ax.set_xticklabels( x, rotation=45 ) ;

#ax.tick_params(which='both', width=2)
#ax.tick_params(which='major', length=10)
#ax.tick_params(which='minor', length=4, color='r')

plt.title("Number of Vms used ("+file_name + ")")
plt.xlabel('Time')
plt.ylabel('Requests')

#plt.grid()
plt.legend()

desc = str(datetime.datetime.now().time())
plt.savefig('graph/'+file_name+' '+desc+'.pdf')
plt.show()

dfm.sum(axis = 0, skipna = True)# axis=0 columns axis = 1 lines

#file_name = 'drive/My Drive/colab_dataset/proactive_schedule_atual_05_convertido.csv'
#dfm.to_csv(file_name+'_min_converted.csv', sep='\t', encoding='utf-8')

#dfm[:15]
