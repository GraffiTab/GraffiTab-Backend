FROM redis:3
RUN apt-get update && apt-get -y install telnet
COPY redis-local.conf /usr/local/etc/redis/redis.conf
COPY docker-healthcheck /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-healthcheck
COPY password.txt /usr/local/bin/
HEALTHCHECK --interval=2s CMD ["docker-healthcheck"]