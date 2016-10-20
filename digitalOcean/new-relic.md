# New Relic configuration

New Relic is a all-in-one monitoring tool which gives a lot of informaticon about the overall performance of the app.

The New Relic website explains clearly how to configure a Java application with it. Here we will only go through the
specific steps to get it going for GraffiTab.


New Relic java agent will need to be installed in both app servers. Once got the zip bundle from New Relic (`newrelic-java-3.32.0.zip`),
we'd need to do the following (check `https://docs.newrelic.com/docs/agents/java-agent/installation/java-agent-manual-installation` for more
details)

```
$ scp newrelic-java-3.32.0.zip user@server
$ ssh server
$ sudo apt-get install unzip
$ unzip newrelic-java-3.32.0.zip -d /opt/graffitab
```

Then:

* Edit `/opt/graffitab/newrelic.yml` and set the `application_name` to a meaningul name
* Then add `-javaagent:/opt/graffitab/newrelic/newrelic.jar` to the `java` startup command in `start.sh`
* Restart the app `stop.sh` followed by `source ~/environment.sh && cd $DO_DEPLOYMENT_DIR && sudo nohup ./start.sh > start.log &`
* Tail the logs to ensure everythin is OK. If tomcat does not start up, try running the java command directly in the terminal in
order to troubleshoot it.
* Check the New Relic agent logs in `newrelic/logs` folder. After few seconds a new application should appear in the New Relic
interface

