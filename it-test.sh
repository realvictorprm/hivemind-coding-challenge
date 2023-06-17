# Deploy the elasticsearch
kubectl apply -f elasticsearch.yaml

# Deploy the volume for the webserver
kubectl apply -f volume.yaml

# Synchronize docker context for upload
eval $(minikube docker-env)

# Build and locally publish the webserver docker image
sbt docker:publishLocal

# Deploy the webserver
kubectl apply -f webserver.yaml

# open port forwarding
start "port-forwarding" py portforward.py
start "integration-test" timeout 5 & sbt it:test
