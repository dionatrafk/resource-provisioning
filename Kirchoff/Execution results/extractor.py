import os

dirs = os.listdir('.')
dirs.sort()

for file in dirs:
  print file

def explore(line,value):
  index = line.find(value)
  if index > -1 :
    line = line[index+len(value):len(line)];
    return line.strip()

def openFile(lines):

  model = None
  interval = None
  days = None
  hours = None
  accepted = None
  rejected = None
  qos = None

  for line in lines:
    if model == None: model = explore(line,"Prediction Model:")
    if interval == None: interval = explore(line,"Billing Time:")
    if days == None: days = explore(line,"Number of days:")
    if hours == None: hours = explore(line,"VM hours:")
    if accepted == None: accepted = explore(line,"Accepted:")
    if rejected == None: rejected = explore(line,"Rejected:")
    if qos == None: qos = explore(line,"QoS violations:")

  val = int(interval)/60
  return model, val, days, hours, accepted, rejected, qos

for file in dirs:
  file = open(file,"r");
  lines = file.readlines()
  print file.name, openFile(lines)




  



    
    
