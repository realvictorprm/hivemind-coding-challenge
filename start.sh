# Deploy the elasticsearch
kubectl apply -f elasticsearch.yaml

# Synchronize docker context for upload
eval $(minikube docker-env)

# Build and locally publish the webserver docker image
sbt docker:publishLocal

# Deploy the webserver
kubectl apply -f webserver.yaml
