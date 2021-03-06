ENVIRON=$DEV_ENV_NAME
DO_MYSQL_DB_HOST=$DO_DEV_MYSQL_HOST
AMAZON_S3_BUCKET_NAME=$AMAZON_S3_DEV_BUCKET_NAME

if [ $CIRCLE_BRANCH == $PRD_BRANCH_NAME ]
then
  export ENVIRON=$PRD_ENV_NAME
  export DO_MYSQL_DB_HOST=$DO_PRD_MYSQL_HOST
  export DO_MYSQL_EXTERNAL_DB_HOST=$DO_PRD_EXTERNAL_MYSQL_HOST
  export AMAZON_S3_BUCKET_NAME=$AMAZON_S3_PRD_BUCKET_NAME
else
  export ENVIRON=$DEV_ENV_NAME
  export DO_MYSQL_DB_HOST=$DO_DEV_MYSQL_HOST
  export DO_MYSQL_EXTERNAL_DB_HOST=$DO_DEV_EXTERNAL_MYSQL_HOST
  export AMAZON_S3_BUCKET_NAME=$AMAZON_S3_DEV_BUCKET_NAME
  export DO_USER=$DO_DEV_USER
  export DO_DEPLOYMENT_DIR=$DO_DEV_DEPLOYMENT_DIR
fi

if [[ $CIRCLE_BRANCH == *"-test"* ]]
then
  export DO_MYSQL_DB_NAME=$DO_MYSQL_DB_NAME"_test"
fi

export ENVNAME=digitalOcean
echo "Building dependencies for $ENVIRON environment: $ENVNAME"
echo "External database host is $DO_MYSQL_EXTERNAL_DB_HOST"
echo "Database host is $DO_MYSQL_DB_HOST"
echo "Database name is $DO_MYSQL_DB_NAME"
echo "Amazon S3 bucket name is $AMAZON_S3_BUCKET_NAME"
