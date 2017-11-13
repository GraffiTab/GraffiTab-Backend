# Provisioning for database server in DigitalOcean

## Initial setup

See same section in `app-server.md`.

## Install MySQL

* Installation, see tutorial [here](https://www.digitalocean.com/community/tutorials/a-basic-mysql-tutorial)
```
apt-get install mysql-server
```

* Configure `utf8mb4` encoding, see it [here](https://mathiasbynens.be/notes/mysql-utf8mb4)

If the database is brand new, there is no tables to migrate, so add the following to `/etc/mysql/my.cnf`

```
[client]
default-character-set    = utf8mb4

[mysql]
default-character-set    =  utf8mb4

[mysqld]
character-set-client-handshake = FALSE
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
```

Comment out the `bind-address` directive to allow the app servers to connect:

```
# bind-address          = 127.0.0.1
```

Log into mysql client and change Host property to '%' in the user's table of `mysql`
database to allow external login (for CircleCI).

```
mysql> use mysql
mysql> update mysql.user set Host = '%' where User = 'root' and Host = '::1';
```

Then restart the database server:
```
/etc/init.d/mysql restart
```

Check the settings are correct by getting into the mysql client and running:
```
mysql> SHOW VARIABLES WHERE Variable_name LIKE 'character\_set\_%' OR Variable_name LIKE 'collation%';
+--------------------------+--------------------+
| Variable_name            | Value              |
+--------------------------+--------------------+
| character_set_client     | utf8mb4            |
| character_set_connection | utf8mb4            |
| character_set_database   | utf8mb4            |
| character_set_filesystem | binary             |
| character_set_results    | utf8mb4            |
| character_set_server     | utf8mb4            |
| character_set_system     | utf8               |
| collation_connection     | utf8mb4_unicode_ci |
| collation_database       | utf8mb4_unicode_ci |
| collation_server         | utf8mb4_unicode_ci |
+--------------------------+--------------------+
```

Check that mysql server listens on all interfaces in the right port by running:
```
netstat -plutn
```

* Import an initial database structure into this server (usually through `phpmyadmin`).

## Install phpmyadmin

See tutorial [here](https://www.digitalocean.com/community/tutorials/how-to-install-and-secure-phpmyadmin-on-ubuntu-14-04)
Username and password for the extra security layer are stored in `environment.sh`.

## Install and configure Redis

* Installation tutorial [here](https://www.digitalocean.com/community/tutorials/how-to-configure-a-redis-cluster-on-ubuntu-14-04)
Run this to additionally install `redis-cli` for testing purposes:
```
apt-get install redis-tools
```

* Information on how to secure [here](https://www.digitalocean.com/community/tutorials/how-to-secure-your-redis-installation-on-ubuntu-14-04).

* How to use Redis brief tutorial [here](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-redis)

The current configuration file with placeholders is provided in `redis.conf`. This file should be placed
in `/etc/redis/redis.conf`, which is read by the main com.graffitab.server.service script, under `/etc/init.d/redis-server`.
Redis should be started as a regular com.graffitab.server.service:

```
$ com.graffitab.server.service redis-server start
```

To test that the installation was correct:
```
# Start up redis
$ com.graffitab.server.service redis-server start | status | stop

# This should give error as we are securing it
$ redis-cli keys '*'

# This should give back an empty set (without auth errors)
$ redis-cli -a yourpassword keys '*'
```

Redis periodically writes a journal file `dump.rdb` which is saved in the Redis working directory
(default is `/var/lib/redis`). This file is restored into memory every time Redis is restarted.
This can be configured in `redis.conf`.

## Add firewall rules

See section in `app-server.md`

## Add swap

See section in `app-server.md`
