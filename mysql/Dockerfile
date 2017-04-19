FROM mysql/mysql-server:5.6
RUN yum -y install nc telnet
RUN yum -y install hostname
COPY docker-healthcheck /usr/local/bin/
HEALTHCHECK --interval=2s CMD ["docker-healthcheck"]