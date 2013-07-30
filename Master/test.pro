
 # Author:  Tim Scheuermann
 # License: MIT

# This is the test profile.
# You can also use this as a reference.

print Hello World, I hope it works!

print Sleep two seconds
sleep 2 Everything behind this will be ignored!
print Okay

print Start a new round
next
print Okay

print Send a transaction
send
print Okay

print Set autosending on 60 transmissions per minute
mode 60
print Okay

sleep 10

print Stop sending
mode 0
print Okay

print And we are done!
