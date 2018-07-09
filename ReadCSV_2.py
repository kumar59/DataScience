#%%
"""
read daily engagement csv file
"""
#%%
import unicodecsv

daily_engagement = []
with open('daily_engagement.csv','rb') as f:
     reader = unicodecsv.DictReader(f)
     daily_engagement=list(reader)

print(' ')
daily_engagement[0]

#%% 
