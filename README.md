# Change Data Capture with Embedded Debezium Demo

## CDC with Debezium

The usual setup is to use Debezium with Kafka which has connectors for various databases, search engines.
This particular example is to demonstrate how to use embedded Debezium engine to build a Spring Boot app to capture changes from a postgres database and index them into ElasticSearch without using Kafka.

Without using Kafka, we can’t use the elasticsearch connector for kafka, so we’ll have to do a bit coding to insert the data into the db.  
The Debezium engine is embedded into Spring Boot, but it still uses Kafka’s storage module to store the offsets. 
(I have also configured to store the history with Kafka's module, but it doesn't work)  
But instead of storing the offsets in Kafka, it’s stored in a local file.  
It would be difficult to scale but if we have enough traffic, we should probably go for the kafka route.  anyways, this little code demonstrate a small server monitoring changes in the database and send all changes to elasticsearch.

Unless you need the data to be inserted into the db and be searchable immediately, i think it’s better to have a separate process detached from your main application to do the indexing so you don’t have to figure out how to index missing entries in the search engine or to rollback a db insert just because search engine is not available.  also, the structure for elasticsearch can be very dynamic.

## This demo copied some code from these web pages.

https://cdmana.com/2021/05/20210501032947779a.html
https://medium.com/swlh/change-data-capture-cdc-with-embedded-debezium-and-springboot-6f10cd33d8ec
https://levelup.gitconnected.com/creating-and-filling-a-postgres-db-with-docker-compose-e1607f6f882f

## resilience4j

I’ve tried out the retry module of resilience4j in the POC.  
With spring boot, it’s really easy to use, just some configuration in the application.yml and an annotation in the code.  
Looks to be a very nice library to use when implementing patterns like circuit breaker, bulkhead, retry, rate/timelimit.

It's much easier to use than using hystrix, but on the other hands, hystrix allows you to have access to the stream directly, so we can aggregate updates and make one call to the external system, rather than one for each change.

https://reflectoring.io/retry-with-springboot-resilience4j/

