# Digital Ocean

The deployment consists of:

- 1 droplet for the load balancer: `devlb.graffitab.com`
- 2 droplets for the app: `dev01.graffitab.com`, `dev02.graffitab.com`
- 1 droplet for the database: `devdb.graffitab.com`

## Add SSH keys

TODO: Explain how to add keys to digital ocean

## Droplet provisioning

### App servers

- Install Java 8
- Install NodeJS 6+
- Upload script `environment.sh` to '~'. Add execution permission
- Create deployment dir. Copy `environment.sh` into it with execution permission

### Load balancer

### Database

- Install MySQL 5.5
- Reconfigure encoding to `utf8mb4`



## Create a deployment user in app droplets

How to create a user for deployments

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

