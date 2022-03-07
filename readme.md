# ss2

Second service stack example. This project is similar to the 
AWS Well-Architected Labs [Fault Isolation with Shuffle Sharing](https://www.wellarchitectedlabs.com/reliability/300_labs/300_fault_isolation_with_shuffle_sharding/)
lab with some notable differences:

* The system set up uses the CDK
* The service is containerized and deployed using ECS on Fargate

Note this instance provides 4 shards, accessible via the URIs

* /hello?tenant=alpha
* /hello?tenant=bravo
* /hello?tenant=charlie
* /hello?tenant=delta
