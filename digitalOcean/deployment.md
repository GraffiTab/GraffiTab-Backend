# General deployment documentation for Digital Ocean

The com.graffitab.server.service deployment consists of:

- 1 droplet for the load balancer (HAProxy): `devlb.graffitab.com`
- 2 droplets for the app (Java fatJars): `dev01.graffitab.com`, `dev02.graffitab.com`
- 1 droplet for the database (MySQL + Redis): `devdb.graffitab.com`

## Add SSH keys

See tutorial on how to add keys [here](https://www.digitalocean.com/community/tutorials/initial-server-setup-with-ubuntu-14-04)

## Droplet provisioning

See documentation under `provision` folder.

## Deployment through CircleCI

Need to add the following environment variables:

- `DO_USER`: the user to do ssh as in both app servers. Need to upload SSH keys to allow login in both `dev01` and `dev02`
- `DO_DEPLOYMENT_DIR`: the directory where the app resides in the app servers
- `DO_DEV01_DOMAIN`: domain of the first app server droplet
- `DO_DEV02_DOMAIN`: domain of the second app server droplet
- `DO_MYSQL_DB_HOST`: host/public ip of the MySQL database
- `DO_MYSQL_DB_NAME`: name of the MySQL database
- `DO_MYSQL_DB_USERNAME`: user to connect to the database
- `DO_MYSQL_DB_PASSWORD`: password for the database

We also need to add private SSH keys to allow CircleCI to make passwordless SSH connections to the app droplets.
To do that:

- Log into CircleCI, go to `Project settings`
- On the left hand side, select `SSH permissions`
- Fill the hostname URL and paste the private SSH key (located in `~/.ssh/id_rsa`)

