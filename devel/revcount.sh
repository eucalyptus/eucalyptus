if [ -z "${1}" ]; then
  echo "Usage: diffcount.sh REVNO"
  exit 1
fi
REVNO=$1 
COUNT='BEGIN{line=0;comment=0}/^\ *\*/{comment++}{line++}END{printf("%d (%d,%d) # line-comments (lines,comments)", line-comment,line,comment)}'
echo Deleted $(bzr log -r${REVNO} -v -S | awk '/^\ *D/{print $2}' | xargs -i bzr cat -r$((REVNO-1)) {} | awk "${COUNT}")
echo Modified $(bzr log -r${REVNO} -v -S | awk '/^\ *M/{print $2}' | xargs -i bzr log -r${REVNO} -p {} | egrep '^[+-][^+-]' | awk "${COUNT}")
echo Renamed $(bzr log -r${REVNO} -v -S | awk '/^\ *R/{print $2}' | xargs -i bzr cat -r$((REVNO-1)) {} | awk "${COUNT}")
echo Added $(bzr log -r${REVNO} -v -S | awk '/^\ *A/{print $2}' | xargs -i bzr cat -r$((REVNO)) {} | awk "${COUNT}")
