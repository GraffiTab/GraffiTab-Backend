#!/bin/bash
rm environment.sh
source deploy/deployEnvironment.sh

# Default to Dev

DO_USER=$DO_DEV_USER
DO_SRV01_DOMAIN=$DO_DEV01_DOMAIN
DO_SRV02_DOMAIN=$DO_DEV02_DOMAIN
DO_DEPLOYMENT_DIR=$DEV_DEPLOYMENT_DIR


if [ $ENVIRON == $PRD_ENV_NAME ]
then
 DO_USER=$DO_PRD_USER
 DO_SRV01_DOMAIN=$DO_PRD01_DOMAIN
 DO_SRV02_DOMAIN=$DO_PRD02_DOMAIN
 DO_DEPLOYMENT_DIR=$PRD_DEPLOYMENT_DIR
fi

if [ $ENVIRON == $DEV_ENV_NAME ]
then
 DO_USER=$DO_DEV_USER
 DO_SRV01_DOMAIN=$DO_DEV01_DOMAIN
 DO_SRV02_DOMAIN=$DO_DEV02_DOMAIN
 DO_DEPLOYMENT_DIR=$DEV_DEPLOYMENT_DIR
 AMAZON_S3_BUCKET_NAME=$AMAZON_S3_DEV_BUCKET_NAME
fi

# Generate environment
echo "== Generating environment =="

echo "export AWS_ACCESS_KEY=$AWS_ACCESS_KEY" >> environment.sh
echo "export AWS_SECRET_KEY=$AWS_SECRET_KEY" >> environment.sh
echo "export FACEBOOK_APP_ID=$FACEBOOK_APP_ID" >> environment.sh
echo "export FACEBOOK_APP_SECRET=$FACEBOOK_APP_SECRET" >> environment.sh
echo "export PN_APNS_DEV_PASSWORD=$PN_APNS_DEV_PASSWORD" >> environment.sh
echo "export PN_APNS_PROD_PASSWORD=$PN_APNS_PROD_PASSWORD" >> environment.sh
echo "export PN_GCM_SENDER_KEY=$PN_GCM_SENDER_KEY" >> environment.sh
echo "export SENDGRID_API_KEY=$SENDGRID_API_KEY" >> environment.sh
echo "export DO_MYSQL_DB_HOST=$DO_MYSQL_DB_HOST" >> environment.sh
echo "export DO_MYSQL_DB_PORT=$DO_MYSQL_DB_PORT" >> environment.sh
echo "export DO_MYSQL_DB_USERNAME=$DO_MYSQL_DB_USERNAME" >> environment.sh
echo "export DO_MYSQL_DB_PASSWORD=$DO_MYSQL_DB_PASSWORD" >> environment.sh
echo "export DO_MYSQL_DB_NAME=$DO_MYSQL_DB_NAME" >> environment.sh
echo "export DO_SERVER_PORT=$DO_SERVER_PORT" >> environment.sh
echo "export DO_REDIS_HOST=$DO_REDIS_HOST" >> environment.sh
echo "export DO_REDIS_PORT=$DO_REDIS_PORT" >> environment.sh
echo "export DO_REDIS_PASSWORD=$DO_REDIS_PASSWORD" >> environment.sh
echo "export DO_DEPLOYMENT_DIR=$DO_DEPLOYMENT_DIR" >> environment.sh
echo "export DEPLOY_USERNAME=$DEPLOY_USERNAME" >> environment.sh
echo "export SHARED_DIRECTORY=$SHARED_DIRECTORY" >> environment.sh
echo "export AMAZON_S3_BUCKET_NAME=$AMAZON_S3_BUCKET_NAME" >> environment.sh
echo "export DO_USER=$DO_USER" >> environment.sh

echo "Generated environment file:"
cat environment.sh

echo "Generating JAR archive to deploy, env is $ENVNAME"
./gradlew stage -Penv=$ENVNAME

echo "Properties:"
echo "DO_USER = $DO_USER"
echo "DO_SRV01_DOMAIN = $DO_SRV01_DOMAIN"
echo "DO_SRV02_DOMAIN = $DO_SRV02_DOMAIN"
echo "DEPLOYMENT_DIR = $DO_DEPLOYMENT_DIR"
echo "AMAZON_S3_BUCKET_NAME = $AMAZON_S3_BUCKET_NAME"

echo "==== Deploying to Digital Ocean - ${ENVIRON}01 ===="
scp graffitab.jar $DO_USER@$DO_SRV01_DOMAIN:$DO_DEPLOYMENT_DIR
scp digitalOcean/start.sh $DO_USER@$DO_SRV01_DOMAIN:$DO_DEPLOYMENT_DIR
scp digitalOcean/stop.sh $DO_USER@$DO_SRV01_DOMAIN:$DO_DEPLOYMENT_DIR
scp deploy/pollForStartup.sh $DO_USER@$DO_SRV01_DOMAIN:$DO_DEPLOYMENT_DIR
scp environment.sh $DO_USER@$DO_SRV01_DOMAIN:~/

echo "==== Stopping application - ${ENVIRON}01 ===="
ssh $DO_USER@$DO_SRV01_DOMAIN 'source ~/environment.sh && chmod +x $DO_DEPLOYMENT_DIR/stop.sh' 2>&1
ssh $DO_USER@$DO_SRV01_DOMAIN 'source ~/environment.sh && bash $DO_DEPLOYMENT_DIR/stop.sh' 2>&1
ssh $DO_USER@$DO_SRV01_DOMAIN 'source ~/environment.sh && chmod +x $DO_DEPLOYMENT_DIR/start.sh' 2>&1

echo "==== Starting application in ${ENVIRON}01 after deployment ===="
# We run the start script as sudo because Tomcat is bound to port 80 on the machine, a privileged port. This can only be done by root.
# We could have changed the port to another one and modify the load balancer configuration accordingly, but this is fine
# for now
ssh $DO_USER@$DO_SRV01_DOMAIN 'source ~/environment.sh && cd $DO_DEPLOYMENT_DIR && sudo nohup ./start.sh > start.log &' 2>&1

echo "= Waiting for startup in ${ENVIRON}01 ="
ssh $DO_USER@$DO_SRV01_DOMAIN 'source ~/environment.sh && chmod +x $DO_DEPLOYMENT_DIR/pollForStartup.sh && bash $DO_DEPLOYMENT_DIR/pollForStartup.sh' 2>&1

echo "==== Deploying to Digital Ocean - ${ENVIRON}02 ===="
scp graffitab.jar $DO_USER@$DO_SRV02_DOMAIN:$DO_DEPLOYMENT_DIR
scp digitalOcean/start.sh $DO_USER@$DO_SRV02_DOMAIN:$DO_DEPLOYMENT_DIR
scp digitalOcean/stop.sh $DO_USER@$DO_SRV02_DOMAIN:$DO_DEPLOYMENT_DIR
scp deploy/pollForStartup.sh $DO_USER@$DO_SRV02_DOMAIN:$DO_DEPLOYMENT_DIR
scp environment.sh $DO_USER@$DO_SRV02_DOMAIN:~/

echo "==== Stopping application - ${ENVIRON}02 ===="
ssh $DO_USER@$DO_SRV02_DOMAIN 'source ~/environment.sh && bash $DO_DEPLOYMENT_DIR/stop.sh' 2>&1
ssh $DO_USER@$DO_SRV02_DOMAIN 'source ~/environment.sh && chmod +x $DO_DEPLOYMENT_DIR/start.sh' 2>&1

echo "==== Starting application in ${ENVIRON}02 after deployment ===="
# We run the start script as sudo because Tomcat is bound to port 80 on the machine, a privileged port. This can only be done by root.
# We could have changed the port to another one and modify the load balancer configuration accordingly, but this is fine
# for now
ssh $DO_USER@$DO_SRV02_DOMAIN 'source ~/environment.sh && cd $DO_DEPLOYMENT_DIR && sudo nohup ./start.sh > start.log &' 2>&1

echo "= Waiting for startup in in ${ENVIRON}02 ="
ssh $DO_USER@$DO_SRV02_DOMAIN 'source ~/environment.sh && chmod +x $DO_DEPLOYMENT_DIR/pollForStartup.sh && bash $DO_DEPLOYMENT_DIR/pollForStartup.sh' 2>&1

echo "Application deployed and started successfully in both app servers: SUCCESS"