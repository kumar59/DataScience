#%%
"""
read daily engagement csv file
"""
#%%
import unicodecsv

project_submissions = []
with open('project_submissions','rb') as f:
     reader = unicodecsv.DictReader(f)
     project_submissions=list(reader)

print(' ')
project_submissions[0]

#%% 
