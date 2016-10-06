ENVIRON=$DEV_ENV_NAME
DO_MYSQL_DB_HOST=$DO_DEV_MYSQL_HOST
AMAZON_S3_BUCKET_NAME=$AMAZON_S3_DEV_BUCKET_NAME

if [ $CIRCLE_BRANCH == $PRD_BRANCH_NAME ]
then
  ENVIRON=$PRD_ENV_NAME
  DO_MYSQL_DB_HOST=$DO_PRD_MYSQL_HOST
  AMAZON_S3_BUCKET_NAME=$AMAZON_S3_PRD_BUCKET_NAME
else
  ENVIRON=$DEV_ENV_NAME
  DO_MYSQL_DB_HOST=$DO_DEV_MYSQL_HOST
  AMAZON_S3_BUCKET_NAME=$AMAZON_S3_DEV_BUCKET_NAME
fi

if [[ $CIRCLE_BRANCH == *"-test"* ]]
then
  DO_MYSQL_DB_NAME=$DO_MYSQL_DB_NAME"_test"
fi

ENVNAME=digitalOcean
echo "Building dependencies for $ENVIRON environment: $ENVNAME"
echo "Database host is $DO_MYSQL_DB_HOST"
echo "Database name is $DO_MYSQL_DB_NAME"
echo "Amazon S3 bucket name is $AMAZON_S3_BUCKET_NAME"
