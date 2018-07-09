#%%
""" 
read csv file
"""
#%%
import unicodecsv

enrollments = []
f = open('enrollments.csv','rb')
reader = unicodecsv.DictReader(f)

for row in reader:
    enrollments.append(row)
    
f.close()

len1 = len(enrollments)
cnt=0
while cnt < len1:
    print(enrollments[cnt])
    cnt=cnt+1
print('first line of array is',enrollments[0])
print('last line of array is',enrollments[len1])

#%%    
