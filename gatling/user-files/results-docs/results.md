# Load tests results

## Registration (October 2016)

* Run 5 times
* Empty database
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

## Load App Home (October 2016)

* Run 5 times
* Only content in the DB are the users created by Registration load tests.
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

## 16th July 2017


## Registration

================================================================================
---- Global Information --------------------------------------------------------
> request count                                        150 (OK=150    KO=0     )
> min response time                                    167 (OK=167    KO=-     )
> max response time                                    584 (OK=584    KO=-     )
> mean response time                                   255 (OK=255    KO=-     )
> std deviation                                         93 (OK=93     KO=-     )
> response time 50th percentile                        204 (OK=204    KO=-     )
> response time 75th percentile                        330 (OK=330    KO=-     )
> response time 95th percentile                        428 (OK=428    KO=-     )
> response time 99th percentile                        548 (OK=548    KO=-     )
> mean requests/sec                                  4.839 (OK=4.839  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                           150 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================


## LoadAppHome

* Ran 5 times, took mean of 95th percentile:

361 432 543 675 1174 -> 543ms in 95th percentile

================================================================================
---- Global Information --------------------------------------------------------
> request count                                       1050 (OK=1050   KO=0     )
> min response time                                     22 (OK=22     KO=-     )
> max response time                                   1348 (OK=1348   KO=-     )
> mean response time                                   109 (OK=109    KO=-     )
> std deviation                                        181 (OK=181    KO=-     )
> response time 50th percentile                         43 (OK=43     KO=-     )
> response time 75th percentile                         68 (OK=68     KO=-     )
> response time 95th percentile                        543 (OK=543    KO=-     )
> response time 99th percentile                        909 (OK=909    KO=-     )
> mean requests/sec                                 31.818 (OK=31.818 KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                          1029 ( 98%)
> 800 ms < t < 1200 ms                                  20 (  2%)
> t > 1200 ms                                            1 (  0%)
> failed                                                 0 (  0%)
================================================================================

* Quickest:

================================================================================
---- Global Information --------------------------------------------------------
> request count                                       1050 (OK=1050   KO=0     )
> min response time                                     19 (OK=19     KO=-     )
> max response time                                   1208 (OK=1208   KO=-     )
> mean response time                                    86 (OK=86     KO=-     )
> std deviation                                        156 (OK=156    KO=-     )
> response time 50th percentile                         33 (OK=33     KO=-     )
> response time 75th percentile                         54 (OK=54     KO=-     )
> response time 95th percentile                        361 (OK=361    KO=-     )
> response time 99th percentile                        953 (OK=953    KO=-     )
> mean requests/sec                                 31.818 (OK=31.818 KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                          1032 ( 98%)
> 800 ms < t < 1200 ms                                  17 (  2%)
> t > 1200 ms                                            1 (  0%)
> failed                                                 0 (  0%)
================================================================================


## Cold start of JVM exammple

Server just restarted, no requests at all, timings for Registration:

================================================================================
---- Global Information --------------------------------------------------------
> request count                                        150 (OK=150    KO=0     )
> min response time                                    193 (OK=193    KO=-     )
> max response time                                   6327 (OK=6327   KO=-     )
> mean response time                                   945 (OK=945    KO=-     )
> std deviation                                       1199 (OK=1199   KO=-     )
> response time 50th percentile                        380 (OK=380    KO=-     )
> response time 75th percentile                       1083 (OK=1083   KO=-     )
> response time 95th percentile                       3229 (OK=3229   KO=-     )
> response time 99th percentile                       5663 (OK=5663   KO=-     )
> mean requests/sec                                  4.839 (OK=4.839  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                           101 ( 67%)
> 800 ms < t < 1200 ms                                  13 (  9%)
> t > 1200 ms                                           36 ( 24%)
> failed                                                 0 (  0%)
================================================================================


## 26th September 2017

### Registration

Ran 5 times, picked quickest (241ms 95th perc)

================================================================================
---- Global Information --------------------------------------------------------
> request count                                        150 (OK=150    KO=0     )
> min response time                                    170 (OK=170    KO=-     )
> max response time                                    409 (OK=409    KO=-     )
> mean response time                                   202 (OK=202    KO=-     )
> std deviation                                         31 (OK=31     KO=-     )
> response time 50th percentile                        195 (OK=195    KO=-     )
> response time 75th percentile                        208 (OK=208    KO=-     )
> response time 95th percentile                        241 (OK=241    KO=-     )
> response time 99th percentile                        357 (OK=357    KO=-     )
> mean requests/sec                                  4.839 (OK=4.839  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                           150 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================

## LoadAppHome

* Ran 5 times, took mean of 95th percentile:

 224 237 251 320 718 -> 251ms 95th


Quickest:

 ================================================================================
 ---- Global Information --------------------------------------------------------
 > request count                                       1050 (OK=1050   KO=0     )
 > min response time                                     19 (OK=19     KO=-     )
 > max response time                                   1479 (OK=1479   KO=-     )
 > mean response time                                    70 (OK=70     KO=-     )
 > std deviation                                        136 (OK=136    KO=-     )
 > response time 50th percentile                         31 (OK=31     KO=-     )
 > response time 75th percentile                         37 (OK=37     KO=-     )
 > response time 95th percentile                        224 (OK=224    KO=-     )
 > response time 99th percentile                        975 (OK=975    KO=-     )
 > mean requests/sec                                 31.818 (OK=31.818 KO=-     )
 ---- Response Time Distribution ------------------------------------------------
 > t < 800 ms                                          1038 ( 99%)
 > 800 ms < t < 1200 ms                                   8 (  1%)
 > t > 1200 ms                                            4 (  0%)
 > failed                                                 0 (  0%)
 ================================================================================
