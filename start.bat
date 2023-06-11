REM Deploy the elasticsearch
kubectl apply -f elasticsearch.yaml

REM Synchronize docker context for upload
& minikube -p minikube docker-env --shell powershell | Invoke-Expression

REM Build and locally publish the webserver docker image
sbt docker:publishLocal

REM Deploy the webserver
kubectl apply -f webserver.yaml
