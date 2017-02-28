# Provisioning of App server in  DigitalOcean

## Initial setup

* Source `environment.sh` script. This script is generated at deployment time from CircleCI environment variables. It can also
be upload manually from the version stored in Google Docs
```
source environment.sh
```

* Check values of some variables in the previous script are:
```
DEPLOY_USERNAME=graffitab
SHARED_DIRECTORY=/data/uploads
DO_DEPLOYMENT_DIR=/opt/graffitab
```

Add the graffitab user
[here](https://www.digitalocean.com/community/tutorials/how-to-create-a-sudo-user-on-ubuntu-quickstart) and
[here](https://www.digitalocean.com/community/tutorials/initial-server-setup-with-ubuntu-14-04).

We could have provided the password directly in a script, these commands will prompt for it, add it to `sudo` usergroup
and add the main deployment folder:
```
adduser $DEPLOY_USERNAME
usermod -aG sudo $DEPLOY_USERNAME
mkdir -p /opt/graffitab
sudo chown graffitab /opt/graffitab
```

To test the created user:
```
 su - $DEPLOY_USERNAME
```

* In order to allow passwordless ssh access as `graffitab` user between the servers we need to exchange ssh keys. First generate:
```
$ ssh-keygen
```

Accept all. Then create `authorized_keys` file inside `.ssh` directory in `/home/graffitab` of other servers and append there
the **public key** `id_rsa.pub`. Add to `autorized_keys` as many public keys as needed, then test from each source:
```
$ ssh graffitab@serverurl
```

Repeat for each server you want to ssh to:

* Create keys
* Create `authorized_keys` if not already. Append **public key** of the incoming servers.

* Add `graffitab` as a passwordless sudoer, so it can run `sudo` commands without password:
```
visudo -f /etc/sudoers.d/90-cloud-init-users
```

Append the following line:
```
graffitab ALL=(ALL) NOPASSWD:ALL
```

If the file does not exist, it can be created anyway through visudo. Also ensure that at the bottom of `/etc/sudoers` file the following line is
present:
```
...
#includedir /etc/sudoers.d
```

Now test a `sudo` command as graffitab, it shouldn't ask for a password:
```
$ su - graffitab
$ sudo apt-get update
```

* Make the SSHFS mount persistent. In order to do that, the file `/etc/fstab` needs to be aware of SSHFS. For example,
to mount `dev02` `/data/uploads` folder into `dev01` server `/data/uploads` mount point, add the following to `/etc/fstab`:
```
graffitab@dev02.graffitab.com:/data/uploads /data/uploads fuse.sshfs _netdev,user,idmap=user,follow_symlinks,identityfile=/home/graffitab/.ssh/id_rsa,allow_other,default_permissions,uid=1000,gid=1000 0 0
```

Where `uid` / `gid` are the result of the commands `id -u graffitab` / `id -g username` respectively.

Also, need to update FUSE configuration so that users other than root can access the mount. In `/etc/fuse.conf` change uncomment the line:
```
allow_other_user
```

Ensure passwordless  is in place for `graffitab` user. Test it by remounting:
```
$ sudo mount -a
```

Restart the server and check the folder is properly mounted.

* Update repositories:
```
 apt-get update
```

**Update/add SSH keys for this user in CircleCI and locally. Also change commands in `circle.yml` accordingly.**


## Install and configure SSHFS

See the tutorial [here](https://www.digitalocean.com/community/tutorials/how-to-use-sshfs-to-mount-remote-file-systems-over-ssh).

This requires the user `graffitab` to be able to ssh into the other droplet without password. Configure this _passwordless_
SSH first, see [here](https://www.digitalocean.com/community/tutorials/initial-server-setup-with-ubuntu-14-04). Then:
```
apt-get install sshfs
mkdir -p $SHARED_DIRECTORY
```

Ensure droplets can ssh each other by exchanging SSH keys. From one of the droplets run:
```
sudo sshfs -o allow_other,IdentityFile=/home/graffitab/.ssh/id_rsa graffitab@prd02.graffitab.com:/data/uploads /data/uploads
```

TODO: investigate how to make this permanent between restart fo the droplets

## Add swap space

Add between 2-4 GB of swap space for the Java processes.
See the tutorial [here](https://www.digitalocean.com/community/tutorials/how-to-add-swap-space-on-ubuntu-16-04) and
[here](https://www.digitalocean.com/community/tutorials/how-to-add-swap-on-ubuntu-14-04)

```
# check for swap space - it will be empty output
sudo swapon -s

# Create 4G swapfile
sudo fallocate -l 4G /swapfile
ls -lh /swapfile

# Give permissions (root rw)
sudo chmod 600 /swapfile

# Set it up as swap space
sudo mkswap /swapfile

# Enable it
sudo swapon /swapfile

# Check it is active
sudo swapon -s
```

Important: make this persistent so it survives server restarts.

We have our swap file enabled, but when we reboot, the server will not automatically enable the file. We can change that though by modifying the fstab file.
Edit the file with root privileges in your text editor:
```
sudo nano /etc/fstab
```

At the bottom of the file, you need to add a line that will tell the operating system to automatically use the file you created:
```
/swapfile   none    swap    sw    0   0
```
Save and close the file when you are finished. The next time you restart the server the swap file should be mounted.

## Install Java and NodeJS

TODO: Add here the link to DO tutorial

Java:
```
add-apt-repository ppa:webupd8team/java
apt-get update
apt-get install oracle-java8-installer
java -version
```

NodeJS [here](https://www.digitalocean.com/community/tutorials/how-to-set-up-a-node-js-application-for-production-on-ubuntu-14-04):
```
sudo apt-get update
sudo apt-get install git
cd ~
wget https://nodejs.org/dist/v6.2.1/node-v6.2.1-linux-x64.tar.xz
mkdir node
tar xvf node-v*.tar.?z --strip-components=1 -C ./node
cd ~
rm -rf node-v*
mkdir node/etc
echo 'prefix=/usr/local' > node/etc/npmrc
sudo mv node /opt/
sudo chown -R root: /opt/node
sudo ln -s /opt/node/bin/node /usr/local/bin/node
sudo ln -s /opt/node/bin/npm /usr/local/bin/npm
```

## Add firewall rules

TODO:

- Install iptables
- Create rules that lock any direct connection from public IPs but SSH
- Allow only the load balancer connections on the private IP
