#!/bin/bash

# this is an initial script designed to import ATimeTracker exported csv files into timewarrior.net

# [1] Pipe CSV into this script like this:
#       $ ./csv_to_timew.bash < all.csv
#     or
#       $ cat all.csv | ./csv_to_timew.bash
#
# [2] Ensure timew is found in $PATH.

# Consume and ignore the header line.
read header

# Using ',' as a field separator, read the three fields.
while IFS=, read name start end
do
  # Replace the ' ' in the timestamp with 'T', per ISO-8601.
  timew track ${start/ /T} - ${end/ /T} "'$name'" :quiet
done
