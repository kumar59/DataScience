#%%
"""
read csv file
"""
#%%
import unicodecsv

enrollments = []
with open('daily_engagement.csv','rb') as f:
     reader = unicodecsv.DictReader(f)
     enrollments=list(reader)

print(' ')
enrollments[0]
enrollments[1]
#%%  
