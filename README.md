# Hivemind Coding Challenge example solution

This solution uses the following technologies to solve the coding challenge from (Hivemind)[]:

- Scala 3 because "Why not?(TM)"
- Zio Core, Zio Streaming, Zio .. to handle the application logic in a nice FP way
- ElasticSearch for the data management and querying (including an elastic4s to handle it in Scala)
- Kubernetes to mimic more realism
- docker combined with minikube for local testing on a k8s cluster

## How to start the webserver?

Before please make sure that you have installed:

- docker
- kubernetes cli
- minikube
- sbt
- python 3.10

If you meet all those requirements then do in the

#### first console with admin rights:

1. start a kubernetes cluster via `minikube start`
2. mount the folder with the test-data via `minikube mount ./test-data:/test-data`

#### second console without admin rights:

1. run.sh / run.bat
2. run `py portforward.py`

After those steps the minikube cluster is started, the elasticsearch and the webserver are deployed to the cluster and
the webserver is reachable on `localhost:8080` thanks to portforwarding.

Now you can make requests to the webserver via e.g. Postman using this url: `localhost:8080/amazon/best-reviews`

