# May need to override this on Windows to the full path to git.exe, e.g. by running
# > set git_exe="C:\Program Files (x86)\Git\bin\git.exe"
# before running make.
git_exe?=git

default:
	echo "There's no default thing to do."

.PHONY: \
	clean \
	default \
	update-libraries

.DELETE_ON_ERROR:

clean:
	rm -rf bin

update-libraries:
	${git_exe} subtree pull --prefix=ext-lib/ByteBlob https://github.com/TOGoS/ByteBlob.git master
