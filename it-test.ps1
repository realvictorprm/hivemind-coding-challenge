# Deploy the elasticsearch
kubectl apply -f elasticsearch.yaml

# Synchronize docker context for upload
& minikube -p minikube docker-env --shell powershell | Invoke-Expression

# Build and locally publish the webserver docker image
sbt docker:publishLocal

# Deploy the webserver
kubectl apply -f webserver.yaml
kubectl scale deployment webserver-deployment --replicas=0
$testDataPath = Join-Path $PSScriptRoot 'test-data'
$testDataPath = $testDataPath + ":/test-data"
$job = Start-Job -ScriptBlock { minikube mount $using:testDataPath }
Receive-Job -Job $job
kubectl scale deployment webserver-deployment --replicas=1
# minikube mount 'test-data:/test-data'
timeout 10

# open port forwarding & execute the integration test in parallel
$portforward = Join-Path $PSScriptRoot 'portforward.py'
$job = Start-Job -ScriptBlock { py $using:portforward }
timeout 1
sbt it:test

kubectl delete deployment webserver-deployment
