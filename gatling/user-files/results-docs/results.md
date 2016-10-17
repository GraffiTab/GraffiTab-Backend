## Registration

* 5 req/s
* Ramp 150 over 30 seconds

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                        150 (OK=150    KO=0     )
> min response time                                    158 (OK=158    KO=-     )
> max response time                                    773 (OK=773    KO=-     )
> mean response time                                   184 (OK=184    KO=-     )
> std deviation                                         59 (OK=59     KO=-     )
> response time 50th percentile                        171 (OK=171    KO=-     )
> response time 75th percentile                        179 (OK=179    KO=-     )
> response time 95th percentile                        225 (OK=225    KO=-     )
> response time 99th percentile                        405 (OK=405    KO=-     )
> mean requests/sec                                      5 (OK=5      KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                           150 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================
```

## Load App Home

* rampUsers(150) over (30 seconds)

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                       1050 (OK=1050   KO=0     )
> min response time                                     31 (OK=31     KO=-     )
> max response time                                    746 (OK=746    KO=-     )
> mean response time                                   101 (OK=101    KO=-     )
> std deviation                                        135 (OK=135    KO=-     )
> response time 50th percentile                         51 (OK=51     KO=-     )
> response time 75th percentile                         65 (OK=65     KO=-     )
> response time 95th percentile                        466 (OK=466    KO=-     )
> response time 99th percentile                        628 (OK=628    KO=-     )
> mean requests/sec                                 31.818 (OK=31.818 KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                          1050 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================
```