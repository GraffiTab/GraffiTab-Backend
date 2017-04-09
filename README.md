<img src="https://drive.google.com/uc?export=download&id=0B8cKnJyOSiKfQkw5c0dWUVppaUU" width="1500">

The GraffiTab app lets you create and share drawings with your audience. Follow your favourite artists, be creative and build your profile.

This is the Backend app for the system. Work is still in progress.


## Run using Docker

This has been tested using Docker for Mac.

* Install Docker for Mac [here](https://download.docker.com/mac/stable/Docker.dmg)
* Once Docker is running, check versions of Docker and Docker Compose
```
$ docker -v
$ docker-compose -v
```

* So far, MySQL is still necessary locally. So install MySQL locally on Mac and import a database structure in it. The database
should be called `graffitab`. Make note of your local IP address (not localhost) as it will be needed in the following step.
* Create a file in `conf` directory called `application.properties` with the following content
```
server.port=8091
security.basic.enabled=false
server.tomcat.accesslog.enabled=true
server.tomcat.accesslog.directory=logs
spring.profiles.active=main
server.session.cookie.name=JSESSIONID
filesystem.tempDir=/tmp
spring.mvc.static-path-pattern=/public/**

health.status.show=false

# Timeout for inactive sessions, 6 months in seconds
server.session.timeout=15552000
session.backups.enabled=false

# Local config
# localhost not valid with Docker
db.host=<YOUR LOCAL IP>
db.port=<LOCAL DB PORT>
db.name=graffitab

db.jdbcUrl=jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&amp;characterEncoding=UTF-8
db.username=root
db.password=

redis.host=redis
redis.port=6379
redis.password=<REDIS_PASSWORD>

# Prefix that gets prepended to view names when building a URL
spring.thymeleaf.prefix=classpath:/templates/
spring.mvc.favicon.enabled=false
#spring.thymeleaf.view-names=

# i18n
spring.messages.basename=i18n/messages
i18n.supportedLanguages=en,es,bg

# Locally we don't have HTTPS, so allow HTTP
protocol.httpsOnlyAllowed=false

# Push sender notifications
pn.apns.env=prod

# Amazon S3 bucket name
s3.bucketName=graffitab-eu1
registration.immediateActivation=true
spring.devtools.remote.secret=graffitabfor
```

* Create or copy file `environment.sh` containing AWS keys, Sendgrid, etc. in the root folder of the project

* Build the server Docker image:
```
./build-docker.sh
```

* Build Redis image:
```
./build-redis.sh
```

* Run with bash script:
```
$ ./runDocker <REDIS_PASSWORD>
```

* Run from Gradle:
```
$ ./gradlew runDocker -PredisPassword=<REDIS_PASSWORD>
```

TODO
* Remove Spring Boot devtools for production builds [here](http://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-devtools.html#using-boot-devtools)
* Add debugging configuration [here](https://medium.com/@lhartikk/development-environment-in-spring-boot-with-docker-734ad6c50b34)

## Troubleshoot

Check with app holds port open:
```
$ lsof -i tcp:portNumber
or
$ netstat -vanp tcp | grep 3000
```

## License

Copyright 2016 GraffiTab

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
