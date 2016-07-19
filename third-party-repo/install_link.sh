#!/bin/bash
sudo cp target/ubuntu/libjnetpcap.so /usr/lib/
if [ $? > 0 ]
then
	exit $?
fi
sudo ldconfig
exit