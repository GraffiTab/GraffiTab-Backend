# GraffiTab authentication

## Login endpoint - returns cookie for created session

* Provide username and password. Session cookie is returned in the Set-Cookie header

```
curl -i -H "Content-Type: application/json" -X POST -d '{"username":"juan","password":"password"}' http://localhost:8091/v1/login
```

* Protected endpoint: provide cookie returned by login endpoint to authenticate. Without the cookie should give 401 Unauthorized


```
curl -i -H "Content-Type: application/json" --cookie "JSESSIONID=C7742099BBC578F3841F7E5C5552D5FF" http://localhost:8091/v1/users/me
```

```
curl -i -H "Content-Type: application/json" -X PUT -d '{ "currentPassword": "password", "newPassword": "password1" }' --cookie "GRAFFITABSESSIONID=1E27F1CE519DF50938E862FE4D32DA40" http://localhost:8091/v1/users/me/changepassword

```

## External provider authentication - return cookie for created session

 * Provide the externalId, accessToken??, externalProvider type and, if found, the underlying user is logged in

```
curl -i -H "Content-Type: application/json" -X POST -d '{ "externalProvider":{ "externalId":"123456789", "accessToken":"hsadoaoi98a7sdiausdi", "externalProviderType":"TWITTER"}}' http://localhost:8091/v1/externalproviders/login
```

## Basic Authentication

```
curl -i -u username:password http://localhost:8090/v1/users
```

## Logout - always provide the session cookie to log out from

```
curl -i --cookie "JSESSIONID=E9DFBB37EC490A08BAA19295360F1EFA" http://localhost:8091/v1/logout
```

## More public endpoints

* Registration
```
curl -i -H "Content-Type: application/json" -X POST -d '{"user":{
        "externalId": "abc",
        "username": "john",
        "firstName": "John",
        "lastName": "Smith",
        "password": "jsmithpass",
        "email": "jsmith@gmail.com"
    }
}' http://localhost:8091/v1/users
```


## Upload cover

curl -X POST -H "Content-Type: multipart/form-data; boundary=----WebYWxkTrZu0gW" -H "Cache-Control: no-cache" -F "file=@graffiti.jpg" "http://localhost:8091/v1/users/me/avatar?username=david&password=password1"
curl -X POST -H "Content-Type: multipart/form-data; boundary=----WebYWxkTrZu0gW" --cookie "JSESSIONID=7af0af3a-2abf-4957-84b2-a765229e51a7" -F "file=@graffiti.jpg" "http://localhost:8091/v1/users/me/avatar"

## Create / update streamable
curl -X POST -H "Content-Type: multipart/form-data; boundary=----WebYWxkTrZu0gW" -H "Authorization: Basic ZGF2aWQ6cGFzc3dvcmQx" -H "Cache-Control: no-cache" -H "Postman-Token: f933fed5-b610-14f7-b233-274eb158c909" -F 'properties={"latitude":55.123, "longitude":3.123456, "roll":12.3, "yaw":13.4, "pitch":1.234};type=application/json' -F "file=@graffiti.jpg" "http://localhost:8091/v1/users/me/streamables/graffiti"
curl -X POST -H "Content-Type: multipart/form-data; boundary=----WebYWxkTrZu0gW" --cookie "JSESSIONID=f4cf5ce5-6462-470f-afef-756e4e2b7767" -H "Cache-Control: no-cache" -F 'properties={"latitude":55.123, "longitude":3.123456, "roll":12.3, "yaw":13.4, "pitch":1.234};type=application/json' -F "file=@graffiti.jpg" "http://localhost:8091/v1/users/me/streamables/graffiti"
curl -X POST -H "Content-Type: multipart/form-data; boundary=----WebYWxkTrZu0gW" --cookie "JSESSIONID=a89067f7-5d99-43e5-b161-1ab1aba11eb3" -H "Cache-Control: no-cache" -F 'properties={"latitude":55.123, "longitude":3.123456, "roll":12.3, "yaw":13.4, "pitch":1.234, "text": "Initial text for @georgi"};type=application/json' -F "file=@graffiti.jpg" "https://dev.graffitab.com/v1/users/me/streamables/graffiti"


curl -X POST --cookie "JSESSIONID=f4cf5ce5-6462-470f-afef-756e4e2b7767"  -H "Content-Type: application/json" -d '{"latitude":55.123, "longitude":3.123456, "roll":12.3, "yaw":13.4, "pitch":1.234}' http://localhost:8091/v1/users/me/streamables/graffiti/import


## Asset polling

curl -X GET --cookie "JSESSIONID=7af0af3a-2abf-4957-84b2-a765229e51a7" "http://localhost:8091/v1/assets/o3wEI4EhEEUnX/progress"
