FROM python:3.5.1
RUN mkdir /deployment/
ADD . /deployment/
RUN pip install -r /deployment/requirements.txt
ENV PYTHONPATH=/deployment/src/main/python
ENTRYPOINT ["python"]
CMD ["/deployment/src/main/python/start.py"]