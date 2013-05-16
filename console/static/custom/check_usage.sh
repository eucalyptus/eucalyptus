#!/bin/bash
echo "Messages that are not used in the code:"
for msg in $(grep '=' Messages.properties | awk '{ print $1}');
do
   # echo "$msg"
   cnt=$(grep -r --exclude-dir="custom" $msg ../.. | wc -l);
   if [ $cnt -eq "0" ]; then
     echo "$msg"
   fi
done
