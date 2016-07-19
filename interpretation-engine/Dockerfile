# Create a container from Debian Jessie.
FROM python:3.5.1

# Update Ubuntu repositories.
RUN apt-get update

# Install Python.
RUN apt-get install -y -q build-essential python-simplejson python-gdal
RUN apt-get install -y python python-pip wget
RUN apt-get install -y python-dev

# Create a working directory.
RUN mkdir deployment

# All all files in current directory to the /deployment directory
# Add only src/main/python instead of .
ADD . /deployment/

# Change permissions for the container init script
# RUN chmod +x /deployment/init.sh

# Run installation of dependencies
RUN pip install wheel
RUN pip install -r /deployment/requirements.txt

ENTRYPOINT ["python"] 
CMD ["/deployment/src/main/python/start.py"]