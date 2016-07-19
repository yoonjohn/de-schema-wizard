FROM mongo:3.2

# Adding files
ADD . /deployment/

# Adding startup script
RUN chmod +x /deployment/mongo_init.sh

CMD ["/deployment/mongo_init.sh"]