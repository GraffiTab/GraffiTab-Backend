# Load balancer server provisioning for DigitalOcean

## Install and configure HAProxy

Check tutorial for newer version 1.6
[here](https://www.digitalocean.com/community/tutorials/how-to-implement-ssl-termination-with-haproxy-on-ubuntu-14-04)

The main steps are:

* Add the repository URL
```
sudo add-apt-repository ppa:vbernat/haproxy-1.6
```

* Update sources
```
sudo apt-get update
```

* Install HAProxy
```
apt-get install haproxy
```

### Configuration file

```
global
        # log /dev/log    local0
        # log /dev/log    local1 notice
        log 127.0.0.1 local2
        daemon
        maxconn 2048
        # chroot /var/lib/haproxy
        # stats socket /run/haproxy/admin.sock mode 660 level admin
        # stats timeout 30s
        user haproxy
        group haproxy
        tune.ssl.default-dh-param 2048
        #daemon

        # Default SSL material locations
        #ca-base /etc/ssl/certs
        #crt-base /etc/ssl/private

        # Default ciphers to use on SSL-enabled listening sockets.
        # For more information, see ciphers(1SSL).
        # ssl-default-bind-ciphers kEECDH+aRSA+AES:kRSA+AES:+AES256:RC4-SHA:!kEDH:!LOW:!EXP:!MD5:!aNULL:!eNULL

defaults
        log     global
        mode    http
        option  forwardfor
        option  http-server-close
        option  httplog
        option  dontlognull
        retries 3
        timeout connect 5000
        timeout client  50000
        timeout server  50000
        errorfile 400 /etc/haproxy/errors/400.http
        errorfile 403 /etc/haproxy/errors/403.http
        errorfile 408 /etc/haproxy/errors/408.http
        errorfile 500 /etc/haproxy/errors/500.http
        errorfile 502 /etc/haproxy/errors/502.http
        errorfile 503 /etc/haproxy/errors/503.http
        errorfile 504 /etc/haproxy/errors/504.http

frontend graffitabdev-http
    bind *:80
    reqadd X-Forwarded-Proto:\ http
    default_backend devnodes

frontend graffitabdev-https
    bind *:443 ssl crt /etc/ssl/private/dev.graffitab.com.pem
    acl p_ext_robots path_end -i robots.txt
    acl p_deep_link path_end -i apple-app-site-association
    http-request set-path /public/%[path] if p_deep_link or p_ext_robots
    reqadd X-Forwarded-Proto:\ https
    default_backend devnodes

backend devnodes
    redirect scheme https if !{ ssl_fc }
    balance roundrobin
    option forwardfor
    option  http-server-close
    #http-request set-header X-Forwarded-Port %[dst_port]
    #http-request add-header X-Forwarded-Proto https if { ssl_fc }
    #option httpchk get /status
    #http-check expect status 200
    option httpchk GET /status
    http-check expect rstring .*
    server $SERV01 $SERV01_IP:80 check inter 30000
    server $SERV02 $SERV02_IP:80 check inter 30000

listen stats
    bind *:1936
    mode http
    stats enable
    stats uri /
    stats realm Haproxy\ Statistics
    stats auth $HAPROXY_USER:$HAPROXY_PASSWORD
```

### Start up

To start the com.graffitab.server.service:
```
com.graffitab.server.service haproxy start
```

To view the logs:
```
tail -F /var/log/haproxy.log
```

## Configure HTTPS

Tutorial [here](https://www.digitalocean.com/community/tutorials/how-to-implement-ssl-termination-with-haproxy-on-ubuntu-14-04)

* Upgrade HAProxy to 1.6
* Generate CSR on the LB server

```
$ mkdir /etc/ssl/dev.graffitab.com
$ openssl req -nodes -newkey rsa:2048 -keyout graffitab.com.key -out graffitab.com.csr
```

* Check the CSR is correct [here](https://decoder.link/result/?stored=c99c8254651dfe03754e1372ff154db7)

* Use the CSR key to get a CRT file from the certification provider

* Get the all the `.crt` files and upload them to the server

* Put together `.key` and `.crt` as a `.pem` file alongside the intermediate certificates in the zip bundle
```
# The order of these commands matters
$ cat COMODORSADomainValidationSecureServerCA.crt COMODORSAAddTrustCA.crt AddTrustExternalCARoot.crt > dev_graffitab_com.ca-bundle
$ cat dev.graffitab.com.key >> dev.graffitab.com.pem
$ cat dev_graffitab_com.crt >> dev.graffitab.com.pem
$ cat dev_graffitab_com.ca-bundle >> dev.graffitab.com.pem
$ cp dev.graffitab.com.pem /etc/ssl/private

# Restart HAproxy
$ sudo com.graffitab.server.service haproxy restart
```

* Store all the `.pem`,`.crt`,`.key` in a safe place

* Configure HAProxy using the configuration file outlined above, substituting the placeholders with the right values

## Troubleshooting

### Logging

Logging to `/var/log/haproxy.log` might not work first time. Check the version of `rsyslog` is at least `7.4.4` and
the configuration files under `/etc/rsyslog.conf` and `/etc/rsyslog.d/49-haproxy.conf` look like the provided ones.
Restart both `haproxy` and `rsyslog` services for the changes to take effect.

Logs for HAProxy should be in `/var/log/haproxy.log`

## Add swap

See section in `app-server.md`.

