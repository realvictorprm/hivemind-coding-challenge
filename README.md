# Hivemind Coding Challenge example solution

This solution uses the following technologies to solve the coding challenge from [Hivemind](https://github.com/HivemindTechnologies/scala-coding-challenge):

- Scala 2.13
- Zio Core, Zio Streaming, Zio .. to handle the application logic in a nice FP way
- ElasticSearch for the data management and querying (including an elastic4s to handle it in Scala)
- Kubernetes to mimic more realism
- docker combined with minikube for local testing on a k8s cluster

## How to run the integration tests?

Before please make sure that you have installed:

- docker
- kubernetes cli
- minikube
- sbt
- python 3.10

If you meet all those requirements then in a console do:

1. start a kubernetes cluster via `minikube start`
1. run `py pre-it-test.py` and give the elasticsearch a couple minutes to start properly
1. run `py it-test.py`


The `it-test.py` script will not stop even in case of error till you exit it manually. That gives you the opportunity to make use of the portforwarding to make calls on `localhost:8080`.

Besides that you can adjust the data given to the webserver by editing or replacing the file in `./test-data/sampleData.json`

You can also just provide a different file in the folder `test-data` and edit the env var `INGEST_FILE_URL` in the `webserver.yaml` file.

In any case, if you want to change the data you have to restart the webserver and that is best done via calling `py restart-webserver.py`.
